package com.trantrongmanh.lab5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.trantrongmanh.lab5.auth.AuthManager;
import com.trantrongmanh.lab5.auth.LoginActivity;

/**
 * Launcher screen: shows for 2 seconds then routes to Login or MainActivity
 * depending on whether the user is already signed in.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate the logo in
        LinearLayout layoutLogo = findViewById(R.id.layoutLogo);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        layoutLogo.startAnimation(fadeIn);

        new Handler().postDelayed(() -> {
            Class<?> dest = AuthManager.getInstance().isLoggedIn()
                    ? MainActivity.class
                    : LoginActivity.class;

            Intent intent = new Intent(SplashActivity.this, dest);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
