package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView; // Importar AdapterView
import android.widget.ArrayAdapter; // Importar ArrayAdapter
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner; // Importar Spinner
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TareasFragment extends Fragment {

    private RecyclerView recyclerViewTareas;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddTask;
    private Spinner filterSpinner; // Nuevo Spinner para el filtro

    private List<Task> tasksList;
    private TaskAdapter tasksAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration tasksListenerRegistration;

    private static final String TAG = "TareasFragment";

    private ColorDrawable background;
    private Drawable deleteIcon;

    private SimpleDateFormat dateFormatter;

    private String currentFilter = "Todas las Tareas"; // Estado actual del filtro

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tareas_view, container, false);

        recyclerViewTareas = view.findViewById(R.id.recyclerViewTareas);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        filterSpinner = view.findViewById(R.id.filterSpinner); // Inicializar Spinner

        tasksList = new ArrayList<>();
        tasksAdapter = new TaskAdapter(tasksList);

        recyclerViewTareas.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTareas.setAdapter(tasksAdapter);

        tasksAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(int position) {
                Task task = tasksList.get(position);
                showEditTaskDialog(task);
            }

            @Override
            public void onTaskLongClick(int position) {
                Task task = tasksList.get(position);
                Toast.makeText(getContext(), "Clic largo en: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTaskCompletedChanged(int position, boolean isChecked) {
                Task task = tasksList.get(position);
                updateTaskCompletionStatus(task.getId(), isChecked);
            }
        });

        // Configurar el Spinner de filtro
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.task_filter_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Filtro seleccionado: " + currentFilter);
                loadTasksFromFirestore(); // Volver a cargar las tareas con el nuevo filtro
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });

        loadTasksFromFirestore(); // Cargar tareas inicialmente

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        background = new ColorDrawable(Color.RED);
        deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

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
                            .setMessage("¿Estás seguro de que quieres eliminar esta tarea: \"" + taskToDelete.getTitle() + "\"?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                if (mAuth.getCurrentUser() != null) {
                                    db.collection("users").document(mAuth.getCurrentUser().getUid())
                                            .collection("tasks").document(taskToDelete.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Tarea eliminada de Firestore: " + taskToDelete.getTitle());
                                                Toast.makeText(requireContext(), "Tarea eliminada: " + taskToDelete.getTitle(), Toast.LENGTH_SHORT).show();
                                                // No es necesario notifyItemRemoved, el listener de Firestore lo hará
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Error al eliminar la tarea de Firestore", e);
                                                Toast.makeText(requireContext(), "Error al eliminar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                tasksAdapter.notifyItemChanged(position); // Revertir el swipe si falla
                                            });
                                } else {
                                    Toast.makeText(requireContext(), "Error: Usuario no autenticado para eliminar tarea.", Toast.LENGTH_SHORT).show();
                                    tasksAdapter.notifyItemChanged(position); // Revertir el swipe
                                }
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                tasksAdapter.notifyItemChanged(position); // Revertir el swipe
                                dialog.dismiss();
                            })
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (itemView == null || deleteIcon == null || background == null) {
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
                    background.setBounds(0, 0, 0, 0);
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
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "loadTasksFromFirestore: Usuario no autenticado.");
            loadingProgressBar.setVisibility(View.GONE);
            updateUI();
            Toast.makeText(getContext(), "Inicia sesión para ver tus tareas.", Toast.LENGTH_LONG).show();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        recyclerViewTareas.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove(); // Remover el listener anterior
        }

        Query query = db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("tasks");

        // Aplicar filtro según la selección del Spinner
        switch (currentFilter) {
            case "Tareas Pendientes":
                query = query.whereEqualTo("completed", false);
                break;
            case "Tareas Completadas":
                query = query.whereEqualTo("completed", true);
                break;
            case "Todas las Tareas":
            default:
                // No se aplica filtro adicional, se cargan todas
                break;
        }

        query = query.orderBy("createdAt", Query.Direction.ASCENDING); // Siempre ordenar por createdAt

        tasksListenerRegistration = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Listen failed.", error);
                loadingProgressBar.setVisibility(View.GONE);
                updateUI();
                Toast.makeText(getContext(), "Error al cargar tareas: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
            tasksAdapter.updateData(tasksList);
            loadingProgressBar.setVisibility(View.GONE);
            updateUI();
        });
    }

    private void addTaskToFirestore(String taskTitle, String taskDescription, Date taskDueDate) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }
        Task newTask = new Task(taskTitle, taskDescription, false, taskDueDate); // Siempre se añade como no completada

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("tasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Tarea añadida a Firestore con ID: " + documentReference.getId());
                    Toast.makeText(requireContext(), "Tarea agregada: " + taskTitle, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al añadir la tarea a Firestore", e);
                    Toast.makeText(requireContext(), "Error al agregar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showEditTaskDialog(final Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar Tarea");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputTitle = new EditText(requireContext());
        inputTitle.setHint("Título de la tarea");
        inputTitle.setText(task.getTitle());
        layout.addView(inputTitle);

        final EditText inputDescription = new EditText(requireContext());
        inputDescription.setHint("Descripción (opcional)");
        inputDescription.setText(task.getDescription());
        layout.addView(inputDescription);

        final EditText inputDueDate = new EditText(requireContext());
        inputDueDate.setHint("Fecha de vencimiento (dd/MM/yyyy)");
        inputDueDate.setFocusable(false);
        inputDueDate.setClickable(true);
        if (task.getDueDate() != null) {
            inputDueDate.setText(dateFormatter.format(task.getDueDate()));
        }
        layout.addView(inputDueDate);

        final Calendar calendar = Calendar.getInstance();
        if (task.getDueDate() != null) {
            calendar.setTime(task.getDueDate());
        }

        inputDueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        inputDueDate.setText(dateFormatter.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newTitle = inputTitle.getText().toString().trim();
            String newDescription = inputDescription.getText().toString().trim();
            Date newDueDate = null;
            try {
                String dueDateString = inputDueDate.getText().toString();
                if (!dueDateString.isEmpty()) {
                    newDueDate = dateFormatter.parse(dueDateString);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al parsear fecha de vencimiento: " + e.getMessage());
                Toast.makeText(requireContext(), "Formato de fecha inválido.", Toast.LENGTH_SHORT).show();
            }

            if (!newTitle.isEmpty()) {
                boolean titleChanged = !newTitle.equals(task.getTitle());
                boolean descriptionChanged = !newDescription.equals(task.getDescription());
                boolean dueDateChanged = (newDueDate == null && task.getDueDate() != null) ||
                        (newDueDate != null && task.getDueDate() == null) ||
                        (newDueDate != null && task.getDueDate() != null && !newDueDate.equals(task.getDueDate()));

                if (titleChanged || descriptionChanged || dueDateChanged) {
                    updateTaskInFirestore(task.getId(), newTitle, newDescription, newDueDate);
                } else {
                    Toast.makeText(requireContext(), "No se realizaron cambios en la tarea", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "El título de la tarea no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateTaskInFirestore(String taskId, String newTitle, String newDescription, Date newDueDate) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updates.put("description", newDescription);
        updates.put("dueDate", newDueDate);

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("tasks").document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tarea actualizada en Firestore para ID: " + taskId);
                    Toast.makeText(requireContext(), "Tarea actualizada a: " + newTitle, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar la tarea en Firestore para ID: " + taskId, e);
                    Toast.makeText(requireContext(), "Error al actualizar tarea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateTaskCompletionStatus(String taskId, boolean isCompleted) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", isCompleted);
        if (isCompleted) {
            updates.put("completedAt", FieldValue.serverTimestamp());
        } else {
            updates.put("completedAt", null);
        }

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("tasks").document(taskId)
                .update(updates)
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

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputTitle = new EditText(requireContext());
        inputTitle.setHint("Título de la tarea");
        layout.addView(inputTitle);

        final EditText inputDescription = new EditText(requireContext());
        inputDescription.setHint("Descripción (opcional)");
        layout.addView(inputDescription);

        final EditText inputDueDate = new EditText(requireContext());
        inputDueDate.setHint("Fecha de vencimiento (opcional)");
        inputDueDate.setFocusable(false);
        inputDueDate.setClickable(true);
        layout.addView(inputDueDate);

        final Calendar calendar = Calendar.getInstance();

        inputDueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        inputDueDate.setText(dateFormatter.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String taskTitle = inputTitle.getText().toString().trim();
            String taskDescription = inputDescription.getText().toString().trim();
            Date taskDueDate = null;
            try {
                String dueDateString = inputDueDate.getText().toString();
                if (!dueDateString.isEmpty()) {
                    taskDueDate = dateFormatter.parse(dueDateString);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al parsear fecha de vencimiento: " + e.getMessage());
                Toast.makeText(requireContext(), "Formato de fecha inválido. La fecha no se guardará.", Toast.LENGTH_LONG).show();
            }

            if (!taskTitle.isEmpty()) {
                addTaskToFirestore(taskTitle, taskDescription, taskDueDate);
            } else {
                Toast.makeText(requireContext(), "El título de la tarea no puede estar vacío", Toast.LENGTH_SHORT).show();
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