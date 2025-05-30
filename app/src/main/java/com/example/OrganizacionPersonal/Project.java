package com.example.OrganizacionPersonal;

import com.google.firebase.firestore.ServerTimestamp; // Importa ServerTimestamp

import java.util.Date; // Importa Date

public class Project {
    private String id;
    private String name;
    private String description;
    private Date createdAt; // Para ordenar y saber cuándo fue creado

    public Project() {
        // Constructor público vacío requerido por Firestore
    }

    public Project(String name, String description) {
        this.name = name;
        this.description = description;
        // createdAt se establecerá automáticamente por Firestore
    }

    // --- Getters y Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ServerTimestamp // Anotación para que Firestore genere la marca de tiempo del servidor
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}