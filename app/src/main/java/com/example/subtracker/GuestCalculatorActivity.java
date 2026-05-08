package com.example.subtracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class GuestCalculatorActivity extends AppCompatActivity {

    private RecyclerView rvServices;
    private CardView cardResult;
    private TextView tvTotal, tvAnalytics, tvLimitMessage;
    private Button btnAddService, btnRegister;

    private FirebaseFirestore db;
    private List<GuestService> servicesList = new ArrayList<>();
    private List<Service> availableServices = new ArrayList<>();
    private List<String> serviceNames = new ArrayList<>();
    private GuestServiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_calculator);

        db = FirebaseFirestore.getInstance();

        rvServices = findViewById(R.id.rvServices);
        cardResult = findViewById(R.id.cardResult);
        tvTotal = findViewById(R.id.tvTotal);
        tvAnalytics = findViewById(R.id.tvAnalytics);
        tvLimitMessage = findViewById(R.id.tvLimitMessage);
        btnAddService = findViewById(R.id.btnAddService);
        btnRegister = findViewById(R.id.btnRegister);

        rvServices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GuestServiceAdapter(servicesList, position -> deleteService(position));
        rvServices.setAdapter(adapter);

        btnAddService.setOnClickListener(v -> showAddServiceDialog());
        btnRegister.setOnClickListener(v -> saveGuestDataAndRegister());

        loadAvailableServices();
        updateTotal();
    }

    private void loadAvailableServices() {
        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    serviceNames.clear();
                    availableServices.clear();

                    serviceNames.add("Выберите сервис");
                    availableServices.add(null);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String icon = doc.getString("icon");
                        Double defaultAmount = doc.getDouble("defaultAmount");

                        Service service = new Service(id, name, icon, defaultAmount);
                        availableServices.add(service);
                        serviceNames.add(icon + " " + name);
                    }
                });
    }

    private void showAddServiceDialog() {
        if (servicesList.size() >= 3) {
            Toast.makeText(this, "Максимум 3 сервиса в гостевом режиме", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_guest_service, null);
        builder.setView(view);

        Spinner spinnerService = view.findViewById(R.id.spinnerService);
        EditText etCustomName = view.findViewById(R.id.etCustomName);
        EditText etAmount = view.findViewById(R.id.etAmount);
        DatePicker datePicker = view.findViewById(R.id.datePicker);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnAdd = view.findViewById(R.id.btnAdd);

        Calendar today = Calendar.getInstance();
        datePicker.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        if (!serviceNames.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, serviceNames);
            spinnerService.setAdapter(adapter);
        }

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) {
                    etCustomName.setVisibility(View.VISIBLE);
                    etAmount.setText("");
                } else if (position > 0) {
                    etCustomName.setVisibility(View.GONE);
                    Service selected = availableServices.get(position);
                    if (selected != null && selected.getDefaultAmount() != null && etAmount.getText().toString().isEmpty()) {
                        etAmount.setText(String.valueOf(selected.getDefaultAmount().intValue()));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String name;
            double amount;
            String icon = "📱";

            int day = datePicker.getDayOfMonth();
            int month = datePicker.getMonth() + 1;
            int year = datePicker.getYear();
            String startDate = day + "." + month + "." + year;

            int selectedPos = spinnerService.getSelectedItemPosition();
            if (selectedPos == 0) {
                name = etCustomName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название сервиса", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Service selected = availableServices.get(selectedPos);
                if (selected == null) return;
                name = selected.getName();
                icon = selected.getIcon();
                if (icon == null || icon.isEmpty()) icon = "📱";
            }

            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            amount = Double.parseDouble(amountStr);

            servicesList.add(new GuestService(name, amount, startDate, icon));
            adapter.notifyItemInserted(servicesList.size() - 1);
            updateTotal();
            dialog.dismiss();

            if (servicesList.size() >= 3) {
                btnAddService.setVisibility(View.GONE);
                tvLimitMessage.setVisibility(View.VISIBLE);
                btnRegister.setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }

    private void deleteService(int position) {
        servicesList.remove(position);
        adapter.notifyItemRemoved(position);
        updateTotal();

        if (servicesList.size() < 3) {
            btnAddService.setVisibility(View.VISIBLE);
            tvLimitMessage.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
        }

        if (servicesList.isEmpty()) {
            cardResult.setVisibility(View.GONE);
        }
    }

    private void updateTotal() {
        if (servicesList.isEmpty()) {
            cardResult.setVisibility(View.GONE);
            return;
        }

        double total = 0;
        StringBuilder analytics = new StringBuilder();

        for (GuestService service : servicesList) {
            total += service.amount;
            analytics.append("• ").append(service.name)
                    .append(": ").append(String.format("%.0f", service.amount))
                    .append(" ₽ (с ").append(service.startDate).append(")\n");
        }

        tvTotal.setText(String.format("💰 Итого: %.2f ₽", total));
        tvAnalytics.setText(analytics.toString());
        cardResult.setVisibility(View.VISIBLE);
    }

    private void saveGuestDataAndRegister() {
        if (servicesList.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы один сервис", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder guestData = new StringBuilder();
        for (GuestService service : servicesList) {
            guestData.append(service.name).append("|")
                    .append(service.amount).append("|")
                    .append(service.startDate).append(";");
        }

        Intent intent = new Intent(GuestCalculatorActivity.this, RegisterActivity.class);
        intent.putExtra("guest_data", guestData.toString());
        startActivity(intent);
    }

    // Вспомогательный класс для хранения данных сервиса
    public static class GuestService {
        String name;
        double amount;
        String startDate;
        String icon;

        public GuestService(String name, double amount, String startDate, String icon) {
            this.name = name;
            this.amount = amount;
            this.startDate = startDate;
            this.icon = icon;
        }
    }
}