package com.example.OrganizacionPersonal;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskClick(int position);
        void onTaskLongClick(int position);
        void onTaskCompletedChanged(int position, boolean isChecked);
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTaskName.setText(task.getTitle());
        holder.checkboxCompleted.setOnCheckedChangeListener(null);
        holder.checkboxCompleted.setChecked(task.isCompleted());

        if (task.isCompleted()) {
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskCompletedChanged(position, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTaskLongClick(position);
            }
            return true;
        });

        // Mostrar fecha de vencimiento
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDueDate.setText("Vence: " + sdf.format(task.getDueDate()));
            holder.tvDueDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDueDate.setText("");
            holder.tvDueDate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTaskName;
        public TextView tvDueDate;
        public CheckBox checkboxCompleted;

        public TaskViewHolder(View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            checkboxCompleted = itemView.findViewById(R.id.checkboxCompleted);
        }
    }

    public void updateData(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        tasks.remove(position);
        notifyItemRemoved(position);
    }
}
