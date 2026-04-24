package com.studybuddy.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.studybuddy.app.models.PomodoroSettings;

@Dao
public interface PomodoroSettingsDao {

    @Query("UPDATE pomodoro_settings SET totalSessions = totalSessions + 1 WHERE userId = :userId")
    void incrementSessions(String userId);

    @Query("SELECT totalSessions FROM pomodoro_settings WHERE userId = :userId")
    int getTotalSessions(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(PomodoroSettings settings);

    @Query("SELECT * FROM pomodoro_settings WHERE userId = :userId LIMIT 1")
    PomodoroSettings getSettingsByUser(String userId);
}

