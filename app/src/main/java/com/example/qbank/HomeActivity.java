package com.example.qbank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class HomeActivity extends AppCompatActivity {

    private Button btnSignOut;
    private EditText etStdSearch;
    private ListView stdCourseListView;

    private DatabaseReference databaseReference;
    private ArrayList<Course> courseList;
    private StdCourseAdapter courseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Views
        btnSignOut = findViewById(R.id.buttonSignOut);
        etStdSearch = findViewById(R.id.et_std_search);
        stdCourseListView = findViewById(R.id.stdcourseListView);

        // Initialize Firebase Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Courses");

        // Initialize ArrayList and Adapter
        courseList = new ArrayList<>();
        courseAdapter = new StdCourseAdapter(this, courseList);
        stdCourseListView.setAdapter(courseAdapter);

        // Load Courses from Firebase
        loadCourses();

        // Handle Sign Out
        btnSignOut.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadCourses() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot courseIdSnapshot : snapshot.getChildren()) {
                    String courseId = courseIdSnapshot.getKey();
                    for (DataSnapshot semesterSnapshot : courseIdSnapshot.getChildren()) {
                        Course course = semesterSnapshot.getValue(Course.class);
                        if (course != null) {
                            course.setCourseCode(courseId);
                            course.setSemester(semesterSnapshot.getKey());
                            courseList.add(course);
                        }
                    }
                }
                courseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load courses.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
