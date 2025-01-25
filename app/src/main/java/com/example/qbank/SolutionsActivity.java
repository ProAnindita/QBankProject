package com.example.qbank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SolutionsActivity extends AppCompatActivity {

    private RecyclerView solutionsRecyclerView;
    private EditText searchSolutionsInput;
    private SolutionAdapter solutionAdapter;
    private List<Solution> solutionList;
    private List<Solution> filteredSolutionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solutions);

        // Initialize views
        solutionsRecyclerView = findViewById(R.id.solutionsRecyclerView);
        searchSolutionsInput = findViewById(R.id.searchSolutionsInput);

        // Initialize lists
        solutionList = new ArrayList<>();
        filteredSolutionList = new ArrayList<>();

        // Set up RecyclerView and Adapter
        solutionAdapter = new SolutionAdapter(this, filteredSolutionList); // Use filteredSolutionList for display
        solutionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        solutionsRecyclerView.setAdapter(solutionAdapter);

        // Load solutions from Firebase
        loadSolutionsFromFirebase();

        // Add search functionality
        searchSolutionsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSolutionsByCourse(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSolutionsFromFirebase() {
        DatabaseReference coursesRef = FirebaseDatabase.getInstance().getReference("Courses");

        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                solutionList.clear();
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    String courseId = courseSnapshot.getKey();
                    for (DataSnapshot semesterSnapshot : courseSnapshot.getChildren()) {
                        String courseSemester = semesterSnapshot.getKey();
                        String courseName = semesterSnapshot.child("courseName").getValue(String.class);

                        for (DataSnapshot solutionSnapshot : semesterSnapshot.child("Solutions").getChildren()) {
                            Solution solution = solutionSnapshot.getValue(Solution.class);
                            if (solution != null) {
                                solution.setCourseId(courseId);
                                solution.setCourseSemester(courseSemester);
                                solution.setCourseName(courseName);

                                // Set the solutionId from the snapshot key
                                solution.setSolutionId(solutionSnapshot.getKey());
                                solutionList.add(solution);
                            }
                        }
                    }
                }

                filterSolutionsByCourse(""); // Display all solutions initially
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SolutionsActivity.this, "Failed to load solutions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterSolutionsByCourse(String query) {
        filteredSolutionList.clear(); // Clear the filtered list

        if (query.isEmpty()) {
            filteredSolutionList.addAll(solutionList); // Show all solutions if the query is empty
        } else {
            for (Solution solution : solutionList) {
                if ((solution.getCourseName() != null && solution.getCourseName().toLowerCase().contains(query.toLowerCase())) ||
                        (solution.getCourseId() != null && solution.getCourseId().toLowerCase().contains(query.toLowerCase()))) {
                    filteredSolutionList.add(solution); // Add matching solutions to the filtered list
                }
            }
        }

        solutionAdapter.notifyDataSetChanged(); // Notify adapter of data changes
    }
    @Override
    public void onBackPressed() {
        // Navigate back to HomeActivity
        super.onBackPressed();
        Intent intent = new Intent(SolutionsActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Optional: Ensures the current activity is removed from the stack
    }

}