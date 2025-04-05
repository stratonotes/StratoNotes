package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.TranslateAnimation;

public class SplashActivity extends Activity {

    private View loadingBar;
    private View splashRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        loadingBar = findViewById(R.id.loadingBar);
        splashRoot = findViewById(R.id.splashRoot);

        loadingBar.post(() -> loadingBar.animate().scaleX(1f).setDuration(1000).start());

        new Handler().postDelayed(() -> {
            TranslateAnimation slideDown = new TranslateAnimation(0, 0, 0, splashRoot.getHeight());
            slideDown.setDuration(600);
            slideDown.setFillAfter(true);
            splashRoot.startAnimation(slideDown);

            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }, 600);

        }, 800); // hold splash minimum 0.5s, starts drain after ~0.8s
    }
}