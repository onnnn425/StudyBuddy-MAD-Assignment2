package com.studybuddy.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.studybuddy.app.services.PomodoroService;

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.managers.PomodoroManager;
import com.studybuddy.app.models.PomodoroSettings;
import com.studybuddy.app.utils.SessionManager;

import java.util.Locale;

public class PomodoroFragment extends Fragment {
    private TextView timerText;
    private TextView timerModeText;
    private Button startButton;
    private Button pauseButton;
    private Button resetButton;
    private Button setTimerButton;

    private boolean isTimerRunning = false;
    private boolean isBreakMode = false;
    private TextView sessionsText;

    private PomodoroManager pomodoroManager;
    private PomodoroManager.TimerListener timerListener;

    public PomodoroFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        timerText = view.findViewById(R.id.timerText);
        timerModeText = view.findViewById(R.id.timerModeText);
        startButton = view.findViewById(R.id.buttonStartPomodoro);
        pauseButton = view.findViewById(R.id.buttonPausePomodoro);
        resetButton = view.findViewById(R.id.buttonResetPomodoro);
        setTimerButton = view.findViewById(R.id.buttonSetTimer);
        sessionsText = view.findViewById(R.id.pomodoroSessionsCount);

        pomodoroManager = PomodoroManager.getInstance();

        loadSavedTimerSettings();

        timerListener = new PomodoroManager.TimerListener() {
            @Override
            public void onTimerUpdate() {
                syncWithManager();
            }

            @Override
            public void onSessionCompleted() {
            }

            @Override
            public void onWorkSessionCompleted() {
                //notification with the real session count
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Work session completed! Take a break!", Toast.LENGTH_LONG).show();
                }
                saveAndRefreshSessions();
            }

            @Override
            public void onBreakSessionCompleted() {
                //notification handled by pomodoroService
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Break completed! Ready to focus?", Toast.LENGTH_LONG).show();
                }
                //timer returns to IDLE after break end
                stopPomodoroService();
            }
        };
        pomodoroManager.addListener(timerListener);

        updateTimerDisplay();
        updateButtonsState();

        startButton.setOnClickListener(v -> {
            pomodoroManager.start();
            startPomodoroService();
            Toast.makeText(getContext(), "Timer Started!", Toast.LENGTH_SHORT).show();
        });

        pauseButton.setOnClickListener(v -> {
            pomodoroManager.pause();
            Toast.makeText(getContext(), "Timer Paused", Toast.LENGTH_SHORT).show();
        });

        resetButton.setOnClickListener(v -> {
            pomodoroManager.reset();
            stopPomodoroService();
            Toast.makeText(getContext(), "Timer Reset", Toast.LENGTH_SHORT).show();
        });

        setTimerButton.setOnClickListener(v -> showSetTimerDialog());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pomodoroManager.removeListener(timerListener);
    }

    private void syncWithManager() {
        PomodoroManager.State state = pomodoroManager.getCurrentState();
        int secondsRemaining = pomodoroManager.getSecondsRemaining();

        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (state == PomodoroManager.State.RUNNING) {
            isTimerRunning = true;
            isBreakMode = false;
            timerModeText.setText("FOCUS TIME");
        } else if (state == PomodoroManager.State.BREAK) {
            isTimerRunning = true;
            isBreakMode = true;
            timerModeText.setText("BREAK TIME");
        } else if (state == PomodoroManager.State.PAUSED) {
            isTimerRunning = false;
            if (isBreakMode) {
                timerModeText.setText("BREAK (PAUSED)");
            } else {
                timerModeText.setText("FOCUS (PAUSED)");
            }
        } else {
            isTimerRunning = false;
            isBreakMode = false;
            timerModeText.setText("READY TO FOCUS");
        }

        updateButtonsState();
    }

    private void updateTimerDisplay() {
        int secondsRemaining = pomodoroManager.getSecondsRemaining();
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateButtonsState() {
        startButton.setEnabled(!isTimerRunning);
        pauseButton.setEnabled(isTimerRunning);
        resetButton.setEnabled(true);
    }

    private void loadSavedTimerSettings() {
        new Thread(() -> {
            String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
            PomodoroSettings settings = null;
            if (userId != null) {
                settings = db.pomodoroSettingsDao().getSettingsByUser(userId);
            }
            int savedWork = settings != null ? settings.getWorkMinutes() : 25;
            int savedBreak = settings != null ? settings.getBreakMinutes() : 5;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    pomodoroManager.setWorkMinutes(savedWork);
                    pomodoroManager.setBreakMinutes(savedBreak);
                    pomodoroManager.reset();
                    loadSessionCount();
                });
            }
        }).start();
    }

    private void showSetTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Set Your Own Timer");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView workLabel = new TextView(getContext());
        workLabel.setText("Work duration (minutes):");
        workLabel.setTextColor(getResources().getColor(R.color.colorOnBackground));
        layout.addView(workLabel);

        EditText editWork = new EditText(getContext());
        editWork.setInputType(InputType.TYPE_CLASS_NUMBER);
        editWork.setText(String.valueOf(pomodoroManager.getWorkMinutes()));
        editWork.setTextColor(getResources().getColor(R.color.colorOnBackground));
        layout.addView(editWork);

        TextView breakLabel = new TextView(getContext());
        breakLabel.setText("Break duration (minutes):");
        breakLabel.setTextColor(getResources().getColor(R.color.colorOnBackground));
        breakLabel.setPadding(0, padding / 2, 0, 0);
        layout.addView(breakLabel);

        EditText editBreak = new EditText(getContext());
        editBreak.setInputType(InputType.TYPE_CLASS_NUMBER);
        editBreak.setText(String.valueOf(pomodoroManager.getBreakMinutes()));
        editBreak.setTextColor(getResources().getColor(R.color.colorOnBackground));
        layout.addView(editBreak);

        builder.setView(layout);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            String workStr = editWork.getText().toString().trim();
            String breakStr = editBreak.getText().toString().trim();

            if (!workStr.isEmpty() && !breakStr.isEmpty()) {
                int workMin = Integer.parseInt(workStr);
                int breakMin = Integer.parseInt(breakStr);

                if (workMin < 1 || workMin > 120) {
                    Toast.makeText(getContext(), "Work time must be between 1 and 120 minutes", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (breakMin < 1 || breakMin > 60) {
                    Toast.makeText(getContext(), "Break time must be between 1 and 60 minutes", Toast.LENGTH_SHORT).show();
                    return;
                }

                pomodoroManager.setWorkMinutes(workMin);
                pomodoroManager.setBreakMinutes(breakMin);
                pomodoroManager.reset();

                String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
                if (userId != null) {
                    int finalWorkMin = workMin;
                    int finalBreakMin = breakMin;
                    new Thread(() -> {
                        StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
                        PomodoroSettings existing = db.pomodoroSettingsDao().getSettingsByUser(userId);
                        int currentSessions = existing != null ? existing.getTotalSessions() : 0;
                        PomodoroSettings updated = new PomodoroSettings(userId, finalWorkMin, finalBreakMin);
                        updated.setTotalSessions(currentSessions);
                        db.pomodoroSettingsDao().save(updated);
                    }).start();
                }

                Toast.makeText(getContext(), "Timer set: " + workMin + "m work / " + breakMin + "m break", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startPomodoroService() {
        if (getContext() == null) return;
        Intent serviceIntent = new Intent(getContext(), PomodoroService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(serviceIntent);
        } else {
            getContext().startService(serviceIntent);
        }
    }

    private void stopPomodoroService() {
        if (getContext() == null) return;
        getContext().stopService(new Intent(getContext(), PomodoroService.class));
    }

    private void saveAndRefreshSessions() {
        //small delay so the DB write completes before we read the updated count
        timerText.postDelayed(this::loadSessionCount, 300);
    }

    private void loadSessionCount() {
        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) {
            return;
        }

        new Thread(() -> {
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
            int total = db.pomodoroSettingsDao().getTotalSessions(userId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (sessionsText != null) {
                        sessionsText.setText("Sessions: " + total);
                    }
                });
            }
        }).start();
    }
}


