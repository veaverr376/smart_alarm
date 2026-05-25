package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {
    // Статическая переменная, чтобы TaskActivity могла её остановить
    public static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Никаких проверок экшенов — просто запускаем будильник на полную программу
        triggerAlarmMassive(context);
    }

    // Вспомогательный метод для полного запуска будильника (музыка + экран задачи)
    public void triggerAlarmMassive(Context context) {
        startYourMusic(context);

        Intent i = new Intent(context, TaskActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "alarm_channel_silent";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Будильник", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null);
            channel.setBypassDnd(true);
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("ПОДЪЕМ!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setOngoing(true);

        if (nm != null) nm.notify(1, builder.build());

        try { context.startActivity(i); } catch (Exception e) {}
    }

    private void startYourMusic(Context context) {
        if (mediaPlayer != null) return; // Если уже поет, не дублируем

        android.content.SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String uriStr = prefs.getString("music_folder", null);

        try {
            mediaPlayer = new MediaPlayer();
            if (uriStr != null) {
                // Если выбрана папка, берем рандомную песню оттуда
                DocumentFile dir = DocumentFile.fromTreeUri(context, Uri.parse(uriStr));
                if (dir != null && dir.listFiles().length > 0) {
                    mediaPlayer.setDataSource(context, dir.listFiles()[new Random().nextInt(dir.listFiles().length)].getUri());
                }
            } else {
                // Если папка не выбрана, берем звук по умолчанию
                Uri defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                mediaPlayer.setDataSource(context, defaultUri);
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) { e.printStackTrace(); }
    }
}