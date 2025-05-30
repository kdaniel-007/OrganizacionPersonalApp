package com.example.OrganizacionPersonal;

import com.google.firebase.firestore.Exclude; // Para excluir el ID de Firestore al guardar
import com.google.firebase.firestore.ServerTimestamp; // Para obtener la marca de tiempo del servidor

import java.util.Date;

public class Task {
    private String id; // El ID del documento de Firestore
    private String name;
    private boolean completed;
    private Date createdAt; // Fecha de creación, generada por el servidor

    public Task() {
        // Constructor público vacío requerido por Firestore para deserialización
    }

    public Task(String name, boolean completed) {
        this.name = name;
        this.completed = completed;
    }

    @Exclude // Excluye este campo cuando se guarda el objeto en Firestore
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @ServerTimestamp // Anotación para que Firestore genere la marca de tiempo del servidor
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}