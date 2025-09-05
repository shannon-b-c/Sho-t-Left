package com.example.shotleft;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import androidx.core.splashscreen.SplashScreen;

import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView logoImageView = findViewById(R.id.logo_image_view);

        // Set initial state (off-screen to the left)
        logoImageView.setTranslationX(-1000f);

        // Use a handler to delay the animation slightly
        new Handler().postDelayed(() -> {
            logoImageView.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(800)
                    .start();
        }, 100);
    }
}