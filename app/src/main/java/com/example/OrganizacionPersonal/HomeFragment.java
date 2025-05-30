package com.example.OrganizacionPersonal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth; // Instancia de Firebase Auth

    public HomeFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance(); // Inicializar Firebase Auth
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.inicio_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        Button btnViewAllCalendar = view.findViewById(R.id.btnViewAllCalendar);
        Button btnViewAllTasks = view.findViewById(R.id.btnViewAllTasks);
        Button btnViewAllProjects = view.findViewById(R.id.btnViewAllProjects);

        // Referencia al TextView del mensaje de bienvenida
        TextView tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);

        // Obtener el usuario actual de Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userName = "Usuario"; // Valor por defecto

            // Intentar obtener el nombre de la cuenta (si está disponible)
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                userName = currentUser.getDisplayName();
            } else if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                // Si no hay nombre, usar la parte del email antes del @
                userName = currentUser.getEmail().split("@")[0];
            }
            tvWelcomeMessage.setText("¡Hola " + userName + "! 👋 Aquí tienes un resumen de tu día.");
        } else {
            // Si no hay usuario logueado (lo cual no debería pasar si llegas aquí desde el login)
            tvWelcomeMessage.setText("¡Hola! 👋 Aquí tienes un resumen de tu día.");
        }


        if (btnViewAllCalendar != null) {
            btnViewAllCalendar.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).loadFragment(new CalendarFragment(), "Calendario");
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_calendar);
                    }
                }
            });
        }

        if (btnViewAllTasks != null) {
            btnViewAllTasks.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).loadFragment(new TareasFragment(), "Tareas");
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_tasks);
                    }
                }
            });
        }

        if (btnViewAllProjects != null) {
            btnViewAllProjects.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).loadFragment(new ProyectosFragment(), "Proyectos");
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_projects);
                    }
                }
            });
        }
    }
}