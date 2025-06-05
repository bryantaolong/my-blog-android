package com.example.my_blog_android.api;

import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.User;
import com.example.my_blog_android.model.ChangePasswordRequest; // 导入 ChangePasswordRequest

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT; // 导入 PUT
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {

    @POST("user/login")
    Call<BaseResponse<User>> login(@Body User user);

    @POST("user/register")
    Call<BaseResponse<User>> register(@Body User user);

    @PUT("user/change-password") // 新增：修改密码接口
    Call<BaseResponse<String>> changePassword(@Body ChangePasswordRequest request);

    @GET("user/{id}") // 新增：根据ID获取用户详情
    Call<BaseResponse<User>> getUserById(@Path("id") Long id);

    @PUT("user/update-profile") // 新增：更新用户个人信息接口
    Call<BaseResponse<User>> updateUserProfile(@Body User user);

    // 新增：根据关键词搜索用户
    @GET("user/search")
    Call<BaseResponse<List<User>>> searchUsers(@Query("query") String query);
}
