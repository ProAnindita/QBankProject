package com.example.qbank;

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

        // Initialize lists and adapter
        solutionList = new ArrayList<>();
        filteredSolutionList = new ArrayList<>();

        solutionAdapter = new SolutionAdapter(this, filteredSolutionList);
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
                filterSolutionsByEmail(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSolutionsFromFirebase() {
        DatabaseReference solutionsRef = FirebaseDatabase.getInstance().getReference("Courses");

        solutionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                solutionList.clear();
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot semesterSnapshot : courseSnapshot.getChildren()) {
                        DataSnapshot solutionsNode = semesterSnapshot.child("Solutions");
                        for (DataSnapshot solutionSnapshot : solutionsNode.getChildren()) {
                            Solution solution = solutionSnapshot.getValue(Solution.class);
                            if (solution != null) {
                                solutionList.add(solution);
                            }
                        }
                    }
                }
                filteredSolutionList.clear();
                filteredSolutionList.addAll(solutionList);
                solutionAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SolutionsActivity.this, "Failed to load solutions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterSolutionsByEmail(String email) {
        filteredSolutionList.clear();
        for (Solution solution : solutionList) {
            if (solution.getUploaderEmail().toLowerCase().contains(email.toLowerCase())) {
                filteredSolutionList.add(solution);
            }
        }
        solutionAdapter.notifyDataSetChanged();
    }
}
