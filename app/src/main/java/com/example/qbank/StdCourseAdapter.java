package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
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
        Button solButton = convertView.findViewById(R.id.sltnButton);

        courseNameTextView.setText(course.getCourseName());
        courseCodeTextView.setText(course.getCourseCode());
        semesterTextView.setText(course.getSemester());

        // Set "View" button functionality
        viewButton.setOnClickListener(v -> showQuestionDialog(course.getImageKey()));
        solButton.setOnClickListener(v -> showSolutionDialog(course));

        return convertView;
    }

    private void showSolutionDialog(Course course) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_solution, null);

        // Get references to TextViews in the solution dialog
        TextView courseIdTextView = dialogView.findViewById(R.id.sl_courseId);
        TextView courseNameTextView = dialogView.findViewById(R.id.sl_coursename);
        TextView semesterTextView = dialogView.findViewById(R.id.sl_courseSem);

        // Set the respective course information in the dialog
        courseIdTextView.setText(course.getCourseCode());
        courseNameTextView.setText(course.getCourseName());
        semesterTextView.setText(course.getSemester());

        // Create and display the dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Handle quit button in the dialog
        Button quitButton = dialogView.findViewById(R.id.quitButton);
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

        // Handle "Back" button click
        quitButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void downloadImage(String imageUrl) {
        // The download functionality remains unchanged
        Toast.makeText(getContext(), "Download functionality", Toast.LENGTH_SHORT).show();
    }
}
