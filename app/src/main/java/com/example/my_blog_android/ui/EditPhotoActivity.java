package com.example.my_blog_android.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.my_blog_android.R;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.utils.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPhotoActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "extra_photo"; // 用于传递图片对象的键

    private ImageView ivPhotoPreview;
    private EditText etName;
    private EditText etDescription;
    private Button btnSave;
    private ProgressBar progressBar;

    private PhotoService photoService;
    private Photo currentPhoto; // 保存当前要修改的图片对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photo);

        if (getSupportActionBar() != null) {
            // Corrected: Use getSupportActionBar() instead of SupportActionBar()
            getSupportActionBar().hide();
        }

        ivPhotoPreview = findViewById(R.id.iv_edit_photo_preview);
        etName = findViewById(R.id.et_edit_photo_name);
        etDescription = findViewById(R.id.et_edit_photo_description);
        btnSave = findViewById(R.id.btn_save_photo);
        progressBar = findViewById(R.id.edit_photo_progress_bar);

        photoService = ApiClient.getPhotoService();

        // 获取传递过来的图片对象
        currentPhoto = (Photo) getIntent().getSerializableExtra(EXTRA_PHOTO);

        if (currentPhoto != null) {
            etName.setText(currentPhoto.getName());
            etDescription.setText(currentPhoto.getDescription());

            // 加载图片预览
            String filePathFromDb = currentPhoto.getFilePath();
            String relativePath = filePathFromDb;
            if (filePathFromDb != null && filePathFromDb.startsWith("uploads/")) {
                relativePath = filePathFromDb.substring("uploads/".length());
            }
            String imageUrl = ApiClient.BASE_URL + "files/" + relativePath;

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivPhotoPreview);

        } else {
            Toast.makeText(this, "无法加载图片数据进行修改", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePhoto();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        etName.setEnabled(!show);
        etDescription.setEnabled(!show);
    }

    private void savePhoto() {
        String newName = etName.getText().toString().trim();
        String newDescription = etDescription.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "图片名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新 currentPhoto 对象
        currentPhoto.setName(newName);
        currentPhoto.setDescription(newDescription); // 描述可以是空的

        showLoading(true);

        photoService.updatePhoto(currentPhoto).enqueue(new Callback<BaseResponse<Photo>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Photo>> call, @NonNull Response<BaseResponse<Photo>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Photo> baseResponse = response.body();
                    if (baseResponse.getCode() == 200) {
                        Toast.makeText(EditPhotoActivity.this, "图片信息修改成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // 设置结果为成功
                        finish(); // 关闭当前 Activity
                    } else {
                        Toast.makeText(EditPhotoActivity.this, "图片信息修改失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e("EditPhotoActivity", "Update failed: " + baseResponse.getMsg());
                    }
                } else {
                    String errorMsg = "图片信息修改请求失败，请稍后再试。";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(EditPhotoActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("EditPhotoActivity", "API Call Failed: " + response.code() + " " + errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Photo>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(EditPhotoActivity.this, "网络错误，无法修改图片信息: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("EditPhotoActivity", "Network Error: ", t);
            }
        });
    }
}
