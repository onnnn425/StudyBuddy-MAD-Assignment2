package com.studybuddy.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.studybuddy.app.MainActivity;
import com.studybuddy.app.R;

public class NotificationHelper {
    private static final String WORK_CHANNEL_ID = "pomodoro_work_channel";
    private static final String BREAK_CHANNEL_ID = "pomodoro_break_channel";
    private static final String WORK_CHANNEL_NAME = "Pomodoro Work Timer";
    private static final String BREAK_CHANNEL_NAME = "Pomodoro Break Timer";
    private static final int WORK_NOTIFICATION_ID = 1001;
    private static final int BREAK_NOTIFICATION_ID = 1002;

    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //work session channel
            NotificationChannel workChannel = new NotificationChannel(
                    WORK_CHANNEL_ID,
                    WORK_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            workChannel.setDescription("Notifications when work session completes");
            workChannel.enableVibration(true);
            workChannel.setVibrationPattern(new long[]{0, 500, 250, 500});

            //break session channel
            NotificationChannel breakChannel = new NotificationChannel(
                    BREAK_CHANNEL_ID,
                    BREAK_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            breakChannel.setDescription("Notifications when break session completes");
            breakChannel.enableVibration(true);
            breakChannel.setVibrationPattern(new long[]{0, 300, 150, 300});

            notificationManager.createNotificationChannel(workChannel);
            notificationManager.createNotificationChannel(breakChannel);
        }
    }

    public void showWorkCompletedNotification(int sessionsCompleted, int breakMinutes) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WORK_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("🎉 Pomodoro Complete!")
                .setContentText("Great job! You've completed " + sessionsCompleted +
                        " session(s). Time for a " + breakMinutes + "-minute break!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500});

        notificationManager.notify(WORK_NOTIFICATION_ID, builder.build());
    }

    public void showBreakCompletedNotification() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BREAK_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("☕ Break Time Over!")
                .setContentText("Your break has ended. Ready to start another focus session?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 300, 150, 300});

        notificationManager.notify(BREAK_NOTIFICATION_ID, builder.build());
    }

    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}
