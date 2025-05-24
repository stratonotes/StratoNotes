package com.stratonotes

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.chrisbanes.photoview.PhotoView
import androidx.core.net.toUri

class FullscreenImageActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val photoView = PhotoView(this)
        photoView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val imageUri = intent.getStringExtra("image_uri")
        if (imageUri != null) {
            photoView.setImageURI(imageUri.toUri())
        }

        setContentView(photoView)
    }
}