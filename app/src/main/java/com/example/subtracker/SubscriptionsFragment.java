package com.example.subtracker;

import android.app.AlertDialog;
import android.graphics.Color;
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

    // Храним выбранную иконку для своего сервиса
    private String selectedCustomIcon = "📱";

    // Список доступных смайликов
    private String[] availableIcons = {
            "📱", "🎬", "🎵", "📺", "🌟", "🍿", "🍎", "🎥", "✈️", "✨", "📦",
            "💳", "🏦", "💙", "⚽", "🎭", "🚀", "📽️", "🎮", "🕹️", "👾",
            "🎙️", "🤖", "🎨", "📐", "✏️", "📓", "☁️", "🔍", "💻", "📚", "🏋️", "🍔"
    };

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
                    serviceNames.add("Выберите сервис");
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
        Button btnChooseIcon = view.findViewById(R.id.btnChooseIcon);
        TextView tvSelectedIcon = view.findViewById(R.id.tvSelectedIcon);

        // Сбрасываем выбранную иконку
        selectedCustomIcon = "📱";
        tvSelectedIcon.setText(selectedCustomIcon);

        // Устанавливаем сегодняшнюю дату
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

        AlertDialog dialog = builder.create();

        // При выборе сервиса подставляем сумму и показываем/скрываем выбор иконки
        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) {
                    // Свой вариант - показываем выбор иконки
                    btnChooseIcon.setVisibility(View.VISIBLE);
                    tvSelectedIcon.setVisibility(View.VISIBLE);
                    if (etAmount.getText().toString().isEmpty()) {
                        etAmount.setText("");
                    }
                } else {
                    // Выбран сервис из базы - скрываем выбор иконки
                    btnChooseIcon.setVisibility(View.GONE);
                    tvSelectedIcon.setVisibility(View.GONE);
                    Service selected = servicesList.get(position);
                    if (selected.getDefaultAmount() != null && etAmount.getText().toString().isEmpty()) {
                        etAmount.setText(String.valueOf(selected.getDefaultAmount().intValue()));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Кнопка выбора иконки
        btnChooseIcon.setOnClickListener(v -> showIconPickerDialog(tvSelectedIcon));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name;
            double amount;
            String icon;

            int day = datePicker.getDayOfMonth();
            int month = datePicker.getMonth() + 1;
            int year = datePicker.getYear();
            String startDate = day + "." + month + "." + year;

            int periodPosition = spinnerPeriodicity.getSelectedItemPosition();
            String periodicity = periodValues[periodPosition];

            int selectedServicePos = spinnerService.getSelectedItemPosition();
            if (selectedServicePos == 0) {
                // Свой вариант
                name = etCustomName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Введите название сервиса", Toast.LENGTH_SHORT).show();
                    return;
                }
                icon = selectedCustomIcon;
            } else {
                Service selected = servicesList.get(selectedServicePos);
                name = selected.getName();
                icon = selected.getIcon();
            }

            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            amount = Double.parseDouble(amountStr);

            addSubscription(name, amount, startDate, periodicity, icon);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showIconPickerDialog(TextView tvSelectedIcon) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_choose_icon, null);
        builder.setView(view);

        GridView gridView = view.findViewById(R.id.gridIcons);
        Button btnConfirm = view.findViewById(R.id.btnConfirmIcon);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, availableIcons);
        gridView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        gridView.setOnItemClickListener((parent, v, position, id) -> {
            selectedCustomIcon = availableIcons[position];
            tvSelectedIcon.setText(selectedCustomIcon);
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addSubscription(String name, double amount, String startDate, String periodicity, String icon) {
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("name", name);
        subscription.put("amount", amount);
        subscription.put("startDate", startDate);
        subscription.put("periodicity", periodicity);
        subscription.put("nextPaymentDate", startDate);
        subscription.put("icon", icon);

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
                        empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        empty.setPadding(16, 50, 16, 16);
                        empty.setTextColor(Color.GRAY);
                        llSubscriptions.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        Double amount = doc.getDouble("amount");
                        String periodicity = doc.getString("periodicity");
                        String nextPaymentDate = doc.getString("nextPaymentDate");
                        String icon = doc.getString("icon");

                        if (icon == null || icon.isEmpty()) {
                            icon = getIconForService(name);
                        }

                        String periodText = "";
                        if ("monthly".equals(periodicity)) periodText = "ежемесячно";
                        else if ("quarterly".equals(periodicity)) periodText = "раз в 3 мес";
                        else if ("yearly".equals(periodicity)) periodText = "ежегодно";

                        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_subscription, null);

                        TextView itemIcon = itemView.findViewById(R.id.itemIcon);
                        TextView itemName = itemView.findViewById(R.id.itemName);
                        TextView itemPeriod = itemView.findViewById(R.id.itemPeriod);
                        TextView itemAmount = itemView.findViewById(R.id.itemAmount);

                        itemIcon.setText(icon);
                        itemName.setText(name);
                        itemPeriod.setText(periodText + " • след. платёж: " + nextPaymentDate);
                        itemAmount.setText(String.format("%.0f", amount) + " ₽");

                        itemView.setOnLongClickListener(v -> {
                            deleteSubscription(id, name);
                            return true;
                        });

                        llSubscriptions.addView(itemView);
                    }
                });
    }

    private String getIconForService(String serviceName) {
        String lowerName = serviceName.toLowerCase();
        if (lowerName.contains("netflix")) return "🎬";
        if (lowerName.contains("spotify")) return "🎵";
        if (lowerName.contains("youtube")) return "📺";
        if (lowerName.contains("yandex")) return "🌟";
        if (lowerName.contains("ivi")) return "🍿";
        if (lowerName.contains("apple")) return "🍎";
        return "📱";
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