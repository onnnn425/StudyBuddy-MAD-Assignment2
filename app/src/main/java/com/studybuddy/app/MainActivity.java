package com.studybuddy.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.managers.PomodoroManager;
import com.studybuddy.app.models.PomodoroSettings;
import com.studybuddy.app.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private BottomNavigationView bottomNavigationView;
    private PomodoroManager.TimerListener pomodoroSessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        registerPomodoroSessionListener();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            replaceFragment(new DashboardFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_tasks) {
                selectedFragment = new TasksFragment();
            } else if (itemId == R.id.nav_timetable) {
                selectedFragment = new TimetableFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.nav_notes) {
                selectedFragment = new NotesFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else {
                selectedFragment = new DashboardFragment();
            }

            replaceFragment(selectedFragment);
            return true;
        });
    }

    private void registerPomodoroSessionListener() {
        pomodoroSessionListener = new PomodoroManager.TimerListener() {
            @Override
            public void onTimerUpdate() {}

            @Override
            public void onSessionCompleted() {}

            @Override
            public void onWorkSessionCompleted() {
                String userId = SessionManager.getInstance(MainActivity.this).getCurrentUserId();
                if (userId == null) return;
                new Thread(() -> {
                    StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(MainActivity.this);
                    PomodoroSettings settings = db.pomodoroSettingsDao().getSettingsByUser(userId);
                    if (settings == null) {
                        PomodoroSettings newSettings = new PomodoroSettings(userId,
                                PomodoroManager.getInstance().getWorkMinutes(),
                                PomodoroManager.getInstance().getBreakMinutes());
                        newSettings.setTotalSessions(1);
                        db.pomodoroSettingsDao().save(newSettings);
                    } else {
                        db.pomodoroSettingsDao().incrementSessions(userId);
                    }
                }).start();
            }
        };
        PomodoroManager.getInstance().addListener(pomodoroSessionListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pomodoroSessionListener != null) {
            PomodoroManager.getInstance().removeListener(pomodoroSessionListener);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainActivity", "Notification permission granted");
            } else {
                android.util.Log.d("MainActivity", "Notification permission denied");
            }
        }
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
