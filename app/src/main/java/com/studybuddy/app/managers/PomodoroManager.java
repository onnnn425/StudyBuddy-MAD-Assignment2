package com.studybuddy.app.managers;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class PomodoroManager {
    public enum State { IDLE, RUNNING, PAUSED, BREAK }

    private static PomodoroManager instance;
    private State currentState = State.IDLE;
    private int workMinutes = 25;
    private int breakMinutes = 5;
    private int secondsRemaining;
    private Handler handler;
    private Runnable timerRunnable;
    private List<TimerListener> listeners;

    private PomodoroManager() {
        handler = new Handler(Looper.getMainLooper());
        listeners = new ArrayList<>();
        reset();
    }

    public static PomodoroManager getInstance() {
        if (instance == null) {
            instance = new PomodoroManager();
        }
        return instance;
    }

    public void start() {
        if (currentState == State.IDLE || currentState == State.PAUSED) {
            currentState = State.RUNNING;
            startTimer();
            notifyListeners();
        }
    }

    public void pause() {
        if (currentState == State.RUNNING) {
            currentState = State.PAUSED;
            handler.removeCallbacks(timerRunnable);
            notifyListeners();
        }
    }

    public void reset() {
        handler.removeCallbacks(timerRunnable);
        currentState = State.IDLE;
        secondsRemaining = workMinutes * 60;
        notifyListeners();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == State.RUNNING) {
                    if (secondsRemaining > 0) {
                        secondsRemaining--;
                        notifyListeners();
                        handler.postDelayed(this, 1000);
                    } else {
                        currentState = State.BREAK;
                        secondsRemaining = breakMinutes * 60;
                        notifyListeners();
                        onSessionCompleted(State.RUNNING);
                        startTimer();
                    }
                } else if (currentState == State.BREAK) {
                    if (secondsRemaining > 0) {
                        secondsRemaining--;
                        notifyListeners();
                        handler.postDelayed(this, 1000);
                    } else {
                        currentState = State.IDLE;
                        secondsRemaining = workMinutes * 60;
                        notifyListeners();
                        onSessionCompleted(State.BREAK);
                    }
                }
            }
        };
        handler.post(timerRunnable);
    }

    private void onSessionCompleted(State completedState) {
        for (TimerListener listener : listeners) {
            if (completedState == State.RUNNING) {
                listener.onWorkSessionCompleted();
            } else if (completedState == State.BREAK) {
                listener.onBreakSessionCompleted();
            }
            listener.onSessionCompleted();
        }
    }

    public State getCurrentState() { return currentState; }
    public int getSecondsRemaining() { return secondsRemaining; }
    public int getMinutesRemaining() { return secondsRemaining / 60; }
    public int getWorkMinutes() { return workMinutes; }
    public int getBreakMinutes() { return breakMinutes; }
    public void setWorkMinutes(int minutes) { this.workMinutes = minutes; }
    public void setBreakMinutes(int minutes) { this.breakMinutes = minutes; }

    public void addListener(TimerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(TimerListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (TimerListener listener : listeners) {
            listener.onTimerUpdate();
        }
    }

    public interface TimerListener {
        void onTimerUpdate();
        void onSessionCompleted();

        default void onWorkSessionCompleted() {
            onSessionCompleted();
        }

        default void onBreakSessionCompleted() {
            onSessionCompleted();
        }
    }
}
