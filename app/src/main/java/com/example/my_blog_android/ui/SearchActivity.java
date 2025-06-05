package com.example.my_blog_android.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.ArticleAdapter;
import com.example.my_blog_android.adapter.PhotoAdapter;
import com.example.my_blog_android.adapter.UserAdapter; // 导入 UserAdapter
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.api.UserService; // 导入 UserService
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.model.User; // 导入 User model
import com.example.my_blog_android.utils.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_SEARCH_QUERY = "search_query";

    private SearchView searchView;
    private RadioGroup rgSearchType;
    private RadioButton rbArticles;
    private RadioButton rbPhotos;
    private RadioButton rbUsers; // 新增用户搜索类型
    private LinearLayout layoutArticleResults;
    private LinearLayout layoutPhotoResults;
    private LinearLayout layoutUserResults; // 新增用户结果布局
    private RecyclerView rvArticleResults;
    private RecyclerView rvPhotoResults;
    private RecyclerView rvUserResults; // 新增用户结果 RecyclerView
    private ProgressBar progressBar;

    private ArticleAdapter articleAdapter;
    private PhotoAdapter photoAdapter;
    private UserAdapter userAdapter; // 新增 UserAdapter

    private ArticleService articleService;
    private PhotoService photoService;
    private UserService userService; // 新增 UserService

    private String currentQuery;
    private int currentSearchType = R.id.rb_search_articles; // 默认搜索文章

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        searchView = findViewById(R.id.search_view_results);
        rgSearchType = findViewById(R.id.rg_search_type);
        rbArticles = findViewById(R.id.rb_search_articles);
        rbPhotos = findViewById(R.id.rb_search_photos);
        rbUsers = findViewById(R.id.rb_search_users);
        layoutArticleResults = findViewById(R.id.layout_article_results);
        layoutPhotoResults = findViewById(R.id.layout_photo_results);
        layoutUserResults = findViewById(R.id.layout_user_results);
        rvArticleResults = findViewById(R.id.rv_article_results);
        rvPhotoResults = findViewById(R.id.rv_photo_results);
        rvUserResults = findViewById(R.id.rv_user_results);
        progressBar = findViewById(R.id.search_progress_bar);

        articleService = ApiClient.getArticleService();
        photoService = ApiClient.getPhotoService();
        userService = ApiClient.getUserService();

        // 设置 RecyclerViews
        rvArticleResults.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(new ArrayList<>());
        rvArticleResults.setAdapter(articleAdapter);

        rvPhotoResults.setLayoutManager(new GridLayoutManager(this, 2));
        photoAdapter = new PhotoAdapter(new ArrayList<>());
        rvPhotoResults.setAdapter(photoAdapter);

        rvUserResults.setLayoutManager(new LinearLayoutManager(this)); // 用户列表通常也用线性布局
        userAdapter = new UserAdapter(new ArrayList<>(), this);
        rvUserResults.setAdapter(userAdapter);

        // 获取传递过来的搜索关键词
        currentQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
        if (currentQuery != null && !currentQuery.isEmpty()) {
            searchView.setQuery(currentQuery, false); // 在搜索框中显示关键词
            performSearch(currentQuery, currentSearchType); // 执行默认搜索 (文章)
        } else {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
        }

        // 设置搜索框监听器，以便在搜索结果页再次搜索
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                performSearch(currentQuery, currentSearchType);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // 设置搜索类型选择器监听
        rgSearchType.setOnCheckedChangeListener((group, checkedId) -> {
            currentSearchType = checkedId;
            performSearch(currentQuery, currentSearchType); // 根据选择的类型重新搜索
        });

        // 默认选中文章，触发一次搜索
        rbArticles.setChecked(true);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutArticleResults.setVisibility(View.GONE);
        layoutPhotoResults.setVisibility(View.GONE);
        layoutUserResults.setVisibility(View.GONE);
    }

    @SuppressLint("NonConstantResourceId")
    private void performSearch(String query, int searchType) {
        if (query == null || query.isEmpty()) {
            Toast.makeText(this, "搜索关键词不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Modified: Replaced switch with if-else if to avoid "Constant expression required" error
        if (searchType == R.id.rb_search_articles) {
            searchArticles(query);
        } else if (searchType == R.id.rb_search_photos) {
            searchPhotos(query);
        } else if (searchType == R.id.rb_search_users) {
            searchUsers(query);
        }
    }

    private void searchArticles(String query) {
        articleService.searchArticles(query).enqueue(new Callback<BaseResponse<List<Article>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Response<BaseResponse<List<Article>>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<Article> articles = response.body().getData();
                    articleAdapter.updateData(articles);
                    layoutArticleResults.setVisibility(View.VISIBLE);
                    if (articles.isEmpty()) {
                        Toast.makeText(SearchActivity.this, "未找到相关文章", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "搜索文章失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("SearchActivity", "Search articles failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(SearchActivity.this, "网络错误，搜索文章失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SearchActivity", "Network error searching articles: ", t);
            }
        });
    }

    private void searchPhotos(String query) {
        photoService.searchPhotos(query).enqueue(new Callback<BaseResponse<List<Photo>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Response<BaseResponse<List<Photo>>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<Photo> photos = response.body().getData();
                    photoAdapter.updateData(photos);
                    layoutPhotoResults.setVisibility(View.VISIBLE);
                    if (photos.isEmpty()) {
                        Toast.makeText(SearchActivity.this, "未找到相关图片", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "搜索图片失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("SearchActivity", "Search photos failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(SearchActivity.this, "网络错误，搜索图片失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SearchActivity", "Network error searching photos: ", t);
            }
        });
    }

    private void searchUsers(String query) {
        userService.searchUsers(query).enqueue(new Callback<BaseResponse<List<User>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<User>>> call, @NonNull Response<BaseResponse<List<User>>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<User> users = response.body().getData();
                    userAdapter.updateData(users);
                    layoutUserResults.setVisibility(View.VISIBLE);
                    if (users.isEmpty()) {
                        Toast.makeText(SearchActivity.this, "未找到相关用户", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "搜索用户失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("SearchActivity", "Search users failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<User>>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(SearchActivity.this, "网络错误，搜索用户失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SearchActivity", "Network error searching users: ", t);
            }
        });
    }
}
