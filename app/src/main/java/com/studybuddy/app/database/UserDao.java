package com.studybuddy.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.studybuddy.app.models.User;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM users WHERE studentId = :studentId LIMIT 1")
    User findByStudentId(String studentId);

    @Query("SELECT * FROM users WHERE studentId = :studentId AND passwordHash = :passwordHash LIMIT 1")
    User login(String studentId, String passwordHash);

    @Update
    void update(User user);
}

