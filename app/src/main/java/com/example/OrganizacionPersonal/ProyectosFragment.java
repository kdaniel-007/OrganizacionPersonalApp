package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface; // Para el diálogo
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper; // Necesario para deslizar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// Importaciones para el dibujo del swipe-to-dismiss
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class ProyectosFragment extends Fragment {

    private RecyclerView recyclerViewProyectos;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddProject;

    private List<Project> projectsList; // Usará la clase Project
    private ProjectAdapter projectsAdapter; // Usará el ProjectAdapter

    private FirebaseFirestore db;
    private ListenerRegistration projectsListenerRegistration;

    private static final String TAG = "ProyectosFragment";

    // Para el swipe-to-dismiss visual
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
        View view = inflater.inflate(R.layout.proyectos_view, container, false);

        // ¡Asegúrate de que estos IDs existan en tu proyectos_view.xml!
        recyclerViewProyectos = view.findViewById(R.id.recyclerViewProyectos);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddProject = view.findViewById(R.id.fabAddProject);

        projectsList = new ArrayList<>();
        projectsAdapter = new ProjectAdapter(projectsList); // Usa ProjectAdapter

        recyclerViewProyectos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewProyectos.setAdapter(projectsAdapter);

        // Configurar el listener de acciones para el adaptador de proyectos
        projectsAdapter.setOnProjectActionListener(new ProjectAdapter.OnProjectActionListener() {
            @Override
            public void onProjectClick(int position) {
                Project project = projectsList.get(position);
                showEditProjectDialog(project); // Clic normal para editar
            }

            @Override
            public void onProjectLongClick(int position) {
                Project project = projectsList.get(position);
                Toast.makeText(getContext(), "Clic largo en proyecto: " + project.getName(), Toast.LENGTH_SHORT).show();
                // Aquí podrías implementar un menú contextual o más opciones
            }
        });

        loadProjectsFromFirestore(); // Carga los proyectos de Firestore

        fabAddProject.setOnClickListener(v -> showAddProjectDialog());


        background = new ColorDrawable(Color.RED);
        // Asegúrate de que este drawable exista en tu res/drawable/
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
                                db.collection("proyectos").document(projectToDelete.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Proyecto eliminado de Firestore: " + projectToDelete.getName());
                                            Toast.makeText(requireContext(), "Proyecto eliminado: " + projectToDelete.getName(), Toast.LENGTH_SHORT).show();
                                            // La UI se actualizará automáticamente a través del SnapshotListener
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Error al eliminar el proyecto de Firestore", e);
                                            Toast.makeText(requireContext(), "Error al eliminar proyecto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            projectsAdapter.notifyItemChanged(position); // El ítem vuelve a su lugar si falla
                                        });
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                projectsAdapter.notifyItemChanged(position); // El ítem vuelve a su lugar
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

    // Método para cargar proyectos desde Firestore en tiempo real
    private void loadProjectsFromFirestore() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        recyclerViewProyectos.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        if (projectsListenerRegistration != null) {
            projectsListenerRegistration.remove();
        }

        db.collection("proyectos") // ¡Nueva colección "proyectos"!
                .orderBy("createdAt", Query.Direction.ASCENDING) // Ordenar por fecha de creación
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for projects.", error);
                        loadingProgressBar.setVisibility(View.GONE);
                        updateUI();
                        return;
                    }

                    projectsList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Project project = doc.toObject(Project.class);
                            project.setId(doc.getId()); // Guardar el ID del documento
                            projectsList.add(project);
                        }
                    }
                    projectsAdapter.updateData(projectsList);
                    loadingProgressBar.setVisibility(View.GONE);
                    updateUI();
                });
    }

    // Método para agregar un nuevo proyecto a Firestore
    private void addProjectToFirestore(String name, String description) {
        Project newProject = new Project(name, description);

        db.collection("proyectos")
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

    // Diálogo para añadir un nuevo proyecto
    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nuevo Proyecto");

        // Crear un layout para el diálogo con dos EditText
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20); // Añadir algo de padding

        final EditText inputName = new EditText(requireContext());
        inputName.setHint("Nombre del Proyecto");
        layout.addView(inputName);

        final EditText inputDescription = new EditText(requireContext());
        inputDescription.setHint("Descripción (opcional)");
        layout.addView(inputDescription);

        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String projectName = inputName.getText().toString().trim();
            String projectDescription = inputDescription.getText().toString().trim();

            if (!projectName.isEmpty()) {
                addProjectToFirestore(projectName, projectDescription);
            } else {
                Toast.makeText(requireContext(), "El nombre del proyecto no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Diálogo para editar un proyecto existente
    private void showEditProjectDialog(final Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar Proyecto");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputName = new EditText(requireContext());
        inputName.setHint("Nuevo nombre del proyecto");
        inputName.setText(project.getName()); // Precargar nombre actual
        layout.addView(inputName);

        final EditText inputDescription = new EditText(requireContext());
        inputDescription.setHint("Nueva descripción del proyecto");
        inputDescription.setText(project.getDescription()); // Precargar descripción actual
        layout.addView(inputDescription);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            String newDescription = inputDescription.getText().toString().trim();

            if (!newName.isEmpty()) {
                updateProjectInFirestore(project.getId(), newName, newDescription);
            } else {
                Toast.makeText(requireContext(), "El nombre del proyecto no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Método para actualizar un proyecto en Firestore
    private void updateProjectInFirestore(String projectId, String newName, String newDescription) {
        db.collection("proyectos").document(projectId)
                .update("name", newName, "description", newDescription) // Actualizar ambos campos
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Proyecto actualizado en Firestore para ID: " + projectId);
                    Toast.makeText(requireContext(), "Proyecto actualizado: " + newName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar el proyecto en Firestore para ID: " + projectId, e);
                    Toast.makeText(requireContext(), "Error al actualizar proyecto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Actualizar la visibilidad de la UI (lista o estado vacío)
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
        // Detener el listener de Firestore cuando el fragmento ya no está visible
        if (projectsListenerRegistration != null) {
            projectsListenerRegistration.remove();
            Log.d(TAG, "Firestore listener removed for projects.");
        }
    }
}