package com.example.qbank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView courseListView;
    private Button adminSignOutButton, goToAddCourseButton;

    private List<Course> courseList;
    private CourseAdapter courseAdapter;

    private ActivityResultLauncher<Intent> activityResultLauncher;

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

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        courseListView = findViewById(R.id.courseListView);
        adminSignOutButton = findViewById(R.id.btnadminsignout);
        goToAddCourseButton = findViewById(R.id.btnGoToAddCourse);

        // Initialize list and adapter
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(this, courseList);
        courseListView.setAdapter(courseAdapter);

        // Initialize ActivityResultLauncher
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(AdminActivity.this, "Course added successfully", Toast.LENGTH_SHORT).show();
                        loadCoursesFromFirebase("");
                    }
                }
        );

        // Load all courses initially
        loadCoursesFromFirebase("");

        // Add search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCoursesFromFirebase(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Navigate to AddCourseActivity using ActivityResultLauncher
        goToAddCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AddCourseActivity.class);
            activityResultLauncher.launch(intent);
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

    private void loadCoursesFromFirebase(String query) {
        CourseFilter.loadAndFilterCourses(query, filteredCourses -> {
            courseList.clear();
            courseList.addAll(filteredCourses);
            courseAdapter.notifyDataSetChanged();
        });
    }
}
