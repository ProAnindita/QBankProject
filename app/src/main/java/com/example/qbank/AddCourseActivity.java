package com.example.qbank;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AddCourseActivity extends AppCompatActivity {

    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/dp4ha5cws/image/upload";
    private static final String CLOUDINARY_UPLOAD_PRESET = "ml_default";

    private EditText courseIdInput, courseNameInput, semesterInput;
    private Button addCourseButton;
    private ImageButton uploadButton;
    private Uri selectedImageUri;
    private String imageUrl;

    private DatabaseReference databaseReference;
    private ActivityResultLauncher<Intent> pickImagesLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Courses");

        // Initialize views
        courseIdInput = findViewById(R.id.courseIdInput);
        courseNameInput = findViewById(R.id.courseNameInput);
        semesterInput = findViewById(R.id.SemesterInput);
        addCourseButton = findViewById(R.id.addCourseButton);
        uploadButton = findViewById(R.id.uploadButton);

        // Initialize image picker
        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();

                        if (selectedImageUri != null) {
                            uploadButton.setImageURI(selectedImageUri);
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        uploadButton.setOnClickListener(v -> openGalleryForImage());

        // Add course button click listener
        addCourseButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageToCloudinary();
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGalleryForImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void uploadImageToCloudinary() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] byteArray = baos.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Map<String, String> params = new HashMap<>();
            params.put("upload_preset", CLOUDINARY_UPLOAD_PRESET);
            params.put("file", "data:image/jpeg;base64," + base64Image);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, CLOUDINARY_UPLOAD_URL, new JSONObject(params),
                    response -> {
                        try {
                            imageUrl = response.getString("secure_url");
                            Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                            saveCourseToFirebase();
                        } catch (Exception e) {
                            Log.e("Cloudinary Response Error", e.getMessage(), e);
                        }
                    }, error -> {
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    String errorMessage = new String(error.networkResponse.data);
                    Log.e("Volley Error", error.networkResponse.statusCode + ": " + errorMessage);
                } else {
                    Log.e("Volley Error", "Unknown error occurred", error);
                }
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            });

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(jsonObjectRequest);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Bitmap Conversion Error", e.getMessage(), e);
            Toast.makeText(this, "Error preparing image for upload", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCourseToFirebase() {
        String courseId = courseIdInput.getText().toString().trim();
        String courseName = courseNameInput.getText().toString().trim();
        String semester = semesterInput.getText().toString().trim();

        if (courseId.isEmpty() || courseName.isEmpty() || semester.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference courseRef = databaseReference.child(courseId).child(semester);

        courseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                courseRef.child("courseName").setValue(courseName);
                courseRef.child("imageKey").setValue(imageUrl)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(AddCourseActivity.this, "Course added successfully", Toast.LENGTH_SHORT).show();
                                resetFields();
                            } else {
                                Toast.makeText(AddCourseActivity.this, "Failed to add course", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddCourseActivity.this, "Error saving course", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetFields() {
        courseIdInput.setText("");
        courseNameInput.setText("");
        semesterInput.setText("");
        uploadButton.setImageResource(R.drawable.ic_upload); // Reset to default icon
        selectedImageUri = null;
    }
}
