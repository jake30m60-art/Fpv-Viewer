package com.herbert.fpvviewer

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

/**
 * FpvViewer – zeigt das Live-Bild einer beliebigen UVC-fähigen Capture Card
 * (egal ob die Quelle DJI, Skyzone, Fatshark o.ä. ist) auf dem Handy an.
 *
 * Ablauf:
 * 1. Handy per USB-OTG-Adapter mit der Capture Card verbinden
 * 2. App öffnen
 * 3. Kamera-Berechtigung erlauben (für die Aufnahme-Funktionen der Bibliothek)
 * 4. Bild erscheint automatisch, sobald die Capture Card erkannt wird
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestCameraPermissionAndShow()
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
