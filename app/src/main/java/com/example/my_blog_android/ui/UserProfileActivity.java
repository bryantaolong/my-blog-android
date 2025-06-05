package com.example.my_blog_android.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button; // 导入 Button
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.ArticleAdapter;
import com.example.my_blog_android.adapter.PhotoAdapter;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.api.UserService;
import com.example.my_blog_android.api.UserFollowService; // 导入新的 UserFollowService
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.model.User;
import com.example.my_blog_android.model.UserFollow; // 导入新的 UserFollow 模型
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "target_user_id";

    private ShapeableImageView profileImage;
    private TextView tvUserName;
    private TextView tvUserId;
    private TextView tvPersonalBio;
    private ProgressBar progressBar;
    private Button btnFollowUser; // 关注按钮

    private RecyclerView rvUserArticles;
    private RecyclerView rvUserPhotos;

    private ArticleAdapter articleAdapter;
    private PhotoAdapter photoAdapter;

    private ArticleService articleService;
    private PhotoService photoService;
    private UserService userService;
    private UserFollowService userFollowService; // 用户关注服务

    private RadioGroup rgUserProfileDisplayType;
    private RadioButton rbUserArticles;
    private RadioButton rbUserPhotos;
    private LinearLayout layoutUserArticleDisplay;
    private LinearLayout layoutUserPhotoDisplay;

    private String targetUserId; // 要查看的用户的ID
    private String currentLoggedInUserId; // 当前登录用户的ID
    private boolean isFollowing = false; // 关注状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化 UI 元素
        profileImage = findViewById(R.id.profile_image_user_profile);
        tvUserName = findViewById(R.id.tv_user_profile_name);
        tvUserId = findViewById(R.id.tv_user_profile_id);
        tvPersonalBio = findViewById(R.id.tv_user_profile_bio);
        progressBar = findViewById(R.id.user_profile_progress_bar);
        btnFollowUser = findViewById(R.id.btn_follow_user); // 初始化关注按钮

        rvUserArticles = findViewById(R.id.rv_user_articles);
        rvUserPhotos = findViewById(R.id.rv_user_photos);

        rgUserProfileDisplayType = findViewById(R.id.rg_user_profile_display_type);
        rbUserArticles = findViewById(R.id.rb_user_articles);
        rbUserPhotos = findViewById(R.id.rb_user_photos);
        layoutUserArticleDisplay = findViewById(R.id.layout_user_article_display);
        layoutUserPhotoDisplay = findViewById(R.id.layout_user_photo_display);

        // 初始化服务
        articleService = ApiClient.getArticleService();
        photoService = ApiClient.getPhotoService();
        userService = ApiClient.getUserService();
        userFollowService = ApiClient.getUserFollowService(); // 初始化用户关注服务
        SessionManager sessionManager = SessionManager.getInstance(this);
        currentLoggedInUserId = sessionManager.getCurrentUserId(); // 获取当前登录用户的ID

        // 获取传递过来的用户ID
        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "用户ID无效，无法加载用户主页", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 如果查看的是自己的主页，或者未登录，则隐藏关注按钮
        if (currentLoggedInUserId == null || currentLoggedInUserId.equals(targetUserId)) {
            btnFollowUser.setVisibility(View.GONE);
        } else {
            btnFollowUser.setVisibility(View.VISIBLE);
            checkFollowStatus(); // 检查当前用户是否已关注目标用户
        }

        // 设置 RecyclerView
        rvUserArticles.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(new ArrayList<>());
        rvUserArticles.setAdapter(articleAdapter);

        rvUserPhotos.setLayoutManager(new GridLayoutManager(this, 2));
        photoAdapter = new PhotoAdapter(new ArrayList<>());
        rvUserPhotos.setAdapter(photoAdapter);

        // 设置显示类型选择器监听
        rgUserProfileDisplayType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_user_articles) {
                switchDisplayMode(true); // 切换到文章显示模式
            } else if (checkedId == R.id.rb_user_photos) {
                switchDisplayMode(false); // 切换到图片显示模式
            }
        });

        // 设置关注按钮点击事件
        btnFollowUser.setOnClickListener(v -> {
            if (currentLoggedInUserId == null) {
                Toast.makeText(UserProfileActivity.this, "请先登录才能关注用户", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isFollowing) {
                unfollowUser(); // 取消关注
            } else {
                followUser(); // 关注
            }
        });

        // 加载用户资料和内容
        loadUserProfile(Long.parseLong(targetUserId));
        loadUserArticles(targetUserId);
        loadUserPhotos(targetUserId);
        switchDisplayMode(true); // 默认显示文章列表
    }

    /**
     * 显示或隐藏加载指示器和内容区域
     * @param show true 显示加载，false 隐藏加载
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        // 隐藏/显示内容区域
        layoutUserArticleDisplay.setVisibility(show ? View.GONE : (rbUserArticles.isChecked() ? View.VISIBLE : View.GONE));
        layoutUserPhotoDisplay.setVisibility(show ? View.GONE : (rbUserPhotos.isChecked() ? View.VISIBLE : View.GONE));
        // 隐藏/显示头像和简介
        profileImage.setVisibility(show ? View.GONE : View.VISIBLE);
        tvUserName.setVisibility(show ? View.GONE : View.VISIBLE);
        tvUserId.setVisibility(show ? View.GONE : View.VISIBLE);
        tvPersonalBio.setVisibility(show ? View.GONE : View.VISIBLE);
        rgUserProfileDisplayType.setVisibility(show ? View.GONE : View.VISIBLE);
        // 关注按钮的可见性取决于是否是自己的主页且已登录
        if (currentLoggedInUserId != null && !currentLoggedInUserId.equals(targetUserId)) {
            btnFollowUser.setVisibility(show ? View.GONE : View.VISIBLE);
        } else {
            btnFollowUser.setVisibility(View.GONE);
        }
    }

    /**
     * 切换显示模式 (文章或图片)
     * @param showArticles true 为文章显示模式，false 为图片显示模式
     */
    private void switchDisplayMode(boolean showArticles) {
        if (showArticles) {
            layoutUserArticleDisplay.setVisibility(View.VISIBLE);
            layoutUserPhotoDisplay.setVisibility(View.GONE);
            // 如果文章列表为空，则尝试加载
            if (articleAdapter.getItemCount() == 0) {
                loadUserArticles(targetUserId);
            }
        } else {
            layoutUserArticleDisplay.setVisibility(View.GONE);
            layoutUserPhotoDisplay.setVisibility(View.VISIBLE);
            // 如果图片列表为空，则尝试加载
            if (photoAdapter.getItemCount() == 0) {
                loadUserPhotos(targetUserId);
            }
        }
    }

    /**
     * 加载用户资料
     * @param userId 用户ID
     */
    private void loadUserProfile(Long userId) {
        showLoading(true);
        userService.getUserById(userId).enqueue(new Callback<BaseResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<User>> call, @NonNull Response<BaseResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    User user = response.body().getData();
                    if (user != null) {
                        tvUserName.setText(user.getUsername());
                        tvUserId.setText("ID: " + user.getId());
                        if (user.getBio() != null && !user.getBio().isEmpty()) {
                            tvPersonalBio.setText(user.getBio());
                        } else {
                            tvPersonalBio.setText("该用户没有设置个人简介。");
                        }
                    } else {
                        Toast.makeText(UserProfileActivity.this, "加载用户资料失败: 用户信息为空", Toast.LENGTH_SHORT).show();
                        Log.e("UserProfileActivity", "User profile data is null.");
                    }
                } else {
                    String errorMsg = "加载用户资料失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(UserProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileActivity", "Failed to load user profile: " + errorMsg);
                }
                showLoading(false); // 隐藏加载指示器
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<User>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this, "网络错误，无法加载用户资料: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserProfileActivity", "Network error loading user profile: ", t);
            }
        });
    }

    /**
     * 加载用户发布的文章
     * @param userId 用户ID
     */
    private void loadUserArticles(String userId) {
        articleService.getArticlesByUser(userId).enqueue(new Callback<BaseResponse<List<Article>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Response<BaseResponse<List<Article>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<Article>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        articleAdapter.updateData(baseResponse.getData());
                    } else {
                        Toast.makeText(UserProfileActivity.this, "加载用户文章失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("UserProfileActivity", "Load user articles failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "加载用户文章请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(UserProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileActivity", "User Articles API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Article>>> call, @NonNull Throwable t) {
                Toast.makeText(UserProfileActivity.this, "网络错误，无法加载用户文章: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserProfileActivity", "User Articles Network Error: ", t);
            }
        });
    }

    /**
     * 加载用户发布的图片
     * @param userId 用户ID
     */
    private void loadUserPhotos(String userId) {
        photoService.getPhotosByUser(userId).enqueue(new Callback<BaseResponse<List<Photo>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Response<BaseResponse<List<Photo>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<Photo>> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        photoAdapter.updateData(baseResponse.getData());
                    } else {
                        Toast.makeText(UserProfileActivity.this, "加载用户图片失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("UserProfileActivity", "Load user photos failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "加载用户图片请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(UserProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileActivity", "User Photos API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<Photo>>> call, @NonNull Throwable t) {
                Toast.makeText(UserProfileActivity.this, "网络错误，无法加载用户图片: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserProfileActivity", "User Photos Network Error: ", t);
            }
        });
    }

    /**
     * 检查当前用户是否已关注目标用户
     */
    private void checkFollowStatus() {
        if (currentLoggedInUserId == null || targetUserId == null) {
            return;
        }
        userFollowService.isFollowing(currentLoggedInUserId, targetUserId).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFollowing = response.body().getData();
                    updateFollowButton(); // 更新按钮UI
                } else {
                    Log.e("UserProfileActivity", "Failed to check follow status: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Throwable t) {
                Log.e("UserProfileActivity", "Network error checking follow status: ", t);
            }
        });
    }

    /**
     * 执行关注操作
     */
    private void followUser() {
        if (currentLoggedInUserId == null || targetUserId == null) {
            Toast.makeText(this, "登录信息无效，无法关注", Toast.LENGTH_SHORT).show();
            return;
        }
        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(Long.parseLong(currentLoggedInUserId));
        userFollow.setFollowingId(Long.parseLong(targetUserId));

        btnFollowUser.setEnabled(false); // 禁用按钮，防止重复点击
        userFollowService.followUser(userFollow).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                btnFollowUser.setEnabled(true); // 重新启用按钮
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFollowing = true;
                    updateFollowButton(); // 更新按钮UI
                    Toast.makeText(UserProfileActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserProfileActivity.this, "关注失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileActivity", "Follow failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                btnFollowUser.setEnabled(true); // 重新启用按钮
                Toast.makeText(UserProfileActivity.this, "网络错误，关注失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserProfileActivity", "Network error following user: ", t);
            }
        });
    }

    /**
     * 执行取消关注操作
     */
    private void unfollowUser() {
        if (currentLoggedInUserId == null || targetUserId == null) {
            Toast.makeText(this, "登录信息无效，无法取消关注", Toast.LENGTH_SHORT).show();
            return;
        }

        btnFollowUser.setEnabled(false); // 禁用按钮

        // 修改此处：直接传递两个 ID 字符串
        userFollowService.unfollowUser(currentLoggedInUserId, targetUserId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                btnFollowUser.setEnabled(true); // 重新启用按钮
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    isFollowing = false;
                    updateFollowButton(); // 更新按钮UI
                    Toast.makeText(UserProfileActivity.this, "取消关注成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserProfileActivity.this, "取消关注失败: " + (response.body() != null ? response.body().getMsg() : response.message()), Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileActivity", "Unfollow failed: " + (response.body() != null ? response.body().getMsg() : response.message()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                btnFollowUser.setEnabled(true); // 重新启用按钮
                Toast.makeText(UserProfileActivity.this, "网络错误，取消关注失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserProfileActivity", "Network error unfollowing user: ", t);
            }
        });
    }

    /**
     * 根据关注状态更新关注按钮的文本和颜色
     */
    private void updateFollowButton() {
        if (isFollowing) {
            btnFollowUser.setText("已关注");
            btnFollowUser.setBackgroundColor(getResources().getColor(R.color.design_default_color_secondary, getTheme())); // 示例：灰色
            btnFollowUser.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        } else {
            btnFollowUser.setText("关注");
            btnFollowUser.setBackgroundColor(getResources().getColor(R.color.colorPrimary, getTheme())); // 示例：主色
            btnFollowUser.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        }
    }
}
