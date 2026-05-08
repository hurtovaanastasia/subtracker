package com.example.subtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    Map<String, MonthData> monthlyMap = new HashMap<>();
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH);
                    int currentYear = cal.get(Calendar.YEAR);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Double amount = doc.getDouble("amount");
                        String startDateStr = doc.getString("startDate");
                        String periodicity = doc.getString("periodicity");

                        if (amount == null || startDateStr == null) continue;

                        String[] parts = startDateStr.split("\\.");
                        if (parts.length != 3) continue;

                        int startDay = Integer.parseInt(parts[0]);
                        int startMonth = Integer.parseInt(parts[1]) - 1;
                        int startYear = Integer.parseInt(parts[2]);

                        int paymentsPerYear;
                        if ("monthly".equals(periodicity)) paymentsPerYear = 12;
                        else if ("quarterly".equals(periodicity)) paymentsPerYear = 4;
                        else paymentsPerYear = 1;

                        for (int i = 0; i < paymentsPerYear && i < 12; i++) {
                            int paymentMonth = startMonth + i * (12 / paymentsPerYear);
                            int paymentYear = startYear;
                            while (paymentMonth >= 12) {
                                paymentMonth -= 12;
                                paymentYear++;
                            }

                            if (paymentYear > currentYear || (paymentYear == currentYear && paymentMonth >= currentMonth)) {
                                String key = String.format("%d-%02d", paymentYear, paymentMonth + 1);

                                MonthData monthData = monthlyMap.get(key);
                                if (monthData == null) {
                                    monthData = new MonthData();
                                    monthlyMap.put(key, monthData);
                                }
                                monthData.total += amount;
                                monthData.services.add(new ServicePayment(name, amount, startDay));
                            }
                        }
                    }

                    List<String> sortedKeys = new ArrayList<>(monthlyMap.keySet());
                    Collections.sort(sortedKeys, (a, b) -> a.compareTo(b));

                    int count = 0;
                    for (String key : sortedKeys) {
                        if (count >= 6) break;

                        String[] yearMonth = key.split("-");
                        int year = Integer.parseInt(yearMonth[0]);
                        int month = Integer.parseInt(yearMonth[1]) - 1;

                        MonthData data = monthlyMap.get(key);
                        addMonthCard(month, year, data);
                        count++;
                    }
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void addMonthCard(int month, int year, MonthData data) {
        if (getContext() == null) return;

        String monthName = getMonthName(month);

        CardView cardView = new CardView(getContext());
        cardView.setRadius(12f);
        cardView.setCardElevation(4f);
        cardView.setUseCompatPadding(true);

        // Устанавливаем цвет фона карточки в зависимости от темы
        if (isDarkTheme()) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.card_dark));
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.card_light));
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout innerLayout = new LinearLayout(getContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(16, 16, 16, 16);

        // Заголовок месяца
        TextView monthHeader = new TextView(getContext());
        monthHeader.setText(monthName + " " + year);
        monthHeader.setTextSize(18);
        monthHeader.setTextColor(ContextCompat.getColor(getContext(), R.color.purple_500));
        monthHeader.setTypeface(monthHeader.getTypeface(), android.graphics.Typeface.BOLD);
        monthHeader.setPadding(0, 0, 0, 8);
        innerLayout.addView(monthHeader);

        // Итоговая сумма
        TextView totalText = new TextView(getContext());
        totalText.setText("💰 Итого: " + String.format("%.2f", data.total) + " ₽");
        totalText.setTextSize(16);
        totalText.setTypeface(totalText.getTypeface(), android.graphics.Typeface.BOLD);
        totalText.setPadding(0, 0, 0, 12);

        if (isDarkTheme()) {
            totalText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary_dark));
        } else {
            totalText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary_light));
        }
        innerLayout.addView(totalText);

        // Разделитель
        View divider = new View(getContext());
        if (isDarkTheme()) {
            divider.setBackgroundColor(Color.parseColor("#444444"));
        } else {
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, 0, 0, 8);
        divider.setLayoutParams(dividerParams);
        innerLayout.addView(divider);

        // Сортируем сервисы по сумме
        List<ServicePayment> sortedServices = new ArrayList<>(data.services);
        sortedServices.sort((a, b) -> Double.compare(b.amount, a.amount));

        // Список сервисов
        for (ServicePayment sp : sortedServices) {
            LinearLayout serviceRow = new LinearLayout(getContext());
            serviceRow.setOrientation(LinearLayout.HORIZONTAL);
            serviceRow.setPadding(0, 8, 0, 8);

            TextView serviceName = new TextView(getContext());
            serviceName.setText("• " + sp.name);
            serviceName.setTextSize(14);
            if (isDarkTheme()) {
                serviceName.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary_dark));
            } else {
                serviceName.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary_light));
            }
            serviceName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView serviceAmount = new TextView(getContext());
            serviceAmount.setText(String.format("%.0f", sp.amount) + " ₽");
            serviceAmount.setTextSize(14);
            serviceAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.purple_500));
            serviceAmount.setTypeface(serviceAmount.getTypeface(), android.graphics.Typeface.BOLD);

            serviceRow.addView(serviceName);
            serviceRow.addView(serviceAmount);
            innerLayout.addView(serviceRow);
        }

        cardView.addView(innerLayout);
        llPlan.addView(cardView);
    }

    private boolean isDarkTheme() {
        if (getContext() == null) return false;
        int currentNightMode = getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void showEmptyState() {
        if (getContext() == null) return;

        TextView empty = new TextView(getContext());
        empty.setText("✨ Нет предстоящих платежей\nДобавьте подписки в разделе \"Подписки\"");
        empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        empty.setPadding(32, 64, 32, 64);
        empty.setTextColor(Color.parseColor("#666666"));
        empty.setTextSize(14);
        llPlan.addView(empty);
    }

    private String getMonthName(int month) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return months[month];
    }

    private static class MonthData {
        double total = 0;
        List<ServicePayment> services = new ArrayList<>();
    }

    private static class ServicePayment {
        String name;
        double amount;
        int day;

        ServicePayment(String name, double amount, int day) {
            this.name = name;
            this.amount = amount;
            this.day = day;
        }
    }
}