package com.example.my_blog_android.api;

import com.example.my_blog_android.model.ArticleComment;
import com.example.my_blog_android.model.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ArticleCommentService {

    @GET("article/comment/article/{articleId}")
    Call<BaseResponse<List<ArticleComment>>> getCommentsByArticle(@Path("articleId") Long articleId);

    @POST("article/comment/publish")
    Call<BaseResponse<ArticleComment>> publishComment(@Body ArticleComment comment);

    @DELETE("article/comment/delete/{id}")
    Call<BaseResponse<String>> deleteComment(@Path("id") Long id);
}
