package com.example.subtracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class PlanFragment extends Fragment {

    private LinearLayout llPlan;
    private FirebaseFirestore db;
    private String currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);

        db = FirebaseFirestore.getInstance();

        if (getActivity() != null) {
            currentUser = getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("current_user", null);
        }

        llPlan = view.findViewById(R.id.llPlan);

        loadPlan();

        return view;
    }

    private void loadPlan() {
        llPlan.removeAllViews();

        db.collection("users")
                .document(currentUser)
                .collection("subscriptions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Карта: месяц и год -> сумма платежей
                    Map<String, Double> monthlyMap = new HashMap<>();
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH);
                    int currentYear = cal.get(Calendar.YEAR);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double amount = doc.getDouble("amount");
                        String startDateStr = doc.getString("startDate");
                        String periodicity = doc.getString("periodicity");

                        if (amount == null || startDateStr == null) continue;

                        // Парсим дату начала
                        String[] parts = startDateStr.split("\\.");
                        if (parts.length != 3) continue;

                        int startDay = Integer.parseInt(parts[0]);
                        int startMonth = Integer.parseInt(parts[1]) - 1;
                        int startYear = Integer.parseInt(parts[2]);

                        // Определяем количество платежей в году
                        int paymentsPerYear;
                        if ("monthly".equals(periodicity)) paymentsPerYear = 12;
                        else if ("quarterly".equals(periodicity)) paymentsPerYear = 4;
                        else paymentsPerYear = 1;

                        // Добавляем платежи
                        for (int i = 0; i < paymentsPerYear; i++) {
                            int paymentMonth = startMonth + i * (12 / paymentsPerYear);
                            int paymentYear = startYear;
                            while (paymentMonth >= 12) {
                                paymentMonth -= 12;
                                paymentYear++;
                            }

                            // Только будущие платежи или текущий месяц
                            if (paymentYear > currentYear || (paymentYear == currentYear && paymentMonth >= currentMonth - 1)) {
                                String key = paymentYear + "-" + (paymentMonth + 1);
                                monthlyMap.put(key, monthlyMap.getOrDefault(key, 0.0) + amount);
                            }
                        }
                    }

                    // Сортируем по дате
                    List<String> sortedKeys = new ArrayList<>(monthlyMap.keySet());
                    Collections.sort(sortedKeys);

                    if (sortedKeys.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("Нет предстоящих платежей");
                        empty.setPadding(16, 16, 16, 16);
                        llPlan.addView(empty);
                        return;
                    }

                    // Показываем план на ближайшие 6 месяцев
                    int count = 0;
                    for (String key : sortedKeys) {
                        if (count >= 6) break;
                        String[] yearMonth = key.split("-");
                        String monthName = getMonthName(Integer.parseInt(yearMonth[1]) - 1);
                        String displayDate = monthName + " " + yearMonth[0];
                        double sum = monthlyMap.get(key);

                        TextView planItem = new TextView(getContext());
                        planItem.setText(displayDate + ": " + String.format("%.2f", sum) + " ₽");
                        planItem.setPadding(16, 16, 16, 16);
                        planItem.setTextSize(16);
                        planItem.setBackgroundResource(android.R.drawable.list_selector_background);

                        llPlan.addView(planItem);
                        count++;
                    }
                });
    }

    private String getMonthName(int month) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return months[month];
    }
}