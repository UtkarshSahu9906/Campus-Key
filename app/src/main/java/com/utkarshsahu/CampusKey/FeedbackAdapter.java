package com.utkarshsahu.CampusKey;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {

    private final List<FeedbackModel> feedbackList;

    public FeedbackAdapter(List<FeedbackModel> feedbackList) {
        this.feedbackList = feedbackList;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        FeedbackModel feedback = feedbackList.get(position);
        String name = feedback.getUsername();
        if (name == null || name.trim().isEmpty()) name = "Anonymous";
        holder.tvUsername.setText(name);
        
        holder.tvComment.setText(feedback.getComment());
        holder.tvDate.setText(feedback.getRelativeDate());
        
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < feedback.getRating(); i++) stars.append("⭐");
        holder.tvRating.setText(stars.toString());
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvRating, tvComment, tvDate;

        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvFeedbackUsername);
            tvRating = itemView.findViewById(R.id.tvFeedbackRating);
            tvComment = itemView.findViewById(R.id.tvFeedbackComment);
            tvDate = itemView.findViewById(R.id.tvFeedbackDate);
        }
    }
}
