package com.studybuddy.app;

import android.app.AlertDialog;
import android.content.Intent;
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

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.models.Note;
import com.studybuddy.app.models.Task;
import com.studybuddy.app.models.User;
import com.studybuddy.app.utils.SessionManager;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView textAvatarInitials;
    private TextView textProfileName;
    private TextView textProfileCourse;
    private TextView textProfileStudentId;
    private TextView textProfileUniversity;
    private TextView textStatTasks;
    private TextView textStatPomodoros;
    private TextView textStatNotes;
    private LinearLayout rowEditProfile;
    private LinearLayout rowEditQuestion;
    private Button buttonLogout;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textAvatarInitials = view.findViewById(R.id.textAvatarInitials);
        textProfileName = view.findViewById(R.id.textProfileName);
        textProfileCourse = view.findViewById(R.id.textProfileCourse);
        textProfileStudentId = view.findViewById(R.id.textProfileStudentId);
        textProfileUniversity = view.findViewById(R.id.textProfileUniversity);
        textStatTasks = view.findViewById(R.id.textStatTasks);
        textStatPomodoros = view.findViewById(R.id.textStatPomodoros);
        textStatNotes = view.findViewById(R.id.textStatNotes);
        rowEditProfile = view.findViewById(R.id.rowEditProfile);
        rowEditQuestion = view.findViewById(R.id.rowEditQuestion);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        rowEditProfile.setOnClickListener(v -> showEditProfileDialog());
        rowEditQuestion.setOnClickListener(v -> showPasswordGateForQuestion());
        buttonLogout.setOnClickListener(v -> logout());

        loadProfile();
        loadStats();

        return view;
    }

    private void loadProfile() {
        new Thread(() -> {
            String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
            User user = null;
            if (userId != null) {
                user = db.userDao().findByStudentId(userId);
            }
            User finalUser = user;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> displayProfile(finalUser));
            }
        }).start();
    }

    private void displayProfile(User user) {
        if (user == null) {
            textProfileName.setText("Your Name");
            textProfileCourse.setText("Tap Edit Profile to set up your info");
            textProfileStudentId.setText("");
            textProfileUniversity.setText("");
            textAvatarInitials.setText("?");
            return;
        }

        String name = user.getName() != null && !user.getName().trim().isEmpty() ? user.getName() : "Your Name";
        String course = user.getCourse() != null ? user.getCourse() : "";
        String university = user.getUniversity() != null ? user.getUniversity() : "";

        textProfileName.setText(name);
        textProfileCourse.setText(course);
        textProfileStudentId.setText("ID: " + user.getStudentId());
        textProfileUniversity.setText(university);
        textAvatarInitials.setText(buildInitials(name));
    }

    private String buildInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(Character.toUpperCase(parts[0].charAt(0)))
                    + Character.toUpperCase(parts[parts.length - 1].charAt(0));
        }
        return String.valueOf(Character.toUpperCase(name.trim().charAt(0)));
    }

    private void loadStats() {
        new Thread(() -> {
            String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
            int tasksCompleted = 0;
            int notesCount = 0;
            int totalSessions = 0;

            if (userId != null) {
                StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
                List<Task> tasks = db.taskDao().getTasksByUser(userId);
                List<Note> notes = db.noteDao().getNotesByUser(userId);
                int i;
                for (i = 0; i < tasks.size(); i++) {
                    if (tasks.get(i).isCompleted()) {
                        tasksCompleted++;
                    }
                }
                notesCount = notes.size();
                totalSessions = db.pomodoroSettingsDao().getTotalSessions(userId);
            }

            int finalTasksCompleted = tasksCompleted;
            int finalNotesCount = notesCount;
            int finalTotalSessions = totalSessions;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    textStatTasks.setText(String.valueOf(finalTasksCompleted));
                    textStatNotes.setText(String.valueOf(finalNotesCount));
                    textStatPomodoros.setText(String.valueOf(finalTotalSessions));
                });
            }
        }).start();
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Profile");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText editName = createField(layout, "Full Name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        EditText editCourse = createField(layout, "Course / Programme", InputType.TYPE_CLASS_TEXT);
        EditText editUniversity = createField(layout, "University", InputType.TYPE_CLASS_TEXT);
        EditText editEmail = createField(layout, "Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        TextView passwordSection = new TextView(getContext());
        passwordSection.setText("Change Password");
        passwordSection.setTextColor(getResources().getColor(R.color.colorOnBackground));
        passwordSection.setTextSize(16);
        passwordSection.setPadding(0, padding / 2, 0, padding / 4);
        layout.addView(passwordSection);

        EditText editOldPassword = createField(layout, "Old Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText editNewPassword = createField(layout, "New Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText editConfirmNewPassword = createField(layout, "Confirm New Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
            User user = db.userDao().findByStudentId(userId);
            if (getActivity() != null && user != null) {
                getActivity().runOnUiThread(() -> {
                    if (user.getName() != null) {
                        editName.setText(user.getName());
                    }
                    if (user.getCourse() != null) {
                        editCourse.setText(user.getCourse());
                    }
                    if (user.getUniversity() != null) {
                        editUniversity.setText(user.getUniversity());
                    }
                    if (user.getEmail() != null) {
                        editEmail.setText(user.getEmail());
                    }
                });
            }
        }).start();

        builder.setView(layout);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String course = editCourse.getText().toString().trim();
            String university = editUniversity.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String oldPassword = editOldPassword.getText().toString();
            String newPassword = editNewPassword.getText().toString();
            String confirmPassword = editConfirmNewPassword.getText().toString();

            if (name.isEmpty()) {
                editName.setError("Name is required");
                editName.requestFocus();
                return;
            }

            if (!newPassword.isEmpty() || !confirmPassword.isEmpty() || !oldPassword.isEmpty()) {
                if (oldPassword.isEmpty()) {
                    editOldPassword.setError("Enter your old password");
                    editOldPassword.requestFocus();
                    return;
                }
                if (newPassword.length() < 6) {
                    editNewPassword.setError("New password must be at least 6 characters");
                    editNewPassword.requestFocus();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    editConfirmNewPassword.setError("Passwords do not match");
                    editConfirmNewPassword.requestFocus();
                    return;
                }
            }

            new Thread(() -> {
                StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
                User user = db.userDao().findByStudentId(userId);
                if (user == null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                if (!newPassword.isEmpty() || !confirmPassword.isEmpty() || !oldPassword.isEmpty()) {
                    String oldPasswordHash = User.hashPassword(oldPassword);
                    if (user.getPasswordHash() == null || !user.getPasswordHash().equals(oldPasswordHash)) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                editOldPassword.setError("Old password is incorrect");
                                editOldPassword.requestFocus();
                            });
                        }
                        return;
                    }
                    user.setPasswordHash(User.hashPassword(newPassword));
                }

                user.setName(name);
                user.setCourse(course);
                user.setUniversity(university);
                user.setEmail(email.isEmpty() ? null : email);
                db.userDao().update(user);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        displayProfile(user);
                        loadStats();
                        Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            }).start();
        }));
        dialog.show();
    }

    private EditText createField(LinearLayout parent, String hint, int inputType) {
        EditText editText = new EditText(getContext());
        editText.setHint(hint);
        editText.setInputType(inputType);
        editText.setTextColor(getResources().getColor(R.color.colorOnBackground));
        editText.setHintTextColor(getResources().getColor(R.color.colorTextMuted));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        editText.setLayoutParams(params);
        parent.addView(editText);
        return editText;
    }

    private void showPasswordGateForQuestion() {
        String userId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Verify Password");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText editPassword = new EditText(getContext());
        editPassword.setHint("Enter your current password");
        editPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editPassword.setTextColor(getResources().getColor(R.color.colorOnBackground));
        editPassword.setHintTextColor(getResources().getColor(R.color.colorTextMuted));
        layout.addView(editPassword);
        builder.setView(layout);

        AlertDialog dialog = builder.setPositiveButton("Next", null).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = editPassword.getText().toString();
            if (entered.isEmpty()) {
                editPassword.setError("Enter your password");
                return;
            }
            new Thread(() -> {
                StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
                User user = db.userDao().findByStudentId(userId);
                if (user != null && User.hashPassword(entered).equals(user.getPasswordHash())) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            dialog.dismiss();
                            showEditQuestionDialog(user);
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> editPassword.setError("Incorrect password"));
                    }
                }
            }).start();
        }));
        dialog.show();
    }

    private void showEditQuestionDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Security Question");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView qLabel = new TextView(getContext());
        qLabel.setText("Security Question");
        qLabel.setTextColor(getResources().getColor(R.color.colorOnBackground));
        layout.addView(qLabel);

        EditText editQuestion = new EditText(getContext());
        editQuestion.setHint("e.g. What is your pet's name?");
        editQuestion.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        editQuestion.setTextColor(getResources().getColor(R.color.colorOnBackground));
        editQuestion.setHintTextColor(getResources().getColor(R.color.colorTextMuted));
        if (user.getSecurityQuestion() != null) editQuestion.setText(user.getSecurityQuestion());
        layout.addView(editQuestion);

        TextView aLabel = new TextView(getContext());
        aLabel.setText("Answer");
        aLabel.setTextColor(getResources().getColor(R.color.colorOnBackground));
        aLabel.setPadding(0, padding / 2, 0, 0);
        layout.addView(aLabel);

        EditText editAnswer = new EditText(getContext());
        editAnswer.setHint("Your answer");
        editAnswer.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        editAnswer.setTextColor(getResources().getColor(R.color.colorOnBackground));
        editAnswer.setHintTextColor(getResources().getColor(R.color.colorTextMuted));
        layout.addView(editAnswer);
        builder.setView(layout);

        AlertDialog dialog = builder.setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String question = editQuestion.getText().toString().trim();
            String answer = editAnswer.getText().toString().trim();
            if (question.isEmpty()) {
                editQuestion.setError("Question is required");
                return;
            }
            if (answer.isEmpty()) {
                editAnswer.setError("Answer is required");
                return;
            }
            new Thread(() -> {
                StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(requireContext());
                user.setSecurityQuestion(question);
                user.setSecurityAnswer(answer.toLowerCase());
                db.userDao().update(user);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Security question updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            }).start();
        }));
        dialog.show();
    }

    private void logout() {
        SessionManager.getInstance(requireContext()).logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}

