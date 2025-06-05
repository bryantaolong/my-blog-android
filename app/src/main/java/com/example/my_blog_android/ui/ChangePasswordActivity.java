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
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.api.UserService;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.ChangePasswordRequest; // 导入 ChangePasswordRequest
import com.example.my_blog_android.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    // private TextInputEditText etOldPassword; // 如果需要旧密码验证，取消注释
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmNewPassword;
    private Button btnChangePassword;
    private ProgressBar progressBar;

    private UserService userService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 确保这些ID与 activity_change_password.xml 中的ID完全匹配
        // etOldPassword = findViewById(R.id.et_old_password); // 如果需要旧密码验证，取消注释
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
        progressBar = findViewById(R.id.change_password_progress_bar);

        userService = ApiClient.getUserService();
        sessionManager = SessionManager.getInstance(this);

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnChangePassword.setEnabled(!show);
        // etOldPassword.setEnabled(!show); // 如果需要旧密码验证，取消注释
        etNewPassword.setEnabled(!show);
        etConfirmNewPassword.setEnabled(!show);
    }

    private void changePassword() {
        String currentUserId = sessionManager.getCurrentUserId();
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "用户未登录，无法修改密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // String oldPassword = etOldPassword.getText().toString().trim(); // 如果需要旧密码验证，取消注释
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // 创建请求对象
        // 如果后端需要旧密码验证，请使用 ChangePasswordRequest(Long.parseLong(currentUserId), oldPassword, newPassword)
        ChangePasswordRequest request = new ChangePasswordRequest(Long.parseLong(currentUserId), newPassword);

        userService.changePassword(request).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<String>> call, @NonNull Response<BaseResponse<String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<String> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(ChangePasswordActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                        finish(); // 修改成功后关闭当前 Activity
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, "密码修改失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("ChangePasswordActivity", "Change password failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "密码修改请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(ChangePasswordActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("ChangePasswordActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<String>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ChangePasswordActivity.this, "网络错误，无法修改密码: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ChangePasswordActivity", "Network Error: ", t);
            }
        });
    }
}
