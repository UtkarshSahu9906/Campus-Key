package com.utkarshsahu.CampusKey;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ThanksAdapter extends RecyclerView.Adapter<ThanksAdapter.ViewHolder> {

    private List<ThanksMember> thanksList;
    private Context context;

    public ThanksAdapter(List<ThanksMember> thanksList, Context context) {
        this.thanksList = thanksList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thanks, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThanksMember member = thanksList.get(position);
        holder.nameTv.setText(member.getName());
        holder.tagTv.setText(member.getTagText());

        // Load image from URL using Glide
        Glide.with(context)
                .load(member.getImageUrl())
                .placeholder(R.drawable.man) // Add a default image in drawable
                .into(holder.profileImg);

        // Open the "Another URL" when the item is clicked
        if (member.getSocialUrl() != null && !member.getSocialUrl().isEmpty()) {
            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(member.getSocialUrl()));
                    context.startActivity(intent);
                } catch (Exception e) {}
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return thanksList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImg;
        TextView nameTv;
        TextView tagTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImg = itemView.findViewById(R.id.imgPromoter);
            nameTv = itemView.findViewById(R.id.tvPromoterName);
            tagTv = itemView.findViewById(R.id.tvInstagramTag);
        }
    }
}
