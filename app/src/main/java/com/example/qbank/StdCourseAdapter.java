package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class StdCourseAdapter extends ArrayAdapter<Course> {

    public StdCourseAdapter(Context context, List<Course> courses) {
        super(context, 0, courses);
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
        Button viewButton = convertView.findViewById(R.id.ViewButton);

        courseNameTextView.setText(course.getCourseName());
        courseCodeTextView.setText(course.getCourseCode());
        semesterTextView.setText(course.getSemester());

        // Set "View" button functionality
        viewButton.setOnClickListener(v -> showQuestionDialog(course.getImageKey()));

        return convertView;
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

        // Handle "Back" button click
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
