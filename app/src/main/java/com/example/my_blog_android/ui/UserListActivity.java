package com.example.my_blog_android.ui;

import android.content.DialogInterface; // 导入 DialogInterface
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // 导入 AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.adapter.UserAdapter;
import com.example.my_blog_android.api.UserFollowService;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.User;
import com.example.my_blog_android.model.UserFollow; // 导入 UserFollow
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// UserListActivity 实现 UserAdapter.OnItemLongClickListener 接口
public class UserListActivity extends AppCompatActivity implements UserAdapter.OnItemLongClickListener {

    public static final String EXTRA_USER_LIST_TYPE = "user_list_type";
    public static final String EXTRA_TARGET_USER_ID = "target_user_id";

    public static final int TYPE_FOLLOWING = 1; // 类型：我关注的用户
    public static final int TYPE_FOLLOWERS = 2; // 类型：关注我的用户 (粉丝)

    private RecyclerView rvUserList;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private UserAdapter userAdapter;
    private UserFollowService userFollowService;
    private SessionManager sessionManager;

    private int listType;
    private String targetUserId; // 要查询的关注/粉丝列表所属的用户ID
    private String currentLoggedInUserId; // 当前登录用户的ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list); // 需要创建这个布局文件

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        rvUserList = findViewById(R.id.rv_user_list);
        progressBar = findViewById(R.id.user_list_progress_bar);
        tvTitle = findViewById(R.id.tv_user_list_title);

        userFollowService = ApiClient.getUserFollowService();
        sessionManager = SessionManager.getInstance(this);
        currentLoggedInUserId = sessionManager.getCurrentUserId(); // 获取当前登录用户ID

        // 从 Intent 获取数据
        listType = getIntent().getIntExtra(EXTRA_USER_LIST_TYPE, -1);
        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);

        if (targetUserId == null || targetUserId.isEmpty() || listType == -1) {
            Toast.makeText(this, "请求用户列表无效。", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 根据列表类型设置标题
        if (listType == TYPE_FOLLOWING) {
            tvTitle.setText("我的关注");
        } else if (listType == TYPE_FOLLOWERS) {
            tvTitle.setText("我的粉丝");
        } else {
            tvTitle.setText("用户列表");
        }

        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(new ArrayList<>(), this); // UserAdapter 需要创建
        rvUserList.setAdapter(userAdapter);
        // 设置用户适配器的长按监听器
        userAdapter.setOnItemLongClickListener(this);

        loadUserList(); // 加载用户列表数据
    }

    /**
     * 显示或隐藏加载指示器
     * @param show true 显示加载，false 隐藏加载
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvUserList.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * 加载用户列表 (关注或粉丝)
     */
    private void loadUserList() {
        showLoading(true);

        Call<BaseResponse<List<User>>> call;
        if (listType == TYPE_FOLLOWING) {
            call = userFollowService.getFollowingUsers(targetUserId);
        } else if (listType == TYPE_FOLLOWERS) {
            call = userFollowService.getFollowerUsers(targetUserId);
        } else {
            showLoading(false);
            Toast.makeText(this, "未知列表类型。", Toast.LENGTH_SHORT).show();
            return;
        }

        call.enqueue(new Callback<BaseResponse<List<User>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<User>>> call, @NonNull Response<BaseResponse<List<User>>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<User> users = response.body().getData();
                    if (users != null) {
                        userAdapter.updateData(users);
                        if (users.isEmpty()) {
                            if (listType == TYPE_FOLLOWING) {
                                Toast.makeText(UserListActivity.this, "您还没有关注任何用户。", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserListActivity.this, "您还没有任何粉丝。", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(UserListActivity.this, "未找到用户。", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "加载用户列表失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(UserListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("UserListActivity", errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<User>>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(UserListActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserListActivity", "Network error loading user list: ", t);
            }
        });
    }

    // --- 实现 UserAdapter.OnItemLongClickListener 接口的方法 ---
    @Override
    public void onItemLongClick(int position, User user) {
        // 只有在“我的关注”列表且当前用户是列表所有者时才显示取消关注选项
        if (listType == TYPE_FOLLOWING && currentLoggedInUserId != null && currentLoggedInUserId.equals(targetUserId)) {
            showUnfollowConfirmationDialog(user, position);
        } else {
            // 对于粉丝列表或其他情况，可以不提供长按操作或提供其他操作
            Toast.makeText(this, "对该用户无长按操作", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示取消关注确认对话框
     * @param user 要取消关注的用户对象
     * @param position 用户在适配器中的位置
     */
    private void showUnfollowConfirmationDialog(User user, int position) {
        new AlertDialog.Builder(this)
                .setTitle("取消关注")
                .setMessage("您确定要取消关注 " + user.getUsername() + " 吗？")
                .setPositiveButton("取消关注", (dialog, which) -> {
                    unfollowUser(user.getId().toString(), position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 执行取消关注操作
     * @param followingUserId 被取消关注的用户ID
     * @param position 用户在适配器中的位置
     */
    private void unfollowUser(String followingUserId, int position) {
        if (currentLoggedInUserId == null || followingUserId == null) {
            Toast.makeText(this, "登录信息无效，无法取消关注", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true); // 显示加载进度条

        // 调用 UserFollowService 的 unfollowUser API
        userFollowService.unfollowUser(currentLoggedInUserId, followingUserId).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                showLoading(false); // 隐藏加载进度条
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    Toast.makeText(UserListActivity.this, "取消关注成功", Toast.LENGTH_SHORT).show();
                    userAdapter.removeItem(position); // 从适配器中移除项
                } else {
                    String errorMsg = "取消关注失败: " + (response.body() != null ? response.body().getMsg() : response.message());
                    Toast.makeText(UserListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("UserListActivity", "Unfollow failed: " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                showLoading(false); // 隐藏加载进度条
                Toast.makeText(UserListActivity.this, "网络错误，取消关注失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UserListActivity", "Network error unfollowing user: ", t);
            }
        });
    }
}
