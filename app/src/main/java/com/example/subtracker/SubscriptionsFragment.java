package com.example.subtracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;

public class SubscriptionsFragment extends Fragment {

    private LinearLayout llSubscriptions;
    private FloatingActionButton fabAdd;
    private FirebaseFirestore db;
    private String currentUser;
    private List<Service> servicesList = new ArrayList<>();
    private List<String> serviceNames = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subscriptions, container, false);

        db = FirebaseFirestore.getInstance();

        if (getActivity() != null) {
            currentUser = getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("current_user", null);
        }

        llSubscriptions = view.findViewById(R.id.llSubscriptions);
        fabAdd = view.findViewById(R.id.fabAdd);

        fabAdd.setOnClickListener(v -> loadServicesAndShowDialog());

        loadSubscriptions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSubscriptions();
    }

    private void loadServicesAndShowDialog() {
        if (getContext() == null) return;

        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    servicesList.clear();
                    serviceNames.clear();
                    serviceNames.add("--- Ввести свой вариант ---");
                    servicesList.add(null);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String icon = doc.getString("icon");
                        Double defaultAmount = doc.getDouble("defaultAmount");

                        Service service = new Service(id, name, icon, defaultAmount);
                        servicesList.add(service);
                        serviceNames.add(icon + " " + name);
                    }

                    showAddSubscriptionDialog();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки сервисов", Toast.LENGTH_SHORT).show();
                    showAddSubscriptionDialog();
                });
    }

    private void showAddSubscriptionDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_subscription, null);
        builder.setView(view);

        Spinner spinnerService = view.findViewById(R.id.dialogSpinnerService);
        EditText etCustomName = view.findViewById(R.id.dialogEtCustomName);
        EditText etAmount = view.findViewById(R.id.dialogEtAmount);
        DatePicker datePicker = view.findViewById(R.id.dialogDatePicker);
        Spinner spinnerPeriodicity = view.findViewById(R.id.dialogSpinnerPeriodicity);
        Button btnCancel = view.findViewById(R.id.dialogBtnCancel);
        Button btnSave = view.findViewById(R.id.dialogBtnSave);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        dialog.show();
        // Устанавливаем сегодняшнюю дату в DatePicker по умолчанию
        Calendar today = Calendar.getInstance();
        datePicker.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        // Настройка спиннера сервисов
        if (!serviceNames.isEmpty()) {
            ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, serviceNames);
            spinnerService.setAdapter(serviceAdapter);
        }

        // Настройка спиннера периодичности
        String[] periods = {"Ежемесячно", "Раз в 3 месяца", "Ежегодно"};
        String[] periodValues = {"monthly", "quarterly", "yearly"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, periods);
        spinnerPeriodicity.setAdapter(periodAdapter);

        

        // При выборе сервиса подставляем сумму по умолчанию
        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position > 0 && servicesList.get(position) != null && etAmount.getText().toString().isEmpty()) {
                    Service selected = servicesList.get(position);
                    if (selected.getDefaultAmount() != null) {
                        etAmount.setText(String.valueOf(selected.getDefaultAmount().intValue()));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name;
            double amount;

            // Получаем дату из DatePicker
            int day = datePicker.getDayOfMonth();
            int month = datePicker.getMonth() + 1; // Month в DatePicker от 0 до 11
            int year = datePicker.getYear();
            String startDate = day + "." + month + "." + year;

            int periodPosition = spinnerPeriodicity.getSelectedItemPosition();
            String periodicity = periodValues[periodPosition];

            int selectedServicePos = spinnerService.getSelectedItemPosition();
            if (selectedServicePos == 0) {
                name = etCustomName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Введите название сервиса", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Service selected = servicesList.get(selectedServicePos);
                name = selected.getName();
            }

            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            amount = Double.parseDouble(amountStr);

            addSubscription(name, amount, startDate, periodicity);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addSubscription(String name, double amount, String startDate, String periodicity) {
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("name", name);
        subscription.put("amount", amount);
        subscription.put("startDate", startDate);
        subscription.put("periodicity", periodicity);

        // Рассчитываем следующую дату платежа
        String nextPaymentDate = calculateNextPaymentDate(startDate, periodicity);
        subscription.put("nextPaymentDate", nextPaymentDate);

        db.collection("users")
                .document(currentUser)
                .collection("subscriptions")
                .add(subscription)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Подписка добавлена", Toast.LENGTH_SHORT).show();
                    loadSubscriptions();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String calculateNextPaymentDate(String startDate, String periodicity) {
        try {
            String[] parts = startDate.split("\\.");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);

            if ("monthly".equals(periodicity)) {
                cal.add(Calendar.MONTH, 1);
            } else if ("quarterly".equals(periodicity)) {
                cal.add(Calendar.MONTH, 3);
            } else if ("yearly".equals(periodicity)) {
                cal.add(Calendar.YEAR, 1);
            }

            return cal.get(Calendar.DAY_OF_MONTH) + "." + (cal.get(Calendar.MONTH) + 1) + "." + cal.get(Calendar.YEAR);
        } catch (Exception e) {
            return startDate;
        }
    }

    private void loadSubscriptions() {
        if (getContext() == null) return;
        llSubscriptions.removeAllViews();

        db.collection("users")
                .document(currentUser)
                .collection("subscriptions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("Нет подписок. Нажмите + чтобы добавить.");
                        empty.setPadding(16, 16, 16, 16);
                        llSubscriptions.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        Double amount = doc.getDouble("amount");
                        String startDate = doc.getString("startDate");
                        String periodicity = doc.getString("periodicity");
                        String nextPaymentDate = doc.getString("nextPaymentDate");

                        String periodText = "";
                        if ("monthly".equals(periodicity)) periodText = "ежемесячно";
                        else if ("quarterly".equals(periodicity)) periodText = "раз в 3 мес";
                        else if ("yearly".equals(periodicity)) periodText = "ежегодно";

                        TextView subView = new TextView(getContext());
                        subView.setText(name + " - " + amount + "₽ (" + periodText + ")\nСлед. платёж: " + nextPaymentDate);
                        subView.setPadding(16, 16, 16, 16);
                        subView.setTextSize(14);
                        subView.setBackgroundResource(android.R.drawable.list_selector_background);

                        subView.setOnLongClickListener(v -> {
                            deleteSubscription(id, name);
                            return true;
                        });

                        llSubscriptions.addView(subView);
                    }
                });
    }

    private void deleteSubscription(String id, String name) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить подписку")
                .setMessage("Удалить " + name + "?")
                .setPositiveButton("Да", (dialog, which) -> {
                    db.collection("users")
                            .document(currentUser)
                            .collection("subscriptions")
                            .document(id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Подписка удалена", Toast.LENGTH_SHORT).show();
                                loadSubscriptions();
                            });
                })
                .setNegativeButton("Нет", null)
                .show();
    }
}