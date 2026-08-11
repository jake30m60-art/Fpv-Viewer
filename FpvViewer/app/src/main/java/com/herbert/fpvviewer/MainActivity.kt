package com.herbert.fpvviewer

import android.Manifest
import android.os.Build
import android.os.Bundle
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
 * 3. Kamera- und Mikrofon-Berechtigung erlauben (für Foto-/Video-Aufnahme)
 * 4. Bild erscheint automatisch, sobald die Capture Card erkannt wird
 *
 * Die Toolbox (Foto/Video/Auflösung) liegt im Kamera-Fragment selbst,
 * da nur dort Zugriff auf die Aufnahme-Funktionen der Bibliothek besteht.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        requestPermissionsAndShow()
    }

    private fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        return permissions.toTypedArray()
    }

    private fun requestPermissionsAndShow() {
        val missing = requiredPermissions().filter {
            PermissionChecker.checkSelfPermission(this, it) != PermissionChecker.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PERMISSIONS)
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
        if (requestCode == REQUEST_PERMISSIONS) {
            // Auch wenn Mikrofon/Speicher abgelehnt wurden, zumindest das Live-Bild zeigen
            // (Kamera-Berechtigung ist die einzig wirklich zwingende).
            showCameraFragment()
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 0
    }
}
