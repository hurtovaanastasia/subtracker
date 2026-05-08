package com.example.subtracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private VPAdapter vpAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupViewPager();

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_subscriptions) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.nav_stats) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (id == R.id.nav_plan) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (id == R.id.nav_profile) {
                viewPager.setCurrentItem(3);
                return true;
            }
            return false;
        });
    }

    private void setupViewPager() {
        List<androidx.fragment.app.Fragment> fragments = new ArrayList<>();
        fragments.add(new SubscriptionsFragment());
        fragments.add(new StatsFragment());
        fragments.add(new PlanFragment());
        fragments.add(new ProfileFragment());

        vpAdapter = new VPAdapter(getSupportFragmentManager(), getLifecycle(), fragments);
        viewPager.setAdapter(vpAdapter);
    }
}