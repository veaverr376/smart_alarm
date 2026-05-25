package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskActivity extends AppCompatActivity {
    private int mathResult;
    private int[][] sampleData = new int[4][4];
    private int[][] userData = new int[4][4];

    // Для Саймона
    private List<Integer> simonSequence = new ArrayList<>();
    private List<Integer> userSimonSequence = new ArrayList<>();
    private Button[] simonButtons = new Button[4];
    private int[] normalColors = {0x88FF0000, 0x8800FF00, 0x88FFFF00, 0x880000FF};
    private int[] brightColors = {0xFFFF0000, 0xFF00FF00, 0xFFFFFF00, 0xFF0000FF};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Разблокировка экрана
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_task);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        int mode = prefs.getInt("game_mode", 0); // 0 - рандом

        if (mode == 0) mode = new Random().nextInt(3) + 1; // Выбирает 1, 2 или 3

        if (mode == 1) setupMath();
        else if (mode == 2) setupBlockBlast();
        else setupSimonSays();
    }

    // --- ИГРА 1: МАТЕМАТИКА (1-100) ---
    private void setupMath() {
        findViewById(R.id.layoutMath).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutTiles).setVisibility(View.GONE);
        findViewById(R.id.layoutSimon).setVisibility(View.GONE);

        Random r = new Random();
        int a = r.nextInt(100) + 1;
        int b = r.nextInt(100) + 1;
        mathResult = a + b;

        ((TextView)findViewById(R.id.mathText)).setText(a + " + " + b + " = ?");

        findViewById(R.id.checkButton).setOnClickListener(v -> {
            EditText input = findViewById(R.id.mathInput);
            String text = input.getText().toString();
            if (!text.isEmpty() && Integer.parseInt(text) == mathResult) exitTask();
            else Toast.makeText(this, "Считай лучше!", Toast.LENGTH_SHORT).show();
        });
    }

    // --- ИГРА 2: ПОВТОРИ УЗОР (3 ТОЧКИ) ---
    private void setupBlockBlast() {
        findViewById(R.id.layoutMath).setVisibility(View.GONE);
        findViewById(R.id.layoutSimon).setVisibility(View.GONE);
        findViewById(R.id.layoutTiles).setVisibility(View.VISIBLE);
        findViewById(R.id.checkButton).setVisibility(View.GONE);

        Random r = new Random();
        for(int i=0; i<4; i++) for(int j=0; j<4; j++) {
            sampleData[i][j] = 0;
            userData[i][j] = 0;
        }

        int count = 0;
        while(count < 3) {
            int rr = r.nextInt(4), cc = r.nextInt(4);
            if(sampleData[rr][cc] == 0) { sampleData[rr][cc] = 1; count++; }
        }

        GridLayout gridSample = findViewById(R.id.gridSample);
        gridSample.removeAllViews();
        for (int i = 0; i < 16; i++) {
            View v = new View(this);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 45; p.height = 45; p.setMargins(4,4,4,4);
            v.setLayoutParams(p);
            v.setBackgroundColor(sampleData[i/4][i%4] == 1 ? Color.YELLOW : Color.DKGRAY);
            gridSample.addView(v);
        }

        GridLayout gridInput = findViewById(R.id.gridInput);
        gridInput.removeAllViews();
        for (int i = 0; i < 16; i++) {
            final int row = i / 4, col = i % 4;
            Button b = new Button(this);
            b.setLayoutParams(new GridLayout.LayoutParams(
                    GridLayout.spec(row), GridLayout.spec(col)));
            b.getLayoutParams().width = 160; b.getLayoutParams().height = 160;
            b.setBackgroundColor(Color.LTGRAY);
            b.setOnClickListener(v -> {
                userData[row][col] = (userData[row][col] == 0) ? 1 : 0;
                b.setBackgroundColor(userData[row][col] == 1 ? Color.parseColor("#00BCD4") : Color.LTGRAY);
                checkPuzzle();
            });
            gridInput.addView(b);
        }
    }

    private void checkPuzzle() {
        for(int i=0; i<4; i++) for(int j=0; j<4; j++)
            if (sampleData[i][j] != userData[i][j]) return;
        exitTask();
    }

    // --- ИГРА 3: САЙМОН ГОВОРИТ (6 ЦВЕТОВ) ---
    private void setupSimonSays() {
        findViewById(R.id.layoutMath).setVisibility(View.GONE);
        findViewById(R.id.layoutTiles).setVisibility(View.GONE);
        findViewById(R.id.layoutSimon).setVisibility(View.VISIBLE);
        findViewById(R.id.checkButton).setVisibility(View.GONE);

        simonButtons[0] = findViewById(R.id.btnRed);
        simonButtons[1] = findViewById(R.id.btnGreen);
        simonButtons[2] = findViewById(R.id.btnYellow);
        simonButtons[3] = findViewById(R.id.btnBlue);

        Random r = new Random();
        simonSequence.clear();
        for (int i = 0; i < 6; i++) simonSequence.add(r.nextInt(4));

        new Handler().postDelayed(this::showSimonSequence, 1000);
    }

    private void showSimonSequence() {
        Handler h = new Handler();
        for (int i = 0; i < simonSequence.size(); i++) {
            final int index = i;
            h.postDelayed(() -> {
                int colorIdx = simonSequence.get(index);
                flashSimonButton(colorIdx);
                if (index == simonSequence.size() - 1) enableSimonInput();
            }, i * 800);
        }
    }

    private void flashSimonButton(int idx) {
        simonButtons[idx].setBackgroundTintList(ColorStateList.valueOf(brightColors[idx]));
        new Handler().postDelayed(() ->
                simonButtons[idx].setBackgroundTintList(ColorStateList.valueOf(normalColors[idx])), 400);
    }

    private void enableSimonInput() {
        ((TextView)findViewById(R.id.simonStatus)).setText("ПОВТОРЯЙ!");
        userSimonSequence.clear();
        for (int i = 0; i < 4; i++) {
            final int id = i;
            simonButtons[i].setOnClickListener(v -> {
                flashSimonButton(id);
                userSimonSequence.add(id);
                int step = userSimonSequence.size() - 1;
                if (userSimonSequence.get(step) != simonSequence.get(step)) {
                    Toast.makeText(this, "Ошибка!", Toast.LENGTH_SHORT).show();
                    enableSimonInput();
                    showSimonSequence();
                } else if (userSimonSequence.size() == simonSequence.size()) exitTask();
            });
        }
    }

    // --- ТВОЙ ДОРАБОТАННЫЙ МЕТОД ВЫХОДА И ЗАПУСКА ЦИКЛА ПРОВЕРКИ ---
    private void exitTask() {
        // 1. Глушим музыку будильника, если она поет
        if (AlarmReceiver.mediaPlayer != null) {
            try {
                AlarmReceiver.mediaPlayer.stop();
                AlarmReceiver.mediaPlayer.release();
                AlarmReceiver.mediaPlayer = null;
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. Снимаем полноэкранное уведомление "ПОДЪЕМ!"
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(1);

        // 3. Планируем запуск тихого проверочного уведомления ровно через 5 минут
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class); // Отправляем в наш AlarmReceiver
        intent.putExtra("action", "START_CHECK"); // Даем маркер проверки

        // Создаем точный PendingIntent для вызова ресивера
        PendingIntent pi = PendingIntent.getBroadcast(this, 888, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (am != null) {
            // Текущее время + 5 минут (5 минут * 60 секунд * 1000 миллисекунд)
            long triggerTime = System.currentTimeMillis() + (5 * 60 * 1000);

            // Надежная установка точного времени в зависимости от версии Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        }

        // 4. Убиваем текущую активность, возвращая телефон в исходное состояние
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Реши задачу!", Toast.LENGTH_SHORT).show();
    }
}