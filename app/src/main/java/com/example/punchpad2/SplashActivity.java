package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashIcon = findViewById(R.id.splashIcon);

        // Optional: small zoom-in effect to start
        //splashIcon.setScaleX(100f);
        //splashIcon.setScaleY(100f);
        //splashIcon.animate()
        //        .scaleX(100f)
        //        .scaleY(100f)
        //        .setDuration(300)
        //        .start();

        new Handler().postDelayed(() -> {
            splashIcon.animate()
                    .translationY(splashIcon.getHeight() * 2f)
                    .setDuration(400)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    })
                    .start();
        }, 1100); // 300ms animation + 1200ms pause = ~1.5s total
    }
}
