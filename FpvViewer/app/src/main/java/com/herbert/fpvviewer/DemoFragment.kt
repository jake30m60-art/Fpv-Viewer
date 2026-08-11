package com.herbert.fpvviewer

import android.media.MediaScannerConnection
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.herbert.fpvviewer.databinding.FragmentDemoBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Zeigt das Live-Bild der angeschlossenen UVC Capture Card.
 * Die eigentliche USB-Erkennung, Berechtigungsanfrage und Wiedergabe
 * übernimmt die CameraFragment-Basisklasse der Bibliothek automatisch.
 *
 * Solange kein Bild ankommt, zeigt ein Overlay ein Icon + Statustext an.
 * Unten rechts liegt ein kleines Symbol, das eine Toolbox mit
 * Foto-/Video-Aufnahme und Auflösungsauswahl ein-/ausblendet.
 * Während einer Videoaufnahme läuft oben links eine REC-Anzeige mit Timer.
 */
class DemoFragment : CameraFragment() {

    private lateinit var mViewBinding: FragmentDemoBinding
    private var isRecordingVideo = false
    private var recordingSeconds = 0

    private val recHandler = Handler(Looper.getMainLooper())
    private val recTimerRunnable = object : Runnable {
        override fun run() {
            recordingSeconds++
            val minutes = recordingSeconds / 60
            val seconds = recordingSeconds % 60
            mViewBinding.recTimeText.text = "REC %02d:%02d".format(minutes, seconds)
            recHandler.postDelayed(this, 1000)
        }
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentDemoBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun initView() {
        super.initView()
        setupToolbox()
    }

    private fun setupToolbox() {
        mViewBinding.toolboxButton.setOnClickListener {
            mViewBinding.toolboxButton.visibility = View.GONE
            mViewBinding.toolboxPanel.visibility = View.VISIBLE
        }

        mViewBinding.closeToolboxBtn.setOnClickListener {
            closeToolbox()
        }

        mViewBinding.captureImageBtn.setOnClickListener {
            takePhoto()
        }

        mViewBinding.captureVideoBtn.setOnClickListener {
            toggleVideoRecording()
        }

        mViewBinding.resolutionBtn.setOnClickListener {
            showResolutionPicker()
        }
    }

    private fun closeToolbox() {
        mViewBinding.toolboxPanel.visibility = View.GONE
        mViewBinding.toolboxButton.visibility = View.VISIBLE
    }

    /** Meldet eine gespeicherte Datei aktiv beim Mediensystem an, damit sie sofort in der Galerie erscheint. */
    private fun scanIntoGallery(path: String?) {
        if (path.isNullOrBlank()) return
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    private fun takePhoto() {
        captureImage(object : ICaptureCallBack {
            override fun onBegin() {}

            override fun onError(error: String?) {
                Toast.makeText(context, "Foto fehlgeschlagen: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onComplete(path: String?) {
                scanIntoGallery(path)
                Toast.makeText(context, "Foto gespeichert", Toast.LENGTH_SHORT).show()
            }
        })
        closeToolbox()
    }

    private fun toggleVideoRecording() {
        if (isRecordingVideo) {
            captureVideoStop()
            return
        }
        captureVideoStart(object : ICaptureCallBack {
            override fun onBegin() {
                isRecordingVideo = true
                mViewBinding.captureVideoBtn.text = "⏹  Aufnahme stoppen"
                recordingSeconds = 0
                mViewBinding.recTimeText.text = "REC 00:00"
                mViewBinding.recIndicator.visibility = View.VISIBLE
                recHandler.postDelayed(recTimerRunnable, 1000)
            }

            override fun onError(error: String?) {
                isRecordingVideo = false
                mViewBinding.captureVideoBtn.text = "🎥  Video aufnehmen"
                mViewBinding.recIndicator.visibility = View.GONE
                recHandler.removeCallbacks(recTimerRunnable)
                Toast.makeText(context, "Video fehlgeschlagen: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onComplete(path: String?) {
                isRecordingVideo = false
                mViewBinding.captureVideoBtn.text = "🎥  Video aufnehmen"
                mViewBinding.recIndicator.visibility = View.GONE
                recHandler.removeCallbacks(recTimerRunnable)
                scanIntoGallery(path)
                Toast.makeText(context, "Video gespeichert", Toast.LENGTH_SHORT).show()
            }
        })
        closeToolbox()
    }

    private fun showResolutionPicker() {
        val sizes = getAllPreviewSizes()
        if (sizes.isNullOrEmpty()) {
            Toast.makeText(context, "Keine Auflösungen gefunden", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = sizes.map { "${it.width} x ${it.height}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Auflösung wählen")
            .setItems(labels) { dialog, index ->
                updateResolution(sizes[index].width, sizes[index].height)
                dialog.dismiss()
            }
            .show()
        closeToolbox()
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

    override fun onDestroyView() {
        super.onDestroyView()
        recHandler.removeCallbacks(recTimerRunnable)
    }
}
