package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TareasFragment extends Fragment {

    private RecyclerView recyclerViewTareas;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddTask;

    private List<Task> tasksList;
    private TaskAdapter tasksAdapter;

    private FirebaseFirestore db;
    private ListenerRegistration tasksListenerRegistration;

    private static final String TAG = "TareasFragment";

    // Declaraciones de Drawable y ColorDrawable para el icono de borrado
    private ColorDrawable background;
    private Drawable deleteIcon;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance(); // Inicializa Firestore
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tareas_view, container, false);

        recyclerViewTareas = view.findViewById(R.id.recyclerViewTareas);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddTask = view.findViewById(R.id.fabAddTask);

        tasksList = new ArrayList<>();
        tasksAdapter = new TaskAdapter(tasksList);

        recyclerViewTareas.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTareas.setAdapter(tasksAdapter);

        // Configurar el listener de acciones para el adaptador
        // ESTA ES LA SECCIÓN CLAVE QUE REQUIERE QUE TaskAdapter.java TENGA LA INTERFAZ OnTaskActionListener
        tasksAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(int position) {
                Task task = tasksList.get(position);
                showEditTaskDialog(task); // Llama al método para mostrar el diálogo de edición
            }

            @Override
            public void onTaskLongClick(int position) {
                Task task = tasksList.get(position);
                Toast.makeText(getContext(), "Clic largo en: " + task.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTaskCompletedChanged(int position, boolean isChecked) {
                Task task = tasksList.get(position);
                updateTaskCompletionStatus(task.getId(), isChecked);
            }
        });

        loadTasksFromFirestore(); // Carga las tareas de Firestore

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Inicializa el fondo rojo y el icono de borrado aquí
        background = new ColorDrawable(Color.RED);
        deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

        // Log de depuración del icono (mantenerlo para verificar la carga)
        if (deleteIcon == null) {
            Log.e(TAG, "ERROR: deleteIcon es NULL incluso después de ContextCompat.getDrawable!");
        } else {
            Log.d(TAG, "deleteIcon cargado exitosamente. Ancho intrínseco: " + deleteIcon.getIntrinsicWidth());
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    final Task taskToDelete = tasksList.get(position);
                    Log.d(TAG, "onSwiped: Tarea deslizada en posición " + position + ", ID: " + taskToDelete.getId());

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar Tarea")
                            .setMessage("¿Estás seguro de que quieres eliminar esta tarea: \"" + taskToDelete.getName() + "\"?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                db.collection("tareas").document(taskToDelete.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Tarea eliminada de Firestore: " + taskToDelete.getName());
                                            Toast.makeText(requireContext(), "Tarea eliminada: " + taskToDelete.getName(), Toast.LENGTH_SHORT).show();
                                            // La UI se actualizará automáticamente a través del SnapshotListener
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Error al eliminar la tarea de Firestore", e);
                                            Toast.makeText(requireContext(), "Error al eliminar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            tasksAdapter.notifyItemChanged(position); // El ítem vuelve a su lugar si falla
                                        });
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                tasksAdapter.notifyItemChanged(position); // El ítem vuelve a su lugar
                                dialog.dismiss();
                            })
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                // Aquí es donde se dibuja el fondo rojo y el icono de borrado
                View itemView = viewHolder.itemView;

                // Asegúrate de que el icono no sea nulo antes de usarlo
                if (deleteIcon == null || background == null) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }

                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) { // Deslizando hacia la derecha
                    background.setBounds(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + ((int) dX), itemView.getBottom());
                    deleteIcon.setBounds(itemView.getLeft() + iconMargin, iconTop,
                            itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth(), iconBottom);
                } else if (dX < 0) { // Deslizando hacia la izquierda
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    deleteIcon.setBounds(itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth(), iconTop,
                            itemView.getRight() - iconMargin, iconBottom);
                } else {
                    background.setBounds(0, 0, 0, 0); // Ocultar el fondo y el icono
                    deleteIcon.setBounds(0, 0, 0, 0);
                }
                background.draw(c);
                deleteIcon.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerViewTareas);

        return view;
    }

    private void loadTasksFromFirestore() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        recyclerViewTareas.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
        }

        tasksListenerRegistration = db.collection("tareas")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        loadingProgressBar.setVisibility(View.GONE);
                        updateUI();
                        return;
                    }

                    tasksList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Task task = doc.toObject(Task.class);
                            task.setId(doc.getId());
                            tasksList.add(task);
                        }
                    }
                    tasksAdapter.updateData(tasksList); // Usa updateData para actualizar los datos
                    loadingProgressBar.setVisibility(View.GONE);
                    updateUI();
                });
    }

    private void addTaskToFirestore(String taskName) {
        Task newTask = new Task(taskName, false);

        db.collection("tareas")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Tarea añadida a Firestore con ID: " + documentReference.getId());
                    Toast.makeText(requireContext(), "Tarea agregada: " + taskName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al añadir la tarea a Firestore", e);
                    Toast.makeText(requireContext(), "Error al agregar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showEditTaskDialog(final Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar Tarea");

        final EditText input = new EditText(requireContext());
        input.setHint("Nuevo nombre de la tarea");
        input.setText(task.getName());
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(task.getName())) {
                updateTaskNameInFirestore(task.getId(), newName);
            } else if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "El nombre de la tarea no puede estar vacío", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No se realizaron cambios en la tarea", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateTaskNameInFirestore(String taskId, String newName) {
        db.collection("tareas").document(taskId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Nombre de tarea actualizado en Firestore para ID: " + taskId);
                    Toast.makeText(requireContext(), "Tarea actualizada a: " + newName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar el nombre de la tarea en Firestore para ID: " + taskId, e);
                    Toast.makeText(requireContext(), "Error al actualizar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateTaskCompletionStatus(String taskId, boolean isCompleted) {
        db.collection("tareas").document(taskId)
                .update("completed", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Estado de completado actualizado para tarea con ID: " + taskId + " a " + isCompleted);
                    Toast.makeText(requireContext(), "Tarea " + (isCompleted ? "completada" : "pendiente"), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar estado de completado para tarea con ID: " + taskId, e);
                    Toast.makeText(requireContext(), "Error al actualizar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nueva Tarea");

        final EditText input = new EditText(requireContext());
        input.setHint("Escribe el nombre de la tarea");
        builder.setView(input);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String taskName = input.getText().toString().trim();
            if (!taskName.isEmpty()) {
                addTaskToFirestore(taskName);
            } else {
                Toast.makeText(requireContext(), "La tarea no puede estar vacía", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUI() {
        if (tasksList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewTareas.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewTareas.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
            Log.d(TAG, "Firestore listener removed.");
        }
    }
}