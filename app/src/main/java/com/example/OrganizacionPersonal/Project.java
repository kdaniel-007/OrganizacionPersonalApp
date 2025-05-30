package com.example.OrganizacionPersonal;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Project {
    @DocumentId
    private String id;
    private String name; // Nombre del proyecto
    private String description; // Descripción del proyecto
    private String status; // Ej: "Activo", "Completado", "Pausado"
    private double progress; // Porcentaje de progreso (0.0 - 100.0)
    private Date startDate; // Fecha de inicio
    private Date endDate; // Fecha de finalización
    @ServerTimestamp
    private Date createdAt; // Timestamp de creación del servidor

    public Project() {
        // Constructor público vacío requerido por Firestore
    }

    // Constructor con todos los campos
    public Project(String name, String description, String status, double progress, Date startDate, Date endDate) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.progress = progress;
        this.startDate = startDate;
        this.endDate = endDate;
        // createdAt se establecerá automáticamente por Firestore
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public double getProgress() { return progress; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public Date getCreatedAt() { return createdAt; }

    // --- Setters (opcionales, pero útiles para la inicialización y manipulación) ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setProgress(double progress) { this.progress = progress; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}