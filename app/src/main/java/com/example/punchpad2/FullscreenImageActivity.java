package com.example.punchpad2;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.github.chrisbanes.photoview.PhotoView;




public class FullscreenImageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PhotoView photoView = new PhotoView(this);
        photoView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        String imageUri = getIntent().getStringExtra("image_uri");
        if (imageUri != null) {
            photoView.setImageURI(Uri.parse(imageUri));
        }

        setContentView(photoView);
    }
}
