package com.studybuddy.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.studybuddy.app.database.TaskDao;
import com.studybuddy.app.dao.TimetableDao;
import com.studybuddy.app.models.Note;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.models.TimetableEntry;
import com.studybuddy.app.database.NoteDao;

@Database(entities = {Task.class, TimetableEntry.class, Note.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract TimetableDao timetableDao();
    public abstract NoteDao noteDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "studybuddy_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}