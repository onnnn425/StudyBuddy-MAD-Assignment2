package com.studybuddy.app;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
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
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.studybuddy.app.managers.TimetableManager;
import com.studybuddy.app.models.TimetableEntry;
import com.studybuddy.app.utils.NotificationWorker;
import com.studybuddy.app.utils.SessionManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TimetableFragment extends Fragment {
    private LinearLayout timetableContainer;
    private TimetableManager timetableManager;
    private String selectedDay;
    private final String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    public TimetableFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);
        timetableContainer = view.findViewById(R.id.timetableContainer);
        FloatingActionButton fabAddEntry = view.findViewById(R.id.fabAddTimetableEntry);
        LinearLayout daySelector = view.findViewById(R.id.daySelectorContainer);

        timetableManager = TimetableManager.getInstance(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE);
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String today = dayNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];

        Bundle args = getArguments();
        boolean forceToday = args != null && args.getBoolean("force_today", false);
        if (forceToday) {
            selectedDay = today;
            prefs.edit().putString("selected_day", today).apply();
        } else {
            selectedDay = prefs.getString("selected_day", today);
        }

        timetableManager.addListener(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::refreshTimetable);
            }
        });

        buildDaySelector(daySelector);
        refreshTimetable();
        fabAddEntry.setOnClickListener(v -> showAddEntryDialog());

        return view;
    }

    private void showTimePickerDialog(EditText timeEditText) {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (timePicker, selectedHour, selectedMinute) -> timeEditText.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)),
                hour, minute, true);
        timePickerDialog.setTitle("Select Time");
        timePickerDialog.show();
    }

    private void scheduleTimetableNotification(TimetableEntry entry) {
        try {
            String[] timeParts = entry.getStartTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar target = Calendar.getInstance();
            int targetDayOfWeek = 1;
            int i;
            for (i = 0; i < days.length; i++) {
                if (days[i].equalsIgnoreCase(entry.getDay())) {
                    targetDayOfWeek = i + 1;
                    break;
                }
            }

            target.set(Calendar.DAY_OF_WEEK, targetDayOfWeek);
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);

            if (target.getTimeInMillis() <= System.currentTimeMillis()) {
                target.add(Calendar.DAY_OF_YEAR, 7);
            }

            long delay = target.getTimeInMillis() - System.currentTimeMillis();

            Data inputData = new Data.Builder()
                    .putString("taskTitle", "Class: " + entry.getTitle() + " at " + entry.getLocation())
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(getContext()).enqueueUniqueWork(
                    entry.getId(),
                    ExistingWorkPolicy.REPLACE,
                    workRequest
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildDaySelector(LinearLayout container) {
        container.removeAllViews();
        String[] displayDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        int i;
        for (i = 0; i < displayDays.length; i++) {
            String day = displayDays[i];
            Button dayButton = new Button(getContext());
            dayButton.setText(day.substring(0, 3));
            dayButton.setPadding(20, 12, 20, 12);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(4, 0, 4, 0);
            dayButton.setLayoutParams(params);

            if (day.equals(selectedDay)) {
                dayButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                dayButton.setTextColor(getResources().getColor(R.color.white));
            } else {
                dayButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorBackground));
                dayButton.setTextColor(getResources().getColor(R.color.colorTextMuted));
            }

            dayButton.setOnClickListener(v -> {
                selectedDay = day;
                requireContext().getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
                        .edit().putString("selected_day", day).apply();
                refreshTimetable();
                buildDaySelector(container);
            });
            container.addView(dayButton);
        }
    }

    private void refreshTimetable() {
        timetableContainer.removeAllViews();

        new Thread(() -> {
            Map<String, List<TimetableEntry>> entriesByDay = timetableManager.getEntriesByDaySync();
            List<TimetableEntry> dayEntries = entriesByDay.get(selectedDay);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (dayEntries == null || dayEntries.isEmpty()) {
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("Please enter your timetable if you are not free!");
                        emptyView.setTextColor(android.graphics.Color.BLACK);
                        emptyView.setPadding(32, 48, 32, 32);
                        emptyView.setGravity(Gravity.CENTER);
                        timetableContainer.addView(emptyView);
                    } else {
                        dayEntries.sort((first, second) -> first.getStartTime().compareTo(second.getStartTime()));
                        int i;
                        for (i = 0; i < dayEntries.size(); i++) {
                            timetableContainer.addView(createEntryCard(dayEntries.get(i)));
                        }
                    }
                });
            }
        }).start();
    }

    private View createEntryCard(TimetableEntry entry) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_timetable_entry, null);
        TextView timeText = card.findViewById(R.id.entryTime);
        TextView titleText = card.findViewById(R.id.entryTitle);
        TextView locationTypeText = card.findViewById(R.id.entryLocationType);
        Button buttonEdit = card.findViewById(R.id.buttonEditEntry);
        Button buttonDelete = card.findViewById(R.id.buttonDeleteEntry);

        timeText.setText(entry.getStartTime() + " - " + entry.getEndTime());
        titleText.setText(entry.getTitle());
        locationTypeText.setText(entry.getType() + "  ·  " + entry.getLocation());

        buttonEdit.setOnClickListener(v -> showEditEntryDialog(entry));

        buttonDelete.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this class?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    timetableManager.deleteEntry(entry.getId());
                    WorkManager.getInstance(getContext()).cancelUniqueWork(entry.getId());
                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show());

        return card;
    }

    private void showAddEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_timetable_entry, null);

        EditText editTitle = dialogView.findViewById(R.id.editEntryTitle);
        EditText editStart = dialogView.findViewById(R.id.editStartTime);
        EditText editEnd = dialogView.findViewById(R.id.editEndTime);
        EditText editLoc = dialogView.findViewById(R.id.editLocation);
        EditText editType = dialogView.findViewById(R.id.editType);

        editStart.setFocusable(false);
        editStart.setOnClickListener(v -> showTimePickerDialog(editStart));
        editEnd.setFocusable(false);
        editEnd.setOnClickListener(v -> showTimePickerDialog(editEnd));

        AlertDialog addDialog = builder.setTitle("Add " + selectedDay + " Entry")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        addDialog.setOnShowListener(d -> addDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String start = editStart.getText().toString().trim();
            String end = editEnd.getText().toString().trim();

            if (title.isEmpty() || start.isEmpty()) {
                Toast.makeText(getContext(), "Title and start time are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!end.isEmpty() && end.compareTo(start) <= 0) {
                Toast.makeText(getContext(), "End time must be later than start time", Toast.LENGTH_SHORT).show();
                return;
            }
            TimetableEntry newEntry = new TimetableEntry(
                    UUID.randomUUID().toString(),
                    title,
                    selectedDay,
                    start,
                    end,
                    editLoc.getText().toString().trim(),
                    editType.getText().toString().trim().isEmpty() ? "Class" : editType.getText().toString().trim()
            );
            String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
            if (userId != null) {
                newEntry.setUserId(userId);
            }
            timetableManager.addEntry(newEntry);
            scheduleTimetableNotification(newEntry);
            Toast.makeText(getContext(), "Entry Saved", Toast.LENGTH_SHORT).show();
            addDialog.dismiss();
        }));

        addDialog.show();
    }

    private void showEditEntryDialog(TimetableEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_timetable_entry, null);

        EditText editTitle = dialogView.findViewById(R.id.editEntryTitle);
        EditText editStart = dialogView.findViewById(R.id.editStartTime);
        EditText editEnd = dialogView.findViewById(R.id.editEndTime);
        EditText editLoc = dialogView.findViewById(R.id.editLocation);
        EditText editType = dialogView.findViewById(R.id.editType);

        editTitle.setText(entry.getTitle());
        editStart.setText(entry.getStartTime());
        editEnd.setText(entry.getEndTime());
        editLoc.setText(entry.getLocation());
        editType.setText(entry.getType());

        editStart.setFocusable(false);
        editStart.setOnClickListener(v -> showTimePickerDialog(editStart));
        editEnd.setFocusable(false);
        editEnd.setOnClickListener(v -> showTimePickerDialog(editEnd));

        AlertDialog editDialog = builder.setTitle("Edit Entry")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        editDialog.setOnShowListener(d -> editDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String start = editStart.getText().toString().trim();
            String end = editEnd.getText().toString().trim();

            if (start.isEmpty()) {
                Toast.makeText(getContext(), "Start time is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!end.isEmpty() && end.compareTo(start) <= 0) {
                Toast.makeText(getContext(), "End time must be later than start time", Toast.LENGTH_SHORT).show();
                return;
            }
            entry.setTitle(editTitle.getText().toString().trim());
            entry.setStartTime(start);
            entry.setEndTime(end);
            entry.setLocation(editLoc.getText().toString().trim());
            entry.setType(editType.getText().toString().trim());
            timetableManager.updateEntry(entry);
            scheduleTimetableNotification(entry);
            Toast.makeText(getContext(), "Entry Updated", Toast.LENGTH_SHORT).show();
            editDialog.dismiss();
        }));

        editDialog.show();
    }
}

