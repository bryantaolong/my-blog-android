package com.example.my_blog_android.api;

import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ArticleService {

    @GET("article/list")
    Call<BaseResponse<List<Article>>> getAllArticles();

    @GET("article/user/{userId}")
    Call<BaseResponse<List<Article>>> getArticlesByUser(@Path("userId") String userId);

    @GET("article/id/{id}")
    Call<BaseResponse<Article>> getArticleById(@Path("id") Long id);

    @POST("article/publish")
    Call<BaseResponse<Article>> publishArticle(@Body Article article);

    @PUT("article/update")
    Call<BaseResponse<Article>> updateArticle(@Body Article article);

    @DELETE("article/delete/{id}")
    Call<BaseResponse<String>> deleteArticle(@Path("id") Long id);

    // 新增：根据关键词搜索文章
    @GET("article/search")
    Call<BaseResponse<List<Article>>> searchArticles(@Query("query") String query);
}