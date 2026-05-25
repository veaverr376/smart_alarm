package com.example.myapplication;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private AlarmAdapter adapter;
    private final List<String> alarmList = new ArrayList<>();
    private static final int PERMISSION_REQUEST_CODE = 101;

    // 1. ЭТОТ МЕТОД ОБЯЗАТЕЛЕН: он обновляет цифры, когда ты закрываешь редактор
    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences settingsPrefs = getSharedPreferences("Settings", MODE_PRIVATE);
        if (settingsPrefs.getBoolean("dark_theme", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        RecyclerView rv = findViewById(R.id.alarmsRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AlarmAdapter(alarmList, new AlarmAdapter.OnAlarmActionListener() {
            @Override
            public void onDelete(int pos) {
                cancelAlarmInSystem(alarmList.get(pos));
                alarmList.remove(pos);
                adapter.notifyDataSetChanged();
                saveAlarms();
            }

            @Override
            public void onToggle(int pos, boolean isEnabled) {
                String time = alarmList.get(pos);
                String[] p = time.split(":");
                int h = Integer.parseInt(p[0]);
                int m = Integer.parseInt(p[1]);

                if (isEnabled) {
                    setSystemAlarm(h, m);
                } else {
                    cancelAlarmInSystem(time);
                }
            }

            @Override
            public void onEdit(int pos) {
                Intent intent = new Intent(MainActivity.this, AlarmEditActivity.class);
                intent.putExtra("current_time", alarmList.get(pos));
                // 2. ПЕРЕДАЕМ ПОЗИЦИЮ: чтобы редактор знал, что менять, а не создавал новый
                intent.putExtra("alarm_pos", pos);
                startActivity(intent);
            }
        });

        rv.setAdapter(adapter);
        loadAlarms();

        findViewById(R.id.setAlarmButton).setOnClickListener(v -> {
            Calendar n = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, m) -> showDaysPicker(h, m),
                    n.get(Calendar.HOUR_OF_DAY), n.get(Calendar.MINUTE), true).show();
        });
    }

    private void cancelAlarmInSystem(String time) {
        String[] p = time.split(":");
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        int requestId = (h * 60) + m;

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, requestId, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (am != null) am.cancel(pi);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Разрешите точные будильники", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDaysPicker(int h, int m) {
        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        boolean[] checked = new boolean[7];
        new AlertDialog.Builder(this)
                .setTitle("Выберите дни")
                .setMultiChoiceItems(days, checked, (d, w, isC) -> checked[w] = isC)
                .setPositiveButton("Готово", (d, w) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                    alarmList.add(time);
                    adapter.notifyDataSetChanged();
                    saveAlarms();
                    setSystemAlarm(h, m);
                    Toast.makeText(this, "Будильник установлен!", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void setSystemAlarm(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);

        if (c.before(Calendar.getInstance())) {
            c.add(Calendar.DATE, 1);
        }

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, AlarmReceiver.class);
        int requestId = (h * 60) + m;

        PendingIntent pi = PendingIntent.getBroadcast(this, requestId, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (am != null) {
            AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(c.getTimeInMillis(), pi);
            am.setAlarmClock(alarmInfo, pi);
        }
    }

    private void saveAlarms() {
        JSONArray jsonArray = new JSONArray(alarmList);
        getSharedPreferences("Alarms", MODE_PRIVATE).edit().putString("list", jsonArray.toString()).apply();
    }

    private void loadAlarms() {
        String json = getSharedPreferences("Alarms", MODE_PRIVATE).getString("list", null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                alarmList.clear();
                for (int i = 0; i < jsonArray.length(); i++) alarmList.add(jsonArray.getString(i));
                if (adapter != null) adapter.notifyDataSetChanged();
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }
}