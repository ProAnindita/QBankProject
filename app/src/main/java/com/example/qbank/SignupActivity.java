package com.example.qbank;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextBatch, editTextPassword, editTextConfirmPassword;
    private Spinner spinnerSection;
    private Button buttonSignUp;
    private TextView textSignIn;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        // Initialize Views
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextBatch = findViewById(R.id.editTextBatch);
        spinnerSection = findViewById(R.id.spinnerSection);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textSignIn = findViewById(R.id.textViewSignIn);

        // Populate Section Spinner
        String[] sections = {"A", "B", "C", "D", "E", "F", "I"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
        spinnerSection.setAdapter(adapter);

        // Sign Up Button Logic
        buttonSignUp.setOnClickListener(v -> validateInputs());

        textSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void validateInputs() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String batch = editTextBatch.getText().toString().trim();
        String section = spinnerSection.getSelectedItem().toString();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Please enter your name");
            editTextName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(batch)) {
            editTextBatch.setError("Please enter your batch");
            editTextBatch.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Please enter a password");
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            editTextConfirmPassword.requestFocus();
            return;
        }

        createFirebaseUser(name, email, batch, section, password);
    }

    private void createFirebaseUser(String name, String email, String batch, String section, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), name, email, batch, section);
                            sendVerificationEmail(user);
                        }
                    } else {
                        // Intercept Firebase's error messages
                        String errorMessage = task.getException().getMessage();

                        // Replace Firebase's default error messages with custom ones
                        if (errorMessage.contains("email address is already in use")) {
                            Toast.makeText(SignupActivity.this, "This email is already registered. Please use another email.", Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("password is invalid")) {
                            Toast.makeText(SignupActivity.this, "The password entered is invalid. Please try again.", Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("email address is badly formatted")) {
                            Toast.makeText(SignupActivity.this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SignupActivity.this, "Signup Failed. Please try again later.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    private void saveUserToDatabase(String userId, String name, String email, String batch, String section) {
        User user = new User(name, email, batch, section);
        database.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "User data saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to save user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Verification email sent. Please verify and log in.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
