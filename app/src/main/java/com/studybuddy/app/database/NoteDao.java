package com.studybuddy.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.studybuddy.app.models.Note;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    List<Note> getAllNotesImmediate();

    @Query("SELECT * FROM notes WHERE subject = :subject ORDER BY lastModified DESC")
    LiveData<List<Note>> getNotesBySubject(String subject);

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY lastModified DESC")
    List<Note> getNotesByUser(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY lastModified DESC")
    LiveData<List<Note>> getNotesLiveDataByUser(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND subject = :subject ORDER BY lastModified DESC")
    LiveData<List<Note>> getNotesBySubjectForUser(String userId, String subject);

    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE :query OR subject LIKE :query) ORDER BY lastModified DESC")
    LiveData<List<Note>> searchNotesByUser(String userId, String query);
}

