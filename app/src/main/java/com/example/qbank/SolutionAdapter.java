package com.example.qbank;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;

public class SolutionAdapter extends RecyclerView.Adapter<SolutionAdapter.SolutionViewHolder> {
    private final Context context;
    private final List<Solution> solutionList;

    public SolutionAdapter(Context context, List<Solution> solutionList) {
        this.context = context;
        this.solutionList = solutionList;
    }

    @NonNull
    @Override
    public SolutionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_solution, parent, false);
        return new SolutionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SolutionViewHolder holder, int position) {
        Solution solution = solutionList.get(position);
        Log.d("SolutionAdapter", "Solution ID: " + solution.getSolutionId());

        // Set Course and Solution Details
        holder.CourseId.setText(solution.getCourseId());
        holder.CourseName.setText(solution.getCourseName());
        holder.CourseSem.setText(solution.getCourseSemester());
        holder.userEmail.setText(solution.getUploaderEmail());
        holder.rating.setText("Rating: " + solution.getAverageRating());

        // Load solution image using Glide
        Glide.with(context)
                .load(solution.getImageUrl())
                .placeholder(R.drawable.ic_profile)
                .into(holder.solutionImage);

        // Fetch and load profile image from Firebase users table
        fetchProfileImage(solution.getUploaderEmail(), holder.profileImage);

        holder.solutionImage.setOnClickListener(v -> showSolutionDialog(solution.getImageUrl(), solution.getCourseId(), solution.getCourseSemester(), solution.getSolutionId()));
    }

    private void showSolutionDialog(String imageUrl, String courseId, String semester, String solutionId) {
        if (solutionId == null || solutionId.isEmpty()) {
            Toast.makeText(context, "Invalid solution ID. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_solution_options, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Initialize dialog views
        Button downloadButton = dialogView.findViewById(R.id.downloadButton);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        Button postButton = dialogView.findViewById(R.id.rateDoneButton);

        // Set up the post button for submitting ratings
        postButton.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating > 0) { // Ensure a valid rating is provided
                saveRatingToFirebase(courseId, semester, solutionId, rating);
                Toast.makeText(context, "Rating submitted!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(context, "Please select a rating before posting.", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle "Download" button click
        downloadButton.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadImage(imageUrl);
            } else {
                Toast.makeText(context, "Image URL is not available.", Toast.LENGTH_SHORT).show();
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void saveRatingToFirebase(String courseId, String semester, String solutionId, float rating) {
        if (solutionId == null || solutionId.isEmpty()) {
            Toast.makeText(context, "Invalid solution ID. Cannot save rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("userEmail", "Unknown Email").replace(".", ",");

        DatabaseReference solutionRef = FirebaseDatabase.getInstance()
                .getReference("Solutions")
                .child(courseId)
                .child(semester)
                .child(solutionId);

        // Save the rating under the user's email
        solutionRef.child("ratings").child(userEmail).setValue(rating).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                recalculateAverageRating(solutionRef); // Recalculate the average rating
                Toast.makeText(context, "Rating saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to save rating.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recalculateAverageRating(DatabaseReference solutionRef) {
        solutionRef.child("ratings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float totalRatings = 0;
                int numberOfRatings = 0;

                for (DataSnapshot ratingSnapshot : snapshot.getChildren()) {
                    Float rating = ratingSnapshot.getValue(Float.class);
                    if (rating != null) {
                        totalRatings += rating;
                        numberOfRatings++;
                    }
                }

                // Calculate the average rating
                float averageRating = numberOfRatings > 0 ? totalRatings / numberOfRatings : 0;

                // Truncate to 1 decimal place
                float truncatedAverageRating = ((int) (averageRating * 10)) / 10.0f;

                // Save the truncated average rating
                solutionRef.child("averageRating").setValue(truncatedAverageRating);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to recalculate average rating.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImage(String imageUrl) {
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            String fileName = "SolutionImage_" + System.currentTimeMillis() + ".jpg";
                            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File file = new File(downloadsDir, fileName);

                            OutputStream outputStream = new FileOutputStream(file);
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.flush();
                            outputStream.close();

                            Toast.makeText(context, "Image downloaded to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Failed to download image.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private void fetchProfileImage(String email, ImageView profileImageView) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Query the users table for the given email
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String profileImageUrl = userSnapshot.child("imageKey").getValue(String.class);
                    if (profileImageUrl != null) {
                        // Load profile image into the ImageView using Glide
                        Glide.with(context)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SolutionAdapter", "Error fetching profile image: " + error.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return solutionList.size();
    }

    public static class SolutionViewHolder extends RecyclerView.ViewHolder {
        TextView userEmail, rating, CourseId, CourseName, CourseSem;
        ImageView solutionImage, profileImage;

        public SolutionViewHolder(@NonNull View itemView) {
            super(itemView);
            CourseId = itemView.findViewById(R.id.CourseId);
            CourseName = itemView.findViewById(R.id.Coursename);
            CourseSem = itemView.findViewById(R.id.semester);
            userEmail = itemView.findViewById(R.id.userEmail);
            rating = itemView.findViewById(R.id.rating);
            solutionImage = itemView.findViewById(R.id.solutionImage);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}
