package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Calendar;

public class AlarmEditActivity extends AppCompatActivity {
    private TextView tvTime;
    private CheckBox[] dayBoxes = new CheckBox[7];
    private int mHour, mMinute;
    private int position; // Позиция будильника в списке

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_editor);

        tvTime = findViewById(R.id.tvEditTime);

        // 1. Получаем данные из интента (то, что прислала MainActivity)
        String currentTime = getIntent().getStringExtra("current_time");
        position = getIntent().getIntExtra("alarm_pos", -1);

        // Инициализируем чекбоксы
        dayBoxes[0] = findViewById(R.id.cbMon);
        dayBoxes[1] = findViewById(R.id.cbTue);
        dayBoxes[2] = findViewById(R.id.cbWed);
        dayBoxes[3] = findViewById(R.id.cbThu);
        dayBoxes[4] = findViewById(R.id.cbFri);
        dayBoxes[5] = findViewById(R.id.cbSat);
        dayBoxes[6] = findViewById(R.id.cbSun);

        // 2. Устанавливаем время из списка, а не 07:00
        if (currentTime != null) {
            tvTime.setText(currentTime);
            mHour = Integer.parseInt(currentTime.split(":")[0]);
            mMinute = Integer.parseInt(currentTime.split(":")[1]);
        } else {
            mHour = 7; mMinute = 0;
        }

        // 3. Загружаем сохраненные дни именно для этого будильника
        SharedPreferences alarmPrefs = getSharedPreferences("Alarms", MODE_PRIVATE);
        String savedDays = alarmPrefs.getString("days_" + position, "");
        for (int i = 0; i < 7; i++) {
            if (savedDays.contains(String.valueOf(i + 1))) {
                dayBoxes[i].setChecked(true);
            }
        }

        tvTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, h, m) -> {
                mHour = h; mMinute = m;
                tvTime.setText(String.format("%02d:%02d", h, m));
            }, mHour, mMinute, true).show();
        });

        findViewById(R.id.btnSaveAlarm).setOnClickListener(v -> saveAlarm());
    }

    private void saveAlarm() {
        StringBuilder days = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (dayBoxes[i].isChecked()) days.append(i + 1).append(",");
        }

        // 4. СОХРАНЯЕМ В ТОТ ЖЕ СПИСОК, ЧТО И В MAINACTIVITY
        SharedPreferences alarmPrefs = getSharedPreferences("Alarms", MODE_PRIVATE);
        String json = alarmPrefs.getString("list", "[]");
        String newTime = tvTime.getText().toString();

        try {
            JSONArray jsonArray = new JSONArray(json);
            if (position != -1 && position < jsonArray.length()) {
                jsonArray.put(position, newTime); // Обновляем существующий
            }

            // Сохраняем и список, и дни для этого конкретного будильника
            alarmPrefs.edit()
                    .putString("list", jsonArray.toString())
                    .putString("days_" + position, days.toString())
                    .apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        scheduleNextAlarm();
        finish(); // Возвращаемся в MainActivity, там сработает onResume и обновит список
    }

    private void scheduleNextAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        // requestId должен зависеть от времени, чтобы будильники не перезаписывали друг друга
        int requestId = (mHour * 60) + mMinute;

        PendingIntent pi = PendingIntent.getBroadcast(this, requestId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, mHour);
        cal.set(Calendar.MINUTE, mMinute);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }
}