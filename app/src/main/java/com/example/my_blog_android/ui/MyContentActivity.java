package com.example.my_blog_android.ui;

import android.content.DialogInterface; // 导入 DialogInterface
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // 导入 Nullable
import androidx.appcompat.app.AlertDialog; // 导入 AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.ArticleAdapter;
import com.example.my_blog_android.adapter.PhotoAdapter;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// MyContentActivity 实现 ArticleAdapter.OnItemLongClickListener 和 PhotoAdapter.OnItemLongClickListener 接口
public class MyContentActivity extends AppCompatActivity implements
        ArticleAdapter.OnItemLongClickListener, // 实现文章适配器的长按监听接口
        PhotoAdapter.OnItemLongClickListener {   // 实现图片适配器的长按监听接口

    private RecyclerView rvMyArticles;
    private RecyclerView rvMyPhotos;
    private ProgressBar progressBar;

    private ArticleAdapter articleAdapter;
    private PhotoAdapter photoAdapter;

    private ArticleService articleService;
    private PhotoService photoService;
    private SessionManager sessionManager;

    private RadioGroup rgMyContentDisplayType;
    private RadioButton rbMyArticles;
    private RadioButton rbMyPhotos;
    private LinearLayout layoutMyArticleDisplay;
    private LinearLayout layoutMyPhotoDisplay;

    private String currentUserId;

    // 请求码，用于区分编辑文章和编辑图片的回调
    private static final int REQUEST_CODE_EDIT_ARTICLE = 101;
    private static final int REQUEST_CODE_EDIT_PHOTO = 102;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_content);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        rvMyArticles = findViewById(R.id.rv_my_articles);
        rvMyPhotos = findViewById(R.id.rv_my_photos);
        progressBar = findViewById(R.id.my_content_progress_bar);

        rgMyContentDisplayType = findViewById(R.id.rg_my_content_display_type);
        rbMyArticles = findViewById(R.id.rb_my_articles);
        rbMyPhotos = findViewById(R.id.rb_my_photos);
        layoutMyArticleDisplay = findViewById(R.id.layout_my_article_display);
        layoutMyPhotoDisplay = findViewById(R.id.layout_my_photo_display);

        articleService = ApiClient.getArticleService();
        photoService = ApiClient.getPhotoService();
        sessionManager = SessionManager.getInstance(this);

        currentUserId = sessionManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户未登录，无法查看我的内容", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up Article RecyclerView
        rvMyArticles.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(new ArrayList<>());
        rvMyArticles.setAdapter(articleAdapter);
        // 设置文章适配器的长按监听器
        articleAdapter.setOnItemLongClickListener(this);

        // Set up Photo RecyclerView
        rvMyPhotos.setLayoutManager(new GridLayoutManager(this, 2));
        photoAdapter = new PhotoAdapter(new ArrayList<>());
        rvMyPhotos.setAdapter(photoAdapter);
        // 设置图片适配器的长按监听器
        photoAdapter.setOnItemLongClickListener(this);

        // Set up display type selector listener
        rgMyContentDisplayType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_my_articles) {
                    switchDisplayMode(true); // Switch to my articles display mode
                } else if (checkedId == R.id.rb_my_photos) {
                    switchDisplayMode(false); // Switch to my photos display mode
                }
            }
        });

        // Default to showing my articles
        loadMyArticles();
        loadMyPhotos(); // Also load photos in background
        switchDisplayMode(true);
    }

    /**
     * Switches the display mode (my articles or my photos)
     * @param showArticles true for my articles display mode, false for my photos display mode
     */
    private void switchDisplayMode(boolean showArticles) {
        if (showArticles) {
            layoutMyArticleDisplay.setVisibility(View.VISIBLE);
            layoutMyPhotoDisplay.setVisibility(View.GONE);
            // 只有当当前显示模式下没有数据时才重新加载，避免不必要的网络请求
            if (articleAdapter.getItemCount() == 0) {
                loadMyArticles();
            }
        } else {
            layoutMyArticleDisplay.setVisibility(View.GONE);
            layoutMyPhotoDisplay.setVisibility(View.VISIBLE);
            // 只有当当前显示模式下没有数据时才重新加载，避免不必要的网络请求
            if (photoAdapter.getItemCount() == 0) {
                loadMyPhotos();
            }
        }
    }

    /**
     * 设置加载状态的统一方法
     * @param isLoading true 表示正在加载，false 表示加载完成
     */
    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (rbMyArticles.isChecked()) {
            layoutMyArticleDisplay.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            layoutMyPhotoDisplay.setVisibility(View.GONE); // 确保另一个布局隐藏
        } else {
            layoutMyArticleDisplay.setVisibility(View.GONE); // 确保另一个布局隐藏
            layoutMyPhotoDisplay.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void loadMyArticles() {
        setLoadingState(true); // 显示加载状态
        articleService.getArticlesByUser(currentUserId).enqueue(new Callback<BaseResponse<List<Article>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Response<BaseResponse<List<Article>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<Article>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        articleAdapter.updateData(baseResponse.getData());
                        if (baseResponse.getData().isEmpty()) {
                            Toast.makeText(MyContentActivity.this, "您还没有发布任何文章", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyContentActivity.this, "加载我的文章失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("MyContentActivity", "Load articles failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "加载我的文章请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(MyContentActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("MyContentActivity", "Articles API Call Failed: " + response.code() + " " + errorMsg);
                }
                if (rbMyArticles.isChecked()) { // 只有当当前显示的是文章列表时才隐藏加载状态
                    setLoadingState(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Throwable t) {
                if (rbMyArticles.isChecked()) { // 只有当当前显示的是文章列表时才隐藏加载状态
                    setLoadingState(false);
                }
                Toast.makeText(MyContentActivity.this, "网络错误，无法加载我的文章: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MyContentActivity", "Articles Network Error: ", t);
            }
        });
    }

    private void loadMyPhotos() {
        setLoadingState(true); // 显示加载状态
        photoService.getPhotosByUser(currentUserId).enqueue(new Callback<BaseResponse<List<Photo>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Response<BaseResponse<List<Photo>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<Photo>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        photoAdapter.updateData(baseResponse.getData());
                        if (baseResponse.getData().isEmpty()) {
                            Toast.makeText(MyContentActivity.this, "您还没有发布任何图片", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyContentActivity.this, "加载我的图片失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("MyContentActivity", "Load photos failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "加载我的图片请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(MyContentActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("MyContentActivity", "Photos API Call Failed: " + response.code() + " " + errorMsg);
                }
                if (rbMyPhotos.isChecked()) { // 只有当当前显示的是图片列表时才隐藏加载状态
                    setLoadingState(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Throwable t) {
                if (rbMyPhotos.isChecked()) { // 只有当当前显示的是图片列表时才隐藏加载状态
                    setLoadingState(false);
                }
                Toast.makeText(MyContentActivity.this, "网络错误，无法加载我的图片: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MyContentActivity", "Photos Network Error: ", t);
            }
        });
    }

    // --- 实现 ArticleAdapter.OnItemLongClickListener 接口的方法 ---
    @Override
    public void onItemLongClick(int position, Article article) {
        showMyContentOptionsDialog("文章", article.getId(), position, "ARTICLE", EditArticleActivity.class, article);
    }

    // --- 实现 PhotoAdapter.OnItemLongClickListener 接口的方法 ---
    @Override
    public void onItemLongClick(int position, Photo photo) {
        showMyContentOptionsDialog("图片", photo.getId(), position, "PHOTO", EditPhotoActivity.class, photo);
    }

    /**
     * 显示我的内容（文章或图片）的编辑/删除选项对话框
     * @param itemTypeDisplay 用于显示给用户的物品类型名称（例如 "文章" 或 "图片"）
     * @param itemId 要操作的项的ID
     * @param position 在 RecyclerView 适配器中的位置
     * @param actualItemType 实际的物品类型，用于API调用（例如 "ARTICLE" 或 "PHOTO"）
     * @param editActivityClass 对应的编辑Activity类 (EditArticleActivity.class 或 EditPhotoActivity.class)
     * @param itemData 完整的文章或图片对象，用于传递给编辑Activity
     */
    private void showMyContentOptionsDialog(String itemTypeDisplay, long itemId, int position, String actualItemType, Class<?> editActivityClass, Object itemData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选项");
        String[] options = {"编辑" + itemTypeDisplay, "删除" + itemTypeDisplay};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // 编辑
                    if ("ARTICLE".equals(actualItemType) && itemData instanceof Article) {
                        Intent intent = new Intent(MyContentActivity.this, editActivityClass);
                        intent.putExtra(EditArticleActivity.EXTRA_ARTICLE, (Article) itemData);
                        startActivityForResult(intent, REQUEST_CODE_EDIT_ARTICLE);
                    } else if ("PHOTO".equals(actualItemType) && itemData instanceof Photo) {
                        Intent intent = new Intent(MyContentActivity.this, editActivityClass);
                        intent.putExtra(EditPhotoActivity.EXTRA_PHOTO, (Photo) itemData);
                        startActivityForResult(intent, REQUEST_CODE_EDIT_PHOTO);
                    }
                    break;
                case 1: // 删除
                    showDeleteConfirmationDialog(itemTypeDisplay, itemId, position, actualItemType);
                    break;
            }
        });
        builder.show();
    }

    /**
     * 显示删除确认对话框
     * @param itemTypeDisplay 用于显示给用户的物品类型名称（例如 "文章" 或 "图片"）
     * @param itemId 要删除的项的ID
     * @param position 在 RecyclerView 适配器中的位置
     * @param actualItemType 实际的物品类型，用于API调用（例如 "ARTICLE" 或 "PHOTO"）
     */
    private void showDeleteConfirmationDialog(String itemTypeDisplay, long itemId, int position, String actualItemType) {
        new AlertDialog.Builder(this)
                .setTitle("删除" + itemTypeDisplay)
                .setMessage("您确定要删除这篇" + itemTypeDisplay + "吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> {
                    if ("ARTICLE".equals(actualItemType)) {
                        deleteArticle(itemId, position);
                    } else if ("PHOTO".equals(actualItemType)) {
                        deletePhoto(itemId, position);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除文章
     * @param articleId 要删除的文章ID
     * @param position 文章在适配器中的位置
     */
    private void deleteArticle(long articleId, int position) {
        setLoadingState(true);
        articleService.deleteArticle(articleId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    Toast.makeText(MyContentActivity.this, "文章删除成功", Toast.LENGTH_SHORT).show();
                    articleAdapter.removeItem(position); // 从适配器中移除项
                } else {
                    String errorMsg = "文章删除失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(MyContentActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("MyContentActivity", "Delete article failed: " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                setLoadingState(false);
                Toast.makeText(MyContentActivity.this, "网络错误，删除文章失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MyContentActivity", "Network error deleting article: ", t);
            }
        });
    }

    /**
     * 删除图片
     * @param photoId 要删除的图片ID
     * @param position 图片在适配器中的位置
     */
    private void deletePhoto(long photoId, int position) {
        setLoadingState(true);
        photoService.deletePhoto(photoId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    Toast.makeText(MyContentActivity.this, "图片删除成功", Toast.LENGTH_SHORT).show();
                    photoAdapter.removeItem(position); // 从适配器中移除项
                } else {
                    String errorMsg = "图片删除失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(MyContentActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("MyContentActivity", "Delete photo failed: " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                setLoadingState(false);
                Toast.makeText(MyContentActivity.this, "网络错误，删除图片失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MyContentActivity", "Network error deleting photo: ", t);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_EDIT_ARTICLE) {
                // 文章编辑成功，重新加载文章列表
                loadMyArticles();
            } else if (requestCode == REQUEST_CODE_EDIT_PHOTO) {
                // 图片编辑成功，重新加载图片列表
                loadMyPhotos();
            }
        }
    }
}
