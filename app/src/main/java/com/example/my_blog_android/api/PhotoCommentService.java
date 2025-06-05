package com.example.my_blog_android.api;

import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.PhotoComment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PhotoCommentService {

    @GET("photo/comment/photo/{photoId}")
    Call<BaseResponse<List<PhotoComment>>> getCommentsByPhoto(@Path("photoId") Long photoId);

    @POST("photo/comment/publish")
    Call<BaseResponse<PhotoComment>> publishComment(@Body PhotoComment comment);

    @DELETE("photo/comment/delete/{id}")
    Call<BaseResponse<String>> deleteComment(@Path("id") Long id);
}
