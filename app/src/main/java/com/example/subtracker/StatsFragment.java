package com.example.subtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class StatsFragment extends Fragment {

    private TextView tvTotalMonth, tvTotalYear, tvMostExpensive, tvMostExpensiveAmount;
    private LinearLayout llPieChart;
    private FirebaseFirestore db;
    private String currentUser;

    // Цвета для секторов диаграммы
    private int[] colors = {
            Color.rgb(255, 99, 132),   // красный
            Color.rgb(54, 162, 235),   // синий
            Color.rgb(255, 206, 86),   // жёлтый
            Color.rgb(75, 192, 192),   // зелёный
            Color.rgb(153, 102, 255),  // фиолетовый
            Color.rgb(255, 159, 64),   // оранжевый
            Color.rgb(199, 199, 199),  // серый
            Color.rgb(83, 102, 255),   // голубой
            Color.rgb(255, 99, 255),   // розовый
            Color.rgb(99, 255, 132)    // салатовый
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        db = FirebaseFirestore.getInstance();

        if (getActivity() != null) {
            currentUser = getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("current_user", null);
        }

        tvTotalMonth = view.findViewById(R.id.tvTotalMonth);
        tvTotalYear = view.findViewById(R.id.tvTotalYear);
        tvMostExpensive = view.findViewById(R.id.tvMostExpensive);
        tvMostExpensiveAmount = view.findViewById(R.id.tvMostExpensiveAmount);
        llPieChart = view.findViewById(R.id.llPieChart);

        loadStats();

        return view;
    }

    private void loadStats() {
        db.collection("users")
                .document(currentUser)
                .collection("subscriptions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalMonth = 0;
                    double totalYear = 0;
                    String mostExpensiveName = "Нет";
                    double mostExpensiveAmount = 0;

                    // Список для диаграммы
                    List<PieItem> pieItems = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Double amount = doc.getDouble("amount");
                        String periodicity = doc.getString("periodicity");

                        if (amount == null) continue;

                        // Приводим к месячному расходу
                        double monthlyAmount = amount;
                        if ("yearly".equals(periodicity)) {
                            monthlyAmount = amount / 12;
                            totalYear += amount;
                        } else if ("quarterly".equals(periodicity)) {
                            monthlyAmount = amount / 3;
                            totalYear += amount * 4;
                        } else {
                            totalYear += amount * 12;
                        }

                        totalMonth += monthlyAmount;

                        // Самый дорогой сервис (по месячному расходу)
                        if (monthlyAmount > mostExpensiveAmount) {
                            mostExpensiveAmount = monthlyAmount;
                            mostExpensiveName = name;
                        }

                        pieItems.add(new PieItem(name, monthlyAmount));
                    }

                    tvTotalMonth.setText(String.format("Ежемесячно: %.2f ₽", totalMonth));
                    tvTotalYear.setText(String.format("Ежегодно: %.2f ₽", totalYear));
                    tvMostExpensive.setText(mostExpensiveName);
                    tvMostExpensiveAmount.setText(String.format("%.2f ₽/мес", mostExpensiveAmount));

                    // Отображаем круговую диаграмму
                    drawPieChart(pieItems, totalMonth);
                })
                .addOnFailureListener(e -> {
                    tvTotalMonth.setText("Ошибка загрузки");
                });
    }

    private void drawPieChart(List<PieItem> items, double total) {
        llPieChart.removeAllViews();

        if (items.isEmpty() || total == 0) {
            TextView empty = new TextView(getContext());
            empty.setText("Нет данных для диаграммы");
            empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            empty.setPadding(16, 16, 16, 16);
            llPieChart.addView(empty);
            return;
        }

        // Сортируем по убыванию суммы
        items.sort((a, b) -> Double.compare(b.amount, a.amount));

        // Создаём горизонтальный LinearLayout для диаграммы
        LinearLayout chartContainer = new LinearLayout(getContext());
        chartContainer.setOrientation(LinearLayout.HORIZONTAL);
        chartContainer.setPadding(0, 16, 0, 16);

        // Левая часть: цветные квадратики с названиями
        LinearLayout legendLayout = new LinearLayout(getContext());
        legendLayout.setOrientation(LinearLayout.VERTICAL);
        legendLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));

        // Правая часть: простая текстовая диаграмма
        LinearLayout barsLayout = new LinearLayout(getContext());
        barsLayout.setOrientation(LinearLayout.VERTICAL);
        barsLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));

        for (int i = 0; i < items.size(); i++) {
            PieItem item = items.get(i);
            double percent = (item.amount / total) * 100;
            int color = colors[i % colors.length];

            // Легенда: цветной квадрат + название
            LinearLayout legendItem = new LinearLayout(getContext());
            legendItem.setOrientation(LinearLayout.HORIZONTAL);
            legendItem.setPadding(0, 4, 0, 4);

            View colorBox = new View(getContext());
            colorBox.setBackgroundColor(color);
            colorBox.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
            colorBox.setPadding(8, 8, 8, 8);

            TextView legendText = new TextView(getContext());
            legendText.setText(item.name + " (" + String.format("%.1f", percent) + "%)");
            legendText.setPadding(8, 0, 0, 0);
            legendText.setTextSize(12);

            legendItem.addView(colorBox);
            legendItem.addView(legendText);
            legendLayout.addView(legendItem);

            // Полоска-диаграмма
            LinearLayout barItem = new LinearLayout(getContext());
            barItem.setOrientation(LinearLayout.HORIZONTAL);
            barItem.setPadding(0, 4, 0, 4);

            TextView barLabel = new TextView(getContext());
            barLabel.setText(String.format("%.0f", item.amount) + "₽");
            barLabel.setWidth(80);
            barLabel.setTextSize(12);

            View bar = new View(getContext());
            int barWidth = (int) (200 * (percent / 100));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(barWidth, 30);
            bar.setBackgroundColor(color);
            bar.setLayoutParams(barParams);

            barItem.addView(barLabel);
            barItem.addView(bar);
            barsLayout.addView(barItem);
        }

        chartContainer.addView(legendLayout);
        chartContainer.addView(barsLayout);
        llPieChart.addView(chartContainer);
    }

    // Внутренний класс для хранения данных диаграммы
    private static class PieItem {
        String name;
        double amount;

        PieItem(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}