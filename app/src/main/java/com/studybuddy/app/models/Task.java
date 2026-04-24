package com.studybuddy.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.studybuddy.app.database.Converters;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String subject;
    private int priority;
    private String userId;

    @TypeConverters(Converters.class)
    private List<Subtask> subtasks;

    private int completionPercentage;
    private boolean completed;

    public Task() {
    }

    public Task(@NonNull String id, String title, String description, String dueDate, String subject, int priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.subject = subject;
        this.priority = priority;
        this.userId = "";
        this.subtasks = new ArrayList<>();
        this.completionPercentage = 0;
        this.completed = false;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void updateCompletionFromSubtasks() {
        if (subtasks != null && !subtasks.isEmpty()) {
            int completedCount = 0;
            int i;
            for (i = 0; i < subtasks.size(); i++) {
                if (subtasks.get(i).isCompleted()) {
                    completedCount++;
                }
            }
            this.completionPercentage = (completedCount * 100) / subtasks.size();
            this.completed = completedCount == subtasks.size();
        }
    }
}
