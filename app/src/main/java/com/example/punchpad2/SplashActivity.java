package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashIcon = findViewById(R.id.splashIcon);

        // Initial scale from 0.8 to 1.0 to create a slight zoom-in effect
        splashIcon.setScaleX(0.8f);
        splashIcon.setScaleY(0.8f);

        splashIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction(() -> {
                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, 300); // Stay visible for 300ms after animation
                })
                .start();
    }
}
