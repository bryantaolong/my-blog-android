package com.example.my_blog_android.ui;

import com.example.my_blog_android.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.adapter.ArticleAdapter;
import com.example.my_blog_android.adapter.PhotoAdapter;

import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvArticles;
    private RecyclerView rvPhotos;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;
    private SearchView searchView;
    private ArticleAdapter articleAdapter;
    private PhotoAdapter photoAdapter;
    private ArticleService articleService;
    private PhotoService photoService;

    private RadioGroup rgDisplayType;
    private RadioButton rbShowArticles;
    private RadioButton rbShowPhotos;
    private LinearLayout layoutArticleDisplay;
    private LinearLayout layoutPhotoDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        rvArticles = findViewById(R.id.rv_articles);
        rvPhotos = findViewById(R.id.rv_photos);
        progressBar = findViewById(R.id.progress_bar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        rgDisplayType = findViewById(R.id.rg_display_type);
        rbShowArticles = findViewById(R.id.rb_show_articles);
        rbShowPhotos = findViewById(R.id.rb_show_photos);
        layoutArticleDisplay = findViewById(R.id.layout_article_display);
        layoutPhotoDisplay = findViewById(R.id.layout_photo_display);

        searchView = findViewById(R.id.search_view);

        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                if (!rbShowArticles.isChecked()) {
                    rbShowArticles.setChecked(true);
                } else {
                    Toast.makeText(MainActivity.this, "您已在首页", Toast.LENGTH_SHORT).show();
                    loadArticles();
                    loadPhotos();
                }
            } else if (itemId == R.id.nav_publish) {
                Intent intent = new Intent(MainActivity.this, PublishActivity.class);
                startActivity(intent);
                finish();
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        });

        // 设置搜索框的监听器 (当用户提交搜索时)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 当用户提交搜索时，跳转到 SearchActivity
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra(SearchActivity.EXTRA_SEARCH_QUERY, query);
                startActivity(intent);
                // 提交后清除焦点，避免键盘停留在当前页面
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 在文本改变时通常不执行搜索，只在提交时搜索
                return false;
            }
        });

        // 移除了 searchView.setOnClickListener，因为 SearchView 默认就能点击输入文本

        articleService = ApiClient.getArticleService();
        photoService = ApiClient.getPhotoService();

        rvArticles.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(new ArrayList<>());
        rvArticles.setAdapter(articleAdapter);

        rvPhotos.setLayoutManager(new GridLayoutManager(this, 2));
        photoAdapter = new PhotoAdapter(new ArrayList<>());
        rvPhotos.setAdapter(photoAdapter);

        rgDisplayType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_show_articles) {
                    switchDisplayMode(true);
                } else if (checkedId == R.id.rb_show_photos) {
                    switchDisplayMode(false);
                }
            }
        });

        loadArticles();
        loadPhotos();
        switchDisplayMode(true);
    }

    /**
     * Switches the display mode (articles or photos)
     * @param showArticles true for article display mode, false for photo display mode
     */
    private void switchDisplayMode(boolean showArticles) {
        if (showArticles) {
            layoutArticleDisplay.setVisibility(View.VISIBLE);
            layoutPhotoDisplay.setVisibility(View.GONE);
            if (articleAdapter.getItemCount() == 0) {
                loadArticles();
            }
        } else {
            layoutArticleDisplay.setVisibility(View.GONE);
            layoutPhotoDisplay.setVisibility(View.VISIBLE);
            if (photoAdapter.getItemCount() == 0) {
                loadPhotos();
            }
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        if (rbShowArticles.isChecked()) {
            layoutArticleDisplay.setVisibility(View.GONE);
        } else {
            layoutPhotoDisplay.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        if (rbShowArticles.isChecked()) {
            layoutArticleDisplay.setVisibility(View.VISIBLE);
        } else {
            layoutPhotoDisplay.setVisibility(View.VISIBLE);
        }
    }

    private void loadArticles() {
        showLoading();
        articleService.getAllArticles().enqueue(new Callback<BaseResponse<List<Article>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Response<BaseResponse<List<Article>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<Article>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        articleAdapter.updateData(baseResponse.getData());
                    } else {
                        Toast.makeText(MainActivity.this, "加载文章失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "加载文章失败: " + response.message(), Toast.LENGTH_SHORT).show();
                }
                if (rbShowArticles.isChecked()) {
                    hideLoading();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Throwable t) {
                if (rbShowArticles.isChecked()) {
                    hideLoading();
                }
                Toast.makeText(MainActivity.this, "网络错误，无法加载文章: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void loadPhotos() {
        showLoading();
        photoService.getAllPhotos().enqueue(new Callback<BaseResponse<List<Photo>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Response<BaseResponse<List<Photo>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<Photo>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        photoAdapter.updateData(baseResponse.getData());
                    } else {
                        Toast.makeText(MainActivity.this, "加载图片失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "加载图片失败: " + response.message(), Toast.LENGTH_SHORT).show();
                }
                if (rbShowPhotos.isChecked()) {
                    hideLoading();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Throwable t) {
                if (rbShowPhotos.isChecked()) {
                    hideLoading();
                }
                Toast.makeText(MainActivity.this, "网络错误，无法加载图片: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }
}
