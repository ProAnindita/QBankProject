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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView courseListView;
    private Button adminSignOutButton, goToAddCourseButton;

    private DatabaseReference databaseReference;
    private ArrayList<Course> courseList;
    private CourseAdapter courseAdapter;

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

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Courses");

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        courseListView = findViewById(R.id.courseListView);
        adminSignOutButton = findViewById(R.id.btnadminsignout);
        goToAddCourseButton = findViewById(R.id.btnGoToAddCourse);

        // Initialize list and adapter
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(this, courseList);
        courseListView.setAdapter(courseAdapter);

        // Load all courses initially
        loadCoursesFromFirebase();

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

        // Navigate to AddCourseActivity
        goToAddCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AddCourseActivity.class);
            startActivity(intent);
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

    private void loadCoursesFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    String courseId = courseSnapshot.getKey();
                    for (DataSnapshot semesterSnapshot : courseSnapshot.getChildren()) {
                        String semester = semesterSnapshot.getKey();
                        Course course = semesterSnapshot.getValue(Course.class);
                        if (course != null) {
                            course.setCourseCode(courseId);
                            course.setSemester(semester);
                            courseList.add(course);
                        }
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
        ArrayList<Course> filteredCourses = new ArrayList<>();

        for (Course course : courseList) {
            if (course.getCourseName().toLowerCase().contains(query.toLowerCase()) ||
                    course.getCourseCode().toLowerCase().contains(query.toLowerCase())) {
                filteredCourses.add(course);
            }
        }

        // Update the adapter with the filtered list
        courseAdapter = new CourseAdapter(this, filteredCourses);
        courseListView.setAdapter(courseAdapter);
        courseAdapter.notifyDataSetChanged();
    }

}
