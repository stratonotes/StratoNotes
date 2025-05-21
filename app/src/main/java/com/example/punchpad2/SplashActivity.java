package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.example.punchpad2.R;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashIcon = findViewById(R.id.splashIcon);

        // Initial position: icon is offscreen below
        splashIcon.setTranslationY(500f);

        // Slide up into circle (0.3s)
        splashIcon.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> new Handler().postDelayed(() -> {
                    // Slide up and out (0.3s after pause)
                    splashIcon.animate()
                            .translationY(-500f)
                            .setDuration(300)
                            .setInterpolator(new AccelerateInterpolator())
                            .withEndAction(() -> {
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            })
                            .start();
                }, 400)) // 400ms pause
                .start();
    }
}
