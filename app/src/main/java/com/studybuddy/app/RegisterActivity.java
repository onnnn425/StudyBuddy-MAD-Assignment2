package com.studybuddy.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.studybuddy.app.database.StudyBuddyDatabase;
import com.studybuddy.app.models.User;
import com.studybuddy.app.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editStudentId;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private EditText editEmail;
    private EditText editCourse;
    private EditText editSecurityQuestion;
    private EditText editSecurityAnswer;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editName = findViewById(R.id.editName);
        editStudentId = findViewById(R.id.editStudentId);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        editEmail = findViewById(R.id.editEmail);
        editCourse = findViewById(R.id.editCourse);
        editSecurityQuestion = findViewById(R.id.editSecurityQuestion);
        editSecurityAnswer = findViewById(R.id.editSecurityAnswer);
        buttonRegister = findViewById(R.id.buttonRegister);

        TextView textBack = findViewById(R.id.textBack);
        textBack.setOnClickListener(v -> finish());

        buttonRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        final String name = editName.getText().toString().trim();
        final String studentId = editStudentId.getText().toString().trim();
        final String password = editPassword.getText().toString();
        final String confirmPassword = editConfirmPassword.getText().toString();
        final String email = editEmail.getText().toString().trim();
        final String course = editCourse.getText().toString().trim();
        final String securityQuestion = editSecurityQuestion.getText().toString().trim();
        final String securityAnswer = editSecurityAnswer.getText().toString().trim();

        if (studentId.isEmpty()) {
            editStudentId.setError("Student ID is required");
            editStudentId.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError("Password is required");
            editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editPassword.setError("Password must be at least 6 characters");
            editPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match");
            editConfirmPassword.requestFocus();
            return;
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Enter a valid email address");
            editEmail.requestFocus();
            return;
        }

        if (name.isEmpty()) {
            editName.setError("Full name is required");
            editName.requestFocus();
            return;
        }

        if (securityQuestion.isEmpty()) {
            editSecurityQuestion.setError("Security question is required");
            editSecurityQuestion.requestFocus();
            return;
        }

        if (securityAnswer.isEmpty()) {
            editSecurityAnswer.setError("Security answer is required");
            editSecurityAnswer.requestFocus();
            return;
        }

        buttonRegister.setEnabled(false);

        new Thread(() -> {
            StudyBuddyDatabase db = StudyBuddyDatabase.getDatabase(RegisterActivity.this);
            User existingUser = db.userDao().findByStudentId(studentId);

            if (existingUser != null) {
                runOnUiThread(() -> {
                    buttonRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Student ID already registered", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            User user = new User(studentId, User.hashPassword(password), email.isEmpty() ? null : email, name, course, "");
            user.setSecurityQuestion(securityQuestion);
            user.setSecurityAnswer(securityAnswer.toLowerCase());
            db.userDao().insert(user);

            SessionManager.getInstance(RegisterActivity.this).login(studentId);

            runOnUiThread(() -> {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }).start();
    }
}

