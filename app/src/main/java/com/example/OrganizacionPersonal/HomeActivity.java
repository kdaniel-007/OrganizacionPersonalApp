package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment activeFragment = null; // Para llevar un registro del fragmento activo

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;
            String title = "";

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                title = "Inicio";
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
                title = "Calendario";
            } else if (itemId == R.id.nav_tasks) { // Este ID ahora coincide con el menú
                selectedFragment = new TareasFragment();
                title = "Tareas";
            } else if (itemId == R.id.nav_projects) { // Este ID ahora coincide con el menú
                selectedFragment = new ProyectosFragment();
                title = "Proyectos";
            }

            // Cargar fragmento solo si es diferente al actual, o si no hay un fragmento activo
            if (selectedFragment != null && (activeFragment == null || !selectedFragment.getClass().equals(activeFragment.getClass()))) {
                loadFragment(selectedFragment, title);
                return true;
            }
            return false; // No se cambia el fragmento (ya es el activo o es nulo)
        });

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            HomeFragment initialFragment = new HomeFragment();
            loadFragment(initialFragment, "Inicio");
            bottomNav.setSelectedItemId(R.id.nav_home); // Esto también disparará el listener y cargará el fragmento.
        }
    }

    void loadFragment(Fragment newFragment, String title) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        int enterAnim = 0;
        int exitAnim = 0;

        // Determinar la dirección de la animación
        int currentTabIndex = getTabIndex(activeFragment);
        int newTabIndex = getTabIndex(newFragment);

        if (newTabIndex > currentTabIndex) { // Moviéndose a la derecha
            enterAnim = R.anim.slide_in_right;
            exitAnim = R.anim.slide_out_left;
        } else if (newTabIndex < currentTabIndex) { // Moviéndose a la izquierda
            enterAnim = R.anim.slide_in_left;
            exitAnim = R.anim.slide_out_right;
        }

        if (enterAnim != 0 && exitAnim != 0) {
            transaction.setCustomAnimations(enterAnim, exitAnim);
        }

        transaction.replace(R.id.fragment_container, newFragment);
        transaction.commit();

        // Actualizar el fragmento activo
        activeFragment = newFragment;

        // Actualizar el título de la Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private int getTabIndex(Fragment fragment) {
        if (fragment instanceof HomeFragment) return 0;
        if (fragment instanceof CalendarFragment) return 1;
        if (fragment instanceof TareasFragment) return 2;
        if (fragment instanceof ProyectosFragment) return 3;
        return -1; // Fragmento desconocido o nulo
    }
}