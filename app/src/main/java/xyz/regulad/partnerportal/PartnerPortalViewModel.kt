package xyz.regulad.partnerportal

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import org.webrtc.PeerConnection.SdpSemantics
import xyz.regulad.blueheaven.util.sha1Hash
import xyz.regulad.partnerportal.navigation.ErrorRoute
import xyz.regulad.partnerportal.navigation.LoadingRoute
import xyz.regulad.partnerportal.util.navigateOneWay
import xyz.regulad.partnerportal.util.showToast

const val startingConnectionValue = "Connecting to partner..."
const val finishedConnectionValue = "Connected to partner!"

class PartnerPortalViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "PartnerPortalViewModel"
    }

    val preferences = UserPreferencesRepository(application)
    lateinit var navController: NavController

    private val _connectingStatus = MutableStateFlow(startingConnectionValue)
    val connectingStatus: StateFlow<String> = _connectingStatus.asStateFlow()

    private fun updateConnectingStatus(newStatus: String) {
        Log.d(TAG, "Updating connecting status to: $newStatus")
        _connectingStatus.value = newStatus
    }

    private fun getSupabase(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = preferences.supabaseUrl,
            supabaseKey = preferences.supabaseAnonKey
        ) {
            install(Postgrest)
            install(Realtime)
        }

    private fun showError(exception: java.lang.Exception) {
        exception.printStackTrace()
        navController.navigateOneWay(ErrorRoute(exception.message ?: "Unknown error"))
    }

    // WebRTC stuff
    private val eglBase = EglBase.create()
    private val peerConnectionFactory = let {
        val options = PeerConnectionFactory.InitializationOptions.builder(this.getApplication())
            .setEnableInternalTracer(true)
            .setInjectableLogger({ message, severity, tag ->
                Log.println(
                    when (severity) {
                        Logging.Severity.LS_VERBOSE -> Log.VERBOSE
                        Logging.Severity.LS_INFO -> Log.INFO
                        Logging.Severity.LS_WARNING -> Log.WARN
                        Logging.Severity.LS_ERROR -> Log.ERROR
                        Logging.Severity.LS_NONE -> Log.ASSERT
                        else -> Log.DEBUG
                    },
                    tag,
                    message
                )
            }, Logging.Severity.LS_INFO)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    // all the ice servers declared in stuns
    private val iceServers = let {
        this.getApplication<Application>().assets.open("stuns").bufferedReader(charset = Charsets.UTF_8).use { reader ->
            reader.readLines().filter { line ->
                !line.startsWith("#") && line.isNotEmpty()
            }.map { line ->
                PeerConnection.IceServer.builder("stun:$line").createIceServer()
            }
        }
    }
    private val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = SdpSemantics.UNIFIED_PLAN
    }

    // video stuff
    private var videoSource: VideoSource? = null
    private fun createVideoTrack(): VideoTrack {
        val source = videoSource ?: createVideoSource().also { videoSource = it }
        return peerConnectionFactory.createVideoTrack("video_track", source)
    }

    private var videoCapturer: VideoCapturer? = null
    private fun createVideoSource(): VideoSource {
        val videoCapturer = videoCapturer ?: createCameraCapturer().also { videoCapturer = it }
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        videoCapturer.initialize(surfaceTextureHelper, this.getApplication(), videoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30) // Adjust resolution and FPS as needed
        return videoSource
    }

    private fun createCameraCapturer(): VideoCapturer {
        return Camera2Capturer(this.getApplication(), getCameraId(true), null)
    }

    private fun getCameraId(isFrontFacing: Boolean): String {
        val cameraManager = this.getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if ((isFrontFacing && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                (!isFrontFacing && facing == CameraCharacteristics.LENS_FACING_BACK)
            ) {
                return cameraId
            }
        }
        throw IllegalStateException("No suitable camera found")
    }

    private var audioSource: AudioSource? = null
    private fun createAudioTrack(): AudioTrack? {
        // TODO: ident a way to always use the frontMic if we can

        val constraints = MediaConstraints().apply {
            optional.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
            optional.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
            optional.add(MediaConstraints.KeyValuePair("autoGainControl", "true"))
        }

        val audioSource = audioSource ?: peerConnectionFactory.createAudioSource(constraints).also { audioSource = it }
        return peerConnectionFactory.createAudioTrack("audio_track_front", audioSource)
    }

    // local streams
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null

    // media handling
    fun cleanupMedia() {
        try {
            videoTrack?.dispose()
        } catch (e: Exception) {
            // ignore
        }
        videoTrack = null
        try {
            videoSource?.dispose()
        } catch (e: Exception) {
            // ignore
        }
        videoSource = null
        try {
            videoCapturer?.dispose()
        } catch (e: Exception) {
            // ignore
        }
        videoCapturer = null

        try {
            audioTrack?.dispose()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
        try {
            audioSource?.dispose()
        } catch (e: Exception) {
            // ignore
        }
        audioSource = null
    }

    // webrtc peer connection
    private suspend fun doConnection() {
        try {
            updateConnectingStatus(startingConnectionValue)
            navController.navigateOneWay(LoadingRoute)

            updateConnectingStatus("Starting Supabase connection...")

            val supabaseClient = try {
                getSupabase()
            } catch (e: Exception) {
                showError(e)
                null
            } ?: return

            val roomCode = preferences.roomCode

            if (roomCode.isEmpty()) {
                throw Exception("Room code is empty!")
            }

            val roomHashBase64 = Base64.encode(roomCode.toByteArray().sha1Hash(), Base64.DEFAULT).decodeToString()
            val signalingChannel = supabaseClient.realtime.channel("room:$roomHashBase64")

            updateConnectingStatus("Initializing audio & video...")

            val attemptedVideoTrack = videoTrack ?: createVideoTrack().also { videoTrack = it }
            val attemptedAudioTrack = audioTrack ?: createAudioTrack().also { audioTrack = it }

            val stream = peerConnectionFactory.createLocalMediaStream("local_media_stream")

            stream.addTrack(attemptedVideoTrack)

            if (attemptedAudioTrack != null) {
                stream.addTrack(attemptedAudioTrack)
            } else {
                Log.w(TAG, "Failed to create audio track")
                getApplication<Application>().showToast("Couldn't start the microphone. Continuing without audio...")
            }

            updateConnectingStatus("Initializing WebRTC...")

            val observer = object : PeerConnection.Observer {
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                }

                override fun onIceCandidate(p0: IceCandidate?) {
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                }

                override fun onAddStream(p0: MediaStream?) {
                }

                override fun onRemoveStream(p0: MediaStream?) {
                }

                override fun onDataChannel(p0: DataChannel?) {
                }

                override fun onRenegotiationNeeded() {
                }
            }

            val peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                observer
            )

            if (peerConnection == null) {
                showError(Exception("Failed to create peer connection!"))
                return
            }

            peerConnection.addTrack(attemptedVideoTrack)
            attemptedAudioTrack?.let { peerConnection.addTrack(it) }

            // all the prep work is now done. we can now do actual signaling
            // 1. both clients create a user id (hope it doesn't collide)
            // 2. both clients issue "advertisement" messages in the room
            // 3. both clients listen for "advertisement" messages in the room, waiting until they hear one that isn't their own (store the other id and attach it to all messages)
            // 4. the client with the lower id sends an offer to the client with the higher id
            // 5. the client with the higher id sends an answer to the client with the lower id, after it receives the offer
            // 6. both clients exchange ice candidates
            // 7. both clients are connected
            // 8. navigate to the connected screen and start the video

            updateConnectingStatus(finishedConnectionValue)
        } catch (e: Exception) {
            showError(e)
        } finally {
            cleanupMedia()
        }
    }

    private var connectionJob: Job? = null

    fun startConnection() {
        synchronized(this) {
            if (connectionJob?.isActive == true) {
                return
            }
            connectionJob = viewModelScope.launch {
                doConnection()
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel()
        eglBase.release()
        peerConnectionFactory.dispose()
        cleanupMedia()
    }
}
