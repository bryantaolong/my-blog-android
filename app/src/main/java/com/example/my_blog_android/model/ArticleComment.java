package com.example.my_blog_android.model;

import java.io.Serializable;
import java.util.Date;

// This class represents the ArticleComment model on the Android client side.
// It must match the fields returned by your backend API.
public class ArticleComment implements Serializable {
    private Long id;
    private Long articleId;  // 关联文章ID
    private String content;
    private String authorId;  // 评论者ID
    private String authorName; // 评论者名称，由后端填充

    private Date createTime;
    private Date updateTime;

    // Default constructor (required for Retrofit/JSON deserialization)
    public ArticleComment() {
    }

    // Constructor for creating new comments (without ID, createTime, updateTime)
    public ArticleComment(Long articleId, String content, String authorId) {
        this.articleId = articleId;
        this.content = content;
        this.authorId = authorId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "ArticleComment{" +
                "id=" + id +
                ", articleId=" + articleId +
                ", content='" + content + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorName='" + authorName + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
