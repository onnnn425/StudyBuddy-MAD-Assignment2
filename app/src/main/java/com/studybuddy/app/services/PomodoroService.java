package com.studybuddy.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.studybuddy.app.MainActivity;
import com.studybuddy.app.R;
import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.managers.PomodoroManager;
import com.studybuddy.app.utils.NotificationHelper;
import com.studybuddy.app.utils.SessionManager;

public class PomodoroService extends Service {
    private static final String CHANNEL_ID = "pomodoro_service_channel";
    private static final int NOTIFICATION_ID = 2001;

    private Handler handler;
    private Runnable timerRunnable;
    private PomodoroManager pomodoroManager;
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        pomodoroManager = PomodoroManager.getInstance();
        notificationHelper = new NotificationHelper(this);

        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createForegroundNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, createForegroundNotification());
        }

        //listen for session completion
        pomodoroManager.addListener(new PomodoroManager.TimerListener() {
            @Override
            public void onTimerUpdate() {
                updateForegroundNotification();
            }

            @Override
            public void onSessionCompleted() {
            }

            @Override
            public void onWorkSessionCompleted() {
                //read the updated session count from DB
                new Thread(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    String userId = SessionManager.getInstance(PomodoroService.this).getCurrentUserId();
                    int sessions = 0;
                    if (userId != null) {
                        sessions = StudyBuddyDatabase.getDatabase(PomodoroService.this)
                                .pomodoroSettingsDao().getTotalSessions(userId);
                    }
                    final int finalSessions = sessions;
                    final int breakMins = pomodoroManager.getBreakMinutes();
                    handler.post(() -> notificationHelper.showWorkCompletedNotification(finalSessions, breakMins));
                }).start();
            }

            @Override
            public void onBreakSessionCompleted() {
                notificationHelper.showBreakCompletedNotification();
            }
        });

        startTimerMonitoring();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro Timer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int minutes = pomodoroManager.getMinutesRemaining();
        int seconds = pomodoroManager.getSecondsRemaining() % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);

        String statusText;
        switch (pomodoroManager.getCurrentState()) {
            case RUNNING:
                statusText = "Focusing... " + timeText;
                break;
            case BREAK:
                statusText = "On break... " + timeText;
                break;
            case PAUSED:
                statusText = "Paused - " + timeText;
                break;
            default:
                statusText = "Ready to focus";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🍅 StudyBuddy Pomodoro")
                .setContentText(statusText)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateForegroundNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createForegroundNotification());
        }
    }

    private void startTimerMonitoring() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateForegroundNotification();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

