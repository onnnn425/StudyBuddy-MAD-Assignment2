package com.studybuddy.app.managers;

import android.content.Context;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.models.TimetableEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarManager {
    private static CalendarManager instance;
    private List<CalendarEventListener> listeners;

    private CalendarManager() {
        listeners = new ArrayList<>();
    }

    public static CalendarManager getInstance() {
        if (instance == null) {
            instance = new CalendarManager();
        }
        return instance;
    }

    public List<Object> getEventsForDate(Context context, String date) {
        List<Object> events = new ArrayList<>();

        if (context == null) return events;

        //get tasks due on this date
        List<Task> allTasks = TaskManager.getInstance(context).getAllTasksSync();
        for (Task task : allTasks) {
            if (task.getDueDate() != null && task.getDueDate().equals(date)) {
                events.add(task);
            }
        }

        //get timetable entries for this dates day of week
        String dayOfWeek = getDayOfWeekFromDate(date);
        List<TimetableEntry> allEntries = TimetableManager.getInstance(context).getAllEntries();

        if (allEntries != null) {
            for (TimetableEntry entry : allEntries) {
                if (entry.getDay() != null && entry.getDay().equalsIgnoreCase(dayOfWeek)) {
                    events.add(entry);
                }
            }
        }

        return events;
    }

    private String getDayOfWeekFromDate(String date) {
        try {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1;
                int day = Integer.parseInt(parts[2]);

                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day);

                int dayOfWeek = cal.get(Calendar.getAvailableLocales()[0].getDisplayCountry().equals("US") ? Calendar.DAY_OF_WEEK : Calendar.DAY_OF_WEEK);
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                
                switch (dayOfWeek) {
                    case Calendar.MONDAY: return "Monday";
                    case Calendar.TUESDAY: return "Tuesday";
                    case Calendar.WEDNESDAY: return "Wednesday";
                    case Calendar.THURSDAY: return "Thursday";
                    case Calendar.FRIDAY: return "Friday";
                    case Calendar.SATURDAY: return "Saturday";
                    case Calendar.SUNDAY: return "Sunday";
                }
            }
        } catch (Exception e) {
            //log error or ignore
        }
        return "";
    }

    public void addEventListener(CalendarEventListener listener) { 
        if (!listeners.contains(listener)) {
            listeners.add(listener); 
        }
    }
    
    public void removeEventListener(CalendarEventListener listener) {
        listeners.remove(listener);
    }

    public interface CalendarEventListener {
        void onEventsChanged();
    }
}