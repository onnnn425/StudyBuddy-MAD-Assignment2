package com.studybuddy.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.studybuddy.app.models.Task;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    List<Task> getAllTasksImmediate();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    List<Task> getTasksByUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksLiveDataByUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND completed = 0 ORDER BY dueDate ASC LIMIT 5")
    List<Task> getUpcomingTasksByUser(String userId);
}
