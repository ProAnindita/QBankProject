package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class StdCourseAdapter extends ArrayAdapter<Course> {
    private ActivityResultLauncher<Intent> activityResultLauncher;
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
                                // Update the ImageView with the selected image URI
                                solutionImageView.setImageURI(selectedImageUri);
                            }
                        }
                    }
            );
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
        viewButton.setOnClickListener(v -> showQuestionDialog(course.getImageKey()));

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
                Toast.makeText(getContext(), "Image selected successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please Pick an image", Toast.LENGTH_SHORT).show();
            }
        });
        quitButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showQuestionDialog(String imageUrl) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_view_question, null);
        ImageView questionImageView = dialogView.findViewById(R.id.Question);
        Button downloadButton = dialogView.findViewById(R.id.downloadButton);
        Button quitButton = dialogView.findViewById(R.id.quitButton);

        // Load image using Glide
        Glide.with(getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .into(questionImageView);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Handle "Download" button click
        downloadButton.setOnClickListener(v -> downloadImage(imageUrl));
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
    }}
