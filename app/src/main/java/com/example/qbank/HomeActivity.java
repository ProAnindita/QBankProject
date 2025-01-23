package com.example.qbank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button btnsignout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnsignout = findViewById(R.id.buttonSignOut);

        btnsignout.setOnClickListener(v -> {
            // Reset login state
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            // Redirect to login activity
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close HomeActivity
        });
    }

    @Override
    public void onBackPressed() {
        // Exit the app on back press
        super.onBackPressed();
        finishAffinity();
    }
}
