package com.example.qbank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user or admin is already logged in
        SharedPreferences userPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);

        boolean isUserLoggedIn = userPrefs.getBoolean("isLoggedIn", false);
        boolean isAdminLoggedIn = adminPrefs.getBoolean("isAdminLoggedIn", false);

        if (isUserLoggedIn) {
            // Redirect to HomeActivity for users
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity to prevent back navigation
            return;
        }

        if (isAdminLoggedIn) {
            // Redirect to AdminActivity for admins
            Intent intent = new Intent(MainActivity.this, AdminActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity to prevent back navigation
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Finding views by ID
        EditText emailInput = findViewById(R.id.editTextEmail);
        EditText passwordInput = findViewById(R.id.editTextPassword);
        Button signInButton = findViewById(R.id.buttonSignIn);
        TextView signUpText = findViewById(R.id.textViewSignUp);
        Button signinAdminButton = findViewById(R.id.buttonAdminSignIn);

        // Admin Login Logic
        signinAdminButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Please enter email");
                emailInput.requestFocus();
            } else if (TextUtils.isEmpty(password)) {
                passwordInput.setError("Please enter password");
                passwordInput.requestFocus();
            } else if (email.equals("anindita@gmail.com") && password.equals("asdf")) {
                // Save admin login state
                SharedPreferences.Editor adminEditor = adminPrefs.edit();
                adminEditor.putBoolean("isAdminLoggedIn", true);
                adminEditor.apply();

                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
                finish(); // Close MainActivity
                Toast.makeText(MainActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Invalid Admin Credentials!", Toast.LENGTH_SHORT).show();
            }
        });

        // User Login Logic
        signInButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Please enter email");
                emailInput.requestFocus();
            } else if (TextUtils.isEmpty(password)) {
                passwordInput.setError("Please enter password");
                passwordInput.requestFocus();
            } else {
                signInFirebaseUser(email, password);
            }
        });

        // Sign Up Logic
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void signInFirebaseUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // Save user login state
                            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.apply();

                            Toast.makeText(MainActivity.this, "Sign-In Successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Close MainActivity
                        } else {
                            Toast.makeText(MainActivity.this, "Please verify your email before signing in.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Sign-In Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
