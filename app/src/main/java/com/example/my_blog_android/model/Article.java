package com.example.my_blog_android.model;

import java.io.Serializable;
import java.util.Date;

// 请确保你的 Article 类有这些字段，并根据需要添加新的字段
public class Article implements Serializable {
    private Long id;
    private String title;
    private String content;
    private String authorId;
    private String authorName; // 新增字段：作者名称

    private Date createTime;
    private Date updateTime;

    // 构造函数 (根据你的实际情况可能有所不同)
    public Article() {
    }

    public Article(Long id, String title, String content, String authorId, String authorName, Date createTime, Date updateTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
