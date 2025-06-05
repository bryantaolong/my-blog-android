package com.example.my_blog_android.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout; // 导入 LinearLayout
import android.widget.ProgressBar;
import android.widget.RadioButton; // 导入 RadioButton
import android.widget.RadioGroup; // 导入 RadioGroup
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.my_blog_android.R;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.PhotoService; // 导入 PhotoService
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo; // 导入 Photo model
import com.example.my_blog_android.utils.ApiClient;
import com.example.my_blog_android.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishActivity extends AppCompatActivity {

    private TextInputEditText etArticleTitle;
    private TextInputEditText etArticleContent;
    private TextInputEditText etPhotoName;
    private TextInputEditText etPhotoDescription;
    private ImageView ivPhotoPreview;
    private Button btnSelectPhoto;
    private Button btnPublish;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;

    private RadioGroup rgPublishType;
    private RadioButton rbPublishArticle;
    private RadioButton rbPublishPhoto;
    private LinearLayout layoutArticlePublish;
    private LinearLayout layoutPhotoPublish;

    private ArticleService articleService;
    private PhotoService photoService; // 声明 PhotoService
    private SessionManager sessionManager;

    private Uri selectedPhotoUri; // 用于存储选择的图片URI

    // 用于处理图片选择结果
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPhotoUri = result.getData().getData();
                    if (selectedPhotoUri != null) {
                        Glide.with(this).load(selectedPhotoUri).into(ivPhotoPreview);
                    }
                } else {
                    Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化文章相关视图组件
        etArticleTitle = findViewById(R.id.et_article_title);
        etArticleContent = findViewById(R.id.et_article_content);

        // 初始化图片相关视图组件
        etPhotoName = findViewById(R.id.et_photo_name);
        etPhotoDescription = findViewById(R.id.et_photo_description);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);

        // 初始化通用视图组件
        btnPublish = findViewById(R.id.btn_publish_article); // 按钮ID保持不变，但文本会动态修改
        progressBar = findViewById(R.id.publish_progress_bar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 初始化发布类型选择器
        rgPublishType = findViewById(R.id.rg_publish_type);
        rbPublishArticle = findViewById(R.id.rb_publish_article);
        rbPublishPhoto = findViewById(R.id.rb_publish_photo);
        layoutArticlePublish = findViewById(R.id.layout_article_publish);
        layoutPhotoPublish = findViewById(R.id.layout_photo_publish);

        // 设置底部导航栏的选中项为“发布”
        bottomNavigationView.setSelectedItemId(R.id.nav_publish);

        // 设置底部导航栏的点击监听器
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(PublishActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else if (itemId == R.id.nav_publish) {
                Toast.makeText(PublishActivity.this, "您已在发布界面", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(PublishActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        });

        // 初始化 Retrofit 服务和 SessionManager
        articleService = ApiClient.getArticleService();
        photoService = ApiClient.getPhotoService(); // 初始化 PhotoService
        sessionManager = SessionManager.getInstance(getApplicationContext());

        // 设置发布按钮的点击监听器
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rbPublishArticle.isChecked()) {
                    publishArticle();
                } else if (rbPublishPhoto.isChecked()) {
                    publishPhoto();
                }
            }
        });

        // 设置选择图片按钮的点击监听器
        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        // 设置发布类型选择器监听
        rgPublishType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_publish_article) {
                    switchPublishMode(true); // 切换到文章发布模式
                } else if (checkedId == R.id.rb_publish_photo) {
                    switchPublishMode(false); // 切换到图片发布模式
                }
            }
        });

        // 默认显示文章发布模式
        switchPublishMode(true);
    }

    /**
     * 切换发布模式 (文章或图片)
     * @param isArticleMode true 为文章模式，false 为图片模式
     */
    private void switchPublishMode(boolean isArticleMode) {
        if (isArticleMode) {
            layoutArticlePublish.setVisibility(View.VISIBLE);
            layoutPhotoPublish.setVisibility(View.GONE);
            btnPublish.setText("发布文章");
        } else {
            layoutArticlePublish.setVisibility(View.GONE);
            layoutPhotoPublish.setVisibility(View.VISIBLE);
            btnPublish.setText("发布图片");
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void publishArticle() {
        String title = etArticleTitle.getText().toString().trim();
        String content = etArticleContent.getText().toString().trim();
        String authorId = sessionManager.getCurrentUserId();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "文章标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "文章内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(authorId)) {
            Toast.makeText(this, "用户未登录，无法发布文章", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setAuthorId(authorId);

        Call<BaseResponse<Article>> call = articleService.publishArticle(article);
        call.enqueue(new Callback<BaseResponse<Article>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Article>> call, @NonNull Response<BaseResponse<Article>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Article> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(PublishActivity.this, "文章发布成功！", Toast.LENGTH_SHORT).show();
                        navigateToMainAndFinish();
                    } else {
                        Toast.makeText(PublishActivity.this, "发布失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("PublishActivity", "Article publish failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "文章发布请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(PublishActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("PublishActivity", "Article API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Article>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(PublishActivity.this, "网络错误，文章发布失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PublishActivity", "Article Network Error: ", t);
            }
        });
    }

    private void publishPhoto() {
        String name = etPhotoName.getText().toString().trim();
        String description = etPhotoDescription.getText().toString().trim();
        String authorId = sessionManager.getCurrentUserId();

        if (selectedPhotoUri == null) {
            Toast.makeText(this, "请选择一张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "图片名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(authorId)) {
            Toast.makeText(this, "用户未登录，无法发布图片", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        try {
            // 将 Uri 转换为 File，或者更安全地使用 InputStream 传输
            File file = uriToFile(selectedPhotoUri);
            if (file == null) {
                Toast.makeText(this, "无法获取图片文件", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedPhotoUri)), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), name);
            RequestBody descriptionPart = RequestBody.create(MediaType.parse("text/plain"), description);
            RequestBody authorIdPart = RequestBody.create(MediaType.parse("text/plain"), authorId);

            Call<BaseResponse<Photo>> call = photoService.uploadPhoto(body, namePart, descriptionPart, authorIdPart);
            call.enqueue(new Callback<BaseResponse<Photo>>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse<Photo>> call, @NonNull Response<BaseResponse<Photo>> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse<Photo> baseResponse = response.body();
                        if (baseResponse.getCode() == 200) {
                            Toast.makeText(PublishActivity.this, "图片发布成功！", Toast.LENGTH_SHORT).show();
                            navigateToMainAndFinish();
                        } else {
                            Toast.makeText(PublishActivity.this, "发布失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            Log.e("PublishActivity", "Photo publish failed: " + baseResponse.getMsg());
                        }
                    } else {
                        String errorMsg = "图片发布请求失败，请稍后再试。";
                        if (response.errorBody() != null) {
                            try {
                                errorMsg = response.errorBody().string();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(PublishActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        Log.e("PublishActivity", "Photo API Call Failed: " + response.code() + " " + errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse<Photo>> call, @NonNull Throwable t) {
                    showLoading(false);
                    Toast.makeText(PublishActivity.this, "网络错误，图片发布失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PublishActivity", "Photo Network Error: ", t);
                }
            });

        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("PublishActivity", "Error processing image: ", e);
        }
    }

    /**
     * 将 Uri 转换为 File (用于文件上传)。
     * 注意：此方法在某些情况下可能无法直接获取到真实文件路径，
     * 对于复杂的 Uri (如来自 Google Photos 或云存储)，可能需要更复杂的处理，
     * 例如将文件复制到应用内部存储。这里提供一个简单的实现。
     */
    private File uriToFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            File tempFile = new File(getCacheDir(), "upload_temp_" + System.currentTimeMillis());
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.close();
            return tempFile;
        } catch (Exception e) {
            Log.e("PublishActivity", "Error converting URI to File: ", e);
            return null;
        }
    }

    /**
     * 显示或隐藏加载指示器，并启用/禁用按钮和文本框
     * @param show true 显示加载，false 隐藏加载
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnPublish.setEnabled(!show);
        // 根据当前模式禁用/启用相应的输入框
        if (rbPublishArticle.isChecked()) {
            etArticleTitle.setEnabled(!show);
            etArticleContent.setEnabled(!show);
        } else {
            etPhotoName.setEnabled(!show);
            etPhotoDescription.setEnabled(!show);
            btnSelectPhoto.setEnabled(!show);
        }
    }

    private void navigateToMainAndFinish() {
        Intent intent = new Intent(PublishActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
