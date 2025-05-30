package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar; // Importar ProgressBar
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Asegúrate de esta importación para ContextCompat
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat; // Importar SimpleDateFormat
import java.util.List;
import java.util.Locale; // Importar Locale

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projects;
    private OnProjectActionListener listener;
    private SimpleDateFormat dateFormatter; // Para formatear la fecha

    public ProjectAdapter(List<Project> projects) {
        this.projects = projects;
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // Inicializar formateador
    }

    public interface OnProjectActionListener {
        void onProjectClick(int position);
        void onProjectLongClick(int position);
    }

    public void setOnProjectActionListener(OnProjectActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.projectName.setText(project.getName());
        holder.projectDescription.setText(project.getDescription());

        // Mostrar fechas
        if (project.getStartDate() != null) {
            holder.projectStartDate.setText("Inicio: " + dateFormatter.format(project.getStartDate()));
            holder.projectStartDate.setVisibility(View.VISIBLE);
        } else {
            holder.projectStartDate.setVisibility(View.GONE);
        }

        if (project.getEndDate() != null) {
            holder.projectEndDate.setText("Fin: " + dateFormatter.format(project.getEndDate()));
            holder.projectEndDate.setVisibility(View.VISIBLE);
        } else {
            holder.projectEndDate.setVisibility(View.GONE);
        }

        // Mostrar estado y progreso
        holder.projectStatus.setText("Estado: " + project.getStatus());
        holder.projectProgressText.setText("Progreso: " + (int) project.getProgress() + "%");
        holder.projectProgressBar.setProgress((int) project.getProgress());

        // Opcional: Cambiar color de estado si quieres
        // Asegúrate de que R.color.organize_light_blue y R.color.md_theme_dark_onError existan en tu colors.xml
        if ("Completado".equals(project.getStatus())) {
            holder.projectStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.organize_light_blue));
        } else {
            holder.projectStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_dark_onError));
        }
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Project> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }

    public class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectName;
        TextView projectDescription;
        TextView projectStartDate; // Nuevo TextView para fecha de inicio
        TextView projectEndDate;   // Nuevo TextView para fecha de fin
        TextView projectStatus;    // Nuevo TextView para estado
        TextView projectProgressText; // Nuevo TextView para progreso en texto
        ProgressBar projectProgressBar; // Nueva ProgressBar para progreso visual

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            // ESTAS SON LAS LÍNEAS QUE DEBEN COINCIDIR CON item_project.xml
            projectName = itemView.findViewById(R.id.projectName);
            projectDescription = itemView.findViewById(R.id.projectDescription);
            projectStartDate = itemView.findViewById(R.id.projectStartDate);
            projectEndDate = itemView.findViewById(R.id.projectEndDate);
            projectStatus = itemView.findViewById(R.id.projectStatus);
            projectProgressText = itemView.findViewById(R.id.projectProgressText);
            projectProgressBar = itemView.findViewById(R.id.projectProgressBar);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onProjectClick(position);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onProjectLongClick(position);
                        return true;
                    }
                }
                return false;
            });
        }
    }
}