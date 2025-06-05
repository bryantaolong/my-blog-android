package com.example.my_blog_android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.my_blog_android.model.User; // 确保导入 User 模型

public class SessionManager {

    private static final String PREF_NAME = "MyBlogSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ROLE = "userRole"; // 新增：用于存储用户角色
    private static final String KEY_USER_BIO = "userBio"; // 新增：用于存储用户简介

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context _context;

    // 单例模式
    private static SessionManager instance;

    private SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * 保存用户登录信息
     * @param user 登录成功的用户对象
     */
    public void saveUser(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        if (user != null) {
            if (user.getId() != null) {
                editor.putString(KEY_USER_ID, String.valueOf(user.getId()));
            } else {
                editor.remove(KEY_USER_ID);
            }
            editor.putString(KEY_USERNAME, user.getUsername());
            editor.putString(KEY_USER_ROLE, user.getRole()); // 保存用户角色
            editor.putString(KEY_USER_BIO, user.getBio()); // 保存用户简介
        }
        editor.apply(); // 使用 apply() 异步提交，提高性能
    }

    /**
     * 获取当前登录的用户ID
     * @return 用户ID，如果未登录则为null
     */
    public String getCurrentUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    /**
     * 获取当前登录的用户名
     * @return 用户名，如果未登录则为null
     */
    public String getCurrentUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    /**
     * 获取当前登录的用户角色
     * @return 用户角色 (例如 "USER", "ADMIN")，如果未登录则为null
     */
    public String getCurrentUserRole() {
        return pref.getString(KEY_USER_ROLE, null);
    }

    /**
     * 获取当前登录的用户简介
     * @return 用户简介，如果未登录或未设置则为null
     */
    public String getCurrentUserBio() {
        return pref.getString(KEY_USER_BIO, null);
    }

    /**
     * 检查用户是否已登录
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * 清除会话信息，用户登出
     */
    public void logoutUser() {
        editor.clear();
        editor.apply(); // 使用 apply() 异步提交
    }
}
