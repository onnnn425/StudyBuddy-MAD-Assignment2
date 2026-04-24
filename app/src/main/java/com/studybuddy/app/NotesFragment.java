package com.studybuddy.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.models.Note;
import com.studybuddy.app.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesFragment extends Fragment {
    private LinearLayout notesContainer;
    private LinearLayout searchBarContainer;
    private EditText editSearchNotes;
    private TextView searchNotes;
    private StudyBuddyDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String currentFilter = "All";
    private boolean isSearchMode = false;

    public NotesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        notesContainer = view.findViewById(R.id.notesContainer);
        FloatingActionButton fabAddNote = view.findViewById(R.id.fabAddNote);
        searchNotes = view.findViewById(R.id.searchNotes);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        editSearchNotes = view.findViewById(R.id.editSearchNotes);
        TextView textCancelSearch = view.findViewById(R.id.textCancelSearch);

        db = StudyBuddyDatabase.getDatabase(getContext());

        loadNotes();

        fabAddNote.setOnClickListener(v -> showAddNoteDialog());

        searchNotes.setOnClickListener(v -> {
            isSearchMode = true;
            searchBarContainer.setVisibility(View.VISIBLE);
            searchNotes.setVisibility(View.GONE);
            editSearchNotes.requestFocus();
        });

        textCancelSearch.setOnClickListener(v -> {
            isSearchMode = false;
            editSearchNotes.setText("");
            searchBarContainer.setVisibility(View.GONE);
            searchNotes.setVisibility(View.VISIBLE);
            loadNotes();
        });

        editSearchNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    loadNotes();
                } else {
                    searchNotes(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadNotes() {
        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) {
            db.noteDao().getAllNotes().observe(getViewLifecycleOwner(), this::refreshNotesList);
            return;
        }

        if (currentFilter.equals("All")) {
            db.noteDao().getNotesLiveDataByUser(userId).observe(getViewLifecycleOwner(), this::refreshNotesList);
        } else {
            db.noteDao().getNotesBySubjectForUser(userId, currentFilter).observe(getViewLifecycleOwner(), this::refreshNotesList);
        }
    }

    private void searchNotes(String query) {
        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) return;
        String likeQuery = "%" + query + "%";
        db.noteDao().searchNotesByUser(userId, likeQuery).observe(getViewLifecycleOwner(), this::refreshNotesList);
    }

    private void refreshNotesList(List<Note> notes) {
        notesContainer.removeAllViews();

        if (notes == null || notes.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText(isSearchMode ? "No notes match your search." : "No notes found for: " + currentFilter);
            emptyView.setPadding(32, 32, 32, 32);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            notesContainer.addView(emptyView);
            return;
        }

        int i;
        for (i = 0; i < notes.size(); i++) {
            notesContainer.addView(createNoteCard(notes.get(i)));
        }
    }

    private View createNoteCard(Note note) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_note, null);

        TextView noteTitle = card.findViewById(R.id.noteTitle);
        TextView noteSubject = card.findViewById(R.id.noteSubject);
        TextView notePreview = card.findViewById(R.id.notePreview);
        TextView noteDate = card.findViewById(R.id.noteDate);
        View colorBar = card.findViewById(R.id.colorBar);
        Button buttonEdit = card.findViewById(R.id.buttonEditNote);
        Button buttonDelete = card.findViewById(R.id.buttonDeleteNote);

        noteTitle.setText(note.getTitle());
        noteSubject.setText(note.getSubject());

        String preview = note.getContent().length() > 50 ? note.getContent().substring(0, 50) + "..." : note.getContent();
        notePreview.setText(preview);
        noteDate.setText("Date: " + note.getLastModified());

        int colorRes = getSubjectColor(note.getSubject());
        colorBar.setBackgroundColor(getResources().getColor(colorRes));

        card.setOnClickListener(v -> showNoteDetailDialog(note));
        buttonEdit.setOnClickListener(v -> showEditNoteDialog(note));
        buttonDelete.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Delete Note")
                .setMessage("Remove this note?")
                .setPositiveButton("Delete", (dialog, which) -> executorService.execute(() -> db.noteDao().delete(note)))
                .setNegativeButton("Cancel", null)
                .show());

        return card;
    }

    private int getSubjectColor(String subject) {
        if ("Mobile Dev".equals(subject)) {
            return android.R.color.holo_blue_dark;
        }
        if ("OS".equals(subject)) {
            return android.R.color.holo_green_dark;
        }
        if ("DBMS".equals(subject)) {
            return android.R.color.holo_orange_dark;
        }
        return android.R.color.darker_gray;
    }

    private void showAddNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText editTitle = dialogView.findViewById(R.id.editNoteTitle);
        EditText editContent = dialogView.findViewById(R.id.editNoteContent);
        EditText editSubject = dialogView.findViewById(R.id.editNoteSubject);

        builder.setTitle("Add New Note")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editTitle.getText().toString().trim();
                    String content = editContent.getText().toString().trim();
                    String subject = editSubject.getText().toString().trim();

                    if (!title.isEmpty() && !content.isEmpty()) {
                        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
                        Note newNote = new Note(UUID.randomUUID().toString(), title, content,
                                subject.isEmpty() ? "General" : subject, date, date);
                        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
                        if (userId != null) {
                            newNote.setUserId(userId);
                        }
                        executorService.execute(() -> db.noteDao().insert(newNote));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditNoteDialog(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note, null);
        EditText editTitle = dialogView.findViewById(R.id.editNoteTitle);
        EditText editContent = dialogView.findViewById(R.id.editNoteContent);
        EditText editSubject = dialogView.findViewById(R.id.editNoteSubject);

        editTitle.setText(note.getTitle());
        editContent.setText(note.getContent());
        editSubject.setText(note.getSubject());

        builder.setTitle("Edit Note")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    note.setTitle(editTitle.getText().toString());
                    note.setContent(editContent.getText().toString());
                    note.setSubject(editSubject.getText().toString());
                    note.setLastModified(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
                    executorService.execute(() -> db.noteDao().update(note));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNoteDetailDialog(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_note_detail, null);
        ((TextView) dialogView.findViewById(R.id.detailNoteTitle)).setText(note.getTitle());
        ((TextView) dialogView.findViewById(R.id.detailNoteContent)).setText(note.getContent());
        builder.setView(dialogView).setPositiveButton("Close", null).show();
    }
}

