package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StdCourseAdapter extends ArrayAdapter<Course> {
    private final ActivityResultLauncher<Intent> activityResultLauncher;
    private Uri selectedImageUri;
    private ImageView solutionImageView;

    public StdCourseAdapter(Context context, List<Course> courses) {
        super(context, 0, courses);

        // Register the ActivityResultLauncher inside the adapter
        if (context instanceof AppCompatActivity) {
            activityResultLauncher = ((AppCompatActivity) context).registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (solutionImageView != null) {
                                solutionImageView.setImageURI(selectedImageUri);
                            }
                        }
                    }
            );
        } else {
            throw new IllegalStateException("Context must be an instance of AppCompatActivity");
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Course course = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.std_course_view, parent, false);
        }

        TextView courseNameTextView = convertView.findViewById(R.id.courseNameTextView);
        TextView courseCodeTextView = convertView.findViewById(R.id.courseCodeTextView);
        TextView semesterTextView = convertView.findViewById(R.id.tvsemester);
        Button solButton = convertView.findViewById(R.id.sltnButton);
        Button viewButton = convertView.findViewById(R.id.ViewButton);

        courseNameTextView.setText(course.getCourseName());
        courseCodeTextView.setText(course.getCourseCode());
        semesterTextView.setText(course.getSemester());

        // Handle "Solution" button click
        solButton.setOnClickListener(v -> showSolutionDialog(course));
        viewButton.setOnClickListener(v -> showQuestionDialog(course.getCourseCode(), course.getSemester()));

        return convertView;
    }

    private void showSolutionDialog(Course course) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_solution, null);

        TextView courseIdTextView = dialogView.findViewById(R.id.sl_courseId);
        TextView courseNameTextView = dialogView.findViewById(R.id.sl_coursename);
        TextView semesterTextView = dialogView.findViewById(R.id.sl_courseSem);
        solutionImageView = dialogView.findViewById(R.id.up_solution);
        Button uploadSolutionButton = dialogView.findViewById(R.id.upSol);
        Button quitButton = dialogView.findViewById(R.id.quitButton);

        // Set course details
        courseIdTextView.setText(course.getCourseCode());
        courseNameTextView.setText(course.getCourseName());
        semesterTextView.setText(course.getSemester());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Handle image selection
        solutionImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            activityResultLauncher.launch(intent);
        });

        // Handle upload button click
        uploadSolutionButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageToCloudinary(course, dialog);
            } else {
                Toast.makeText(getContext(), "Please Pick an image", Toast.LENGTH_SHORT).show();
            }
        });
        quitButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void uploadImageToCloudinary(Course course, AlertDialog dialog) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            String base64Image = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);

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
                            saveImageUrlToFirebase(course, imageUrl);
                            dialog.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Image upload successful, but failed to save URL.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(getContext(), "Image upload failed.", Toast.LENGTH_SHORT).show()
            );

            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to process image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageUrlToFirebase(Course course, String imageUrl) {
        // Retrieve the uploader's email from SharedPreferences
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String uploaderEmail = sharedPreferences.getString("userEmail", "Unknown Email");

        // Reference to the Solutions node under the specific course and semester
        DatabaseReference solutionsRef = FirebaseDatabase.getInstance().getReference("Courses")
                .child(course.getCourseCode())
                .child(course.getSemester())
                .child("Solutions");

        // Create a new node for this solution
        DatabaseReference newSolutionRef = solutionsRef.push();

        // Create a map to store image URL and uploader email
        Map<String, String> solutionData = new HashMap<>();
        solutionData.put("imageUrl", imageUrl);
        solutionData.put("uploaderEmail", uploaderEmail);

        // Save the data in Firebase
        newSolutionRef.setValue(solutionData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Solution uploaded successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save solution in Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuestionDialog(String courseCode, String semester) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_view_question, null);
        ImageView questionImageView = dialogView.findViewById(R.id.Question);
        Button downloadButton = dialogView.findViewById(R.id.downloadButton);
        Button quitButton = dialogView.findViewById(R.id.quitButton);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("Courses")
                .child(courseCode)
                .child(semester);

        // Fetch imageKey from Firebase
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String imageKey = snapshot.child("imageKey").getValue(String.class);

                    if (imageKey != null) {
                        // Load the image using Glide
                        Glide.with(getContext())
                                .load(imageKey)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_error)
                                .into(questionImageView);
                        questionImageView.setTag(imageKey);
                    } else {
                        Toast.makeText(getContext(), "Image key is null or missing.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Data not found for this course and semester.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Handle "Download" button click
        downloadButton.setOnClickListener(v -> {
            String imageUrl = questionImageView.getTag() != null ? questionImageView.getTag().toString() : null;
            if (imageUrl != null) {
                downloadImage(imageUrl);
            } else {
                Toast.makeText(getContext(), "Image URL is not available.", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle "Quit" button click
        quitButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void downloadImage(String imageUrl) {
        Glide.with(getContext())
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            String fileName = "QuestionImage_" + System.currentTimeMillis() + ".jpg";
                            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File file = new File(downloadsDir, fileName);

                            OutputStream outputStream = new FileOutputStream(file);
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.flush();
                            outputStream.close();

                            Toast.makeText(getContext(), "Image downloaded to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Failed to download image.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle placeholder if needed
                    }
                });
    }
}
