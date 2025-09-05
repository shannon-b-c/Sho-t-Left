package com.example.shotleft;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageView;

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