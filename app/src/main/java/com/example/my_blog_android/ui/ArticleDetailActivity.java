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

import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.ArticleCommentAdapter;
import com.example.my_blog_android.api.ArticleCommentService;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.UserFavoriteService;
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.ArticleComment;
import com.example.my_blog_android.model.BaseResponse;
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

public class ArticleDetailActivity extends AppCompatActivity implements ArticleCommentAdapter.OnCommentDeletedListener {

    public static final String EXTRA_ARTICLE_ID = "article_id";
    private static final int REQUEST_CODE_EDIT_ARTICLE = 1;

    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvTime;
    private TextView tvContent;
    private ProgressBar progressBar;
    private Button btnEditArticle;
    private Button btnDeleteArticle;
    private ImageView ivFavoriteArticle;

    private RecyclerView rvComments;
    private EditText etCommentInput;
    private Button btnPublishComment;
    private ProgressBar commentProgressBar;

    private ArticleService articleService;
    private ArticleCommentService articleCommentService;
    private UserFavoriteService userFavoriteService;
    private SessionManager sessionManager;

    private Article currentArticle;
    private ArticleCommentAdapter commentAdapter;
    private boolean isFavorited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tvTitle = findViewById(R.id.tv_detail_title);
        tvAuthor = findViewById(R.id.tv_detail_author);
        tvTime = findViewById(R.id.tv_detail_time);
        tvContent = findViewById(R.id.tv_detail_content);
        progressBar = findViewById(R.id.detail_progress_bar);
        btnEditArticle = findViewById(R.id.btn_edit_article);
        btnDeleteArticle = findViewById(R.id.btn_delete_article);
        ivFavoriteArticle = findViewById(R.id.iv_favorite_article);

        rvComments = findViewById(R.id.rv_comments);
        etCommentInput = findViewById(R.id.et_comment_input);
        btnPublishComment = findViewById(R.id.btn_publish_comment);
        commentProgressBar = findViewById(R.id.comment_progress_bar);

        articleService = ApiClient.getArticleService();
        articleCommentService = ApiClient.getArticleCommentService();
        userFavoriteService = ApiClient.getUserFavoriteService();
        sessionManager = SessionManager.getInstance(this);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new ArticleCommentAdapter(new ArrayList<>(), this);
        commentAdapter.setOnCommentDeletedListener(this);
        rvComments.setAdapter(commentAdapter);

        long articleId = getIntent().getLongExtra(EXTRA_ARTICLE_ID, -1);

        if (articleId != -1) {
            loadArticleDetail(articleId);
            loadArticleComments(articleId);
        } else {
            Toast.makeText(this, "文章ID无效，无法加载详情", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEditArticle.setOnClickListener(v -> {
            if (currentArticle != null) {
                Intent intent = new Intent(ArticleDetailActivity.this, EditArticleActivity.class);
                intent.putExtra(EditArticleActivity.EXTRA_ARTICLE, currentArticle);
                startActivityForResult(intent, REQUEST_CODE_EDIT_ARTICLE);
            } else {
                Toast.makeText(ArticleDetailActivity.this, "文章数据未加载，无法编辑", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteArticle.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnPublishComment.setOnClickListener(v -> publishComment());

        tvAuthor.setOnClickListener(v -> {
            if (currentArticle != null && currentArticle.getAuthorId() != null) {
                Intent intent = new Intent(ArticleDetailActivity.this, UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.EXTRA_USER_ID, currentArticle.getAuthorId());
                startActivity(intent);
            } else {
                Toast.makeText(ArticleDetailActivity.this, "无法获取作者信息", Toast.LENGTH_SHORT).show();
            }
        });

        ivFavoriteArticle.setOnClickListener(v -> {
            String currentUserId = sessionManager.getCurrentUserId();
            if (currentUserId == null) {
                Toast.makeText(ArticleDetailActivity.this, "请先登录才能收藏", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentArticle == null || currentArticle.getId() == null) {
                Toast.makeText(ArticleDetailActivity.this, "文章信息未加载", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFavorited) {
                // 修改这里：调用 removeFavorite 时传递单独的参数
                removeFavorite(currentUserId, "ARTICLE", String.valueOf(currentArticle.getId()));
            } else {
                addFavorite(Long.parseLong(currentUserId), "ARTICLE", currentArticle.getId());
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        tvTitle.setVisibility(show ? View.GONE : View.VISIBLE);
        tvAuthor.setVisibility(show ? View.GONE : View.VISIBLE);
        tvTime.setVisibility(show ? View.GONE : View.VISIBLE);
        tvContent.setVisibility(show ? View.GONE : View.VISIBLE);
        btnEditArticle.setVisibility(View.GONE);
        btnDeleteArticle.setVisibility(View.GONE);
        ivFavoriteArticle.setVisibility(show ? View.GONE : View.VISIBLE);

        etCommentInput.setVisibility(show ? View.GONE : View.VISIBLE);
        btnPublishComment.setVisibility(show ? View.GONE : View.VISIBLE);
        rvComments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showCommentLoading(boolean show) {
        commentProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvComments.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void loadArticleDetail(long id) {
        showLoading(true);

        articleService.getArticleById(id).enqueue(new Callback<BaseResponse<Article>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Article>> call, @NonNull Response<BaseResponse<Article>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Article> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        currentArticle = baseResponse.getData();
                        tvTitle.setText(currentArticle.getTitle());
                        if (currentArticle.getAuthorName() != null && !currentArticle.getAuthorName().isEmpty()) {
                            tvAuthor.setText("作者: " + currentArticle.getAuthorName());
                        } else {
                            tvAuthor.setText("作者ID: " + currentArticle.getAuthorId());
                        }
                        if (currentArticle.getCreateTime() != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            tvTime.setText("发布时间: " + dateFormat.format(currentArticle.getCreateTime()));
                        } else {
                            tvTime.setText("发布时间: N/A");
                        }
                        tvContent.setText(currentArticle.getContent());

                        String currentUserId = sessionManager.getCurrentUserId();
                        String currentUserRole = sessionManager.getCurrentUserRole();
                        String articleAuthorId = String.valueOf(currentArticle.getAuthorId());

                        Log.d("ArticlePermissionDebug", "Article ID: " + currentArticle.getId() + ", Author ID: " + articleAuthorId);
                        Log.d("ArticlePermissionDebug", "Current User ID: " + currentUserId + ", Current User Role: " + currentUserRole);
                        Log.d("ArticlePermissionDebug", "isAuthor: " + (currentUserId != null && currentUserId.equals(articleAuthorId)) + ", isAdmin: " + ("ADMIN".equals(currentUserRole)));

                        boolean isAuthor = currentUserId != null && currentUserId.equals(articleAuthorId);
                        boolean isAdmin = "ADMIN".equals(currentUserRole);

                        if (isAuthor) {
                            btnEditArticle.setVisibility(View.VISIBLE);
                            btnDeleteArticle.setVisibility(View.VISIBLE);
                        } else if (isAdmin) {
                            btnEditArticle.setVisibility(View.GONE);
                            btnDeleteArticle.setVisibility(View.VISIBLE);
                        } else {
                            btnEditArticle.setVisibility(View.GONE);
                            btnDeleteArticle.setVisibility(View.GONE);
                        }

                        if (currentUserId != null) {
                            checkFavoriteStatus(Long.parseLong(currentUserId), "ARTICLE", currentArticle.getId());
                        } else {
                            ivFavoriteArticle.setImageResource(R.drawable.ic_favorite_border);
                        }

                    } else {
                        Toast.makeText(ArticleDetailActivity.this, "加载文章详情失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("ArticleDetailActivity", "Load failed: " + baseResponse.getMsg());
                        finish();
                    }
                } else {
                    String errorMsg = "加载文章详情请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(ArticleDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("ArticleDetailActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Article>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ArticleDetailActivity.this, "网络错误，无法加载文章详情: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ArticleDetailActivity", "Network Error: ", t);
                finish();
            }
        });
    }

    private void loadArticleComments(long articleId) {
        showCommentLoading(true);
        articleCommentService.getCommentsByArticle(articleId).enqueue(new Callback<BaseResponse<List<ArticleComment>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<ArticleComment>>> call, @NonNull Response<BaseResponse<List<ArticleComment>>> response) {
                showCommentLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<ArticleComment>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        commentAdapter.updateComments(baseResponse.getData());
                    } else {
                        Toast.makeText(ArticleDetailActivity.this, "加载评论失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("ArticleDetailActivity", "Load comments failed: " + baseResponse.getMsg());
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
                    Toast.makeText(ArticleDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("ArticleDetailActivity", "Comments API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<ArticleComment>>> call, @NonNull Throwable t) {
                showCommentLoading(false);
                Toast.makeText(ArticleDetailActivity.this, "网络错误，无法加载评论: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ArticleDetailActivity", "Comments Network Error: ", t);
            }
        });
    }

    private void publishComment() {
        String commentContent = etCommentInput.getText().toString().trim();
        String authorId = sessionManager.getCurrentUserId();
        Long articleId = currentArticle != null ? currentArticle.getId() : -1L;

        if (TextUtils.isEmpty(commentContent)) {
            Toast.makeText(this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(authorId)) {
            Toast.makeText(this, "用户未登录，无法发布评论", Toast.LENGTH_SHORT).show();
            return;
        }
        if (articleId == -1L) {
            Toast.makeText(this, "文章ID无效，无法发布评论", Toast.LENGTH_SHORT).show();
            return;
        }

        showCommentLoading(true);
        btnPublishComment.setEnabled(false);

        ArticleComment newComment = new ArticleComment(articleId, commentContent, authorId);

        articleCommentService.publishComment(newComment).enqueue(new Callback<BaseResponse<ArticleComment>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<ArticleComment>> call, @NonNull Response<BaseResponse<ArticleComment>> response) {
                showCommentLoading(false);
                btnPublishComment.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<ArticleComment> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        Toast.makeText(ArticleDetailActivity.this, "评论发布成功！", Toast.LENGTH_SHORT).show();
                        etCommentInput.setText("");
                        loadArticleComments(articleId);
                    } else {
                        Toast.makeText(ArticleDetailActivity.this, "评论发布失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("ArticleDetailActivity", "Publish comment failed: " + baseResponse.getMsg());
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
                    Toast.makeText(ArticleDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("ArticleDetailActivity", "Publish comment API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<ArticleComment>> call, @NonNull Throwable t) {
                showCommentLoading(false);
                btnPublishComment.setEnabled(true);
                Toast.makeText(ArticleDetailActivity.this, "网络错误，评论发布失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ArticleDetailActivity", "Publish comment Network Error: ", t);
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除文章")
                .setMessage("您确定要删除这篇文章吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteArticle())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteArticle() {
        if (currentArticle == null || currentArticle.getId() == null) {
            Toast.makeText(this, "文章数据无效，无法删除", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        articleService.deleteArticle(currentArticle.getId()).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(ArticleDetailActivity.this, "文章删除成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(ArticleDetailActivity.this, "文章删除失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("ArticleDetailActivity", "Delete failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "文章删除请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(ArticleDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("ArticleDetailActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ArticleDetailActivity.this, "网络错误，无法删除文章: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ArticleDetailActivity", "Network Error: ", t);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_ARTICLE && resultCode == RESULT_OK) {
            if (currentArticle != null) {
                loadArticleDetail(currentArticle.getId());
                loadArticleComments(currentArticle.getId());
            }
        }
    }

    @Override
    public void onCommentDeleted() {
        if (currentArticle != null) {
            loadArticleComments(currentArticle.getId());
        }
    }

    /**
     * 检查文章的收藏状态并更新UI
     * @param userId 当前用户ID
     * @param itemType 收藏项类型 (例如 "ARTICLE")
     * @param itemId 收藏项ID (文章ID)
     */
    private void checkFavoriteStatus(Long userId, String itemType, Long itemId) {
        userFavoriteService.isFavorited(String.valueOf(userId), itemType, String.valueOf(itemId)).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFavorited = response.body().getData();
                    updateFavoriteButtonUI();
                } else {
                    Log.e("ArticleDetailActivity", "Failed to check favorite status: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Throwable t) {
                Log.e("ArticleDetailActivity", "Network error checking favorite status: ", t);
            }
        });
    }

    /**
     * 更新收藏按钮的图标
     */
    private void updateFavoriteButtonUI() {
        if (isFavorited) {
            ivFavoriteArticle.setImageResource(R.drawable.ic_favorite);
        } else {
            ivFavoriteArticle.setImageResource(R.drawable.ic_favorite_border);
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
        ivFavoriteArticle.setEnabled(false);
        userFavoriteService.addFavorite(favorite).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                ivFavoriteArticle.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFavorited = true;
                    updateFavoriteButtonUI();
                    Toast.makeText(ArticleDetailActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ArticleDetailActivity.this, "收藏失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("ArticleDetailActivity", "Add favorite failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                ivFavoriteArticle.setEnabled(true);
                Toast.makeText(ArticleDetailActivity.this, "网络错误，收藏失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ArticleDetailActivity", "Network error adding favorite: ", t);
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
        ivFavoriteArticle.setEnabled(false);
        // 修改这里：调用 removeFavorite 时传递单独的参数
        userFavoriteService.removeFavorite(userId, itemType, itemId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                ivFavoriteArticle.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFavorited = false;
                    updateFavoriteButtonUI();
                    Toast.makeText(ArticleDetailActivity.this, "取消收藏成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ArticleDetailActivity.this, "取消收藏失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("ArticleDetailActivity", "Remove favorite failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                ivFavoriteArticle.setEnabled(true);
                Toast.makeText(ArticleDetailActivity.this, "网络错误，取消收藏失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ArticleDetailActivity", "Network error removing favorite: ", t);
            }
        });
    }
}
