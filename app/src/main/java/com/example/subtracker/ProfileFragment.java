package com.example.subtracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private TextView tvUsername;
    private Button btnLogout;
    private String currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        View view = inflator.inflate(R.layout.fragment_profile, container, false);

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            currentUser = prefs.getString("current_user", null);
        }

        tvUsername = view.findViewById(R.id.tvUsername);
        btnLogout = view.findViewById(R.id.btnLogout);

        tvUsername.setText(currentUser != null ? currentUser : "Гость");

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                SharedPreferences prefs = getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
                prefs.edit().remove("current_user").apply();
                Toast.makeText(getActivity(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        });

        return view;
    }
}