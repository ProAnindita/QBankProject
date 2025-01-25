package com.example.qbank;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private Button btnSignOut;
    private Button gotoSol;
    private EditText etStdSearch;
    private ListView stdCourseListView;
    private ImageView stdProfileImageView;
    private TextView stdEmail;

    private DatabaseReference coursesReference;
    private DatabaseReference usersReference;
    private ArrayList<Course> courseList;
    private List<Course> filteredCourseList = new ArrayList<>();

    private StdCourseAdapter courseAdapter;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Views
        btnSignOut = findViewById(R.id.buttonSignOut);
        etStdSearch = findViewById(R.id.et_std_search);
        gotoSol= findViewById(R.id.btnGoToSltn);
        // Add this inside the onCreate method, after initializing etStdSearch
        etStdSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCourses(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });



// Add this method to handle course filtering

        stdCourseListView = findViewById(R.id.stdcourseListView);
        TextView stdEmailTextView = findViewById(R.id.std_email);
        stdProfileImageView = findViewById(R.id.stdprofile);

        // Retrieve the logged-in user's email
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("userEmail", "Unknown Email");
        stdEmailTextView.setText(userEmail);

        // Initialize Firebase References
        coursesReference = FirebaseDatabase.getInstance().getReference("Courses");
        usersReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize ArrayList and Adapter
        courseList = new ArrayList<>();
        courseAdapter = new StdCourseAdapter(this, courseList);
        stdCourseListView.setAdapter(courseAdapter);

        // Load Profile Picture from Firebase
        loadUserProfilePicture(userEmail);

        // Load Courses from Firebase
        loadCourses();

        // Handle Profile Picture Click
        stdProfileImageView.setOnClickListener(v -> openProfileUploadDialog(userEmail));

        gotoSol.setOnClickListener(v->{
            Intent intent = new Intent(HomeActivity.this, SolutionsActivity.class);
            startActivity(intent);
            finish();
        });
        // Handle Sign Out
        btnSignOut.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.remove("userEmail");
            editor.apply();

            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfilePicture(String userEmail) {
        usersReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String imageKey = userSnapshot.child("imageKey").getValue(String.class);
                    if (imageKey != null) {
                        Glide.with(HomeActivity.this).load(imageKey).placeholder(R.drawable.ic_profile).into(stdProfileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load profile picture.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void filterCourses(String query) {
    // Use the CourseFilter class to handle filtering logic
    CourseFilter.loadAndFilterCourses(query, filteredList -> {
        courseList.clear();
        courseList.addAll(filteredList);
        courseAdapter.notifyDataSetChanged(); // Notify the adapter to refresh the ListView
    });
}





    private void loadCourses() {
        coursesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear(); // Clear the current list
                for (DataSnapshot courseIdSnapshot : snapshot.getChildren()) {
                    String courseId = courseIdSnapshot.getKey();
                    for (DataSnapshot semesterSnapshot : courseIdSnapshot.getChildren()) {
                        // Ensure valid course data is present
                        if (semesterSnapshot.hasChild("courseName")) {
                            String courseName = semesterSnapshot.child("courseName").getValue(String.class);
                            String semester = semesterSnapshot.getKey();

                            Course course = new Course();
                            course.setCourseCode(courseId);
                            course.setCourseName(courseName);
                            course.setSemester(semester);

                            courseList.add(course);
                        }
                    }
                }
                courseAdapter.notifyDataSetChanged(); // Notify adapter of data changes
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load courses.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void openProfileUploadDialog(String userEmail) {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.std_upload_profile, null);
        ImageView dialogProfileImageView = dialogView.findViewById(R.id.profile); // Dialog box profile image view
        Button doneButton = dialogView.findViewById(R.id.done);

        // Load the current profile picture from Firebase into the dialog's ImageView
        usersReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String imageKey = userSnapshot.child("imageKey").getValue(String.class);
                    if (imageKey != null) {
                        Glide.with(HomeActivity.this).load(imageKey).placeholder(R.drawable.ic_profile).into(dialogProfileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load profile picture.", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Open Gallery to Select Image
        dialogProfileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
        });

        // Handle Done Button Click
        doneButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                // Show the loading bar
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Uploading image...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // Upload the selected image to Cloudinary
                uploadProfileImageToCloudinary(selectedImageUri, userEmail, usersReference, progressDialog, dialog);
            } else {
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        // Store reference to the dialog's ImageView for updating later
        this.dialogProfileImageView = dialogProfileImageView;
    }

    // Add a field for the dialog's ImageView
    private ImageView dialogProfileImageView;

    // Update onActivityResult to set the image on the dialog's ImageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Update the dialog's ImageView with the selected image
            if (dialogProfileImageView != null) {
                dialogProfileImageView.setImageURI(selectedImageUri);
            }
        }
    }

    private void uploadProfileImageToCloudinary(Uri selectedImageUri, String userEmail, DatabaseReference usersReference, ProgressDialog progressDialog, AlertDialog dialog) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            Map<String, String> params = new HashMap<>();
            params.put("upload_preset", "ml_default");
            params.put("file", "data:image/jpeg;base64," + base64Image);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    "https://api.cloudinary.com/v1_1/dp4ha5cws/image/upload",
                    new JSONObject(params),
                    response -> {
                        try {
                            String imageUrl = response.getString("secure_url");

                            // Update Firebase with the new imageKey
                            usersReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                        userSnapshot.getRef().child("imageKey").setValue(imageUrl)
                                                .addOnCompleteListener(task -> {
                                                    progressDialog.dismiss(); // Hide progress dialog
                                                    if (task.isSuccessful()) {
                                                        // Update the HomeActivity profile picture
                                                        Glide.with(HomeActivity.this).load(imageUrl).into(stdProfileImageView);
                                                        Toast.makeText(HomeActivity.this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                                                        dialog.dismiss(); // Close the dialog
                                                    } else {
                                                        Toast.makeText(HomeActivity.this, "Failed to update profile picture in Firebase.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    progressDialog.dismiss();
                                    Toast.makeText(HomeActivity.this, "Failed to update Firebase.", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } catch (Exception e) {
                            progressDialog.dismiss();
                            e.printStackTrace();
                            Toast.makeText(this, "Error uploading image.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Image upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            );

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(request);

        } catch (IOException e) {
            progressDialog.dismiss();
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
