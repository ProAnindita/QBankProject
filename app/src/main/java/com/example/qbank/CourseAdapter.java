package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseAdapter extends ArrayAdapter<Course> {

    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/dp4ha5cws/image/upload";
    private static final String CLOUDINARY_UPLOAD_PRESET = "ml_default";

    private DatabaseReference databaseReference;
    private Context context;
    private Uri selectedImageUri;
    private ImageView currentImageView;

    private final ActivityResultLauncher<Intent> imagePickerLauncher;

    public CourseAdapter(Context context, List<Course> courses) {
        super(context, 0, courses);
        this.context = context;
        databaseReference = FirebaseDatabase.getInstance().getReference("Courses");

        // Initialize ActivityResultLauncher
        imagePickerLauncher = ((AppCompatActivity) context).registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (currentImageView != null && selectedImageUri != null) {
                            // Display the selected image in the ImageView
                            currentImageView.setImageURI(selectedImageUri);
                        }
                    }
                }
        );
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Course course = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_course, parent, false);
        }

        TextView courseNameTextView = convertView.findViewById(R.id.courseNameTextView);
        TextView courseCodeTextView = convertView.findViewById(R.id.courseCodeTextView);
        TextView semesterTextView = convertView.findViewById(R.id.tvsemester);
        Button updateButton = convertView.findViewById(R.id.updateButton);
        Button deleteButton = convertView.findViewById(R.id.deleteButton);

        courseNameTextView.setText(course.getCourseName());
        courseCodeTextView.setText(course.getCourseCode());
        semesterTextView.setText(course.getSemester());

        updateButton.setOnClickListener(v -> showUpdateDialog(course));
        deleteButton.setOnClickListener(v -> showDeleteDialog(course));

        return convertView;
    }

    private void showDeleteDialog(Course course) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course?\n\n"
                        + "Course ID: " + course.getCourseCode() + "\n"
                        + "Course Name: " + course.getCourseName() + "\n"
                        + "Semester: " + course.getSemester())
                .setPositiveButton("Yes", (dialog, which) -> {
                    DatabaseReference semesterRef = databaseReference.child(course.getCourseCode()).child(course.getSemester());
                    semesterRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Course deleted successfully!", Toast.LENGTH_SHORT).show();
                            remove(course);
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "Failed to delete course.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showUpdateDialog(Course course) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialogue_update_course, null);

        EditText editCourseId = dialogView.findViewById(R.id.editCourseId);
        EditText editCourseName = dialogView.findViewById(R.id.editCourseName);
        EditText editSemester = dialogView.findViewById(R.id.SemesterInput);
        ImageView questionImageView = dialogView.findViewById(R.id.Question);
        Button doneButton = dialogView.findViewById(R.id.doneButton);
        Button quitButton = dialogView.findViewById(R.id.quitButton);

        editCourseId.setText(course.getCourseCode());
        editCourseName.setText(course.getCourseName());
        editSemester.setText(course.getSemester());

        // Load the current image
        Glide.with(getContext())
                .load(course.getImageKey())
                .placeholder(R.drawable.ic_profile)
                .into(questionImageView);

        // Set the current ImageView for image selection
        questionImageView.setOnClickListener(v -> {
            currentImageView = questionImageView;
            openGalleryForImage();
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        doneButton.setOnClickListener(v -> {
            String newCourseId = editCourseId.getText().toString().trim();
            String newCourseName = editCourseName.getText().toString().trim();
            String newSemester = editSemester.getText().toString().trim();

            if (TextUtils.isEmpty(newCourseId) || TextUtils.isEmpty(newCourseName) || TextUtils.isEmpty(newSemester)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // If a new image is selected, upload it to Cloudinary first
            if (selectedImageUri != null) {
                uploadNewImageToCloudinary(selectedImageUri, newImageUrl -> {
                    // Set the updated image URL
                    course.setImageKey(newImageUrl);

                    // Check if the course ID has changed
                    if (!course.getCourseCode().equals(newCourseId)) {
                        // Create a new instance under the updated course ID
                        DatabaseReference newCourseRef = databaseReference.child(newCourseId).child(newSemester);
                        newCourseRef.child("courseName").setValue(newCourseName);
                        newCourseRef.child("imageKey").setValue(newImageUrl).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Delete the old course ID
                                DatabaseReference oldCourseRef = databaseReference.child(course.getCourseCode());
                                oldCourseRef.removeValue().addOnCompleteListener(deleteTask -> {
                                    if (deleteTask.isSuccessful()) {
                                        course.setCourseCode(newCourseId);
                                        course.setCourseName(newCourseName);
                                        course.setSemester(newSemester);
                                        notifyDataSetChanged();
                                        Toast.makeText(getContext(), "Course and image updated successfully!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(getContext(), "Failed to delete old course instance.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(getContext(), "Failed to create new course instance.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Update fields under the same course ID
                        DatabaseReference courseRef = databaseReference.child(course.getCourseCode()).child(newSemester);
                        courseRef.child("courseName").setValue(newCourseName);
                        courseRef.child("imageKey").setValue(newImageUrl).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                course.setCourseName(newCourseName);
                                course.setSemester(newSemester);
                                notifyDataSetChanged();
                                Toast.makeText(getContext(), "Course and image updated successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(getContext(), "Failed to update course details.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            } else {
                // Handle case where no new image is selected
                if (!course.getCourseCode().equals(newCourseId)) {
                    // Create a new instance under the updated course ID
                    DatabaseReference newCourseRef = databaseReference.child(newCourseId).child(newSemester);
                    newCourseRef.child("courseName").setValue(newCourseName);
                    newCourseRef.child("imageKey").setValue(course.getImageKey()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Delete the old course ID
                            DatabaseReference oldCourseRef = databaseReference.child(course.getCourseCode());
                            oldCourseRef.removeValue().addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    course.setCourseCode(newCourseId);
                                    course.setCourseName(newCourseName);
                                    course.setSemester(newSemester);
                                    notifyDataSetChanged();
                                    Toast.makeText(getContext(), "Course updated successfully!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(getContext(), "Failed to delete old course instance.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "Failed to create new course instance.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Update fields without changing the image
                    DatabaseReference courseRef = databaseReference.child(course.getCourseCode()).child(newSemester);
                    courseRef.child("courseName").setValue(newCourseName).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            course.setCourseName(newCourseName);
                            course.setSemester(newSemester);
                            notifyDataSetChanged();
                            Toast.makeText(getContext(), "Course updated successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update course details.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        quitButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openGalleryForImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void uploadNewImageToCloudinary(Uri selectedImageUri, OnImageUploadCompleteListener listener) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            Map<String, String> params = new HashMap<>();
            params.put("upload_preset", CLOUDINARY_UPLOAD_PRESET);
            params.put("file", "data:image/jpeg;base64," + base64Image);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    CLOUDINARY_UPLOAD_URL,
                    new JSONObject(params),
                    response -> {
                        try {
                            String newImageUrl = response.getString("secure_url");
                            listener.onSuccess(newImageUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing upload response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(getContext(), "Image upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            );

            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            requestQueue.add(request);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to process selected image.", Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnImageUploadCompleteListener {
        void onSuccess(String imageUrl);
    }
}
