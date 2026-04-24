package com.studybuddy.app;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;
import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.managers.TimetableManager;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.models.TimetableEntry;
import com.studybuddy.app.utils.SessionManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private TextView selectedDateText;
    private RecyclerView rvTasks;
    private TextView tvTimetableLabel;
    private LinearLayout timetableContainer;

    private LocalDate selectedDate = LocalDate.now();
    private YearMonth currentVisibleMonth = YearMonth.now();
    private final Map<String, List<Task>> tasksByDate = new HashMap<>();
    private TaskListAdapter taskAdapter;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        selectedDateText = view.findViewById(R.id.selectedDateText);
        rvTasks = view.findViewById(R.id.rvTasks);
        tvTimetableLabel = view.findViewById(R.id.tvTimetableLabel);
        timetableContainer = view.findViewById(R.id.timetableContainer);

        ImageButton btnPrev = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = view.findViewById(R.id.btnNextMonth);
        btnPrev.setOnClickListener(v ->
                calendarView.smoothScrollToMonth(currentVisibleMonth.minusMonths(1)));
        btnNext.setOnClickListener(v ->
                calendarView.smoothScrollToMonth(currentVisibleMonth.plusMonths(1)));

        setupRecyclerView();
        setupCalendar();
        loadTasks();

        return view;
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskListAdapter();
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);
        rvTasks.setNestedScrollingEnabled(false);
    }

    private void setupCalendar() {
        YearMonth startMonth = currentVisibleMonth.minusMonths(12);
        YearMonth endMonth = currentVisibleMonth.plusMonths(12);

        calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY);
        calendarView.scrollToMonth(currentVisibleMonth);

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                container.currentDay = day;
                container.tvDay.setText(String.valueOf(day.getDate().getDayOfMonth()));

                boolean isCurrentMonth = day.getPosition() == DayPosition.MonthDate;
                boolean isSelected = day.getDate().equals(selectedDate);
                boolean isToday = day.getDate().equals(LocalDate.now());

                //reset state
                container.tvDay.setAlpha(isCurrentMonth ? 1.0f : 0.35f);

                if (isSelected) {
                    container.viewDayBackground.setVisibility(View.VISIBLE);
                    container.viewDayBackground.setBackgroundResource(R.drawable.bg_day_selected);
                    container.tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else if (isToday) {
                    container.viewDayBackground.setVisibility(View.VISIBLE);
                    container.viewDayBackground.setBackgroundResource(R.drawable.bg_day_today);
                    container.tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                } else {
                    container.viewDayBackground.setVisibility(View.GONE);
                    container.tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground));
                }

                //priority dots
                container.llDots.removeAllViews();
                if (isCurrentMonth) {
                    String dateStr = day.getDate().format(DATE_FMT);
                    List<Task> tasksForDay = tasksByDate.get(dateStr);
                    if (tasksForDay != null && !tasksForDay.isEmpty()) {
                        int count = Math.min(tasksForDay.size(), 3);
                        for (int i = 0; i < count; i++) {
                            View dot = new View(requireContext());
                            GradientDrawable circle = new GradientDrawable();
                            circle.setShape(GradientDrawable.OVAL);
                            circle.setColor(ContextCompat.getColor(requireContext(),
                                    getPriorityColor(tasksForDay.get(i).getPriority())));
                            dot.setBackground(circle);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(7, 7);
                            lp.setMargins(2, 0, 2, 0);
                            dot.setLayoutParams(lp);
                            container.llDots.addView(dot);
                        }
                    }
                }
            }
        });

        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<MonthHeaderViewContainer>() {
            @NonNull
            @Override
            public MonthHeaderViewContainer create(@NonNull View view) {
                return new MonthHeaderViewContainer(view);
            }

            @Override
            public void bind(@NonNull MonthHeaderViewContainer container, @NonNull CalendarMonth month) {
                //day of week labels
            }
        });

        calendarView.setMonthScrollListener(month -> {
            currentVisibleMonth = month.getYearMonth();
            String name = currentVisibleMonth.getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.getDefault());
            tvMonthYear.setText(name + " " + currentVisibleMonth.getYear());
            return kotlin.Unit.INSTANCE;
        });

        String initName = currentVisibleMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
        tvMonthYear.setText(initName + " " + currentVisibleMonth.getYear());
    }

    private void loadTasks() {
        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());

        if (userId != null) {
            db.taskDao().getTasksLiveDataByUser(userId).observe(getViewLifecycleOwner(),
                    this::rebuildTasksMap);
        } else {
            db.taskDao().getAllTasks().observe(getViewLifecycleOwner(),
                    this::rebuildTasksMap);
        }
    }

    private void rebuildTasksMap(List<Task> tasks) {
        tasksByDate.clear();
        if (tasks != null) {
            for (Task task : tasks) {
                String date = task.getDueDate();
                if (date != null && !date.isEmpty() && !date.equals("No Date")) {
                    tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
                }
            }

            for (List<Task> dayTasks : tasksByDate.values()) {
                dayTasks.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            }
        }
        calendarView.notifyCalendarChanged();
        updateSelectedDateUI();
    }

    private void updateSelectedDateUI() {
        if (selectedDate == null) return;
        String dateStr = selectedDate.format(DATE_FMT);
        String month = selectedDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        selectedDateText.setText("Tasks on " + month + " " + selectedDate.getDayOfMonth()
                + ", " + selectedDate.getYear());

        List<Task> tasks = tasksByDate.getOrDefault(dateStr, new ArrayList<>());
        taskAdapter.setTasks(tasks);

        loadTimetableForDate(selectedDate);
    }

    private void loadTimetableForDate(LocalDate date) {
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        String label = date.equals(LocalDate.now()) ? "Today's Timetable" : dayName + "'s Timetable";
        tvTimetableLabel.setText(label);

        new Thread(() -> {
            Map<String, List<TimetableEntry>> entriesByDay =
                    TimetableManager.getInstance(requireContext()).getEntriesByDaySync();
            List<TimetableEntry> entries = entriesByDay.get(dayName);
            if (entries == null) {
                entries = new ArrayList<>();
            }

            List<TimetableEntry> sorted = new ArrayList<>(entries);
            Collections.sort(sorted, (a, b) -> {
                String ta = a.getStartTime() != null ? a.getStartTime() : "";
                String tb = b.getStartTime() != null ? b.getStartTime() : "";
                return ta.compareTo(tb);
            });

            final List<TimetableEntry> finalEntries = sorted;

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                timetableContainer.removeAllViews();
                if (finalEntries.isEmpty()) {
                    TextView emptyView = new TextView(requireContext());
                    emptyView.setText("No classes scheduled");
                    emptyView.setTextSize(13f);
                    emptyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextMuted));
                    emptyView.setPadding(0, 0, 0, 16);
                    timetableContainer.addView(emptyView);
                } else {
                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                    for (TimetableEntry entry : finalEntries) {
                        View row = inflater.inflate(R.layout.item_dashboard_timetable,
                                timetableContainer, false);
                        TextView dashTime = row.findViewById(R.id.dashTime);
                        TextView dashTitle = row.findViewById(R.id.dashTitle);
                        TextView dashInfo = row.findViewById(R.id.dashInfo);

                        String startTime = entry.getStartTime() != null ? entry.getStartTime() : "";
                        String endTime = entry.getEndTime() != null ? entry.getEndTime() : "";
                        dashTime.setText(startTime.isEmpty() ? "" : startTime + (endTime.isEmpty() ? "" : "\n" + endTime));
                        dashTitle.setText(entry.getTitle() != null ? entry.getTitle() : "");

                        String type = entry.getType() != null ? entry.getType() : "";
                        String location = entry.getLocation() != null ? entry.getLocation() : "";
                        if (!type.isEmpty() && !location.isEmpty()) {
                            dashInfo.setText(type + " · " + location);
                        } else if (!type.isEmpty()) {
                            dashInfo.setText(type);
                        } else if (!location.isEmpty()) {
                            dashInfo.setText(location);
                        } else {
                            dashInfo.setText("");
                        }

                        timetableContainer.addView(row);
                    }
                }
            });
        }).start();
    }

    private int getPriorityColor(int priority) {
        if (priority == 2) return R.color.colorError;      //high
        if (priority == 1) return R.color.colorSecondary;  //medium
        return R.color.colorPrimary;                        //low
    }


    class DayViewContainer extends ViewContainer {
        final TextView tvDay;
        final View viewDayBackground;
        final LinearLayout llDots;
        CalendarDay currentDay;

        DayViewContainer(@NonNull View view) {
            super(view);
            tvDay = view.findViewById(R.id.tvDay);
            viewDayBackground = view.findViewById(R.id.viewDayBackground);
            llDots = view.findViewById(R.id.llDots);

            view.setOnClickListener(v -> {
                if (currentDay != null && currentDay.getPosition() == DayPosition.MonthDate) {
                    LocalDate prev = selectedDate;
                    selectedDate = currentDay.getDate();
                    calendarView.notifyDateChanged(selectedDate);
                    if (prev != null && !prev.equals(selectedDate)) {
                        calendarView.notifyDateChanged(prev);
                    }
                    updateSelectedDateUI();
                }
            });
        }
    }

    static class MonthHeaderViewContainer extends ViewContainer {
        MonthHeaderViewContainer(@NonNull View view) {
            super(view);
        }
    }


    class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder> {
        private List<Task> tasks = new ArrayList<>();

        void setTasks(List<Task> newTasks) {
            tasks = newTasks != null ? newTasks : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_task, parent, false);
            return new TaskViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            holder.bind(tasks.get(position));
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            final View priorityBar;
            final TextView tvTitle, tvSubject, tvPriority, tvOverdue;
            final ProgressBar progressBar;

            TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                priorityBar = itemView.findViewById(R.id.priorityBar);
                tvTitle = itemView.findViewById(R.id.tvTaskTitle);
                tvSubject = itemView.findViewById(R.id.tvTaskSubject);
                tvPriority = itemView.findViewById(R.id.tvPriority);
                progressBar = itemView.findViewById(R.id.progressBar);
                tvOverdue = itemView.findViewById(R.id.tvOverdue);
            }

            void bind(Task task) {
                tvTitle.setText(task.getTitle());
                String subject = task.getSubject();
                tvSubject.setText(subject != null && !subject.isEmpty() ? subject : "");

                int colorRes = getPriorityColor(task.getPriority());
                int color = ContextCompat.getColor(requireContext(), colorRes);
                priorityBar.setBackgroundColor(color);
                tvPriority.setBackgroundColor(color);

                String label = task.getPriority() == 2 ? "HIGH"
                        : (task.getPriority() == 1 ? "MED" : "LOW");
                tvPriority.setText(label);

                progressBar.setProgress(task.getCompletionPercentage());

                if (task.isCompleted()) {
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    tvTitle.setAlpha(0.5f);
                } else {
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    tvTitle.setAlpha(1.0f);
                }

                boolean isOverdue = false;
                if (!task.isCompleted()) {
                    String due = task.getDueDate();
                    if (due != null && !due.isEmpty() && !due.equals("No Date")) {
                        try {
                            LocalDate dueDate = LocalDate.parse(due, DATE_FMT);
                            isOverdue = dueDate.isBefore(LocalDate.now());
                        } catch (Exception ignored) {}
                    }
                }
                tvOverdue.setVisibility(isOverdue ? View.VISIBLE : View.GONE);
            }
        }
    }
}

