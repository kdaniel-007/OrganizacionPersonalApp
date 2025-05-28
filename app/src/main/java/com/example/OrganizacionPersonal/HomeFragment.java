package com.example.OrganizacionPersonal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // ¡Importa Button!
import android.widget.TextView;
import android.widget.Toast; // ¡Importa Toast!

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth; // ¡Importa FirebaseAuth!

public class HomeFragment extends Fragment {

    private TextView tvBienvenida;
    private Button btnLogout; // Declara el botón

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.inicio_view, container, false);

        tvBienvenida = view.findViewById(R.id.tvBienvenida);
        btnLogout = view.findViewById(R.id.btnLogout); // Inicializa el botón con su ID

        // (Opcional) Mostrar el email del usuario en el mensaje de bienvenida
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            tvBienvenida.setText("Bienvenido, " + (email != null ? email : "Usuario"));
        } else {
            tvBienvenida.setText("Bienvenido");
        }

        // Establece el Listener para el botón de cerrar sesión
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Cierra la sesión de Firebase

            Toast.makeText(getActivity(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirige al usuario de vuelta a MainActivity (la pantalla de login)
            Intent intent = new Intent(getActivity(), MainActivity.class);
            // Estas banderas son cruciales para limpiar la pila de actividades
            // y prevenir que el usuario regrese a HomeActivity con el botón 'atrás'
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            // Si quieres cerrar HomeActivity completamente (opcional, pero buena práctica aquí)
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}