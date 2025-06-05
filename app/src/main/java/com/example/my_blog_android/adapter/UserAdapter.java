package com.example.my_blog_android.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast; // 导入 Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.model.User;
import com.example.my_blog_android.ui.UserProfileActivity;
import com.google.android.material.imageview.ShapeableImageView; // 导入 ShapeableImageView

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private Context context;

    // START: Added for long-press functionality
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, User user);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    // END: Added for long-press functionality

    public UserAdapter(List<User> users, Context context) {
        this.users = users;
        this.context = context;
    }

    public void updateData(List<User> newUsers) {
        this.users.clear();
        if (newUsers != null) { // 确保传入的列表不为 null
            this.users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    // START: Added for deletion
    /**
     * 从适配器中移除指定位置的用户项
     * @param position 要移除项的位置
     */
    public void removeItem(int position) {
        if (position >= 0 && position < users.size()) {
            users.remove(position);
            notifyItemRemoved(position);
        }
    }
    // END: Added for deletion

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvUsername.setText(user.getUsername());
        holder.tvUserId.setText("ID: " + user.getId());
        // 设置默认头像或从 URL 加载（如果用户有头像 URL）
        holder.ivAvatar.setImageResource(R.drawable.ic_person); // 假设 ic_person 是默认用户图标
        // TODO: 如果 User 模型中有头像 URL 字段，可以使用 Glide 等库加载
        // 例如：Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.ic_person).into(holder.ivAvatar);

        holder.itemView.setOnClickListener(v -> {
            // 点击用户项时跳转到用户主页
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_USER_ID, String.valueOf(user.getId()));
            context.startActivity(intent);
        });

        // START: Added for long-press functionality
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(position, user);
                return true; // Consume the long click
            }
            return false;
        });
        // END: Added for long-press functionality
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar; // 使用 ShapeableImageView 作为头像
        TextView tvUsername;
        TextView tvUserId;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar); // 假设 item_user.xml 中有 iv_user_avatar
            tvUsername = itemView.findViewById(R.id.tv_user_username); // 假设 item_user.xml 中有 tv_user_username
            tvUserId = itemView.findViewById(R.id.tv_user_id); // 假设 item_user.xml 中有 tv_user_id
        }
    }
}
