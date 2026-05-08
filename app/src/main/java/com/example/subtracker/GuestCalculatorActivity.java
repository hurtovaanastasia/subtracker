package com.example.subtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuestCalculatorActivity extends AppCompatActivity {

    private EditText etService1, etAmount1, etDate1;
    private EditText etService2, etAmount2, etDate2;
    private EditText etService3, etAmount3, etDate3;
    private TextView tvTotal, tvAnalytics;
    private Button btnCalculate, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_calculator);

        etService1 = findViewById(R.id.etService1);
        etAmount1 = findViewById(R.id.etAmount1);
        etDate1 = findViewById(R.id.etDate1);

        etService2 = findViewById(R.id.etService2);
        etAmount2 = findViewById(R.id.etAmount2);
        etDate2 = findViewById(R.id.etDate2);

        etService3 = findViewById(R.id.etService3);
        etAmount3 = findViewById(R.id.etAmount3);
        etDate3 = findViewById(R.id.etDate3);

        tvTotal = findViewById(R.id.tvTotal);
        tvAnalytics = findViewById(R.id.tvAnalytics);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnSave = findViewById(R.id.btnSave);

        btnCalculate.setOnClickListener(v -> calculateTotal());

        btnSave.setOnClickListener(v -> saveAndRegister());
    }

    private void calculateTotal() {
        double total = 0;
        StringBuilder analytics = new StringBuilder("Ваши траты:\n");

        if (!etService1.getText().toString().isEmpty()) {
            double amount1 = parseAmount(etAmount1.getText().toString());
            total += amount1;
            analytics.append("• ").append(etService1.getText().toString())
                    .append(": ").append(amount1).append("₽\n");
        }

        if (!etService2.getText().toString().isEmpty()) {
            double amount2 = parseAmount(etAmount2.getText().toString());
            total += amount2;
            analytics.append("• ").append(etService2.getText().toString())
                    .append(": ").append(amount2).append("₽\n");
        }

        if (!etService3.getText().toString().isEmpty()) {
            double amount3 = parseAmount(etAmount3.getText().toString());
            total += amount3;
            analytics.append("• ").append(etService3.getText().toString())
                    .append(": ").append(amount3).append("₽\n");
        }

        tvTotal.setText("Итого в месяц: " + total + " ₽");
        tvAnalytics.setText(analytics.toString());
    }

    private void saveAndRegister() {
        // Собираем данные гостя
        List<Map<String, Object>> guestSubscriptions = new ArrayList<>();

        addSubscriptionToList(guestSubscriptions, etService1, etAmount1, etDate1);
        addSubscriptionToList(guestSubscriptions, etService2, etAmount2, etDate2);
        addSubscriptionToList(guestSubscriptions, etService3, etAmount3, etDate3);

        if (guestSubscriptions.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы один сервис", Toast.LENGTH_SHORT).show();
            return;
        }

        // Передаём данные в экран регистрации
        Intent intent = new Intent(GuestCalculatorActivity.this, RegisterActivity.class);

        // Кладём данные в Intent как строку JSON (простой способ)
        StringBuilder jsonData = new StringBuilder();
        for (Map<String, Object> sub : guestSubscriptions) {
            jsonData.append(sub.get("name")).append("|")
                    .append(sub.get("amount")).append("|")
                    .append(sub.get("date")).append(";");
        }
        intent.putExtra("guest_data", jsonData.toString());
        startActivity(intent);
    }

    private void addSubscriptionToList(List<Map<String, Object>> list,
                                       EditText etName, EditText etAmount, EditText etDate) {
        String name = etName.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (!name.isEmpty() && !amountStr.isEmpty()) {
            Map<String, Object> sub = new HashMap<>();
            sub.put("name", name);
            sub.put("amount", Double.parseDouble(amountStr));
            sub.put("date", date.isEmpty() ? "01.01.2025" : date);
            list.add(sub);
        }
    }

    private double parseAmount(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}