package com.studybuddy.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    @NonNull
    private String userId = "";

    private String name;
    private String course;
    private String studentId;
    private String university;

    public UserProfile() {
    }

    public UserProfile(@NonNull String userId, String name, String course, String studentId_info, String university) {
        this.userId = userId;
        this.name = name;
        this.course = course;
        this.studentId = studentId_info;
        this.university = university;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }
}
