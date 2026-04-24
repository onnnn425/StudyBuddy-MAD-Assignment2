package com.studybuddy.app.managers;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.database.TaskDao;
import com.studybuddy.app.models.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {
    private static TaskManager instance;
    private final TaskDao taskDao;
    private final List<Runnable> listeners = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TaskManager(Context context) {
        StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(context);
        this.taskDao = db.taskDao();
    }

    public static synchronized TaskManager getInstance(Context context) {
        if (instance == null) {
            instance = new TaskManager(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TaskManager must be initialized with context before use");
        }
        return instance;
    }

    public LiveData<List<Task>> getAllTasksLiveData() {
        return taskDao.getAllTasks();
    }

    public List<Task> getAllTasksSync() {
        return taskDao.getAllTasksImmediate();
    }

    public void addTask(Task task) {
        executor.execute(() -> {
            taskDao.insert(task);
            notifyListeners();
        });
    }

    public void updateTask(Task task) {
        executor.execute(() -> {
            taskDao.update(task);
            notifyListeners();
        });
    }

    public void deleteTask(Task task) {
        executor.execute(() -> {
            taskDao.delete(task);
            notifyListeners();
        });
    }

    public void addListener(Runnable listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}