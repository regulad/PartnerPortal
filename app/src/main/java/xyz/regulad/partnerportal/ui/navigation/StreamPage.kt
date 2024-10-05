package xyz.regulad.partnerportal.ui.navigation

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.Serializable
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import xyz.regulad.partnerportal.PartnerPortalViewModel
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage
import xyz.regulad.partnerportal.ui.minecraft.MinecraftText

@Serializable
data object StreamRoute

@Composable
fun WebRTCVideoView(viewModel: PartnerPortalViewModel, videoTrack: VideoTrack, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(viewModel.eglBase.eglBaseContext, object : RendererEvents {
                    override fun onFirstFrameRendered() {
                        Log.d("WebRTCVideoView", "First frame rendered")
                    }

                    override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                        Log.d("WebRTCVideoView", "Frame resolution changed: $p0, $p1, $p2")
                    }
                })
                setEnableHardwareScaler(true)
                setMirror(true)
            }
        },
        modifier = modifier,
        update = { view ->
            Log.d("WebRTCVideoView", "Adding sink to video track")
            videoTrack.addSink(view)
        },
        onRelease = { view ->
            videoTrack.removeSink(view)
            view.release()
//            viewModel.eglBase.release() // we don't own the eglBase, so we can't release it
        }
    )
}

@Composable
fun StreamPage(viewModel: PartnerPortalViewModel) {
    val outgoingVideoTrack by viewModel.outgoingVideoTrack.collectAsState()
    val outgoingAudioTrack by viewModel.outgoingAudioTrack.collectAsState()

    val incomingVideoTrack by viewModel.incomingVideoTrack.collectAsState()
    val incomingAudioTrack by viewModel.incomingAudioTrack.collectAsState()

    var muteDesired by remember { mutableStateOf(false) }
    var deafenDesired by remember { mutableStateOf(false) }
    var cameraDesired by remember { mutableStateOf(true) }
    var displayDesired by remember { mutableStateOf(true) }

    LaunchedEffect(outgoingAudioTrack, muteDesired) {
        outgoingAudioTrack?.setEnabled(!muteDesired)
    }

    LaunchedEffect(outgoingVideoTrack, cameraDesired) {
        outgoingVideoTrack?.setEnabled(cameraDesired)
    }

    LaunchedEffect(incomingAudioTrack, deafenDesired) {
        incomingAudioTrack?.setEnabled(!deafenDesired)
    }

    LaunchedEffect(incomingVideoTrack, displayDesired) {
        incomingVideoTrack?.setEnabled(displayDesired)
    }

    val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    DisposableEffect(incomingAudioTrack) {
        if (incomingAudioTrack != null) {
            audioManager.isSpeakerphoneOn = true

            return@DisposableEffect onDispose {
                audioManager.isSpeakerphoneOn = false
            }
        } else {
            return@DisposableEffect onDispose {
                // no-op
            }
        }
    }

    // ui should contain the following
    // - full screen for partner's camera8/
    // - mute/unmute button
    // - deafen/undeafen button
    // - camera on/off button
    // - display on/off button

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (incomingVideoTrack == null) {
            LoadingPage(viewModel = viewModel)
        } else {
            WebRTCVideoView(viewModel, incomingVideoTrack!!, Modifier.fillMaxSize())
        }

        // column in bottom left corner with controls/feedback
        val iconSize = 24.dp

        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Row {
                Surface(
                    onClick = {
                        muteDesired = !muteDesired
                    },
                    color = Color.Transparent
                ) {
                    // mute/unmute button
                    if (!muteDesired) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Microphone On",
                            modifier = Modifier.size(iconSize)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MicOff,
                            contentDescription = "Microphone Off",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                Surface(
                    onClick = {
                        deafenDesired = !deafenDesired
                    },
                    color = Color.Transparent
                ) {
                    // deafen/undeafen button
                    if (!deafenDesired) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume On",
                            modifier = Modifier.size(iconSize)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Volume Off",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                Surface(
                    onClick = {
                        cameraDesired = !cameraDesired
                    },
                    color = Color.Transparent
                ) {
                    // camera on/off button
                    if (cameraDesired) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = "Camera On",
                            modifier = Modifier.size(iconSize)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.VideocamOff,
                            contentDescription = "Camera Off",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                Surface(
                    onClick = {
                        displayDesired = !displayDesired
                    },
                    color = Color.Transparent
                ) {
                    // display on/off button
                    if (displayDesired) {
                        Icon(
                            imageVector = Icons.Filled.Tv,
                            contentDescription = "Display On",
                            modifier = Modifier.size(iconSize)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.TvOff,
                            contentDescription = "Display Off",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            // moto g4 play dimensions:
            // 704dp x 392dp

            // if we wanted to add a view for our camera, we could do it here
        }
    }
}

@Composable
fun LoadingPage(viewModel: PartnerPortalViewModel) {
    val connectingStatus by viewModel.connectingStatus.collectAsState()

    MinecraftBackgroundImage("portal.gif")

    Box(modifier = Modifier.fillMaxSize()) {
        MinecraftText(
            connectingStatus,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
