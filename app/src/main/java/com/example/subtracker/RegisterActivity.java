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
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etLogin, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvError, btnBackToLogin;
    private FirebaseFirestore db;
    private String guestData; // Данные из гостевого режима

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvError = findViewById(R.id.tvError);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        // Получаем данные из гостевого режима
        guestData = getIntent().getStringExtra("guest_data");

        btnBackToLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(login) || TextUtils.isEmpty(password)) {
            showError("Заполните все поля");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Пароли не совпадают");
            return;
        }

        if (password.length() < 3) {
            showError("Пароль должен быть не менее 3 символов");
            return;
        }

        // Проверяем, не занят ли логин
        db.collection("users").document(login).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        showError("Логин уже занят");
                    } else {
                        createUser(login, password);
                    }
                })
                .addOnFailureListener(e -> showError("Ошибка: " + e.getMessage()));
    }

    private void createUser(String login, String password) {
        Map<String, Object> user = new HashMap<>();
        user.put("password", password);
        user.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(login).set(user)
                .addOnSuccessListener(aVoid -> {
                    // Сохраняем данные гостя, если они есть
                    if (guestData != null && !guestData.isEmpty()) {
                        saveGuestSubscriptions(login);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> showError("Ошибка: " + e.getMessage()));
    }

    private void saveGuestSubscriptions(String login) {
        // Разбираем данные: "Netflix|799|15.01.2025;Spotify|199|20.01.2025;"
        String[] subscriptions = guestData.split(";");

        for (String sub : subscriptions) {
            if (sub.isEmpty()) continue;
            String[] parts = sub.split("\\|");
            if (parts.length >= 2) {
                String name = parts[0];
                double amount = Double.parseDouble(parts[1]);
                String date = parts.length >= 3 ? parts[2] : "01.01.2025";

                Map<String, Object> subscription = new HashMap<>();
                subscription.put("name", name);
                subscription.put("amount", amount);
                subscription.put("startDate", date);
                subscription.put("periodicity", "monthly");
                subscription.put("nextPaymentDate", date);

                // Сохраняем в подколлекцию пользователя
                db.collection("users")
                        .document(login)
                        .collection("subscriptions")
                        .add(subscription);
            }
        }

        Toast.makeText(RegisterActivity.this, "Регистрация успешна! Ваши подписки сохранены.", Toast.LENGTH_LONG).show();

        // Автоматически входим
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putString("current_user", login).apply();

        // Переходим в главный экран
        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(android.view.View.VISIBLE);
    }
}