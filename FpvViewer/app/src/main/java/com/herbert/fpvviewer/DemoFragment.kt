package com.herbert.fpvviewer

import android.content.ContentValues
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import java.io.File

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

    /**
     * Verschiebt die von der Bibliothek gespeicherte Datei explizit in ein eigenes
     * Album "FpvViewer" (Pictures/FpvViewer bzw. Movies/FpvViewer), damit sie
     * zuverlässig in der Galerie erscheint - unabhängig davon, wo/wie die
     * Bibliothek sie ursprünglich intern abgelegt hat.
     */
    private fun moveIntoAlbum(sourcePath: String?, isImage: Boolean): String? {
        if (sourcePath.isNullOrBlank()) return null
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val collection = if (isImage) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                val relativeDir = if (isImage) {
                    "${Environment.DIRECTORY_PICTURES}/FpvViewer"
                } else {
                    "${Environment.DIRECTORY_MOVIES}/FpvViewer"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isImage) "image/jpeg" else "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                }
                val uri = resolver.insert(collection, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                sourceFile.delete()
                "$relativeDir/${sourceFile.name}"
            } else {
                val baseDir = Environment.getExternalStoragePublicDirectory(
                    if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
                )
                val albumDir = File(baseDir, "FpvViewer")
                albumDir.mkdirs()
                val destFile = File(albumDir, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)
                sourceFile.delete()
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun takePhoto() {
        captureImage(object : ICaptureCallBack {
            override fun onBegin() {}

            override fun onError(error: String?) {
                Toast.makeText(context, "Foto fehlgeschlagen: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onComplete(path: String?) {
                val albumPath = moveIntoAlbum(path, isImage = true)
                Toast.makeText(
                    context,
                    if (albumPath != null) "Foto im Album \"FpvViewer\" gespeichert" else "Foto gespeichert: $path",
                    Toast.LENGTH_LONG
                ).show()
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
                // Toolbox bewusst NICHT schließen, sonst ist der Stop-Button nicht erreichbar
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
                val albumPath = moveIntoAlbum(path, isImage = false)
                Toast.makeText(
                    context,
                    if (albumPath != null) "Video im Album \"FpvViewer\" gespeichert" else "Video gespeichert: $path",
                    Toast.LENGTH_LONG
                ).show()
                closeToolbox()
            }
        })
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
