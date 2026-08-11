package com.herbert.fpvviewer

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.TextureView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jiangdg.usbcamera.UVCCameraHelper
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera

/**
 * FpvViewer – zeigt das Live-Bild einer beliebigen UVC-fähigen Capture Card
 * (egal ob die Quelle DJI, Skyzone, Fatshark o.ä. ist) auf dem Handy an,
 * und lässt die Auflösung/Bildrate frei wählen statt sie fest auf 720p zu
 * begrenzen, wie es viele fertige Apps tun.
 *
 * Ablauf:
 * 1. Handy per USB-OTG-Adapter mit der Capture Card verbinden
 * 2. App öffnen (oder sie startet automatisch, weil im Manifest registriert)
 * 3. Android fragt nach USB-Berechtigung -> erlauben
 * 4. Unten erscheinen Buttons mit den vom Gerät unterstützten Auflösungen
 * 5. Auflösung antippen -> Bild startet in der gewählten Qualität
 */
class MainActivity : AppCompatActivity(), CameraDialog.CameraDialogParent {

    private lateinit var textureView: TextureView
    private lateinit var statusText: TextView
    private lateinit var resolutionBar: LinearLayout
    private var mCameraHelper: UVCCameraHelper? = null
    private var isRequest = false

    private val listener = object : UVCCameraHelper.OnMyDevConnectListener {
        override fun onAttachDev(device: UsbDevice?) {
            statusText.text = "Capture Card erkannt, verbinde..."
            if (!isRequest) {
                isRequest = true
                mCameraHelper?.requestPermission(0)
            }
        }

        override fun onDettachDev(device: UsbDevice?) {
            if (isRequest) {
                isRequest = false
                mCameraHelper?.closeCamera()
                statusText.text = "Warte auf Capture Card..."
                resolutionBar.removeAllViews()
            }
        }

        override fun onConnectDev(device: UsbDevice?, isConnected: Boolean) {
            if (!isConnected) {
                statusText.text = "Verbindung fehlgeschlagen"
                return
            }
            statusText.text = ""
            populateSupportedResolutions()
        }

        override fun onDisConnectDev(device: UsbDevice?) {
            statusText.text = "Getrennt"
            resolutionBar.removeAllViews()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        statusText = findViewById(R.id.statusText)
        resolutionBar = findViewById(R.id.resolutionBar)

        mCameraHelper = UVCCameraHelper.getInstance()
        // MJPEG deckt bei den meisten Capture Cards die höheren Auflösungen ab (u.a. 1080p).
        // Falls dein Gerät stattdessen YUY2 meldet, hier auf FRAME_FORMAT_YUYV umstellen.
        mCameraHelper?.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
        mCameraHelper?.initUSBMonitor(this, textureView, listener)
        mCameraHelper?.setOnPreviewFrameListener(null)
    }

    /**
     * Liest die vom angeschlossenen Gerät tatsächlich unterstützten Auflösungen
     * aus und zeigt sie als antippbare Buttons an - statt eine fest einzucodieren.
     *
     * HINWEIS: getSupportedSizeList() existiert auf dem UVCCamera-Objekt der
     * zugrunde liegenden Bibliothek. Je nach Bibliotheksversion heißt der Zugriffs-
     * weg vom Helper aus eventuell anders (z.B. mCameraHelper?.uvcCamera statt
     * getUVCCamera()) - falls Android Studio hier einen Fehler zeigt, in der
     * UVCCameraHelper-Klasse (über "Go to definition") nachsehen, wie das Camera-
     * Objekt heißt, und die Zeile unten entsprechend anpassen.
     */
    private fun populateSupportedResolutions() {
        resolutionBar.removeAllViews()
        val camera: UVCCamera = mCameraHelper?.uvcCamera ?: return
        val sizeList = try {
            camera.getSupportedSizeList()
        } catch (e: Exception) {
            null
        } ?: return

        for (size in sizeList) {
            val button = Button(this)
            button.text = "${size.width}x${size.height}"
            button.setOnClickListener {
                mCameraHelper?.updateResolution(size.width, size.height)
            }
            resolutionBar.addView(button)
        }
    }

    override fun onStart() {
        super.onStart()
        mCameraHelper?.registerUSB()
    }

    override fun onStop() {
        super.onStop()
        mCameraHelper?.unregisterUSB()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCameraHelper?.isCameraOpened == true) {
            mCameraHelper?.releaseCamera()
        }
        mCameraHelper?.release()
    }

    // Erforderlich durch CameraDialog.CameraDialogParent
    override fun getUSBMonitor(): USBMonitor? = mCameraHelper?.usbMonitor

    override fun onDialogResult(canceled: Boolean) {
        // nicht benötigt für diesen einfachen Anwendungsfall
    }
}
