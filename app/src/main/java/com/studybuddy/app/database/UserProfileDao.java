package com.studybuddy.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.studybuddy.app.models.UserProfile;

@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE userId = :userId LIMIT 1")
    UserProfile getProfileByUser(String userId);
}
