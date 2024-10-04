package xyz.regulad.partnerportal

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.toRoute
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.webrtc.*
import org.webrtc.CameraVideoCapturer.CameraEventsHandler
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.SdpSemantics
import xyz.regulad.blueheaven.util.sha1Hash
import xyz.regulad.partnerportal.navigation.ErrorRoute
import xyz.regulad.partnerportal.navigation.LoadingRoute
import xyz.regulad.partnerportal.navigation.StartupRoute
import xyz.regulad.partnerportal.navigation.StreamRoute
import xyz.regulad.partnerportal.util.navigateOneWay
import xyz.regulad.partnerportal.util.showToast
import kotlin.coroutines.resume

const val startingConnectionValue = "Connecting to partner..."
const val finishedConnectionValue = "Connected to partner!"

@OptIn(DelicateCoroutinesApi::class)
suspend fun <T> Flow<T>.collectFirst(filter: suspend (T) -> Boolean): T {
    return coroutineScope {
        val channel = Channel<T>(1)
        val job = launch {
            collect {
                if (channel.isClosedForSend) {
                    return@collect
                }
                try {
                    if (filter(it)) {
                        channel.send(it)
                        channel.close()
                    }
                } catch (e: Exception) {
                    channel.close(e)
                }
            }
        }
        val result = try {
            channel.receive()
        } finally {
            job.cancel()
        }
        result
    }
}

suspend fun PeerConnection.setLocalDescriptionAsync(description: SessionDescription) {
    suspendCancellableCoroutine<Unit> { continuation ->
        setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onSetSuccess() {
                continuation.resume(Unit)
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
                continuation.cancel(Exception("Failed to set local description: $p0"))
            }
        }, description)
    }
}

suspend fun PeerConnection.setRemoteDescriptionAsync(description: SessionDescription) {
    suspendCancellableCoroutine<Unit> { continuation ->
        setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onSetSuccess() {
                continuation.resume(Unit)
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
                continuation.cancel(Exception("Failed to set remote description: $p0"))
            }
        }, description)
    }
}

suspend fun Handler.runSuspending(block: () -> Unit) {
    suspendCancellableCoroutine<Unit> { continuation ->
        post {
            try {
                block()
                continuation.resume(Unit)
            } catch (e: Exception) {
                continuation.cancel(e)
            }
        }
    }
}

@Serializable
data class AdvertisementPayload(
    val userId: Long,
)

@Serializable
data class OfferPayload(
    val typeCanonicalForm: String,
    val sdpDescription: String,
) {
    constructor(sessionDescription: SessionDescription) : this(
        sessionDescription.type.canonicalForm(),
        sessionDescription.description
    )

    fun toSessionDescription(): SessionDescription {
        return SessionDescription(SessionDescription.Type.fromCanonicalForm(typeCanonicalForm), sdpDescription)
    }
}

@Serializable
data class IceCandidatePayload(
    val sourceId: Long,
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val sdp: String
) {
    constructor(sourceId: Long, iceCandidate: IceCandidate) : this(
        sourceId,
        iceCandidate.sdpMid,
        iceCandidate.sdpMLineIndex,
        iceCandidate.sdp
    )

    fun toIceCandidate(): IceCandidate {
        return IceCandidate(sdpMid, sdpMLineIndex, sdp)
    }
}

class PartnerPortalViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "PartnerPortalViewModel"
    }

    val preferences = UserPreferencesRepository(application)
    lateinit var navController: NavController

    // connecting state
    private val _connectingStatus = MutableStateFlow(startingConnectionValue)
    val connectingStatus: StateFlow<String> = _connectingStatus.asStateFlow()

    private fun updateConnectingStatus(newStatus: String) {
        Log.d(TAG, "Updating connecting status to: $newStatus")
        _connectingStatus.value = newStatus
    }

    // incoming video stream state
    private val _incomingVideoStream = MutableStateFlow<VideoTrack?>(null)
    val incomingVideoTrack: StateFlow<VideoTrack?> = _incomingVideoStream.asStateFlow()

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
    var eglBase: EglBase = EglBase.create()!!
    private val videoEncoderFactory =
        HardwareVideoEncoderFactory(eglBase.eglBaseContext, true, true) // software broken on Android 7
    private val videoDecoderFactory =
        HardwareVideoDecoderFactory(eglBase.eglBaseContext) // software broken on Android 7
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

        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
    }

    // all the ice servers declared in stuns
    private val iceServers = let {
        this.getApplication<Application>().assets.open("stuns").bufferedReader(charset = Charsets.UTF_8).use { reader ->
            reader.readLines().filter { line ->
                !line.startsWith("#") && line.isNotEmpty()
            }.map { line ->
                PeerConnection.IceServer.builder(line).createIceServer()
            }
        }
    }
    private val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = SdpSemantics.UNIFIED_PLAN // harder to work with
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
        videoCapturer.startCapture(1280, 720, 15) // TODO: dynamic
        return videoSource
    }

    enum class CameraStatus {
        READY,
        ERROR,
    }

    private val cameraStatusChannel =
        Channel<Pair<VideoCapturer, CameraStatus>>(Channel.BUFFERED) // must be a normally buffered channel
    private fun createCameraCapturer(): VideoCapturer {
        var capturer: VideoCapturer? = null
        capturer = Camera2Capturer(this.getApplication(), getCameraId(true), object : CameraEventsHandler {
            override fun onCameraError(p0: String?) {
                Log.e(TAG, "Camera error: $p0")
                cameraStatusChannel.trySend(capturer!! to CameraStatus.ERROR)
            }

            override fun onCameraDisconnected() {
                Log.e(TAG, "Camera disconnected")
            }

            override fun onCameraFreezed(p0: String?) {
                Log.e(TAG, "Camera freezed: $p0")
            }

            override fun onCameraOpening(p0: String?) {
                Log.d(TAG, "Camera opening: $p0")
            }

            override fun onFirstFrameAvailable() {
                Log.d(TAG, "First frame available")
                cameraStatusChannel.trySend(capturer!! to CameraStatus.READY)
            }

            override fun onCameraClosed() {
                Log.d(TAG, "Camera closed")
            }
        })
        return capturer
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
        return peerConnectionFactory.createAudioTrack("audio_track", audioSource)
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

    val looper: Looper = getApplication<Application>().mainLooper
    val handler = Handler(looper)

    // webrtc peer connection
    private suspend fun doConnection() {
        val ourUserId = (Math.random() * Long.MAX_VALUE).toLong()

        var iceCandidateJob: Job? = null
        var peerConnection: PeerConnection? = null

        try {
            updateConnectingStatus(startingConnectionValue)
            handler.runSuspending {
                navController.navigate(LoadingRoute) {
                    popUpTo(StartupRoute)
                    launchSingleTop = true
                }
            }

            updateConnectingStatus("Starting Supabase connection...")

            val supabaseClient = getSupabase()

            val roomCode = preferences.roomCode

            if (roomCode.isEmpty()) {
                throw Exception("Room code is empty!")
            }

            val roomHashBase64 = Base64.encode(roomCode.toByteArray().sha1Hash(), Base64.DEFAULT).decodeToString()
            val signalingChannel = supabaseClient.realtime.channel("room:$roomHashBase64")
            signalingChannel.subscribe(true)

            updateConnectingStatus("Initializing audio & video...")

            val attemptedVideoTrack = videoTrack ?: createVideoTrack().also { videoTrack = it }
            val attemptedAudioTrack = audioTrack ?: createAudioTrack().also { audioTrack = it }

            if (attemptedAudioTrack == null) {
                Log.w(TAG, "Failed to create audio track")
                getApplication<Application>().showToast("Couldn't start the microphone. Continuing without audio...")
            }

            updateConnectingStatus("Initializing WebRTC...")

            val stateChannel = Channel<IceConnectionState>(Channel.CONFLATED)
            var connectionEstablished = false
            peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                    }

                    override fun onIceConnectionChange(p0: IceConnectionState?) {
                        Log.d(TAG, "ICE connection state: $p0")
                        if (p0 != null) {
                            stateChannel.trySend(p0)
                        }
                        when (p0) {
                            IceConnectionState.CONNECTED -> {
                                if (!connectionEstablished) {
                                    connectionEstablished = true
                                }
                            }

                            IceConnectionState.DISCONNECTED -> {
                                handler.post {
                                    showError(Exception("Connection lost"))
                                    cancelConnection()
                                }
                            }

                            IceConnectionState.CLOSED -> {
                                handler.post {
                                    if (navController.currentBackStackEntry?.toRoute<StartupRoute>() != StartupRoute) {
                                        showError(Exception("Connection closed"))
                                    } else {
                                        Log.d(TAG, "Connection closed after we stopped caring")
                                    }
                                    cancelConnection()
                                }
                            }

                            IceConnectionState.FAILED -> {
                                handler.post {
                                    showError(Exception("Connection failed"))
                                    cancelConnection()
                                }
                            }

                            else -> {
                            }
                        }
                    }

                    override fun onIceConnectionReceivingChange(p0: Boolean) {
                    }

                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    }

                    override fun onIceCandidate(p0: IceCandidate?) {
                        Log.d(TAG, "Got ice candidate: $p0")
                        if (p0 != null) {
                            val payload = IceCandidatePayload(ourUserId, p0)
                            viewModelScope.launch {
                                signalingChannel.broadcast("ice_candidate", payload)
                            }
                        }
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
                        Log.d(TAG, "Renegotiation needed")
                    }

                    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>) {
                    }

                    override fun onTrack(p0: RtpTransceiver) {
                        Log.d(TAG, "Got track: ${p0.receiver.track()}")
                        if (p0.receiver.track() is VideoTrack) {
                            _incomingVideoStream.value = p0.receiver.track() as VideoTrack
                        }
                    }

                    override fun onRemoveTrack(p0: RtpReceiver?) {
                    }
                }
            )

            if (peerConnection == null) {
                throw Exception("Failed to create peer connection")
            }

            updateConnectingStatus("Starting camera...")

            // create transceivers
            // wait for camera to be ready
            val cameraStatus = cameraStatusChannel.receive()
            if (cameraStatus.second == CameraStatus.ERROR) {
                throw Exception("Camera error")
            }

            // all the prep work is now done. we can now do actual signaling
            // 1. both clients create a user id (hope it doesn't collide)
            // 2. both clients issue "advertisement" messages in the room
            // 3. both clients listen for "advertisement" messages in the room, waiting until they hear one that isn't their own (store the other id and attach it to all messages)
            // 4. the client with the lower id sends an offer to the client with the higher id
            // 5. the client with the higher id sends an answer to the client with the lower id, after it receives the offer
            // 6. both clients exchange ice candidates
            // 7. both clients are connected
            // 8. navigate to the connected screen and start the video

            updateConnectingStatus("Searching for partner...")

            val ourAdvertisement = AdvertisementPayload(ourUserId)
            val advertisementJob = viewModelScope.launch {
                while (isActive) {
                    Log.d(TAG, "Broadcasting advertisement: $ourAdvertisement")
                    signalingChannel.broadcast("advertisement", ourAdvertisement)
                    delay(2000)
                }
            }
            val advertisementStream = signalingChannel.broadcastFlow<AdvertisementPayload>("advertisement")

            val peerUserId = advertisementStream.collectFirst { incomingAdvertisement ->
                Log.d(TAG, "Got advertisement from partner: $incomingAdvertisement")
                incomingAdvertisement.userId != ourUserId
            }.userId

            val offerStream = signalingChannel.broadcastFlow<OfferPayload>("offer")

            // do ICE candidate receiving
            val iceCandidateStream = signalingChannel.broadcastFlow<IceCandidatePayload>("ice_candidate")
            iceCandidateJob = viewModelScope.launch {
                iceCandidateStream.collect { iceCandidatePayload ->
                    if (iceCandidatePayload.sourceId == peerUserId) {
                        peerConnection.addIceCandidate(iceCandidatePayload.toIceCandidate())
                    }
                }
            }

            updateConnectingStatus("Waiting for partner...")

            // do handshake
            var answerSendingJob: Job? = null
            val weAreTheInitiator = ourUserId < peerUserId
            if (weAreTheInitiator) {
                updateConnectingStatus("Creating offer...")

                // add transceivers
                val videoTransceiver = peerConnection.addTransceiver(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                    RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.SEND_RECV,
                        listOf("video_track"),
                    )
                )
                if (!videoTransceiver.sender.setTrack(attemptedVideoTrack, false)) {
                    throw Exception("Failed to set video track")
                }

                val audioTransceiver = peerConnection.addTransceiver(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                    RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.SEND_RECV,
                        listOf("audio_track")
                    )
                )
                if (!audioTransceiver.sender.setTrack(attemptedAudioTrack, false)) {
                    throw Exception("Failed to set audio track")
                }

                val offerDescription = suspendCancellableCoroutine { completion ->
                    peerConnection.createOffer(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                            if (p0 == null) {
                                completion.cancel(Exception("Failed to create offer: null"))
                                return
                            }
                            completion.resume(p0)
                        }

                        override fun onSetSuccess() {
                        }

                        override fun onCreateFailure(p0: String?) {
                            completion.cancel(Exception("Failed to create offer: $p0"))
                        }

                        override fun onSetFailure(p0: String?) {
                        }
                    }, MediaConstraints())
                }

                peerConnection.setLocalDescriptionAsync(offerDescription)

                updateConnectingStatus("Sending offer...")

                val offerSendingJob = viewModelScope.launch {
                    while (isActive) {
                        signalingChannel.broadcast("offer", OfferPayload(offerDescription))
                        delay(2000)
                    }
                }

                updateConnectingStatus("Waiting for answer...")

                val answer = offerStream.collectFirst { offerPayload ->
                    val sdp = offerPayload.toSessionDescription()
                    sdp.type == SessionDescription.Type.ANSWER
                }.toSessionDescription()

                Log.d(TAG, "Got answer: ${answer.description}")

                updateConnectingStatus("Opening connection...")

                peerConnection.setRemoteDescriptionAsync(answer)
                offerSendingJob.cancel()
            } else {
                updateConnectingStatus("Waiting for offer...")

                val offerDescription = offerStream.collectFirst { offerPayload ->
                    val sdp = offerPayload.toSessionDescription()
                    sdp.type == SessionDescription.Type.OFFER
                }.toSessionDescription()

                Log.d(TAG, "Got offer: ${offerDescription.description}")

                peerConnection.setRemoteDescriptionAsync(offerDescription)

                updateConnectingStatus("Sending answer...")

                // write our transceivers
                val videoTransceiver = peerConnection.transceivers[0]
                videoTransceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                videoTransceiver.sender.setTrack(attemptedVideoTrack, false)

                val audioTransceiver = peerConnection.transceivers[1]
                audioTransceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                audioTransceiver.sender.setTrack(attemptedAudioTrack, false)

                // has to happen after the offer is set
                val answerDescription = suspendCancellableCoroutine { continuation ->
                    peerConnection.createAnswer(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                            if (p0 == null) {
                                continuation.cancel(Exception("Failed to create answer: null"))
                                return
                            }
                            continuation.resume(p0)
                        }

                        override fun onSetSuccess() {
                        }

                        override fun onCreateFailure(p0: String?) {
                            continuation.cancel(Exception("Failed to create answer: $p0"))
                        }

                        override fun onSetFailure(p0: String?) {
                        }
                    }, MediaConstraints())
                }

                peerConnection.setLocalDescriptionAsync(answerDescription)

                answerSendingJob = viewModelScope.launch {
                    while (isActive) {
                        signalingChannel.broadcast("offer", OfferPayload(answerDescription))
                        delay(2000)
                    }
                }
            }

            updateConnectingStatus("Waiting for connection...")

            advertisementJob.cancel() // no need to keep broadcasting
            while (true) {
                val status = stateChannel.receive()
                if (status == IceConnectionState.CONNECTED) {
                    break
                }
            }
            answerSendingJob?.cancel()

            updateConnectingStatus(finishedConnectionValue)

            handler.runSuspending {
                navController.navigate(StreamRoute) {
                    popUpTo(StartupRoute)
                    launchSingleTop = true
                }
            }

            awaitCancellation()
        } catch (e: Exception) {
            if (e !is CancellationException) {
                handler.runSuspending {
                    showError(e)
                }
            }
        } finally {
            cleanupMedia()
            peerConnection?.dispose()
            iceCandidateJob?.cancel()
        }
    }

    private var connectionJob: Job? = null

    fun startConnection() {
        synchronized(this) {
            viewModelScope.launch {
                if (connectionJob?.isCancelled == false) {
                    // wait to end
                    connectionJob!!.join()
                }
                if (connectionJob?.isActive == true) {
                    return@launch
                }
                connectionJob = viewModelScope.launch {
                    doConnection()
                }
            }
        }
    }

    fun restartConnection() {
        cancelConnection()
        startConnection()
    }

    fun cancelConnection() {
        connectionJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelConnection()
        eglBase.release()
        eglBase = EglBase.create()!! // swap w/ fresh EglBase
        peerConnectionFactory.dispose()
        cleanupMedia()
    }
}
