package com.example.my_blog_android.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class Photo implements Serializable {
    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("filePath")
    private String filePath;
    @SerializedName("fileType")
    private String fileType;
    @SerializedName("fileSize")
    private Long fileSize;
    @SerializedName("authorId")
    private String authorId;
    @SerializedName("authorName")
    private String authorName;
    @SerializedName("createTime")
    private Date createTime;
    @SerializedName("updateTime")
    private Date updateTime;

    // Constructors (required for Gson)
    public Photo() {}

    public Photo(Long id, String name, String description, String filePath, String fileType, Long fileSize, String authorId, String authorName,Date createTime, Date updateTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFilePath() { return filePath; }
    public String getFileType() { return fileType; }
    public Long getFileSize() { return fileSize; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public Date getCreateTime() { return createTime; }
    public Date getUpdateTime() { return updateTime; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorId = authorName; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "Photo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileSize=" + fileSize +
                ", authorId='" + authorId + '\'' +
                ", authorName='" + authorName + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}