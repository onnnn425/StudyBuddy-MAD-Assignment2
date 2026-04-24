package com.studybuddy.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.managers.TaskManager;
import com.studybuddy.app.models.User;
import com.studybuddy.app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonSignUp;
    private TextView textForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        TaskManager.getInstance(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textForgotPassword = findViewById(R.id.textForgotPassword);

        editTextEmail.setHint("Student ID");

        buttonLogin.setOnClickListener(v -> attemptLogin());

        buttonSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        textForgotPassword.setOnClickListener(v -> showForgotPasswordFlow());
    }

    private void attemptLogin() {
        final String studentId = editTextEmail.getText().toString().trim();
        final String password = editTextPassword.getText().toString();

        if (studentId.isEmpty()) {
            editTextEmail.setError("Student ID is required");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        buttonLogin.setEnabled(false);

        new Thread(() -> {
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(LoginActivity.this);
            String passwordHash = User.hashPassword(password);
            User user = db.userDao().login(studentId, passwordHash);

            runOnUiThread(() -> {
                buttonLogin.setEnabled(true);
                if (user != null) {
                    SessionManager.getInstance(LoginActivity.this).login(studentId);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Student ID or password", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showForgotPasswordFlow() {
        final EditText studentIdInput = new EditText(this);
        studentIdInput.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout studentIdLayout = new LinearLayout(this);
        studentIdLayout.setOrientation(LinearLayout.VERTICAL);
        studentIdLayout.setPadding(50, 40, 50, 10);
        studentIdLayout.addView(studentIdInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog studentIdDialog = new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setMessage("Enter your Student ID:")
                .setView(studentIdLayout)
                .setPositiveButton("Next", null)
                .setNegativeButton("Cancel", null)
                .create();

        studentIdDialog.setOnShowListener(dialog -> studentIdDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String studentId = studentIdInput.getText().toString().trim();

            if (studentId.isEmpty()) {
                studentIdInput.setError("Student ID is required");
                studentIdInput.requestFocus();
                return;
            }

            studentIdDialog.dismiss();

            new Thread(() -> {
                StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(LoginActivity.this);
                User user = db.userDao().findByStudentId(studentId);

                if (user == null) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "No account found with this Student ID.", Toast.LENGTH_SHORT).show());
                    return;
                }

                String securityQuestion = user.getSecurityQuestion();
                if (securityQuestion == null || securityQuestion.trim().isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "No security question set for this account.", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> {
                    final EditText answerInput = new EditText(LoginActivity.this);
                    answerInput.setInputType(InputType.TYPE_CLASS_TEXT);

                    LinearLayout answerLayout = new LinearLayout(LoginActivity.this);
                    answerLayout.setOrientation(LinearLayout.VERTICAL);
                    answerLayout.setPadding(50, 40, 50, 10);
                    answerLayout.addView(answerInput, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    AlertDialog answerDialog = new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Security Question")
                            .setMessage(user.getSecurityQuestion())
                            .setView(answerLayout)
                            .setPositiveButton("Submit", null)
                            .setNegativeButton("Cancel", null)
                            .create();

                    answerDialog.setOnShowListener(answerDlg -> answerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                        String inputAnswer = answerInput.getText().toString().trim().toLowerCase();
                        String storedAnswer = user.getSecurityAnswer();

                        if (storedAnswer == null || !inputAnswer.equals(storedAnswer)) {
                            Toast.makeText(LoginActivity.this, "Incorrect answer. Please try again.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final EditText newPasswordInput = new EditText(LoginActivity.this);
                        newPasswordInput.setHint("New Password");
                        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                        final EditText confirmPasswordInput = new EditText(LoginActivity.this);
                        confirmPasswordInput.setHint("Confirm Password");
                        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                        LinearLayout passwordLayout = new LinearLayout(LoginActivity.this);
                        passwordLayout.setOrientation(LinearLayout.VERTICAL);
                        passwordLayout.setPadding(50, 40, 50, 10);
                        passwordLayout.addView(newPasswordInput, new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        passwordLayout.addView(confirmPasswordInput, new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));

                        AlertDialog passwordDialog = new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("Set New Password")
                                .setView(passwordLayout)
                                .setPositiveButton("Save", null)
                                .setNegativeButton("Cancel", null)
                                .create();

                        passwordDialog.setOnShowListener(passwordDlg -> passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                            final String newPassword = newPasswordInput.getText().toString();
                            final String confirmPassword = confirmPasswordInput.getText().toString();

                            if (newPassword.length() < 6) {
                                Toast.makeText(LoginActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (!newPassword.equals(confirmPassword)) {
                                Toast.makeText(LoginActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new Thread(() -> {
                                StudyBuddyDatabase db2 = StudyBuddyDatabase.getDatabase(LoginActivity.this);
                                String hashedPassword = User.hashPassword(newPassword);
                                user.setPasswordHash(hashedPassword);
                                db2.userDao().update(user);

                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Password reset successful. Please log in.", Toast.LENGTH_SHORT).show();
                                    passwordDialog.dismiss();
                                    answerDialog.dismiss();
                                });
                            }).start();
                        }));

                        passwordDialog.show();
                    }));

                    answerDialog.show();
                });
            }).start();
        }));

        studentIdDialog.show();
    }
}

