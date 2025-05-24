package com.stratonotes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.example.punchpad2.R

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashIcon = findViewById<View>(R.id.splashIcon)

        // Initial position: icon is offscreen below
        splashIcon.translationY = 500f

        // Slide up into circle (0.3s)
        splashIcon.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                Handler().postDelayed({
                    // Slide up and out (0.3s after pause)
                    splashIcon.animate()
                        .translationY(-500f)
                        .setDuration(300)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            startActivity(
                                Intent(
                                    this@SplashActivity,
                                    MainActivity::class.java
                                )
                            )
                            overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            finish()
                        }
                        .start()
                }, 400)
            } // 400ms pause
            .start()
    }
}