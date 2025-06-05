package com.example.my_blog_android.ui;

import android.content.DialogInterface; // 导入 DialogInterface
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
import androidx.appcompat.app.AlertDialog; // 导入 AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.ArticleAdapter;
import com.example.my_blog_android.adapter.PhotoAdapter;
import com.example.my_blog_android.api.UserFavoriteService;
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

// FavoriteListActivity 实现 ArticleAdapter.OnItemLongClickListener 和 PhotoAdapter.OnItemLongClickListener 接口
public class FavoriteListActivity extends AppCompatActivity implements
        ArticleAdapter.OnItemLongClickListener,
        PhotoAdapter.OnItemLongClickListener {

    private TextView tvTitle;
    private RadioGroup rgFavoriteDisplayType;
    private RadioButton rbFavoriteArticles;
    private RadioButton rbFavoritePhotos;
    private LinearLayout layoutFavoriteArticleDisplay;
    private LinearLayout layoutFavoritePhotoDisplay;
    private RecyclerView rvFavoriteArticles;
    private RecyclerView rvFavoritePhotos;
    private ProgressBar progressBar;

    private ArticleAdapter articleAdapter;
    private PhotoAdapter photoAdapter;
    private UserFavoriteService userFavoriteService;
    private SessionManager sessionManager;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化 UI 元素
        tvTitle = findViewById(R.id.tv_favorite_list_title);
        rgFavoriteDisplayType = findViewById(R.id.rg_favorite_display_type);
        rbFavoriteArticles = findViewById(R.id.rb_favorite_articles);
        rbFavoritePhotos = findViewById(R.id.rb_favorite_photos);
        layoutFavoriteArticleDisplay = findViewById(R.id.layout_favorite_article_display);
        layoutFavoritePhotoDisplay = findViewById(R.id.layout_favorite_photo_display);
        rvFavoriteArticles = findViewById(R.id.rv_favorite_articles);
        rvFavoritePhotos = findViewById(R.id.rv_favorite_photos);
        progressBar = findViewById(R.id.favorite_list_progress_bar);

        // 初始化服务和会话管理器
        userFavoriteService = ApiClient.getUserFavoriteService();
        sessionManager = SessionManager.getInstance(this);

        // 获取当前登录用户ID
        currentUserId = sessionManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户未登录，无法查看收藏", Toast.LENGTH_SHORT).show();
            finish(); // 结束当前Activity
            return;
        }

        // 配置文章 RecyclerView
        rvFavoriteArticles.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(new ArrayList<>());
        rvFavoriteArticles.setAdapter(articleAdapter);
        // 设置文章适配器的长按监听器，将当前Activity作为监听器
        articleAdapter.setOnItemLongClickListener(this);

        // 配置图片 RecyclerView
        rvFavoritePhotos.setLayoutManager(new GridLayoutManager(this, 2));
        photoAdapter = new PhotoAdapter(new ArrayList<>());
        rvFavoritePhotos.setAdapter(photoAdapter);
        // 设置图片适配器的长按监听器，将当前Activity作为监听器
        photoAdapter.setOnItemLongClickListener(this);

        // 设置显示类型选择器监听器
        rgFavoriteDisplayType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_favorite_articles) {
                switchDisplayMode(true); // 切换到文章显示模式
            } else if (checkedId == R.id.rb_favorite_photos) {
                switchDisplayMode(false); // 切换到图片显示模式
            }
        });

        // 默认加载收藏文章和图片
        loadFavoriteArticles();
        loadFavoritePhotos();
        switchDisplayMode(true); // 默认显示文章列表
    }

    /**
     * 显示或隐藏加载指示器和内容区域
     * @param show true 显示加载，false 隐藏加载
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        // 当加载时隐藏列表布局，加载完成后根据选中状态显示
        layoutFavoriteArticleDisplay.setVisibility(show ? View.GONE : (rbFavoriteArticles.isChecked() ? View.VISIBLE : View.GONE));
        layoutFavoritePhotoDisplay.setVisibility(show ? View.GONE : (rbFavoritePhotos.isChecked() ? View.VISIBLE : View.GONE));
    }

    /**
     * 切换显示模式 (文章或图片)
     * @param showArticles true 为文章显示模式，false 为图片显示模式
     */
    private void switchDisplayMode(boolean showArticles) {
        if (showArticles) {
            layoutFavoriteArticleDisplay.setVisibility(View.VISIBLE);
            layoutFavoritePhotoDisplay.setVisibility(View.GONE);
            // 只有当当前显示模式下没有数据时才重新加载，避免不必要的网络请求
            if (articleAdapter.getItemCount() == 0) {
                loadFavoriteArticles();
            }
        } else {
            layoutFavoriteArticleDisplay.setVisibility(View.GONE);
            layoutFavoritePhotoDisplay.setVisibility(View.VISIBLE);
            // 只有当当前显示模式下没有数据时才重新加载，避免不必要的网络请求
            if (photoAdapter.getItemCount() == 0) {
                loadFavoritePhotos();
            }
        }
    }

    /**
     * 加载用户收藏的文章列表
     */
    private void loadFavoriteArticles() {
        showLoading(true);
        userFavoriteService.getFavoriteArticles(currentUserId).enqueue(new Callback<BaseResponse<List<Article>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Response<BaseResponse<List<Article>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<Article> articles = response.body().getData();
                    if (articles != null) {
                        articleAdapter.updateData(articles);
                        if (articles.isEmpty()) {
                            Toast.makeText(FavoriteListActivity.this, "您还没有收藏任何文章", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(FavoriteListActivity.this, "未找到收藏文章", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "加载收藏文章失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(FavoriteListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("FavoriteListActivity", errorMsg);
                }
                showLoading(false);
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(FavoriteListActivity.this, "网络错误，无法加载收藏文章: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("FavoriteListActivity", "Network error loading favorite articles: ", t);
            }
        });
    }

    /**
     * 加载用户收藏的图片列表
     */
    private void loadFavoritePhotos() {
        showLoading(true);
        userFavoriteService.getFavoritePhotos(currentUserId).enqueue(new Callback<BaseResponse<List<Photo>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Response<BaseResponse<List<Photo>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<Photo> photos = response.body().getData();
                    if (photos != null) {
                        photoAdapter.updateData(photos);
                        if (photos.isEmpty()) {
                            Toast.makeText(FavoriteListActivity.this, "您还没有收藏任何图片", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(FavoriteListActivity.this, "未找到收藏图片", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "加载收藏图片失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(FavoriteListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("FavoriteListActivity", errorMsg);
                }
                showLoading(false);
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(FavoriteListActivity.this, "网络错误，无法加载收藏图片: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("FavoriteListActivity", "Network error loading favorite photos: ", t);
            }
        });
    }

    /**
     * 实现 ArticleAdapter.OnItemLongClickListener 接口的方法
     * 当文章项被长按时调用
     * @param position 被长按项在适配器中的位置
     * @param article 被长按的文章对象
     */
    @Override
    public void onItemLongClick(int position, Article article) {
        // 处理文章长按事件：显示确认删除对话框
        showDeleteConfirmationDialog("文章", String.valueOf(article.getId()), position, "ARTICLE");
    }

    /**
     * 实现 PhotoAdapter.OnItemLongClickListener 接口的方法
     * 当图片项被长按时调用
     * @param position 被长按项在适配器中的位置
     * @param photo 被长按的图片对象
     */
    @Override
    public void onItemLongClick(int position, Photo photo) {
        // 处理图片长按事件：显示确认删除对话框
        showDeleteConfirmationDialog("图片", String.valueOf(photo.getId()), position, "PHOTO");
    }

    /**
     * 显示删除确认对话框
     * @param itemTypeDisplay 用于显示给用户的物品类型名称（例如 "文章" 或 "图片"）
     * @param itemId 要删除的收藏项的ID
     * @param position 在 RecyclerView 适配器中的位置
     * @param actualItemType 实际的物品类型，用于API调用（例如 "ARTICLE" 或 "PHOTO"）
     */
    private void showDeleteConfirmationDialog(String itemTypeDisplay, String itemId, int position, String actualItemType) {
        new AlertDialog.Builder(this)
                .setTitle("删除收藏") // 对话框标题
                .setMessage("您确定要从收藏中删除这篇" + itemTypeDisplay + "吗？") // 对话框内容
                .setPositiveButton("删除", (dialog, which) -> { // 确定按钮
                    removeFavoriteItem(itemId, position, actualItemType); // 调用删除方法
                })
                .setNegativeButton("取消", null) // 取消按钮，点击不执行任何操作
                .show(); // 显示对话框
    }

    /**
     * 调用 API 移除用户收藏的项
     * @param itemId 要删除的收藏项的ID
     * @param position 在 RecyclerView 适配器中的位置
     * @param itemType 收藏项的类型（"ARTICLE" 或 "PHOTO"）
     */
    private void removeFavoriteItem(String itemId, int position, String itemType) {
        String currentUserId = sessionManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户未登录，无法执行删除操作", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true); // 显示加载进度条

        // 调用 UserFavoriteService 的 removeFavorite API
        userFavoriteService.removeFavorite(currentUserId, itemType, itemId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                showLoading(false); // 隐藏加载进度条
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    Toast.makeText(FavoriteListActivity.this, "取消收藏成功", Toast.LENGTH_SHORT).show();
                    // 根据 itemType 从对应的适配器中移除项，并通知 RecyclerView 刷新 UI
                    if ("ARTICLE".equals(itemType)) {
                        articleAdapter.removeItem(position);
                    } else if ("PHOTO".equals(itemType)) {
                        photoAdapter.removeItem(position);
                    }
                } else {
                    String errorMsg = "取消收藏失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(FavoriteListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("FavoriteListActivity", errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                showLoading(false); // 隐藏加载进度条
                Toast.makeText(FavoriteListActivity.this, "网络错误，取消收藏失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("FavoriteListActivity", "Network error removing favorite: ", t);
            }
        });
    }
}
