package com.example.my_blog_android.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.PhotoCommentAdapter;
import com.example.my_blog_android.api.PhotoCommentService;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.api.UserFavoriteService;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.model.PhotoComment;
import com.example.my_blog_android.model.UserFavorite;
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoDetailActivity extends AppCompatActivity implements PhotoCommentAdapter.OnCommentDeletedListener {

    public static final String EXTRA_PHOTO_ID = "photo_id";
    private static final int REQUEST_CODE_EDIT_PHOTO = 2;

    private ImageView ivPhoto;
    private TextView tvName;
    private TextView tvDescription;
    private TextView tvAuthor;
    private TextView tvTime;
    private ProgressBar progressBar;
    private Button btnEditPhoto;
    private Button btnDeletePhoto;
    private ImageView ivFavoritePhoto;

    private RecyclerView rvComments;
    private EditText etCommentInput;
    private Button btnPublishComment;
    private ProgressBar commentProgressBar;

    private PhotoService photoService;
    private PhotoCommentService photoCommentService;
    private UserFavoriteService userFavoriteService;
    private SessionManager sessionManager;

    private Photo currentPhoto;
    private PhotoCommentAdapter commentAdapter;
    private boolean isFavorited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ivPhoto = findViewById(R.id.iv_detail_photo);
        tvName = findViewById(R.id.tv_detail_photo_name);
        tvDescription = findViewById(R.id.tv_detail_photo_description);
        tvAuthor = findViewById(R.id.tv_detail_photo_author);
        tvTime = findViewById(R.id.tv_detail_photo_time);
        progressBar = findViewById(R.id.detail_photo_progress_bar);
        btnEditPhoto = findViewById(R.id.btn_edit_photo);
        btnDeletePhoto = findViewById(R.id.btn_delete_photo);
        ivFavoritePhoto = findViewById(R.id.iv_favorite_photo);

        rvComments = findViewById(R.id.rv_comments);
        etCommentInput = findViewById(R.id.et_comment_input);
        btnPublishComment = findViewById(R.id.btn_publish_comment);
        commentProgressBar = findViewById(R.id.comment_progress_bar);

        photoService = ApiClient.getPhotoService();
        photoCommentService = ApiClient.getPhotoCommentService();
        userFavoriteService = ApiClient.getUserFavoriteService();
        sessionManager = SessionManager.getInstance(this);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new PhotoCommentAdapter(new ArrayList<>(), this);
        commentAdapter.setOnCommentDeletedListener(this);
        rvComments.setAdapter(commentAdapter);

        long photoId = getIntent().getLongExtra(EXTRA_PHOTO_ID, -1);

        if (photoId != -1) {
            loadPhotoDetail(photoId);
            loadPhotoComments(photoId);
        } else {
            Toast.makeText(this, "图片ID无效，无法加载详情", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEditPhoto.setOnClickListener(v -> {
            if (currentPhoto != null) {
                Intent intent = new Intent(PhotoDetailActivity.this, EditPhotoActivity.class);
                intent.putExtra(EditPhotoActivity.EXTRA_PHOTO, currentPhoto);
                startActivityForResult(intent, REQUEST_CODE_EDIT_PHOTO);
            } else {
                Toast.makeText(PhotoDetailActivity.this, "图片数据未加载，无法编辑", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeletePhoto.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnPublishComment.setOnClickListener(v -> publishPhotoComment());

        tvAuthor.setOnClickListener(v -> {
            if (currentPhoto != null && currentPhoto.getAuthorId() != null) {
                Intent intent = new Intent(PhotoDetailActivity.this, UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.EXTRA_USER_ID, currentPhoto.getAuthorId());
                startActivity(intent);
            } else {
                Toast.makeText(PhotoDetailActivity.this, "无法获取作者信息", Toast.LENGTH_SHORT).show();
            }
        });

        ivFavoritePhoto.setOnClickListener(v -> {
            String currentUserId = sessionManager.getCurrentUserId();
            if (currentUserId == null) {
                Toast.makeText(PhotoDetailActivity.this, "请先登录才能收藏", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentPhoto == null || currentPhoto.getId() == null) {
                Toast.makeText(PhotoDetailActivity.this, "图片信息未加载", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFavorited) {
                // 修改这里：调用 removeFavorite 时传递单独的参数
                removeFavorite(currentUserId, "PHOTO", String.valueOf(currentPhoto.getId()));
            } else {
                addFavorite(Long.parseLong(currentUserId), "PHOTO", currentPhoto.getId());
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        ivPhoto.setVisibility(show ? View.GONE : View.VISIBLE);
        tvName.setVisibility(show ? View.GONE : View.VISIBLE);
        tvDescription.setVisibility(show ? View.GONE : View.VISIBLE);
        tvAuthor.setVisibility(show ? View.GONE : View.VISIBLE);
        tvTime.setVisibility(show ? View.GONE : View.VISIBLE);
        btnEditPhoto.setVisibility(View.GONE);
        btnDeletePhoto.setVisibility(View.GONE);
        ivFavoritePhoto.setVisibility(show ? View.GONE : View.VISIBLE);

        etCommentInput.setVisibility(show ? View.GONE : View.VISIBLE);
        btnPublishComment.setVisibility(show ? View.GONE : View.VISIBLE);
        rvComments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showCommentLoading(boolean show) {
        commentProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvComments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void loadPhotoDetail(long id) {
        showLoading(true);

        photoService.getPhotoById(id).enqueue(new Callback<BaseResponse<Photo>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Photo>> call, @NonNull Response<BaseResponse<Photo>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Photo> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        currentPhoto = baseResponse.getData();

                        String filePathFromDb = currentPhoto.getFilePath();
                        String relativePath = filePathFromDb;
                        if (filePathFromDb != null && filePathFromDb.startsWith("uploads/")) {
                            relativePath = filePathFromDb.substring("uploads/".length());
                        }
                        String imageUrl = ApiClient.BASE_URL + "files/" + relativePath;

                        Glide.with(PhotoDetailActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image)
                                .into(ivPhoto);

                        tvName.setText(currentPhoto.getName());
                        tvDescription.setText(currentPhoto.getDescription() != null && !currentPhoto.getDescription().isEmpty() ? currentPhoto.getDescription() : "无描述");

                        if (currentPhoto.getAuthorName() != null && !currentPhoto.getAuthorName().isEmpty()) {
                            tvAuthor.setText("作者: " + currentPhoto.getAuthorName());
                        } else {
                            tvAuthor.setText("作者ID: " + currentPhoto.getAuthorId());
                        }

                        if (currentPhoto.getCreateTime() != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            tvTime.setText("发布时间: " + dateFormat.format(currentPhoto.getCreateTime()));
                        } else {
                            tvTime.setText("发布时间: N/A");
                        }

                        String currentUserId = sessionManager.getCurrentUserId();
                        String currentUserRole = sessionManager.getCurrentUserRole();
                        String photoAuthorId = String.valueOf(currentPhoto.getAuthorId());

                        Log.d("PhotoPermissionDebug", "Photo ID: " + currentPhoto.getId() + ", Author ID: " + photoAuthorId);
                        Log.d("PhotoPermissionDebug", "Current User ID: " + currentUserId + ", Current User Role: " + currentUserRole);

                        boolean isAuthor = currentUserId != null && currentUserId.equals(photoAuthorId);
                        boolean isAdmin = "ADMIN".equals(currentUserRole);

                        if (isAuthor) {
                            btnEditPhoto.setVisibility(View.VISIBLE);
                            btnDeletePhoto.setVisibility(View.VISIBLE);
                        } else if (isAdmin) {
                            btnEditPhoto.setVisibility(View.GONE);
                            btnDeletePhoto.setVisibility(View.VISIBLE);
                        } else {
                            btnEditPhoto.setVisibility(View.GONE);
                            btnDeletePhoto.setVisibility(View.GONE);
                        }

                        if (currentUserId != null) {
                            checkFavoriteStatus(Long.parseLong(currentUserId), "PHOTO", currentPhoto.getId());
                        } else {
                            ivFavoritePhoto.setImageResource(R.drawable.ic_favorite_border);
                        }

                    } else {
                        Toast.makeText(PhotoDetailActivity.this, "加载图片详情失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("PhotoDetailActivity", "Load failed: " + baseResponse.getMsg());
                        finish();
                    }
                } else {
                    String errorMsg = "加载图片详情请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(PhotoDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("PhotoDetailActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Photo>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(PhotoDetailActivity.this, "网络错误，无法加载图片详情: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PhotoDetailActivity", "Network Error: ", t);
                finish();
            }
        });
    }

    private void loadPhotoComments(long photoId) {
        showCommentLoading(true);
        photoCommentService.getCommentsByPhoto(photoId).enqueue(new Callback<BaseResponse<List<PhotoComment>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<PhotoComment>>> call, @NonNull Response<BaseResponse<List<PhotoComment>>> response) {
                showCommentLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<PhotoComment>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        commentAdapter.updateComments(baseResponse.getData());
                    } else {
                        Toast.makeText(PhotoDetailActivity.this, "加载评论失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("PhotoDetailActivity", "Load comments failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "加载评论请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(PhotoDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("PhotoDetailActivity", "Comments API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<PhotoComment>>> call, @NonNull Throwable t) {
                showCommentLoading(false);
                Toast.makeText(PhotoDetailActivity.this, "网络错误，无法加载评论: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PhotoDetailActivity", "Comments Network Error: ", t);
            }
        });
    }

    private void publishPhotoComment() {
        String commentContent = etCommentInput.getText().toString().trim();
        String authorId = sessionManager.getCurrentUserId();
        Long photoId = currentPhoto != null ? currentPhoto.getId() : -1L;

        if (TextUtils.isEmpty(commentContent)) {
            Toast.makeText(this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(authorId)) {
            Toast.makeText(this, "用户未登录，无法发布评论", Toast.LENGTH_SHORT).show();
            return;
        }
        if (photoId == -1L) {
            Toast.makeText(this, "图片ID无效，无法发布评论", Toast.LENGTH_SHORT).show();
            return;
        }

        showCommentLoading(true);
        btnPublishComment.setEnabled(false);

        PhotoComment newComment = new PhotoComment(photoId, commentContent, authorId);

        photoCommentService.publishComment(newComment).enqueue(new Callback<BaseResponse<PhotoComment>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<PhotoComment>> call, @NonNull Response<BaseResponse<PhotoComment>> response) {
                showCommentLoading(false);
                btnPublishComment.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<PhotoComment> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        Toast.makeText(PhotoDetailActivity.this, "评论发布成功！", Toast.LENGTH_SHORT).show();
                        etCommentInput.setText("");
                        loadPhotoComments(photoId);
                    } else {
                        Toast.makeText(PhotoDetailActivity.this, "评论发布失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("PhotoDetailActivity", "Publish comment failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "评论发布请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(PhotoDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("PhotoDetailActivity", "Publish comment API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<PhotoComment>> call, @NonNull Throwable t) {
                showCommentLoading(false);
                btnPublishComment.setEnabled(true);
                Toast.makeText(PhotoDetailActivity.this, "网络错误，评论发布失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PhotoDetailActivity", "Publish comment Network Error: ", t);
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除图片")
                .setMessage("您确定要删除这张图片吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deletePhoto())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deletePhoto() {
        if (currentPhoto == null || currentPhoto.getId() == null) {
            Toast.makeText(this, "图片数据无效，无法删除", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        photoService.deletePhoto(currentPhoto.getId()).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(PhotoDetailActivity.this, "图片删除成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(PhotoDetailActivity.this, "图片删除失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("PhotoDetailActivity", "Delete failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "图片删除请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(PhotoDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("PhotoDetailActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(PhotoDetailActivity.this, "网络错误，无法删除图片: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PhotoDetailActivity", "Network Error: ", t);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_PHOTO && resultCode == RESULT_OK) {
            if (currentPhoto != null) {
                loadPhotoDetail(currentPhoto.getId());
                loadPhotoComments(currentPhoto.getId());
            }
        }
    }

    @Override
    public void onCommentDeleted() {
        if (currentPhoto != null) {
            loadPhotoComments(currentPhoto.getId());
        }
    }

    /**
     * 检查图片的收藏状态并更新UI
     * @param userId 当前用户ID
     * @param itemType 收藏项类型 (例如 "PHOTO")
     * @param itemId 收藏项ID (图片ID)
     */
    private void checkFavoriteStatus(Long userId, String itemType, Long itemId) {
        userFavoriteService.isFavorited(String.valueOf(userId), itemType, String.valueOf(itemId)).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFavorited = response.body().getData();
                    updateFavoriteButtonUI();
                } else {
                    Log.e("PhotoDetailActivity", "Failed to check favorite status: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Throwable t) {
                Log.e("PhotoDetailActivity", "Network error checking favorite status: ", t);
            }
        });
    }

    /**
     * 更新收藏按钮的图标
     */
    private void updateFavoriteButtonUI() {
        if (isFavorited) {
            ivFavoritePhoto.setImageResource(R.drawable.ic_favorite);
        } else {
            ivFavoritePhoto.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    /**
     * 添加收藏
     * @param userId 用户ID
     * @param itemType 收藏类型
     * @param itemId 收藏项ID
     */
    private void addFavorite(Long userId, String itemType, Long itemId) {
        UserFavorite favorite = new UserFavorite(userId, itemType, itemId);
        ivFavoritePhoto.setEnabled(false);
        userFavoriteService.addFavorite(favorite).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                ivFavoritePhoto.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFavorited = true;
                    updateFavoriteButtonUI();
                    Toast.makeText(PhotoDetailActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PhotoDetailActivity.this, "收藏失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("PhotoDetailActivity", "Add favorite failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                ivFavoritePhoto.setEnabled(true);
                Toast.makeText(PhotoDetailActivity.this, "网络错误，收藏失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PhotoDetailActivity", "Network error adding favorite: ", t);
            }
        });
    }

    /**
     * 取消收藏
     * @param userId 用户ID
     * @param itemType 收藏类型
     * @param itemId 收藏项ID
     */
    private void removeFavorite(String userId, String itemType, String itemId) { // 参数类型改为 String
        ivFavoritePhoto.setEnabled(false);
        // 修改这里：调用 removeFavorite 时传递单独的参数
        userFavoriteService.removeFavorite(userId, itemType, itemId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                ivFavoritePhoto.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFavorited = false;
                    updateFavoriteButtonUI();
                    Toast.makeText(PhotoDetailActivity.this, "取消收藏成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PhotoDetailActivity.this, "取消收藏失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("PhotoDetailActivity", "Remove favorite failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                ivFavoritePhoto.setEnabled(true);
                Toast.makeText(PhotoDetailActivity.this, "网络错误，取消收藏失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PhotoDetailActivity", "Network error removing favorite: ", t);
            }
        });
    }
}
