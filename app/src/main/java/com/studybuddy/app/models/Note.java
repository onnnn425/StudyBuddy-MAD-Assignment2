package com.studybuddy.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String content;
    private String subject;
    private String dateCreated;
    private String lastModified;
    private String userId;

    public Note() {
    }

    public Note(@NonNull String id, String title, String content, String subject, String dateCreated, String lastModified) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.subject = subject;
        this.dateCreated = dateCreated;
        this.lastModified = lastModified;
        this.userId = "";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
