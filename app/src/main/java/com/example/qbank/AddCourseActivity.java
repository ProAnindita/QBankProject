package com.example.qbank;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddCourseActivity extends AppCompatActivity {

    private EditText courseIdInput, courseNameInput, semesterInput;
    private Button addCourseButton;
    private ImageButton uploadButton;
    private List<Uri> selectedImages = new ArrayList<>();
    private ActivityResultLauncher<Intent> pickImagesLauncher;

    private DatabaseReference databaseReference;

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
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            selectedImages.clear();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                selectedImages.add(imageUri);
                            }
                        } else if (result.getData().getData() != null) {
                            Uri imageUri = result.getData().getData();
                            selectedImages.clear();
                            selectedImages.add(imageUri);
                        }

                        if (!selectedImages.isEmpty()) {
                            uploadButton.setImageURI(selectedImages.get(0));
                            Toast.makeText(this, selectedImages.size() + " images selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        uploadButton.setOnClickListener(v -> openGalleryForMultipleImages());

        // Add course button click listener
        addCourseButton.setOnClickListener(v -> addCourseToFirebase());
    }

    private void openGalleryForMultipleImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
    }

    private void addCourseToFirebase() {
        String courseId = courseIdInput.getText().toString().trim();
        String courseName = courseNameInput.getText().toString().trim();
        String semester = semesterInput.getText().toString().trim();

        if (courseId.isEmpty() || courseName.isEmpty() || semester.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference courseRef = databaseReference.child(courseId).child(semester);

        // Check if the course already exists
        courseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Course already exists, show dialog
                    showEditDialog(courseId, semester, courseName);
                } else {
                    // Add new course
                    saveCourseToDatabase(courseRef, courseName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddCourseActivity.this, "Error checking course existence", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCourseToDatabase(DatabaseReference courseRef, String courseName) {
        courseRef.child("courseName").setValue(courseName);
        courseRef.child("imageKey").setValue("")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Course added successfully", Toast.LENGTH_SHORT).show();
                        courseIdInput.setText("");
                        courseNameInput.setText("");
                        semesterInput.setText("");
                    } else {
                        Toast.makeText(this, "Failed to add course", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDialog(String courseId, String semester, String currentCourseName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Course Already Added")
                .setMessage("The course is already added. Do you want to edit it?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Edit the course
                    DatabaseReference courseRef = databaseReference.child(courseId).child(semester);
                    courseRef.child("courseName").setValue(currentCourseName)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                   // Toast.makeText(this, "Course updated successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to update course", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }
}
