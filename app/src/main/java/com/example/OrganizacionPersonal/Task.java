package com.example.OrganizacionPersonal;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Task {
    private String id; // El ID del documento de Firestore
    private String title; // Cambiado de 'name' a 'title' para consistencia
    private String description; // Nuevo campo
    private boolean completed;
    private Date dueDate; // Nuevo campo: Fecha de vencimiento
    private Date createdAt; // Fecha de creación, generada por el servidor
    private Date completedAt; // Nuevo campo: Fecha de completado

    public Task() {
        // Constructor público vacío requerido por Firestore para deserialización
    }

    // Constructor para crear nuevas tareas (sin ID inicial, Firestore lo asigna)
    public Task(String title, String description, boolean completed, Date dueDate) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.dueDate = dueDate;
        // createdAt y completedAt se establecerán por Firestore o lógicamente
    }

    // Getters y Setters

    @Exclude // Excluye este campo cuando se guarda el objeto en Firestore
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() { // Cambiado de getName() a getTitle()
        return title;
    }

    public void setTitle(String title) { // Cambiado de setName() a setTitle()
        this.title = title;
    }

    public String getDescription() { // Getter para el nuevo campo
        return description;
    }

    public void setDescription(String description) { // Setter para el nuevo campo
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Date getDueDate() { // Getter para el nuevo campo
        return dueDate;
    }

    public void setDueDate(Date dueDate) { // Setter para el nuevo campo
        this.dueDate = dueDate;
    }

    @ServerTimestamp
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @ServerTimestamp // Este campo se actualizará con un timestamp del servidor cuando se modifique
    public Date getCompletedAt() { // Getter para el nuevo campo
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) { // Setter para el nuevo campo
        this.completedAt = completedAt;
    }
}