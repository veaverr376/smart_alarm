package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private List<String> songNames = new ArrayList<>();
    private ArrayAdapter<String> songAdapter;
    private SharedPreferences prefs;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Кнопка "Назад"
        if (findViewById(R.id.backButton) != null) {
            findViewById(R.id.backButton).setOnClickListener(v -> finish());
        }

        // 2. ВЫБОР РЕЖИМА ИГРЫ
        setupGameModeSelection();

        // 3. КНОПКА ТЕСТА
        setupTestButton();

        // 4. Музыкальный раздел
        setupMusicSection();
    }

    private void setupGameModeSelection() {
        RadioGroup gameModeGroup = findViewById(R.id.gameModeGroup);
        if (gameModeGroup == null) return;

        int savedMode = prefs.getInt("game_mode", 0);

        // Ставим галочку при загрузке (добавили проверку для Саймона — 3)
        if (savedMode == 0 && findViewById(R.id.radioRandom) != null)
            ((RadioButton)findViewById(R.id.radioRandom)).setChecked(true);
        else if (savedMode == 1 && findViewById(R.id.radioMath) != null)
            ((RadioButton)findViewById(R.id.radioMath)).setChecked(true);
        else if (savedMode == 2 && findViewById(R.id.radioPuzzle) != null)
            ((RadioButton)findViewById(R.id.radioPuzzle)).setChecked(true);
        else if (savedMode == 3 && findViewById(R.id.radioSimon) != null)
            ((RadioButton)findViewById(R.id.radioSimon)).setChecked(true);

        gameModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;
            if (checkedId == R.id.radioMath) mode = 1;
            else if (checkedId == R.id.radioPuzzle) mode = 2;
            else if (checkedId == R.id.radioSimon) mode = 3; // Сохраняем 3 для Саймона

            prefs.edit().putInt("game_mode", mode).apply();

            String msg = "Режим: " + (mode == 3 ? "Саймон" : (mode == 1 ? "Математика" : (mode == 2 ? "Пазл" : "Рандом")));
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTestButton() {
        Button testButton = findViewById(R.id.testAlarmButton);
        if (testButton != null) {
            testButton.setOnClickListener(v -> {
                Toast.makeText(this, "Тест через 5 сек! Выйди из приложения!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(this, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                long triggerTime = System.currentTimeMillis() + 5000;

                if (am != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    }
                }
            });
        }
    }

    private void setupMusicSection() {
        lv = findViewById(R.id.songsListView);
        if (lv == null) return;

        songAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, songNames);
        lv.setAdapter(songAdapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSong = songNames.get(position);
            prefs.edit().putString("selected_song_name", selectedSong).apply();
        });

        Button selectFolderBtn = findViewById(R.id.selectFolderButton);
        if (selectFolderBtn != null) {
            selectFolderBtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 100);
            });
        }

        String savedUri = prefs.getString("music_folder", null);
        if (savedUri != null) loadSongsFromFolder(Uri.parse(savedUri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("music_folder", treeUri.toString()).apply();
                loadSongsFromFolder(treeUri);
            }
        }
    }

    private void loadSongsFromFolder(Uri treeUri) {
        try {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir != null && pickedDir.isDirectory()) {
                songNames.clear();
                for (DocumentFile file : pickedDir.listFiles()) {
                    String name = file.getName();
                    if (name != null && (name.endsWith(".mp3") || name.endsWith(".wav"))) {
                        songNames.add(name);
                    }
                }
                if (songAdapter != null) songAdapter.notifyDataSetChanged();
                restoreSelectedCheck();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void restoreSelectedCheck() {
        String savedSong = prefs.getString("selected_song_name", null);
        if (savedSong != null && lv != null) {
            int position = songNames.indexOf(savedSong);
            if (position != -1) lv.setItemChecked(position, true);
        }
    }
}