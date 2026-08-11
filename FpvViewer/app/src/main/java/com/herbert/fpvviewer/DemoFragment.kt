package com.herbert.fpvviewer

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.herbert.fpvviewer.databinding.FragmentDemoBinding
import com.jiangdg.ausbc.CameraFragment
import com.jiangdg.ausbc.callback.IAspectRatio
import com.jiangdg.ausbc.widget.AspectRatioTextureView

/**
 * Zeigt das Live-Bild der angeschlossenen UVC Capture Card.
 * Die eigentliche USB-Erkennung, Berechtigungsanfrage und Wiedergabe
 * übernimmt die CameraFragment-Basisklasse der Bibliothek automatisch.
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

    override fun getGravity(): Int = Gravity.TOP
}
