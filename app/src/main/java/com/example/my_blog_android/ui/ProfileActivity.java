package com.example.my_blog_android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.my_blog_android.R;
import com.example.my_blog_android.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private TextView userNameTextView;
    private TextView userIdTextView;
    private TextView personalBioTextView;

    private LinearLayout llMyArticles;

    private LinearLayout llMyFavorites; // 我的收藏
    private LinearLayout llMyFollowing; // 我的关注
    private LinearLayout llMyFollowers; // 我的粉丝
    private LinearLayout llBrowsingHistory;
    private LinearLayout llEditProfile;
    private LinearLayout llAccountSettings;
    private LinearLayout llLogout;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        userNameTextView = findViewById(R.id.user_name);
        userIdTextView = findViewById(R.id.user_id);
        personalBioTextView = findViewById(R.id.tv_personal_bio);

        llMyArticles = findViewById(R.id.ll_my_articles);
        llMyFavorites = findViewById(R.id.ll_my_favorites); // 初始化我的收藏
        llMyFollowing = findViewById(R.id.ll_my_following); // 初始化
        llMyFollowers = findViewById(R.id.ll_my_followers); // 初始化
        llBrowsingHistory = findViewById(R.id.ll_browsing_history);
        llEditProfile = findViewById(R.id.ll_edit_profile);
        llAccountSettings = findViewById(R.id.ll_account_settings);
        llLogout = findViewById(R.id.ll_logout);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        SessionManager sessionManager = SessionManager.getInstance(this);

        String currentUsername = sessionManager.getCurrentUsername();
        String currentUserId = sessionManager.getCurrentUserId();
        String currentUserBio = sessionManager.getCurrentUserBio();

        if (currentUsername != null && !currentUsername.isEmpty()) {
            userNameTextView.setText(currentUsername);
        } else {
            userNameTextView.setText("用户名: 未知");
        }

        if (currentUserId != null && !currentUserId.isEmpty()) {
            userIdTextView.setText("用户ID: " + currentUserId);
        } else {
            userIdTextView.setText("用户ID: 未知");
        }

        if (currentUserBio != null && !currentUserBio.isEmpty()) {
            personalBioTextView.setText(currentUserBio);
        } else {
            personalBioTextView.setText("这里是个人简介，一句话介绍自己。");
        }

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else if (itemId == R.id.nav_publish) {
                Intent intent = new Intent(ProfileActivity.this, PublishActivity.class);
                startActivity(intent);
                finish();
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(ProfileActivity.this, "您已在个人中心", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        llMyArticles.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MyContentActivity.class);
            startActivity(intent);
        });


        // 为“我的收藏”列表项添加点击事件
        llMyFavorites.setOnClickListener(v -> {
            if (currentUserId != null) {
                Intent intent = new Intent(ProfileActivity.this, FavoriteListActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(ProfileActivity.this, "请先登录查看收藏列表", Toast.LENGTH_SHORT).show();
            }
        });

        // 为“我的关注”列表项添加点击事件
        llMyFollowing.setOnClickListener(v -> {
            if (currentUserId != null) {
                Intent intent = new Intent(ProfileActivity.this, UserListActivity.class);
                intent.putExtra(UserListActivity.EXTRA_USER_LIST_TYPE, UserListActivity.TYPE_FOLLOWING);
                intent.putExtra(UserListActivity.EXTRA_TARGET_USER_ID, currentUserId);
                startActivity(intent);
            } else {
                Toast.makeText(ProfileActivity.this, "请先登录查看关注列表", Toast.LENGTH_SHORT).show();
            }
        });

        // 为“我的粉丝”列表项添加点击事件
        llMyFollowers.setOnClickListener(v -> {
            if (currentUserId != null) {
                Intent intent = new Intent(ProfileActivity.this, UserListActivity.class);
                intent.putExtra(UserListActivity.EXTRA_USER_LIST_TYPE, UserListActivity.TYPE_FOLLOWERS);
                intent.putExtra(UserListActivity.EXTRA_TARGET_USER_ID, currentUserId);
                startActivity(intent);
            } else {
                Toast.makeText(ProfileActivity.this, "请先登录查看粉丝列表", Toast.LENGTH_SHORT).show();
            }
        });

        llBrowsingHistory.setOnClickListener(v -> {
            Toast.makeText(ProfileActivity.this, "浏览历史功能待实现", Toast.LENGTH_SHORT).show();
        });

        llEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        llAccountSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        llLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Toast.makeText(ProfileActivity.this, "已退出登录", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sessionManager = SessionManager.getInstance(this);
        String updatedBio = sessionManager.getCurrentUserBio();
        if (updatedBio != null && !updatedBio.isEmpty()) {
            personalBioTextView.setText(updatedBio);
        } else {
            personalBioTextView.setText("这里是个人简介，一句话介绍自己。");
        }
    }
}
