package com.example.my_blog_android.api;

import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.Photo;
import com.example.my_blog_android.model.UserFavorite;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query; // 导入 Query

import java.util.List;

public interface UserFavoriteService {

    @POST("favorite")
    Call<BaseResponse<String>> addFavorite(@Body UserFavorite userFavorite);

    // 修改为使用 @Query 参数传递信息
    @DELETE("favorite")
    Call<BaseResponse<String>> removeFavorite(
            @Query("userId") String userId,
            @Query("itemType") String itemType,
            @Query("itemId") String itemId);

    @GET("favorite/isFavorited/{userId}/{itemType}/{itemId}")
    Call<BaseResponse<Boolean>> isFavorited(
            @Path("userId") String userId,
            @Path("itemType") String itemType,
            @Path("itemId") String itemId);

    @GET("favorite/articles/{userId}")
    Call<BaseResponse<List<Article>>> getFavoriteArticles(@Path("userId") String userId);

    @GET("favorite/photos/{userId}")
    Call<BaseResponse<List<Photo>>> getFavoritePhotos(@Path("userId") String userId);
}
