package com.studybuddy.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_settings")
public class PomodoroSettings {

    @PrimaryKey
    @NonNull
    private String userId = "";

    private int workMinutes;
    private int breakMinutes;
    private int totalSessions;

    public PomodoroSettings() {
    }

    public PomodoroSettings(@NonNull String userId, int workMinutes, int breakMinutes) {
        this.userId = userId;
        this.workMinutes = workMinutes;
        this.breakMinutes = breakMinutes;
        this.totalSessions = 0;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public int getWorkMinutes() {
        return workMinutes;
    }

    public void setWorkMinutes(int workMinutes) {
        this.workMinutes = workMinutes;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public void setBreakMinutes(int breakMinutes) {
        this.breakMinutes = breakMinutes;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }
}

