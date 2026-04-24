package com.studybuddy.app.managers;

import com.studybuddy.app.models.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NoteManager {
    private static NoteManager instance;
    private List<Note> notes;
    private List<Runnable> listeners;

    private NoteManager() {
        notes = new ArrayList<>();
        listeners = new ArrayList<>();
        addSampleData();
    }

    public static NoteManager getInstance() {
        if (instance == null) {
            instance = new NoteManager();
        }
        return instance;
    }

    private void addSampleData() {
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        notes.add(new Note(UUID.randomUUID().toString(), "Activity Lifecycle Notes",
                "onCreate -> onStart -> onResume -> onPause -> onStop -> onDestroy",
                "Mobile Dev", date, date));
        notes.add(new Note(UUID.randomUUID().toString(), "Process Scheduling Algorithms",
                "FCFS, SJF, Round Robin, Priority Scheduling",
                "OS", "12 Apr 2026", "12 Apr 2026"));
    }

    public List<Note> getAllNotes() {
        return new ArrayList<>(notes);
    }

    public List<Note> getNotesBySubject(String subject) {
        if (subject.equals("All")) {
            return getAllNotes();
        }
        List<Note> filtered = new ArrayList<>();
        int i;
        for (i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.getSubject().equals(subject)) {
                filtered.add(note);
            }
        }
        return filtered;
    }

    public void addNote(Note note) {
        notes.add(note);
        notifyListeners();
    }

    public void updateNote(Note note) {
        int i;
        for (i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(note.getId())) {
                notes.set(i, note);
                break;
            }
        }
        notifyListeners();
    }

    public void deleteNote(String noteId) {
        int i;
        for (i = notes.size() - 1; i >= 0; i--) {
            if (notes.get(i).getId().equals(noteId)) {
                notes.remove(i);
            }
        }
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        int i;
        for (i = 0; i < listeners.size(); i++) {
            listeners.get(i).run();
        }
    }
}
