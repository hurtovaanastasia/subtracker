package com.example.subtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister, btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(StartActivity.this, LoginActivity.class));
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(StartActivity.this, RegisterActivity.class));
        });

        btnGuest.setOnClickListener(v -> {
            startActivity(new Intent(StartActivity.this, GuestCalculatorActivity.class));
        });
    }
}