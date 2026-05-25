package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Проверяем, что телефон именно загрузился
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            // Получаем доступ к настройкам
            SharedPreferences prefs = context.getSharedPreferences("Alarms", Context.MODE_PRIVATE);

            // Читаем список твоих будильников
            String json = prefs.getString("list", null);

            if (json != null) {
                // Здесь нужно вызвать твой метод для переустановки будильников
                // Чтобы после перезагрузки они снова попали в систему
                rebuildAlarms(context, json);
            }
        }
    }

    private void rebuildAlarms(Context context, String json) {
        // Тут будет логика восстановления (мы её напишем, когда наладим запуск)
        // Пока оставим пустым, чтобы не было ошибок
    }
}