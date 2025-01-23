package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CourseAdapter extends ArrayAdapter<Course> {

    private DatabaseReference databaseReference;

    public CourseAdapter(Context context, List<Course> courses) {
        super(context, 0, courses);
        databaseReference = FirebaseDatabase.getInstance().getReference("Courses");
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

    private void showUpdateDialog(Course course) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialogue_update_course, null);

        EditText editCourseId = dialogView.findViewById(R.id.editCourseId);
        EditText editCourseName = dialogView.findViewById(R.id.editCourseName);
        EditText editSemester = dialogView.findViewById(R.id.SemesterInput);
        Button doneButton = dialogView.findViewById(R.id.doneButton);
        Button quitButton = dialogView.findViewById(R.id.quitButton);

        editCourseId.setText(course.getCourseCode());
        editCourseName.setText(course.getCourseName());
        editSemester.setText(course.getSemester());

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

            if (!course.getCourseCode().equals(newCourseId)) {
                databaseReference.child(course.getCourseCode()).removeValue();
            }

            Course updatedCourse = new Course(newCourseName, newCourseId, newSemester);
            databaseReference.child(newCourseId).setValue(updatedCourse)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Course updated successfully", Toast.LENGTH_SHORT).show();
                            course.setCourseCode(newCourseId);
                            course.setCourseName(newCourseName);
                            course.setSemester(newSemester);
                            notifyDataSetChanged();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update course", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        quitButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteDialog(Course course) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course?\n\n"
                        + "Course ID: " + course.getCourseCode() + "\n"
                        + "Course Name: " + course.getCourseName() + "\n"
                        + "Semester: " + course.getSemester())
                .setPositiveButton("Yes", (dialog, which) -> {
                    databaseReference.child(course.getCourseCode()).removeValue()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Course deleted successfully", Toast.LENGTH_SHORT).show();
                                    remove(course);
                                    notifyDataSetChanged();
                                } else {
                                    Toast.makeText(getContext(), "Failed to delete course", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }
}
