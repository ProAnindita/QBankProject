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

        holder.userEmail.setText(solution.getUploaderEmail());
        holder.timestamp.setText("Submitted on: " + solution.getTimestamp());

        // Load solution image using Glide
        Glide.with(context)
                .load(solution.getImageUrl())
                .placeholder(R.drawable.ic_profile)
                .into(holder.solutionImage);
    }

    @Override
    public int getItemCount() {
        return solutionList.size();
    }

    public static class SolutionViewHolder extends RecyclerView.ViewHolder {
        TextView userEmail, timestamp;
        ImageView solutionImage;

        public SolutionViewHolder(@NonNull View itemView) {
            super(itemView);
            userEmail = itemView.findViewById(R.id.userEmail);
            timestamp = itemView.findViewById(R.id.timestamp);
            solutionImage = itemView.findViewById(R.id.solutionImage);
        }
    }
}
