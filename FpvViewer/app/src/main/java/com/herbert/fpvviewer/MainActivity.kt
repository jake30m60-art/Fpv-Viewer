package com.herbert.fpvviewer

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * FpvViewer – zeigt das Live-Bild einer beliebigen UVC-fähigen Capture Card
 * (egal ob die Quelle DJI, Skyzone, Fatshark o.ä. ist) auf dem Handy an.
 *
 * Ablauf:
 * 1. Handy per USB-OTG-Adapter mit der Capture Card verbinden
 * 2. App öffnen
 * 3. Kamera-Berechtigung erlauben (für die Aufnahme-Funktionen der Bibliothek)
 * 4. Bild erscheint automatisch, sobald die Capture Card erkannt wird
 *
 * Unten rechts liegt ein kleines, halbtransparentes Symbol: antippen blendet
 * die Systemleiste (Uhrzeit/Status) ein, nochmal antippen blendet sie wieder aus.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private var systemBarsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        hideSystemBars()
        setupToolboxButton()
        requestCameraPermissionAndShow()
    }

    private fun setupToolboxButton() {
        val toolboxButton = findViewById<View>(R.id.toolboxButton)
        toolboxButton.setOnClickListener {
            if (systemBarsVisible) {
                hideSystemBars()
            } else {
                showSystemBars()
            }
        }
    }

    private fun hideSystemBars() {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        systemBarsVisible = false
        findViewById<View>(R.id.toolboxButton)?.animate()?.alpha(0.3f)?.setDuration(200)?.start()
    }

    private fun showSystemBars() {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        systemBarsVisible = true
        findViewById<View>(R.id.toolboxButton)?.animate()?.alpha(1f)?.setDuration(200)?.start()
    }

    private fun requestCameraPermissionAndShow() {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
            return
        }
        showCameraFragment()
    }

    private fun showCameraFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DemoFragment())
            .commitAllowingStateLoss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            showCameraFragment()
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 0
    }
}
