package com.example.my_blog_android.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent; // 导入 Intent
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.api.PhotoCommentService;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.PhotoComment;
import com.example.my_blog_android.ui.UserProfileActivity;
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoCommentAdapter extends RecyclerView.Adapter<PhotoCommentAdapter.CommentViewHolder> {

    private List<PhotoComment> comments;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private PhotoCommentService commentService;
    private SessionManager sessionManager;

    public interface OnCommentDeletedListener {
        void onCommentDeleted();
    }

    private OnCommentDeletedListener onCommentDeletedListener;

    public void setOnCommentDeletedListener(OnCommentDeletedListener listener) {
        this.onCommentDeletedListener = listener;
    }

    public PhotoCommentAdapter(List<PhotoComment> comments, Context context) {
        this.comments = comments;
        this.context = context;
        this.commentService = ApiClient.getPhotoCommentService();
        this.sessionManager = SessionManager.getInstance(context);
    }

    public void updateComments(List<PhotoComment> newComments) {
        this.comments.clear();
        this.comments.addAll(newComments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        PhotoComment comment = comments.get(position);

        holder.tvAuthorName.setText(comment.getAuthorName() != null && !comment.getAuthorName().isEmpty() ? comment.getAuthorName() : "未知用户");
        holder.tvContent.setText(comment.getContent());
        if (comment.getCreateTime() != null) {
            holder.tvTime.setText(dateFormat.format(comment.getCreateTime()));
        } else {
            holder.tvTime.setText("N/A");
        }

        // 使评论作者名可点击，跳转到用户主页
        holder.tvAuthorName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (comment.getAuthorId() != null && !comment.getAuthorId().isEmpty()) {
                    Intent intent = new Intent(context, UserProfileActivity.class);
                    intent.putExtra(UserProfileActivity.EXTRA_USER_ID, comment.getAuthorId());
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "无法获取评论者信息", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // --- 评论删除权限判断 ---
        String currentUserId = sessionManager.getCurrentUserId();
        String currentUserRole = sessionManager.getCurrentUserRole();
        String commentAuthorId = comment.getAuthorId();

        boolean isCommentAuthor = currentUserId != null && currentUserId.equals(commentAuthorId);
        boolean isAdmin = "ADMIN".equals(currentUserRole);

        if (isCommentAuthor || isAdmin) {
            holder.btnDeleteComment.setVisibility(View.VISIBLE);
            holder.btnDeleteComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmationDialog(comment.getId(), holder.getAdapterPosition());
                }
            });
        } else {
            holder.btnDeleteComment.setVisibility(View.GONE);
            holder.btnDeleteComment.setOnClickListener(null);
        }
        // --- 评论删除权限判断结束 ---
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private void showDeleteConfirmationDialog(Long commentId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("删除评论")
                .setMessage("您确定要删除这条评论吗？此操作不可撤销。")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteComment(commentId, position);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteComment(Long commentId, int position) {
        commentService.deleteComment(commentId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(context, "评论删除成功", Toast.LENGTH_SHORT).show();
                        comments.remove(position);
                        notifyItemRemoved(position);
                        if (onCommentDeletedListener != null) {
                            onCommentDeletedListener.onCommentDeleted();
                        }
                    } else {
                        Toast.makeText(context, "评论删除失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "评论删除请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                Toast.makeText(context, "网络错误，删除评论失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvContent, tvTime;
        Button btnDeleteComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tv_comment_author_name);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            btnDeleteComment = itemView.findViewById(R.id.btn_delete_comment);
        }
    }
}
