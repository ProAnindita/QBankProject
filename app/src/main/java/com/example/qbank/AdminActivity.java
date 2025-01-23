package com.example.qbank;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    private EditText searchEditText, courseIdInput, courseNameInput, semesterInput;
    private Button addCourseButton, adminSignOutButton;
    private ListView courseListView;

    private DatabaseReference databaseReference;
    private ArrayList<Course> courseList;
    private CourseAdapter courseAdapter;
    private List<Uri> selectedImages = new ArrayList<>();
    private ActivityResultLauncher<Intent> pickImagesLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        boolean isAdminLoggedIn = sharedPreferences.getBoolean("isAdminLoggedIn", false);

        if (!isAdminLoggedIn) {
            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_admin);
        ImageButton uploadButton = findViewById(R.id.uploadButton);

// Initialize the launcher for picking multiple images
        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            // Multiple images selected
                            int count = result.getData().getClipData().getItemCount();
                            selectedImages.clear();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                selectedImages.add(imageUri);
                            }
                        } else if (result.getData().getData() != null) {
                            // Single image selected
                            Uri imageUri = result.getData().getData();
                            selectedImages.clear();
                            selectedImages.add(imageUri);
                        }

                        // Update the ImageButton with the first selected image
                        if (!selectedImages.isEmpty()) {
                            uploadButton.setImageURI(selectedImages.get(0));
                            Toast.makeText(this, selectedImages.size() + " images selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

// Set the click listener for the upload button
        uploadButton.setOnClickListener(v -> openGalleryForMultipleImages());

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Courses");

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        courseIdInput = findViewById(R.id.courseIdInput);
        courseNameInput = findViewById(R.id.courseNameInput);
        semesterInput = findViewById(R.id.SemesterInput);
        addCourseButton = findViewById(R.id.addCourseButton);
        adminSignOutButton = findViewById(R.id.btnadminsignout);
        courseListView = findViewById(R.id.courseListView);

        // Initialize list and adapter
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(this, courseList);
        courseListView.setAdapter(courseAdapter);

        // Load all courses initially
        loadCoursesFromFirebase();

        // Add course
        addCourseButton.setOnClickListener(v -> {
            String courseId = courseIdInput.getText().toString().trim();
            String courseName = courseNameInput.getText().toString().trim();
            String semester = semesterInput.getText().toString().trim();

            if (courseId.isEmpty() || courseName.isEmpty() || semester.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Course course = new Course(courseName, courseId, semester);
            databaseReference.child(courseId).setValue(course)
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
        });

        // Add search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCourses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Admin sign-out
        adminSignOutButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isAdminLoggedIn", false);
            editor.apply();

            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    private void openGalleryForMultipleImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
    }

    private void loadCoursesFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    Course course = courseSnapshot.getValue(Course.class);
                    if (course != null) {
                        courseList.add(course);
                    }
                }
                courseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load courses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterCourses(String query) {
        ArrayList<Course> filteredList = new ArrayList<>();
        for (Course course : courseList) {
            if (course.getCourseCode().toLowerCase().contains(query.toLowerCase()) ||
                    course.getCourseName().toLowerCase().contains(query.toLowerCase()) ||
                    course.getSemester().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(course);
            }
        }
        courseAdapter = new CourseAdapter(AdminActivity.this, filteredList);
        courseListView.setAdapter(courseAdapter);
    }
}
