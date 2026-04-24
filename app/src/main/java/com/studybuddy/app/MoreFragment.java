package com.studybuddy.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MoreFragment extends Fragment {

    public MoreFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        Button buttonPomodoro = view.findViewById(R.id.buttonPomodoro);
        Button buttonProfile = view.findViewById(R.id.buttonProfile);

        buttonPomodoro.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening Pomodoro Timer...", Toast.LENGTH_SHORT).show();
            PomodoroFragment pomodoroFragment = new PomodoroFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, pomodoroFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        buttonProfile.setOnClickListener(v -> openFragment(new ProfileFragment()));

        return view;
    }

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
