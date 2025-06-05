package com.example.my_blog_android.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.my_blog_android.R;
import com.example.my_blog_android.api.UserService;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.User; // 导入 User 模型
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etEditBio;
    private Button btnSaveProfile;
    private ProgressBar progressBar;

    private UserService userService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etEditBio = findViewById(R.id.et_edit_bio);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        progressBar = findViewById(R.id.edit_profile_progress_bar);

        userService = ApiClient.getUserService();
        sessionManager = SessionManager.getInstance(this);

        // 加载当前用户的个人简介
        String currentBio = sessionManager.getCurrentUserBio();
        if (currentBio != null && !currentBio.isEmpty()) {
            etEditBio.setText(currentBio);
        }

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    private void saveProfile() {
        String newBio = etEditBio.getText().toString().trim();
        String userId = sessionManager.getCurrentUserId();

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "用户未登录，无法保存个人信息", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        User userToUpdate = new User();
        userToUpdate.setId(Long.parseLong(userId));
        userToUpdate.setBio(newBio); // 只更新 bio 字段

        // 调用 API 更新个人信息
        userService.updateUserProfile(userToUpdate).enqueue(new Callback<BaseResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<User>> call, @NonNull Response<BaseResponse<User>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<User> baseResponse = response.body();
                    if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                        // 更新 SessionManager 中的用户简介
                        sessionManager.saveUser(baseResponse.getData());
                        Toast.makeText(EditProfileActivity.this, "个人信息保存成功！", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // 设置结果，以便 ProfileActivity 刷新
                        finish();
                    } else {
                        Toast.makeText(EditProfileActivity.this, "保存失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("EditProfileActivity", "Save failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "保存请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("EditProfileActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<User>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(EditProfileActivity.this, "网络错误，保存失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("EditProfileActivity", "Network Error: ", t);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!show);
        etEditBio.setEnabled(!show);
    }
}
