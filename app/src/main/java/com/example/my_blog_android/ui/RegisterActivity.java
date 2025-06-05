package com.example.my_blog_android.ui; // 注意：您的包名是 .ui

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.my_blog_android.R;
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.api.UserService;
import com.google.android.material.textfield.TextInputEditText;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 注意：将RegisterActivity从ui包移动到根包或者保持一致性。
// 如果您希望它在ui包下，那么AndroidManifest.xml中也需要更新为 .ui.RegisterActivity
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLoginLink;

    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // 设置布局文件

        // 初始化视图组件
        etUsername = findViewById(R.id.et_register_username);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvBackToLoginLink = findViewById(R.id.tv_back_to_login_link);

        // 初始化 Retrofit 服务
        userService = ApiClient.getUserService();

        // 设置注册按钮点击事件
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 移除此处提前的Toast，等待网络请求结果
                // Toast.makeText(RegisterActivity.this, "尝试注册...", Toast.LENGTH_SHORT).show();

                // 禁用按钮，防止重复点击
                btnRegister.setEnabled(false);

                // Retrofit 注册示例
                User registerRequest = new User();
                registerRequest.setUsername(username);
                registerRequest.setPassword(password);
                // 角色会由后端默认设置为 "USER"

                userService.register(registerRequest).enqueue(new Callback<BaseResponse<User>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                        // 重新启用按钮
                        btnRegister.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<User> baseResponse = response.body();
                            if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                                Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                                finish(); // 注册成功后返回登录页
                            } else {
                                Toast.makeText(RegisterActivity.this, baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "注册失败: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                        // 重新启用按钮
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                    }
                });
            }
        });

        // 设置返回登录链接点击事件
        tvBackToLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前Activity，返回上一个Activity（LoginActivity）
            }
        });
    }
}