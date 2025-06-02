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
import android.widget.SeekBar; // Importar SeekBar
import android.widget.Spinner; // Importar Spinner
import android.widget.TextView; // Importar TextView (para mostrar el progreso)
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays; // Importar Arrays
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProyectosFragment extends Fragment {

    private RecyclerView recyclerViewProyectos;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddProject;

    private List<Project> projectsList;
    private ProjectAdapter projectsAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration projectsListenerRegistration;

    private static final String TAG = "ProyectosFragment";

    private ColorDrawable background;
    private Drawable deleteIcon;

    private SimpleDateFormat dateFormatter;

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
        View view = inflater.inflate(R.layout.proyectos_view, container, false);

        recyclerViewProyectos = view.findViewById(R.id.recyclerViewProyectos);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddProject = view.findViewById(R.id.fabAddProject);

        projectsList = new ArrayList<>();
        projectsAdapter = new ProjectAdapter(projectsList);

        recyclerViewProyectos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewProyectos.setAdapter(projectsAdapter);

        projectsAdapter.setOnProjectActionListener(new ProjectAdapter.OnProjectActionListener() {
            @Override
            public void onProjectClick(int position) {
                Project project = projectsList.get(position);
                showEditProjectDialog(project);
            }

            @Override
            public void onProjectLongClick(int position) {
                Project project = projectsList.get(position);
                Toast.makeText(getContext(), "Clic largo en proyecto: " + project.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        loadProjectsFromFirestore();

        fabAddProject.setOnClickListener(v -> showAddProjectDialog());

        background = new ColorDrawable(Color.RED);
        deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    final Project projectToDelete = projectsList.get(position);
                    Log.d(TAG, "onSwiped: Proyecto deslizado en posición " + position + ", ID: " + projectToDelete.getId());

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar Proyecto")
                            .setMessage("¿Estás seguro de que quieres eliminar este proyecto: \"" + projectToDelete.getName() + "\"?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                if (mAuth.getCurrentUser() != null) {
                                    db.collection("users").document(mAuth.getCurrentUser().getUid())
                                            .collection("projects").document(projectToDelete.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Proyecto eliminado de Firestore: " + projectToDelete.getName());
                                                Toast.makeText(requireContext(), "Proyecto eliminado: " + projectToDelete.getName(), Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Error al eliminar el proyecto de Firestore", e);
                                                Toast.makeText(requireContext(), "Error al eliminar proyecto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                projectsAdapter.notifyItemChanged(position);
                                            });
                                } else {
                                    Toast.makeText(requireContext(), "Error: Usuario no autenticado para eliminar proyecto.", Toast.LENGTH_SHORT).show();
                                    projectsAdapter.notifyItemChanged(position);
                                }
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                projectsAdapter.notifyItemChanged(position);
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

        itemTouchHelper.attachToRecyclerView(recyclerViewProyectos);

        return view;
    }

    private void loadProjectsFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "loadProjectsFromFirestore: Usuario no autenticado.");
            loadingProgressBar.setVisibility(View.GONE);
            updateUI();
            Toast.makeText(getContext(), "Inicia sesión para ver tus proyectos.", Toast.LENGTH_LONG).show();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        recyclerViewProyectos.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        if (projectsListenerRegistration != null) {
            projectsListenerRegistration.remove();
        }

        projectsListenerRegistration = db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("projects")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for projects.", error);
                        loadingProgressBar.setVisibility(View.GONE);
                        updateUI();
                        Toast.makeText(getContext(), "Error al cargar proyectos: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    projectsList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Project project = doc.toObject(Project.class);
                            project.setId(doc.getId());
                            projectsList.add(project);
                        }
                    }
                    projectsAdapter.updateData(projectsList);
                    loadingProgressBar.setVisibility(View.GONE);
                    updateUI();
                });
    }

    private void addProjectToFirestore(String name, String description, String status, double progress, Date startDate, Date endDate) { // Añadir status y progress
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Project newProject = new Project(name, description, status, progress, startDate, endDate);
        // El createdAt se establece por el servidor

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("projects")
                .add(newProject)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Proyecto añadido a Firestore con ID: " + documentReference.getId());
                    Toast.makeText(requireContext(), "Proyecto agregado: " + name, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al añadir el proyecto a Firestore", e);
                    Toast.makeText(requireContext(), "Error al agregar proyecto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nuevo Proyecto");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputName = new EditText(requireContext());
        inputName.setHint("Nombre del Proyecto");
        layout.addView(inputName);

        final EditText inputDescription = new EditText(requireContext());
        inputDescription.setHint("Descripción (opcional)");
        layout.addView(inputDescription);

        // Campo para la fecha de inicio
        final EditText inputStartDate = new EditText(requireContext());
        inputStartDate.setHint("Fecha de inicio (dd/MM/yyyy)");
        inputStartDate.setFocusable(false);
        inputStartDate.setClickable(true);
        layout.addView(inputStartDate);

        // Campo para la fecha de fin
        final EditText inputEndDate = new EditText(requireContext());
        inputEndDate.setHint("Fecha de fin (opcional)");
        inputEndDate.setFocusable(false);
        inputEndDate.setClickable(true);
        layout.addView(inputEndDate);

        // Spinner para el estado del proyecto
        final Spinner statusSpinner = new Spinner(requireContext());
        String[] statuses = {"Activo", "En Pausa", "Completado", "Cancelado"}; // Opciones de estado
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        // Selecciona "Activo" por defecto
        statusSpinner.setSelection(Arrays.asList(statuses).indexOf("Activo"));
        layout.addView(statusSpinner);

        // SeekBar para el progreso
        final TextView progressTextView = new TextView(requireContext());
        progressTextView.setText("Progreso: 0%");
        layout.addView(progressTextView);

        final SeekBar progressBar = new SeekBar(requireContext());
        progressBar.setMax(100); // 0-100%
        progressBar.setProgress(0); // Valor inicial
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressTextView.setText("Progreso: " + progress + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(progressBar);


        final Calendar calendarStartDate = Calendar.getInstance();
        final Calendar calendarEndDate = Calendar.getInstance();

        inputStartDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendarStartDate.set(year, month, dayOfMonth);
                        inputStartDate.setText(dateFormatter.format(calendarStartDate.getTime()));
                    },
                    calendarStartDate.get(Calendar.YEAR),
                    calendarStartDate.get(Calendar.MONTH),
                    calendarStartDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        inputEndDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendarEndDate.set(year, month, dayOfMonth);
                        inputEndDate.setText(dateFormatter.format(calendarEndDate.getTime()));
                    },
                    calendarEndDate.get(Calendar.YEAR),
                    calendarEndDate.get(Calendar.MONTH),
                    calendarEndDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String projectName = inputName.getText().toString().trim();
            String projectDescription = inputDescription.getText().toString().trim();
            String projectStatus = (String) statusSpinner.getSelectedItem(); // Obtener estado
            double projectProgress = progressBar.getProgress(); // Obtener progreso

            Date projectStartDate = null;
            Date projectEndDate = null;

            try {
                String startDateString = inputStartDate.getText().toString();
                if (!startDateString.isEmpty()) {
                    projectStartDate = dateFormatter.parse(startDateString);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha de inicio: " + e.getMessage());
                Toast.makeText(requireContext(), "Formato de fecha de inicio inválido. No se guardará.", Toast.LENGTH_LONG).show();
            }

            try {
                String endDateString = inputEndDate.getText().toString();
                if (!endDateString.isEmpty()) {
                    projectEndDate = dateFormatter.parse(endDateString);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha de fin: " + e.getMessage());
                Toast.makeText(requireContext(), "Formato de fecha de fin inválido. No se guardará.", Toast.LENGTH_LONG).show();
            }

            if (!projectName.isEmpty()) {
                addProjectToFirestore(projectName, projectDescription, projectStatus, projectProgress, projectStartDate, projectEndDate);
            } else {
                Toast.makeText(requireContext(), "El nombre del proyecto no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditProjectDialog(final Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar Proyecto");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputName = new EditText(requireContext());
        inputName.setHint("Nuevo nombre del proyecto");
        inputName.setText(project.getName());
        layout.addView(inputName);

        final EditText inputDescription = new EditText(requireContext());
        inputDescription.setHint("Nueva descripción del proyecto");
        inputDescription.setText(project.getDescription());
        layout.addView(inputDescription);

        final EditText inputStartDate = new EditText(requireContext());
        inputStartDate.setHint("Fecha de inicio (dd/MM/yyyy)");
        inputStartDate.setFocusable(false);
        inputStartDate.setClickable(true);
        if (project.getStartDate() != null) {
            inputStartDate.setText(dateFormatter.format(project.getStartDate()));
        }
        layout.addView(inputStartDate);

        final EditText inputEndDate = new EditText(requireContext());
        inputEndDate.setHint("Fecha de fin (opcional)");
        inputEndDate.setFocusable(false);
        inputEndDate.setClickable(true);
        if (project.getEndDate() != null) {
            inputEndDate.setText(dateFormatter.format(project.getEndDate()));
        }
        layout.addView(inputEndDate);

        // Spinner para el estado del proyecto
        final Spinner statusSpinner = new Spinner(requireContext());
        String[] statuses = {"Activo", "En Pausa", "Completado", "Cancelado"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        // Selecciona el estado actual del proyecto
        int currentStatusIndex = Arrays.asList(statuses).indexOf(project.getStatus());
        if (currentStatusIndex != -1) {
            statusSpinner.setSelection(currentStatusIndex);
        } else {
            statusSpinner.setSelection(Arrays.asList(statuses).indexOf("Activo")); // Default si el estado no coincide
        }
        layout.addView(statusSpinner);

        // SeekBar para el progreso
        final TextView progressTextView = new TextView(requireContext());
        progressTextView.setText("Progreso: " + (int) project.getProgress() + "%");
        layout.addView(progressTextView);

        final SeekBar progressBar = new SeekBar(requireContext());
        progressBar.setMax(100);
        progressBar.setProgress((int) project.getProgress()); // Cargar progreso actual
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressTextView.setText("Progreso: " + progress + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(progressBar);


        final Calendar calendarStartDate = Calendar.getInstance();
        if (project.getStartDate() != null) {
            calendarStartDate.setTime(project.getStartDate());
        }

        final Calendar calendarEndDate = Calendar.getInstance();
        if (project.getEndDate() != null) {
            calendarEndDate.setTime(project.getEndDate());
        }

        inputStartDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendarStartDate.set(year, month, dayOfMonth);
                        inputStartDate.setText(dateFormatter.format(calendarStartDate.getTime()));
                    },
                    calendarStartDate.get(Calendar.YEAR),
                    calendarStartDate.get(Calendar.MONTH),
                    calendarStartDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        inputEndDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendarEndDate.set(year, month, dayOfMonth);
                        inputEndDate.setText(dateFormatter.format(calendarEndDate.getTime()));
                    },
                    calendarEndDate.get(Calendar.YEAR),
                    calendarEndDate.get(Calendar.MONTH),
                    calendarEndDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            String newDescription = inputDescription.getText().toString().trim();
            String newStatus = (String) statusSpinner.getSelectedItem(); // Obtener nuevo estado
            double newProgress = progressBar.getProgress(); // Obtener nuevo progreso

            Date newStartDate = null;
            Date newEndDate = null;

            try {
                String startDateString = inputStartDate.getText().toString();
                if (!startDateString.isEmpty()) {
                    newStartDate = dateFormatter.parse(startDateString);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha de inicio: " + e.getMessage());
                Toast.makeText(requireContext(), "Formato de fecha de inicio inválido.", Toast.LENGTH_SHORT).show();
            }

            try {
                String endDateString = inputEndDate.getText().toString();
                if (!endDateString.isEmpty()) {
                    newEndDate = dateFormatter.parse(endDateString);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error al parsear fecha de fin: " + e.getMessage());
                Toast.makeText(requireContext(), "Formato de fecha de fin inválido.", Toast.LENGTH_SHORT).show();
            }

            if (!newName.isEmpty()) {
                // Comprobar si hay cambios significativos en cualquier campo
                boolean nameChanged = !newName.equals(project.getName());
                boolean descriptionChanged = !newDescription.equals(project.getDescription());
                boolean startDateChanged = (newStartDate == null && project.getStartDate() != null) ||
                        (newStartDate != null && project.getStartDate() == null) ||
                        (newStartDate != null && project.getStartDate() != null && !newStartDate.equals(project.getStartDate()));
                boolean endDateChanged = (newEndDate == null && project.getEndDate() != null) ||
                        (newEndDate != null && project.getEndDate() == null) ||
                        (newEndDate != null && project.getEndDate() != null && !newEndDate.equals(project.getEndDate()));
                boolean statusChanged = !newStatus.equals(project.getStatus()); // Nuevo
                boolean progressChanged = newProgress != project.getProgress(); // Nuevo

                if (nameChanged || descriptionChanged || startDateChanged || endDateChanged || statusChanged || progressChanged) {
                    updateProjectInFirestore(project.getId(), newName, newDescription, newStatus, newProgress, newStartDate, newEndDate); // Pasar todos los datos
                } else {
                    Toast.makeText(requireContext(), "No se realizaron cambios en el proyecto", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "El nombre del proyecto no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateProjectInFirestore(String projectId, String newName, String newDescription, String newStatus, double newProgress, Date newStartDate, Date newEndDate) { // Añadir status y progress
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("description", newDescription);
        updates.put("status", newStatus); // Actualizar status
        updates.put("progress", newProgress); // Actualizar progress
        updates.put("startDate", newStartDate);
        updates.put("endDate", newEndDate);

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("projects").document(projectId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Proyecto actualizado en Firestore para ID: " + projectId);
                    Toast.makeText(requireContext(), "Proyecto actualizado: " + newName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar el proyecto en Firestore para ID: " + projectId, e);
                    Toast.makeText(requireContext(), "Error al actualizar proyecto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUI() {
        if (projectsList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewProyectos.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewProyectos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (projectsListenerRegistration != null) {
            projectsListenerRegistration.remove();
            Log.d(TAG, "Firestore listener removed for projects.");
        }
    }
}