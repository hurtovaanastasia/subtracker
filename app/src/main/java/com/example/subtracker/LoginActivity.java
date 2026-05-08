package com.example.subtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin;
    private TextView tvError, btnGoToRegister;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(login) || TextUtils.isEmpty(password)) {
            showError("Введите логин и пароль");
            return;
        }

        db.collection("users").document(login).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String correctPassword = documentSnapshot.getString("password");
                        if (password.equals(correctPassword)) {
                            saveUserSession(login);
                            Toast.makeText(LoginActivity.this, "Добро пожаловать, " + login, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            showError("Неверный пароль");
                        }
                    } else {
                        showError("Пользователь не найден");
                    }
                })
                .addOnFailureListener(e -> showError("Ошибка: " + e.getMessage()));
    }

    private void saveUserSession(String login) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putString("current_user", login).apply();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(android.view.View.VISIBLE);
    }
}