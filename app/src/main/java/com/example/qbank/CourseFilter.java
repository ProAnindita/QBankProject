package com.example.qbank;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CourseFilter {

    public interface OnCoursesFilteredListener {
        void onCoursesFiltered(List<Course> filteredList);
    }

    // Method to load courses and filter by query
    public static void loadAndFilterCourses(String query, OnCoursesFilteredListener listener) {
        DatabaseReference coursesRef = FirebaseDatabase.getInstance().getReference("Courses");

        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Course> courseList = new ArrayList<>();

                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    String courseId = courseSnapshot.getKey();
                    for (DataSnapshot semesterSnapshot : courseSnapshot.getChildren()) {
                        // Ensure valid course data is present
                        if (semesterSnapshot.hasChild("courseName")) {
                            String semester = semesterSnapshot.getKey();
                            String courseName = semesterSnapshot.child("courseName").getValue(String.class);

                            // Only create and add valid course objects
                            if (courseName != null && courseId != null && semester != null) {
                                Course course = new Course();
                                course.setCourseCode(courseId);
                                course.setSemester(semester);
                                course.setCourseName(courseName);

                                courseList.add(course);
                            }
                        }
                    }
                }

                // Apply filtering logic
                List<Course> filteredCourses = new ArrayList<>();
                for (Course course : courseList) {
                    if ((course.getCourseName() != null && course.getCourseName().toLowerCase().contains(query.toLowerCase())) ||
                            (course.getCourseCode() != null && course.getCourseCode().toLowerCase().contains(query.toLowerCase()))) {
                        filteredCourses.add(course);
                    }
                }

                listener.onCoursesFiltered(filteredCourses); // Pass the filtered list back
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Notify listener of failure with an empty list
                listener.onCoursesFiltered(new ArrayList<>());
            }
        });
    }
}
