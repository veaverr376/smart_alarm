package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {
    private List<String> alarms;
    private OnAlarmActionListener listener;

    // 1. ИСПРАВЛЕННЫЙ ИНТЕРФЕЙС (теперь с onEdit)
    public interface OnAlarmActionListener {
        void onDelete(int position);
        void onToggle(int position, boolean isEnabled);
        void onEdit(int position); // Эту строчку мы добавили
    }

    public AlarmAdapter(List<String> alarms, OnAlarmActionListener listener) {
        this.alarms = alarms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.timeText.setText(alarms.get(position));

        // Кнопка удаления
        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(holder.getAdapterPosition());
        });

        // Кнопка редактирования (шестеренка)
        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(holder.getAdapterPosition());
        });

        // Переключатель
        holder.aSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (listener != null) listener.onToggle(holder.getAdapterPosition(), isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        ImageButton deleteBtn;
        ImageButton editBtn; // Добавили переменную для шестеренки
        Switch aSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.alarmTimeText);
            deleteBtn = itemView.findViewById(R.id.deleteAlarmButton);
            aSwitch = itemView.findViewById(R.id.alarmSwitch);

            // 2. СВЯЗЫВАЕМ С ID ИЗ ТВОЕГО XML
            editBtn = itemView.findViewById(R.id.btnEditAlarm);
        }
    }
}