package com.example.OrganizacionPersonal;

import android.content.Intent;
import android.graphics.drawable.PictureDrawable; // Importación necesaria
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView; // Importación necesaria
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.caverock.androidsvg.SVG; // Importación necesaria
import com.caverock.androidsvg.SVGParseException; // Importación necesaria
import com.google.firebase.auth.FirebaseAuth;


public class HomeFragment extends Fragment {

    private TextView tvBienvenida;
    private Button btnLogout;
    private ImageView imageView; // Declaración de ImageView

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.inicio_view, container, false);

        tvBienvenida = view.findViewById(R.id.tvBienvenida);
        btnLogout = view.findViewById(R.id.btnLogout); // Inicializa el botón con su ID
        imageView = view.findViewById(R.id.imageView); // Inicializa el ImageView con su ID

        // Carga el SVG desde la carpeta de recursos 'res/raw'
        try {
            // ¡IMPORTANTE! Asegúrate de que tu archivo SVG se llame 'logo.svg' en la carpeta res/raw
            // Y que el nombre del recurso sea R.raw.logo
            SVG svg = SVG.getFromResource(getResources(), R.raw.logo); // <-- ¡Cambio clave aquí!
            if (svg != null) {
                // Puedes ajustar el tamaño del SVG si es necesario.
                // Por ejemplo, para que el SVG se ajuste al tamaño del ImageView,
                // no es necesario setDocumentWidth/Height si el ImageView ya tiene medidas fijas o wrap_content/match_parent.
                // Si el SVG es demasiado grande, o se ve mal, puedes probar:
                // svg.setDocumentViewBox(0, 0, svg.getDocumentWidth(), svg.getDocumentHeight()); // Restablece viewBox
                // svg.setDocumentWidth(imageView.getMeasuredWidth() > 0 ? imageView.getMeasuredWidth() : 500); // Ejemplo de ajuste
                // svg.setDocumentHeight(imageView.getMeasuredHeight() > 0 ? imageView.getMeasuredHeight() : 500); // Ejemplo de ajuste

                imageView.setImageDrawable(new PictureDrawable(svg.renderToPicture()));
            } else {
                Toast.makeText(getActivity(), "SVG es nulo, no se pudo cargar. Revisa el archivo 'logo.svg'.", Toast.LENGTH_LONG).show();
            }
        } catch (SVGParseException e) {
            // Manejar el error si el archivo SVG no se puede parsear (ej. formato inválido)
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error al parsear el logo SVG: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error desconocido al cargar SVG: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

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