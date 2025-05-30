package com.example.OrganizacionPersonal;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    // Cambiamos el nombre de la interfaz a OnTaskActionListener
    private OnTaskActionListener listener;

    // Nueva interfaz para manejar todas las acciones de la tarea
    public interface OnTaskActionListener {
        void onTaskClick(int position);
        void onTaskLongClick(int position);
        void onTaskCompletedChanged(int position, boolean isChecked); // Nuevo método para el CheckBox
    }

    // Método para establecer el listener (ahora OnTaskActionListener)
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

        holder.tvTaskName.setText(task.getName());
        holder.checkboxCompleted.setOnCheckedChangeListener(null); // Desactivar listener para evitar bucles
        holder.checkboxCompleted.setChecked(task.isCompleted()); // Establecer el estado del CheckBox

        // Añadir/Remover efecto tachado basado en el estado completado
        if (task.isCompleted()) {
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Volver a añadir el listener para el CheckBox
        holder.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskCompletedChanged(position, isChecked);
            }
        });

        // Opcional: manejar el clic en el ítem (para edición)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(position);
            }
        });

        // Opcional: manejar el clic largo en el ítem
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTaskLongClick(position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTaskName;
        public CheckBox checkboxCompleted; // Declarar el CheckBox

        public TaskViewHolder(View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            checkboxCompleted = itemView.findViewById(R.id.checkboxCompleted); // Enlazar el CheckBox
        }
    }

    // Método para actualizar los datos del adaptador
    public void updateData(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged(); // Notifica al RecyclerView que los datos han cambiado
    }

    // removeItem se mantiene pero no se usa directamente para la eliminación de Firestore
    public void removeItem(int position) {
        tasks.remove(position);
        notifyItemRemoved(position);
    }
}