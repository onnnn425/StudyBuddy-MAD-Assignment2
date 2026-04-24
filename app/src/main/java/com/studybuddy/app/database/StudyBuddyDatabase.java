package com.studybuddy.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.studybuddy.app.dao.TimetableDao;
import com.studybuddy.app.models.Note;
import com.studybuddy.app.models.PomodoroSettings;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.models.TimetableEntry;
import com.studybuddy.app.models.User;
import com.studybuddy.app.models.UserProfile;

@Database(entities = {Task.class, Note.class, TimetableEntry.class, PomodoroSettings.class, UserProfile.class, User.class}, version = 7, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class StudyBuddyDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract NoteDao noteDao();
    public abstract TimetableDao timetableDao();
    public abstract PomodoroSettingsDao pomodoroSettingsDao();
    public abstract UserProfileDao userProfileDao();
    public abstract UserDao userDao();

    private static volatile StudyBuddyDatabase INSTANCE;

    public static StudyBuddyDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (StudyBuddyDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    StudyBuddyDatabase.class, "study_buddy_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

