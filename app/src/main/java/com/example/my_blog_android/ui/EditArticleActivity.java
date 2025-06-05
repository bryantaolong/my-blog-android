package com.example.my_blog_android.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.my_blog_android.R;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.utils.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditArticleActivity extends AppCompatActivity {

    public static final String EXTRA_ARTICLE = "extra_article"; // 用于传递文章对象的键

    private EditText etTitle;
    private EditText etContent;
    private Button btnSave;
    private ProgressBar progressBar;

    private ArticleService articleService;
    private Article currentArticle; // 保存当前要修改的文章对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_article);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etTitle = findViewById(R.id.et_edit_article_title);
        etContent = findViewById(R.id.et_edit_article_content);
        btnSave = findViewById(R.id.btn_save_article);
        progressBar = findViewById(R.id.edit_article_progress_bar);

        articleService = ApiClient.getArticleService();

        // 获取传递过来的文章对象
        currentArticle = (Article) getIntent().getSerializableExtra(EXTRA_ARTICLE);

        if (currentArticle != null) {
            etTitle.setText(currentArticle.getTitle());
            etContent.setText(currentArticle.getContent());
        } else {
            Toast.makeText(this, "无法加载文章数据进行修改", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveArticle();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        etTitle.setEnabled(!show);
        etContent.setEnabled(!show);
    }

    private void saveArticle() {
        String newTitle = etTitle.getText().toString().trim();
        String newContent = etContent.getText().toString().trim();

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "文章标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newContent.isEmpty()) {
            Toast.makeText(this, "文章内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新 currentArticle 对象
        currentArticle.setTitle(newTitle);
        currentArticle.setContent(newContent);

        showLoading(true);

        articleService.updateArticle(currentArticle).enqueue(new Callback<BaseResponse<Article>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Article>> call, @NonNull Response<BaseResponse<Article>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Article> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(EditArticleActivity.this, "文章修改成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // 设置结果为成功
                        finish(); // 关闭当前 Activity
                    } else {
                        Toast.makeText(EditArticleActivity.this, "文章修改失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("EditArticleActivity", "Update failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "文章修改请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(EditArticleActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("EditArticleActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Article>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(EditArticleActivity.this, "网络错误，无法修改文章: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("EditArticleActivity", "Network Error: ", t);
            }
        });
    }
}
