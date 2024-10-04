package xyz.regulad.partnerportal.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.Serializable
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import xyz.regulad.partnerportal.PartnerPortalViewModel
import xyz.regulad.partnerportal.ui.minecraft.MinecraftBackgroundImage

@Serializable
data object StreamRoute

@Composable
fun WebRTCVideoView(viewModel: PartnerPortalViewModel, videoTrack: VideoTrack, modifier: Modifier = Modifier) {
    val eglBase = viewModel.eglBase

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglBase.eglBaseContext, object : RendererEvents {
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
            eglBase.release()
        }
    )
}

@Composable
fun StreamPage(viewModel: PartnerPortalViewModel) {
    val videoTrack by viewModel.incomingVideoTrack.collectAsState()

    if (videoTrack != null) {
        WebRTCVideoView(viewModel, videoTrack!!, modifier = Modifier.fillMaxSize())
    } else {
        MinecraftBackgroundImage("portal.gif")
    }
}
