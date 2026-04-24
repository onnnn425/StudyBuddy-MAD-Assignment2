package com.studybuddy.app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.studybuddy.app.models.TimetableEntry;

import java.util.List;

@Dao
public interface TimetableDao {
    @Query("SELECT * FROM timetable_entries")
    List<TimetableEntry> getAllEntries();

    @Query("SELECT * FROM timetable_entries")
    LiveData<List<TimetableEntry>> getAllEntriesLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimetableEntry entry);

    @Update
    void update(TimetableEntry entry);

    @Query("DELETE FROM timetable_entries WHERE id = :entryId")
    void deleteById(String entryId);

    @Query("SELECT * FROM timetable_entries WHERE userId = :userId")
    List<TimetableEntry> getEntriesByUser(String userId);
}
