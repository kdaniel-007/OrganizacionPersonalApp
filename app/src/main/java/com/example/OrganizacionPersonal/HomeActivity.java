package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.content.Intent; // Necesario para iniciar otra actividad
import android.os.Bundle;
import android.util.Log; // Para mensajes de log

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth; // Importar FirebaseAuth

// Para el menú del Toolbar
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment activeFragment = null;
    private MaterialToolbar toolbar; // Asegúrate de tener esta declaración
    private FirebaseAuth mAuth; // Declarar FirebaseAuth

    private static final String TAG = "HomeActivity"; // Para logs

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance(); // Inicializar FirebaseAuth

        toolbar = findViewById(R.id.toolbar);
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
            } else if (itemId == R.id.nav_tasks) {
                selectedFragment = new TareasFragment();
                title = "Tareas";
            } else if (itemId == R.id.nav_projects) {
                selectedFragment = new ProyectosFragment();
                title = "Proyectos";
            }

            if (selectedFragment != null && (activeFragment == null || !selectedFragment.getClass().equals(activeFragment.getClass()))) {
                loadFragment(selectedFragment, title);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            HomeFragment initialFragment = new HomeFragment();
            loadFragment(initialFragment, "Inicio");
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // Método para inflar el menú del Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_toolbar_menu, menu); // Infla tu nuevo menú
        return true;
    }

    // Método para manejar clics en los ítems del menú del Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logoutUser(); // Llamar al método de cerrar sesión
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Método para cerrar la sesión del usuario
    private void logoutUser() {
        mAuth.signOut(); // Cierra la sesión de Firebase

        // Navegar de vuelta a la MainActivity (pantalla de login)
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpia la pila de actividades
        startActivity(intent);
        finish(); // Finaliza HomeActivity para que el usuario no pueda volver con el botón de atrás
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Usuario cerró sesión.");
    }


    void loadFragment(Fragment newFragment, String title) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        int enterAnim = 0;
        int exitAnim = 0;

        int currentTabIndex = getTabIndex(activeFragment);
        int newTabIndex = getTabIndex(newFragment);

        if (newTabIndex > currentTabIndex) {
            enterAnim = R.anim.slide_in_right;
            exitAnim = R.anim.slide_out_left;
        } else if (newTabIndex < currentTabIndex) {
            enterAnim = R.anim.slide_in_left;
            exitAnim = R.anim.slide_out_right;
        }

        if (enterAnim != 0 && exitAnim != 0) {
            transaction.setCustomAnimations(enterAnim, exitAnim);
        }

        transaction.replace(R.id.fragment_container, newFragment);
        transaction.commit();

        activeFragment = newFragment;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private int getTabIndex(Fragment fragment) {
        if (fragment instanceof HomeFragment) return 0;
        if (fragment instanceof CalendarFragment) return 1;
        if (fragment instanceof TareasFragment) return 2;
        if (fragment instanceof ProyectosFragment) return 3;
        return -1;
    }
}