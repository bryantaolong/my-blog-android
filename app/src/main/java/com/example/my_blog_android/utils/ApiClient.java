package com.example.my_blog_android.utils;

import com.example.my_blog_android.api.ArticleCommentService;
import com.example.my_blog_android.api.ArticleService;
import com.example.my_blog_android.api.PhotoCommentService;
import com.example.my_blog_android.api.PhotoService;
import com.example.my_blog_android.api.UserFavoriteService;
import com.example.my_blog_android.api.UserFollowService;
import com.example.my_blog_android.api.UserService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class ApiClient {

    public static final String BASE_URL = "http://10.0.2.2:8080/"; // 模拟器访问PC的localhost地址
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static UserService getUserService() {
        return getClient().create(UserService.class);
    }

    public static ArticleService getArticleService() {
        return getClient().create(ArticleService.class);
    }

    public static PhotoService getPhotoService() {
        return getClient().create(PhotoService.class);
    }

    public static ArticleCommentService getArticleCommentService() {
        return getClient().create(ArticleCommentService.class);
    }

    public static PhotoCommentService getPhotoCommentService() {
        return getClient().create(PhotoCommentService.class);
    }

    public static UserFollowService getUserFollowService() {
        return getClient().create(UserFollowService.class);
    }

    public static UserFavoriteService getUserFavoriteService() {
        return getClient().create(UserFavoriteService.class);
    }
}