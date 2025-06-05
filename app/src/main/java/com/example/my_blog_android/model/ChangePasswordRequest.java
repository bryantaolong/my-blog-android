package com.example.my_blog_android.model;

import java.io.Serializable;

// 用于向后端发送修改密码请求的数据模型
public class ChangePasswordRequest implements Serializable {
    private Long userId;
    // private String oldPassword; // 如果后端需要旧密码验证，请取消注释此行
    private String newPassword;

    private String oldPassword;

    public ChangePasswordRequest(Long userId, String newPassword) {
        this.userId = userId;
        this.newPassword = newPassword;
    }

    // 如果后端需要旧密码验证，请取消注释以下构造函数
     public ChangePasswordRequest(Long userId, String oldPassword, String newPassword) {
         this.userId = userId;
         this.oldPassword = oldPassword;
         this.newPassword = newPassword;
     }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

     public String getOldPassword() { return oldPassword; } // 如果后端需要旧密码验证，请取消注释
     public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; } // 如果后端需要旧密码验证，请取消注释

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
