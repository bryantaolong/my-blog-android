package com.example.my_blog_android.model;

import java.io.Serializable;
import java.util.Date;

public class UserFavorite implements Serializable {
    private Long id;
    private Long userId;
    private String itemType; // "ARTICLE" 或 "PHOTO"
    private Long itemId;
    private Date createTime;

    public UserFavorite() {
    }

    public UserFavorite(Long userId, String itemType, Long itemId) {
        this.userId = userId;
        this.itemType = itemType;
        this.itemId = itemId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
