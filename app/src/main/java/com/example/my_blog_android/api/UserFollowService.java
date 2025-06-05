package com.example.my_blog_android.api;

import com.example.my_blog_android.model.BaseResponse;
import com.example.my_blog_android.model.User; // 导入 User 模型
import com.example.my_blog_android.model.UserFollow; // 导入 UserFollow 模型
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;

public interface UserFollowService {

    @POST("follow")
    Call<BaseResponse<String>> followUser(@Body UserFollow userFollow);

    @DELETE("follow/{followerId}/{followingId}")
    Call<BaseResponse<String>> unfollowUser(
            @Path("followerId") String followerId,
            @Path("followingId") String followingId
    );

    @GET("follow/following/{userId}")
    Call<BaseResponse<List<User>>> getFollowingUsers(@Path("userId") String userId);

    @GET("follow/followers/{userId}")
    Call<BaseResponse<List<User>>> getFollowerUsers(@Path("userId") String userId);

    @GET("follow/isFollowing/{followerId}/{followingId}")
    Call<BaseResponse<Boolean>> isFollowing(@Path("followerId") String followerId, @Path("followingId") String followingId);
}
