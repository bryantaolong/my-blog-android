package com.example.my_blog_android.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.my_blog_android.R;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.ui.PhotoDetailActivity;
import com.example.my_blog_android.utils.ApiClient;

import java.util.List;

// 独立的 PhotoAdapter 类
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private List<Photo> photos;

    // START: Added for long-press functionality
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, Photo photo);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    // END: Added for long-press functionality

    public PhotoAdapter(List<Photo> photos) {
        this.photos = photos;
    }

    public void updateData(List<Photo> newPhotos) {
        this.photos.clear();
        this.photos.addAll(newPhotos);
        notifyDataSetChanged();
    }

    // START: Added for deletion
    public void removeItem(int position) {
        if (position >= 0 && position < photos.size()) {
            photos.remove(position);
            notifyItemRemoved(position);
        }
    }
    // END: Added for deletion

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Photo photo = photos.get(position);
        holder.tvName.setText(photo.getName());
        // Display author name if available, otherwise display author ID
        if (photo.getAuthorName() != null && !photo.getAuthorName().isEmpty()) {
            holder.tvAuthor.setText("作者: " + photo.getAuthorName());
        } else {
            holder.tvAuthor.setText("作者ID: " + photo.getAuthorId());
        }

        // Correct image URL splicing logic
        String filePathFromDb = photo.getFilePath(); // e.g., "uploads/1/image.jpg"
        String relativePath = filePathFromDb;

        // Check and remove "uploads/" prefix if it exists
        if (filePathFromDb != null && filePathFromDb.startsWith("uploads/")) {
            relativePath = filePathFromDb.substring("uploads/".length());
        }

        // Concatenate the final image URL
        String imageUrl = ApiClient.BASE_URL + "files/" + relativePath;

        // Log the constructed URL for debugging
        Log.d("PhotoAdapter", "Loading image from URL: " + imageUrl);

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.ivPhoto);

        // Set click listener for the photo item to navigate to the photo detail page
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the ID of the clicked photo
                long clickedPhotoId = photo.getId();
                // Create an Intent to navigate to PhotoDetailActivity
                Intent intent = new Intent(v.getContext(), PhotoDetailActivity.class);
                // Pass the photo ID to the detail page
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, clickedPhotoId);
                v.getContext().startActivity(intent);
            }
        });

        // START: Added for long-press functionality
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(position, photo);
                return true; // Consume the long click
            }
            return false;
        });
        // END: Added for long-press functionality
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    // ViewHolder 类也从 MainActivity 中移到这里
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvAuthor;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvName = itemView.findViewById(R.id.tv_photo_name);
            tvAuthor = itemView.findViewById(R.id.tv_photo_author);
        }
    }
}
