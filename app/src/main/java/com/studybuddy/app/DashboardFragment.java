package com.studybuddy.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.managers.PomodoroManager;
import com.studybuddy.app.managers.TaskManager;
import com.studybuddy.app.managers.TimetableManager;
import com.studybuddy.app.models.PomodoroSettings;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.models.TimetableEntry;
import com.studybuddy.app.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private TextView welcomeText;
    private LinearLayout upcomingTasksContainer;
    private LinearLayout timetableDashboardContainer;
    private Button pomodoroStartButton;
    private TextView pomodoroSessionsText;
    private TextView pomodoroTimerDisplay;
    private TextView textTodayDay;
    private TextView textQuoteContent;
    private TextView textQuoteAuthor;
    private TextView textQuoteRefresh;
    private PomodoroManager pomodoroManager;
    private PomodoroManager.TimerListener pomodoroListener;

    public DashboardFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        welcomeText = view.findViewById(R.id.textDashboardWelcome);
        upcomingTasksContainer = view.findViewById(R.id.upcomingTasksContainer);
        timetableDashboardContainer = view.findViewById(R.id.timetableDashboardContainer);
        pomodoroStartButton = view.findViewById(R.id.buttonPomodoroStart);
        pomodoroSessionsText = view.findViewById(R.id.pomodoroSessionsText);
        pomodoroTimerDisplay = view.findViewById(R.id.pomodoroTimerDisplay);
        textTodayDay = view.findViewById(R.id.textTodayDay);

        textQuoteContent = view.findViewById(R.id.textQuoteContent);
        textQuoteAuthor = view.findViewById(R.id.textQuoteAuthor);
        textQuoteRefresh = view.findViewById(R.id.textQuoteRefresh);

        pomodoroManager = PomodoroManager.getInstance();

        loadSavedTimerSettings();
        setWelcomeMessage();
        setTodayLabel();
        loadData();
        setupListeners();
        fetchQuote();

        textQuoteRefresh.setOnClickListener(v -> fetchQuote());

        pomodoroStartButton.setOnClickListener(v -> openFragment(new PomodoroFragment()));

        TextView viewAllTasks = view.findViewById(R.id.textViewAllTasks);
        if (viewAllTasks != null) {
            viewAllTasks.setOnClickListener(v -> openFragment(new TasksFragment()));
        }

        TextView viewAllTimetable = view.findViewById(R.id.textViewAllTimetable);
        if (viewAllTimetable != null) {
            viewAllTimetable.setOnClickListener(v -> {
                TimetableFragment timetableFragment = new TimetableFragment();
                Bundle args = new Bundle();
                args.putBoolean("force_today", true);
                timetableFragment.setArguments(args);
                openFragment(timetableFragment);
            });
        }

        return view;
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
                    if (pomodoroManager.getCurrentState() == PomodoroManager.State.IDLE) {
                        pomodoroManager.setWorkMinutes(savedWork);
                        pomodoroManager.setBreakMinutes(savedBreak);
                        pomodoroManager.reset();
                    }
                });
            }
        }).start();
    }

    private void setWelcomeMessage() {
        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) {
            welcomeText.setText("Hi, Student!");
            return;
        }

        new Thread(() -> {
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
            com.studybuddy.app.models.User user = db.userDao().findByStudentId(userId);
            String displayName = "Student";
            if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                String[] parts = user.getName().trim().split("\\s+");
                if (parts.length > 0) {
                    displayName = parts[0];
                }
            }
            String finalDisplayName = displayName;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> welcomeText.setText("Hi, " + finalDisplayName + "!"));
            }
        }).start();
    }

    private void setTodayLabel() {
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String todayName = dayNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];
        if (textTodayDay != null) {
            textTodayDay.setText(todayName);
        }
    }

    private void loadData() {
        loadUpcomingTasks();
        loadTodayTimetable();
        updatePomodoroStats();
    }

    private void setupListeners() {
        TaskManager taskManager = TaskManager.getInstance(requireContext());
        taskManager.addListener(() -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(this::loadUpcomingTasks);
            }
        });

        TimetableManager timetableManager = TimetableManager.getInstance(requireContext());
        timetableManager.addListener(() -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(this::loadTodayTimetable);
            }
        });

        pomodoroListener = new PomodoroManager.TimerListener() {
            @Override
            public void onTimerUpdate() {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(DashboardFragment.this::updatePomodoroStats);
                }
            }

            @Override
            public void onSessionCompleted() {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(DashboardFragment.this::updatePomodoroStats);
                }
            }
        };
        pomodoroManager.addListener(pomodoroListener);
    }

    private void loadUpcomingTasks() {
        if (upcomingTasksContainer == null) {
            return;
        }

        Context appContext = getContext();
        if (appContext == null) {
            return;
        }

        new Thread(() -> {
            String userId = SessionManager.getInstance(appContext).getCurrentUserId();
            List<Task> upcoming = new ArrayList<>();
            if (userId != null) {
                StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(appContext);
                List<Task> fetched = db.taskDao().getUpcomingTasksByUser(userId);
                // Only show active tasks: not completed, not at 100% progress, and not overdue
                for (Task t : fetched) {
                    if (!t.isCompleted() && t.getCompletionPercentage() < 100 && !isOverdue(t.getDueDate())) {
                        upcoming.add(t);
                    }
                }
                Collections.sort(upcoming, new Comparator<Task>() {
                    @Override
                    public int compare(Task first, Task second) {
                        return Integer.compare(second.getPriority(), first.getPriority());
                    }
                });
            }

            List<Task> finalUpcoming = upcoming;
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    upcomingTasksContainer.removeAllViews();
                    if (finalUpcoming.isEmpty()) {
                        addEmptyState(upcomingTasksContainer, "No pending tasks!");
                    } else {
                        int i;
                        for (i = 0; i < finalUpcoming.size(); i++) {
                            View row = createTaskRow(finalUpcoming.get(i));
                            if (row != null) {
                                upcomingTasksContainer.addView(row);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void loadTodayTimetable() {
        if (timetableDashboardContainer == null) {
            return;
        }

        Context appContext = getContext();
        if (appContext == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String today = days[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        new Thread(() -> {
            Map<String, List<TimetableEntry>> schedule = TimetableManager.getInstance(appContext).getEntriesByDaySync();
            List<TimetableEntry> todayEntries = schedule.get(today);
            if (todayEntries == null) {
                todayEntries = new ArrayList<>();
            }
            Collections.sort(todayEntries, new Comparator<TimetableEntry>() {
                @Override
                public int compare(TimetableEntry first, TimetableEntry second) {
                    return first.getStartTime().compareTo(second.getStartTime());
                }
            });

            List<TimetableEntry> finalTodayEntries = todayEntries;
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    timetableDashboardContainer.removeAllViews();
                    if (finalTodayEntries.isEmpty()) {
                        addEmptyState(timetableDashboardContainer, "No classes on " + today + ". Add them in the Timetable tab.");
                    } else {
                        int i;
                        for (i = 0; i < finalTodayEntries.size(); i++) {
                            View row = createTimetableRow(finalTodayEntries.get(i));
                            if (row != null) {
                                timetableDashboardContainer.addView(row);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private View createTaskRow(Task task) {
        if (getContext() == null) {
            return null;
        }
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_dashboard_task, null);
        TextView title = row.findViewById(R.id.dashboardTaskTitle);
        TextView date = row.findViewById(R.id.dashboardTaskDueDate);
        View dot = row.findViewById(R.id.priorityDot);

        if (title != null) {
            title.setText(task.getTitle());
        }
        if (date != null) {
            date.setText(task.getDueDate());
        }

        int colorRes = task.getPriority() == 2 ? R.color.colorError
                : task.getPriority() == 1 ? R.color.colorSecondary : R.color.colorPrimary;

        if (dot != null) {
            if (dot.getBackground() != null) {
                dot.getBackground().setTint(ContextCompat.getColor(getContext(), colorRes));
            } else {
                dot.setBackgroundColor(ContextCompat.getColor(getContext(), colorRes));
            }
        }

        return row;
    }

    private View createTimetableRow(TimetableEntry entry) {
        if (getContext() == null) {
            return null;
        }
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_dashboard_timetable, null);
        TextView timeText = row.findViewById(R.id.dashTime);
        TextView titleText = row.findViewById(R.id.dashTitle);
        TextView infoText = row.findViewById(R.id.dashInfo);

        if (timeText != null) {
            timeText.setText(entry.getStartTime());
        }
        if (titleText != null) {
            titleText.setText(entry.getTitle());
        }
        if (infoText != null) {
            infoText.setText(entry.getType() + " · " + entry.getLocation());
        }

        return row;
    }

    private void updatePomodoroStats() {
        new Thread(() -> {
            String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
            int total = 0;
            if (userId != null) {
                total = StudyBuddyDatabase.getDatabase(requireContext()).pomodoroSettingsDao().getTotalSessions(userId);
            }
            int finalTotal = total;
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pomodoroSessionsText != null) {
                        pomodoroSessionsText.setText("Sessions: " + finalTotal);
                    }
                });
            }
        }).start();

        if (pomodoroTimerDisplay != null) {
            int secondsRemaining = pomodoroManager.getSecondsRemaining();
            int minutes = secondsRemaining / 60;
            int seconds = secondsRemaining % 60;
            pomodoroTimerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

            if (pomodoroStartButton != null) {
                PomodoroManager.State state = pomodoroManager.getCurrentState();
                if (state == PomodoroManager.State.RUNNING || state == PomodoroManager.State.BREAK) {
                    pomodoroStartButton.setText("In Progress");
                } else if (state == PomodoroManager.State.PAUSED) {
                    pomodoroStartButton.setText("Paused");
                } else {
                    pomodoroStartButton.setText("Start Focus");
                }
            }
        }
    }

    private boolean isOverdue(String dueDate) {
        if (dueDate == null || dueDate.isEmpty() || dueDate.equals("No Date")) return false;
        Long due = parseDateMillis(dueDate);
        if (due == null) return false;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return due < today.getTimeInMillis();
    }

    private Long parseDateMillis(String dueDate) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDate).getTime();
        } catch (ParseException ignored) {
            try {
                return new SimpleDateFormat("yyyy-M-d", Locale.getDefault()).parse(dueDate).getTime();
            } catch (ParseException e) {
                return null;
            }
        }
    }

    private void addEmptyState(LinearLayout container, String message) {
        if (getContext() == null) {
            return;
        }
        TextView textView = new TextView(getContext());
        textView.setText(message);
        textView.setTextSize(12);
        textView.setPadding(0, 10, 0, 10);
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextMuted));
        container.addView(textView);
    }

    private void openFragment(Fragment fragment) {
        if (getActivity() != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void fetchQuote() {
        if (textQuoteContent == null) return;
        textQuoteContent.setText("Loading quote...");
        textQuoteAuthor.setText("");

        new Thread(() -> {
            String quote = null;
            String author = null;
            try {
                URL url = new URL("https://zenquotes.io/api/random");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                JSONArray jsonArray = new JSONArray(response.toString());
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                quote = jsonObject.getString("q");
                author = jsonObject.getString("a");
            } catch (Exception e) {
                quote = "The secret of getting ahead is getting started.";
                author = "Mark Twain";
            }

            final String finalQuote = quote;
            final String finalAuthor = author;
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (textQuoteContent != null) {
                        textQuoteContent.setText("\u201c" + finalQuote + "\u201d");
                    }
                    if (textQuoteAuthor != null) {
                        textQuoteAuthor.setText("\u2014 " + finalAuthor);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pomodoroManager.removeListener(pomodoroListener);
    }
}


