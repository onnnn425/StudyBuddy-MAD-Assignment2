package com.studybuddy.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    private String studentId = "";
    private String passwordHash;
    private String email;
    private String name;
    private String course;
    private String university;
    private String securityQuestion;
    private String securityAnswer;

    public User() {
    }

    public User(@NonNull String studentId, String passwordHash, String email, String name, String course, String university) {
        this.studentId = studentId;
        this.passwordHash = passwordHash;
        this.email = email;
        this.name = name;
        this.course = course;
        this.university = university;
    }

    @NonNull
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(@NonNull String studentId) {
        this.studentId = studentId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(password.getBytes());
            StringBuilder builder = new StringBuilder();
            int i;
            for (i = 0; i < hashBytes.length; i++) {
                builder.append(String.format("%02x", hashBytes[i] & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}

