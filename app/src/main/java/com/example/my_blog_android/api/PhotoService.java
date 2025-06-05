package com.example.my_blog_android.api;

import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
// TODO: 如果需要，可以添加上传、修改、删除图片的方法
// import okhttp3.MultipartBody;
// import okhttp3.RequestBody;
// import retrofit2.http.DELETE;
// import retrofit2.http.Multipart;
// import retrofit2.http.POST;
// import retrofit2.http.PUT;
// import retrofit2.http.Part;
// import retrofit2.http.Query;


public interface PhotoService {

    @GET("photo/list")
        // 假设您也会添加一个获取所有图片的接口
    Call<BaseResponse<List<Photo>>> getAllPhotos();

    @GET("photo/user/{userId}")
    Call<BaseResponse<List<Photo>>> getPhotosByUser(@Path("userId") String userId);

    @GET("photo/{id}")
    Call<BaseResponse<Photo>> getPhotoById(@Path("id") Long id);

    @Multipart
    @POST("photo/upload")
    Call<BaseResponse<Photo>> uploadPhoto(
            @Part MultipartBody.Part file,
            @Part("name") RequestBody name,
            @Part("description") RequestBody description, // description可以是可选的
            @Part("authorId") RequestBody authorId
    );

    @DELETE("photo/delete/{id}")
    Call<BaseResponse<String>> deletePhoto(@Path("id") Long id);

    @PUT("photo/update")
    Call<BaseResponse<Photo>> updatePhoto(@Body Photo photo);

    // 新增：根据关键词搜索图片
    @GET("photo/search")
    Call<BaseResponse<List<Photo>>> searchPhotos(@Query("query") String query);
}