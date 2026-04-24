package com.studybuddy.app.managers;

import android.content.Context;

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.models.TimetableEntry;
import com.studybuddy.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimetableManager {
    private static TimetableManager instance;
    private final StudyBuddyDatabase db;
    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Runnable> listeners = new ArrayList<>();

    private TimetableManager(Context context) {
        appContext = context.getApplicationContext();
        db = StudyBuddyDatabase.getDatabase(appContext);
    }

    public static synchronized TimetableManager getInstance(Context context) {
        if (instance == null) {
            instance = new TimetableManager(context);
        }
        return instance;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void addEntry(TimetableEntry entry) {
        executor.execute(() -> {
            db.timetableDao().insert(entry);
            notifyListeners();
        });
    }

    public void updateEntry(TimetableEntry entry) {
        executor.execute(() -> {
            db.timetableDao().update(entry);
            notifyListeners();
        });
    }

    public void deleteEntry(String id) {
        executor.execute(() -> {
            db.timetableDao().deleteById(id);
            notifyListeners();
        });
    }

    public Map<String, List<TimetableEntry>> getEntriesByDaySync() {
        String userId = SessionManager.getInstance(appContext).getCurrentUserId();
        List<TimetableEntry> all = new ArrayList<>();
        if (userId != null) {
            all = db.timetableDao().getEntriesByUser(userId);
        }

        Map<String, List<TimetableEntry>> map = new HashMap<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        int i;
        for (i = 0; i < days.length; i++) {
            map.put(days[i], new ArrayList<>());
        }
        for (i = 0; i < all.size(); i++) {
            TimetableEntry entry = all.get(i);
            if (map.containsKey(entry.getDay())) {
                map.get(entry.getDay()).add(entry);
            }
        }
        return map;
    }

    public List<TimetableEntry> getAllEntries() {
        try {
            String userId = SessionManager.getInstance(appContext).getCurrentUserId();
            if (userId == null) return new ArrayList<>();
            return executor.submit(new java.util.concurrent.Callable<List<TimetableEntry>>() {
                @Override
                public List<TimetableEntry> call() {
                    return db.timetableDao().getEntriesByUser(userId);
                }
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void notifyListeners() {
        int i;
        for (i = 0; i < listeners.size(); i++) {
            listeners.get(i).run();
        }
    }
}

