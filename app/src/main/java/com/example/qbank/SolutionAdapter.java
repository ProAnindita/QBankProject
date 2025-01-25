package com.example.qbank;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        // Set Course and Solution Details
        holder.CourseId.setText(solution.getCourseId());
        holder.CourseName.setText(solution.getCourseName());
        holder.CourseSem.setText(solution.getCourseSemester());
        holder.userEmail.setText(solution.getUploaderEmail());

        // Load solution image using Glide
        Glide.with(context)
                .load(solution.getImageUrl())
                .placeholder(R.drawable.ic_profile)
                .into(holder.solutionImage);

        // Fetch and load profile image from Firebase users table
        fetchProfileImage(solution.getUploaderEmail(), holder.profileImage);
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
                                .placeholder(R.drawable.ic_profile) // Default placeholder image
                                .into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors if needed
            }
        });
    }

    @Override
    public int getItemCount() {
        return solutionList.size();
    }

    public static class SolutionViewHolder extends RecyclerView.ViewHolder {
        TextView userEmail,  CourseId, CourseName, CourseSem;
        ImageView solutionImage, profileImage;

        public SolutionViewHolder(@NonNull View itemView) {
            super(itemView);
            CourseId = itemView.findViewById(R.id.CourseId);
            CourseName = itemView.findViewById(R.id.Coursename);
            CourseSem = itemView.findViewById(R.id.semester);
            userEmail = itemView.findViewById(R.id.userEmail);
            solutionImage = itemView.findViewById(R.id.solutionImage);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}
