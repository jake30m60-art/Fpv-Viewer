package com.herbert.fpvviewer

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.herbert.fpvviewer.databinding.FragmentDemoBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Zeigt das Live-Bild der angeschlossenen UVC Capture Card.
 * Die eigentliche USB-Erkennung, Berechtigungsanfrage und Wiedergabe
 * übernimmt die CameraFragment-Basisklasse der Bibliothek automatisch.
 *
 * Solange kein Bild ankommt, zeigt ein Overlay ein Icon + Statustext an.
 * Sobald die Kamera erfolgreich öffnet, blendet sich das Overlay aus.
 */
class DemoFragment : CameraFragment() {

    private lateinit var mViewBinding: FragmentDemoBinding

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentDemoBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return mViewBinding.cameraViewContainer
    }

    // Zentriert statt oben ausgerichtet, damit kein einseitiger schwarzer Rand entsteht
    override fun getGravity(): Int = Gravity.CENTER

    // Fordert explizit 1080p an, statt sich auf eine niedrige Standardauflösung zu verlassen.
    // Falls die Capture Card 1080p nicht unterstützt, fällt die Bibliothek automatisch
    // auf die nächstbeste verfügbare Auflösung zurück.
    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(1920)
            .setPreviewHeight(1080)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
            .setAspectRatioShow(true)
            .create()
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                mViewBinding.waitingOverlay.visibility = View.GONE
            }
            ICameraStateCallBack.State.CLOSED -> {
                mViewBinding.waitingText.text = "Warte auf USB-Gerät..."
                mViewBinding.waitingOverlay.visibility = View.VISIBLE
            }
            ICameraStateCallBack.State.ERROR -> {
                mViewBinding.waitingText.text = msg ?: "Fehler beim Verbinden"
                mViewBinding.waitingOverlay.visibility = View.VISIBLE
            }
        }
    }
}
