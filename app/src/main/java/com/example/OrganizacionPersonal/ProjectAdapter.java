package com.example.OrganizacionPersonal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projects;
    private OnProjectActionListener listener;

    // Interfaz para manejar las acciones en los ítems del proyecto
    public interface OnProjectActionListener {
        void onProjectClick(int position); // Clic normal para ver detalles o editar
        void onProjectLongClick(int position); // Clic largo para eliminar o más opciones
    }

    // Método para establecer el listener
    public void setOnProjectActionListener(OnProjectActionListener listener) {
        this.listener = listener;
    }

    public ProjectAdapter(List<Project> projects) {
        this.projects = projects;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false); // Infla el layout del ítem de proyecto
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);

        holder.tvProjectName.setText(project.getName());
        holder.tvProjectDescription.setText(project.getDescription());

        // Configurar el click listener para el ítem completo
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick(position);
            }
        });

        // Configurar el long click listener para el ítem completo
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onProjectLongClick(position);
            }
            return true; // Consumir el evento para que no se dispare el click normal
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    // Clase ViewHolder que enlaza las vistas del layout item_project.xml
    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        public TextView tvProjectName;
        public TextView tvProjectDescription;

        public ProjectViewHolder(View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvProjectDescription = itemView.findViewById(R.id.tvProjectDescription);
        }
    }

    // Método para actualizar los datos del adaptador, similar al de TaskAdapter
    public void updateData(List<Project> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }
}