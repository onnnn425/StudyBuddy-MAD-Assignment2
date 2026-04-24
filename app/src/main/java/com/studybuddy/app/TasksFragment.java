package com.studybuddy.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.models.Subtask;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.utils.NotificationWorker;
import com.studybuddy.app.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TasksFragment extends Fragment {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_ACTIVE = 1;
    private static final int FILTER_COMPLETED = 2;
    private static final int FILTER_OVERDUE = 3;

    private static final int SORT_DUE_DATE = 0;
    private static final int SORT_PRIORITY = 1;
    private static final int SORT_RECENT = 2;

    private LinearLayout tasksContainer;
    private LinearLayout searchBarContainer;
    private EditText editSearchTasks;
    private TextView textToggleSearch;
    private TextView textCancelSearch;
    private Button filterAll, filterActive, filterCompleted, filterOverdue;
    private Spinner spinnerSortTasks;

    private StudyBuddyDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<Task> allTasks = new ArrayList<>();
    private String searchQuery = "";
    private int currentFilter = FILTER_ALL;
    private int currentSort = SORT_DUE_DATE;

    public TasksFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        tasksContainer = view.findViewById(R.id.tasksContainer);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        editSearchTasks = view.findViewById(R.id.editSearchTasks);
        textToggleSearch = view.findViewById(R.id.textToggleSearch);
        textCancelSearch = view.findViewById(R.id.textCancelSearch);
        filterAll = view.findViewById(R.id.filterAll);
        filterActive = view.findViewById(R.id.filterActive);
        filterCompleted = view.findViewById(R.id.filterCompleted);
        filterOverdue = view.findViewById(R.id.filterOverdue);
        spinnerSortTasks = view.findViewById(R.id.spinnerSortTasks);

        db = StudyBuddyDatabase.getDatabase(requireContext());

        setupSearchBar();
        setupFilterChips();
        setupSortSpinner();

        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId != null) {
            db.taskDao().getTasksLiveDataByUser(userId).observe(getViewLifecycleOwner(), tasks -> {
                allTasks = tasks != null ? tasks : new ArrayList<>();
                applyFiltersAndSort();
            });
        } else {
            db.taskDao().getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
                allTasks = tasks != null ? tasks : new ArrayList<>();
                applyFiltersAndSort();
            });
        }

        view.findViewById(R.id.fabAddTask).setOnClickListener(v -> showAddTaskDialog());
        return view;
    }

    private void setupSearchBar() {
        textToggleSearch.setOnClickListener(v -> {
            searchBarContainer.setVisibility(View.VISIBLE);
            editSearchTasks.requestFocus();
        });

        textCancelSearch.setOnClickListener(v -> {
            editSearchTasks.setText("");
            searchBarContainer.setVisibility(View.GONE);
            searchQuery = "";
            applyFiltersAndSort();
        });

        editSearchTasks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        filterAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        filterActive.setOnClickListener(v -> setFilter(FILTER_ACTIVE));
        filterCompleted.setOnClickListener(v -> setFilter(FILTER_COMPLETED));
        filterOverdue.setOnClickListener(v -> setFilter(FILTER_OVERDUE));
        updateFilterChipStyles();
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        applyFiltersAndSort();
    }

    private void updateFilterChipStyles() {
        styleChip(filterAll, currentFilter == FILTER_ALL);
        styleChip(filterActive, currentFilter == FILTER_ACTIVE);
        styleChip(filterCompleted, currentFilter == FILTER_COMPLETED);
        styleChip(filterOverdue, currentFilter == FILTER_OVERDUE);
    }

    private void styleChip(Button chip, boolean selected) {
        if (selected) {
            chip.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorBackground));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextMuted));
        }
    }

    private void setupSortSpinner() {
        String[] sortOptions = {"Due date", "Priority", "Recently added"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, sortOptions);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerSortTasks.setAdapter(adapter);
        spinnerSortTasks.setSelection(currentSort);
        spinnerSortTasks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != currentSort) {
                    currentSort = position;
                    applyFiltersAndSort();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFiltersAndSort() {
        List<Task> filtered = new ArrayList<>();
        for (Task task : allTasks) {
            //search filter
            if (!searchQuery.isEmpty()) {
                String title = task.getTitle() == null ? "" : task.getTitle().toLowerCase(Locale.getDefault());
                if (!title.contains(searchQuery)) continue;
            }
            //status filter
            boolean passes;
            switch (currentFilter) {
                case FILTER_ACTIVE:
                    passes = !task.isCompleted();
                    break;
                case FILTER_COMPLETED:
                    passes = task.isCompleted();
                    break;
                case FILTER_OVERDUE:
                    passes = !task.isCompleted() && isOverdue(task.getDueDate());
                    break;
                case FILTER_ALL:
                default:
                    passes = true;
            }
            if (passes) filtered.add(task);
        }

        //sort
        switch (currentSort) {
            case SORT_PRIORITY:
                Collections.sort(filtered, (a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
                break;
            case SORT_RECENT:
                //most recently added, if the DB returns insertion order, reverse it
                Collections.reverse(filtered);
                break;
            case SORT_DUE_DATE:
            default:
                Collections.sort(filtered, new Comparator<Task>() {
                    @Override
                    public int compare(Task a, Task b) {
                        Long da = parseDateMillis(a.getDueDate());
                        Long db2 = parseDateMillis(b.getDueDate());
                        if (da == null && db2 == null) return 0;
                        if (da == null) return 1; 
                        if (db2 == null) return -1;
                        return Long.compare(da, db2);
                    }
                });
        }

        refreshTasksList(filtered);
    }

    private Long parseDateMillis(String dueDate) {
        if (dueDate == null || dueDate.isEmpty() || dueDate.equals("No Date")) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDate).getTime();
        } catch (ParseException ignored) {
            //legacy non zero padded dates
            try {
                return new SimpleDateFormat("yyyy-M-d", Locale.getDefault()).parse(dueDate).getTime();
            } catch (ParseException e) {
                return null;
            }
        }
    }

    private boolean isOverdue(String dueDate) {
        Long due = parseDateMillis(dueDate);
        if (due == null) return false;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return due < today.getTimeInMillis();
    }

    private void refreshTasksList(List<Task> tasks) {
        tasksContainer.removeAllViews();

        if (tasks == null || tasks.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText(getEmptyMessage());
            emptyView.setPadding(32, 32, 32, 32);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tasksContainer.addView(emptyView);
            return;
        }

        for (Task task : tasks) {
            tasksContainer.addView(createTaskCard(task));
        }
    }

    private String getEmptyMessage() {
        if (!searchQuery.isEmpty()) return "No tasks match \"" + searchQuery + "\".";
        switch (currentFilter) {
            case FILTER_ACTIVE: return "No active tasks. Nice work!";
            case FILTER_COMPLETED: return "No completed tasks yet.";
            case FILTER_OVERDUE: return "Nothing overdue. You're on top of it!";
            default: return "No tasks yet. Tap + to add a task.";
        }
    }

    private View createTaskCard(Task task) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_task, null);

        TextView taskTitle = card.findViewById(R.id.taskTitle);
        TextView taskDueDate = card.findViewById(R.id.taskDueDate);
        TextView taskPriority = card.findViewById(R.id.taskPriority);
        ProgressBar progressBar = card.findViewById(R.id.taskProgress);
        TextView progressText = card.findViewById(R.id.progressText);
        LinearLayout subtasksContainer = card.findViewById(R.id.subtasksContainer);
        Button buttonEdit = card.findViewById(R.id.buttonEditTask);
        Button buttonDelete = card.findViewById(R.id.buttonDeleteTask);
        Button buttonAddSubtask = card.findViewById(R.id.buttonAddSubtask);

        taskTitle.setText(task.getTitle());

        //show overdue visually
        boolean overdue = !task.isCompleted() && isOverdue(task.getDueDate());
        if (overdue) {
            taskDueDate.setText("Due: " + task.getDueDate() + "  ·  OVERDUE");
            taskDueDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorError));
        } else {
            taskDueDate.setText("Due: " + task.getDueDate());
            taskDueDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextMuted));
        }

        String priorityText = task.getPriority() == 2 ? "HIGH" : (task.getPriority() == 1 ? "MED" : "LOW");
        int priorityColor = task.getPriority() == 2 ? R.color.colorError
                : (task.getPriority() == 1 ? R.color.colorSecondary : R.color.colorPrimary);
        taskPriority.setText(priorityText);
        taskPriority.setBackgroundColor(getResources().getColor(priorityColor));

        int progress = task.getCompletionPercentage();
        progressBar.setProgress(progress);
        progressText.setText(progress + "%");

        subtasksContainer.removeAllViews();

        if (task.getSubtasks() != null) {
            for (Subtask subtask : task.getSubtasks()) {
                View subtaskRow = LayoutInflater.from(getContext()).inflate(R.layout.item_subtask, null);
                CheckBox checkBox = subtaskRow.findViewById(R.id.subtaskCheckbox);
                Button deleteButton = subtaskRow.findViewById(R.id.buttonDeleteSubtask);

                checkBox.setText(subtask.getTitle());
                checkBox.setChecked(subtask.isCompleted());

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    subtask.setCompleted(isChecked);
                    task.updateCompletionFromSubtasks();
                    executorService.execute(() -> {
                        db.taskDao().update(task);
                        scheduleNotification(task);
                    });
                });

                deleteButton.setOnClickListener(v -> {
                    task.getSubtasks().remove(subtask);
                    task.updateCompletionFromSubtasks();
                    executorService.execute(() -> {
                        db.taskDao().update(task);
                        scheduleNotification(task);
                    });
                });

                subtasksContainer.addView(subtaskRow);
            }
        }

        buttonEdit.setOnClickListener(v -> showEditTaskDialog(task));

        buttonDelete.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> executorService.execute(() -> {
                    db.taskDao().delete(task);
                    WorkManager.getInstance(getContext()).cancelUniqueWork(task.getId());
                }))
                .setNegativeButton("Cancel", null)
                .show());

        buttonAddSubtask.setOnClickListener(v -> showAddSubtaskDialog(task));

        return card;
    }

    private void showAddSubtaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_subtask, null);
        EditText editSubtaskTitle = dialogView.findViewById(R.id.editSubtaskTitle);

        builder.setTitle("Add Subtask")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = editSubtaskTitle.getText().toString().trim();
                    if (!title.isEmpty()) {
                        if (task.getSubtasks() == null) {
                            task.setSubtasks(new ArrayList<>());
                        }
                        task.getSubtasks().add(new Subtask(UUID.randomUUID().toString(), title));
                        task.updateCompletionFromSubtasks();
                        executorService.execute(() -> db.taskDao().update(task));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task, null);

        EditText editTitle = dialogView.findViewById(R.id.editTaskTitle);
        EditText editDueDate = dialogView.findViewById(R.id.editDueDate);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerPriority);

        editDueDate.setOnClickListener(v -> showDatePicker(editDueDate));

        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, priorities);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerPriority.setAdapter(adapter);

        builder.setTitle("Add New Task")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = editTitle.getText().toString();
                    String dueDate = editDueDate.getText().toString();
                    int priority = spinnerPriority.getSelectedItemPosition();

                    if (!title.isEmpty()) {
                        Task newTask = new Task(UUID.randomUUID().toString(), title, "",
                                dueDate.isEmpty() ? "No Date" : dueDate, "General", priority);
                        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
                        if (userId != null) {
                            newTask.setUserId(userId);
                        }

                        executorService.execute(() -> {
                            db.taskDao().insert(newTask);
                            scheduleNotification(newTask);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task, null);

        EditText editTitle = dialogView.findViewById(R.id.editTaskTitle);
        EditText editDueDate = dialogView.findViewById(R.id.editDueDate);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerPriority);

        editTitle.setText(task.getTitle());
        editDueDate.setText(task.getDueDate());
        editDueDate.setOnClickListener(v -> showDatePicker(editDueDate));

        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, priorities);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerPriority.setAdapter(adapter);
        spinnerPriority.setSelection(task.getPriority());

        builder.setTitle("Edit Task")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    task.setTitle(editTitle.getText().toString());
                    task.setDueDate(editDueDate.getText().toString());
                    task.setPriority(spinnerPriority.getSelectedItemPosition());
                    executorService.execute(() -> {
                        db.taskDao().update(task);
                        scheduleNotification(task);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker(EditText dateField) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_Alert,
                (view, year1, monthOfYear, dayOfMonth) ->
                        dateField.setText(String.format(Locale.US, "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth)),
                year, month, day);
        datePickerDialog.show();
    }

    private void scheduleNotification(Task task) {
        if (task.getDueDate() == null || task.getDueDate().isEmpty() || task.getDueDate().equals("No Date")) {
            return;
        }

        Long dueMillis = parseDateMillis(task.getDueDate());
        if (dueMillis == null) return;

        long delay = dueMillis - System.currentTimeMillis();

        if (delay > 0) {
            Data inputData = new Data.Builder()
                    .putString("taskTitle", task.getTitle())
                    .build();

            OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(getContext()).enqueueUniqueWork(
                    task.getId(),
                    ExistingWorkPolicy.REPLACE,
                    notificationWork
            );
        }
    }
}
