package com.studybuddy.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "studybuddy_session";
    private static final String KEY_LOGGED_IN_STUDENT_ID = "logged_in_student_id";

    private static SessionManager instance;
    private final SharedPreferences preferences;

    private SessionManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void login(String studentId) {
        preferences.edit().putString(KEY_LOGGED_IN_STUDENT_ID, studentId).apply();
    }

    public void logout() {
        preferences.edit().clear().apply();
    }

    public String getCurrentUserId() {
        return preferences.getString(KEY_LOGGED_IN_STUDENT_ID, null);
    }

    public boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }
}
