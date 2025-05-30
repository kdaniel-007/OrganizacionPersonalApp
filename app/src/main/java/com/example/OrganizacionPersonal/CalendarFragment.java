package com.example.OrganizacionPersonal;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Toast; // Añadir import para Toast

import androidx.annotation.NonNull; // Importar para @NonNull
import androidx.annotation.Nullable; // Importar para @Nullable
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class CalendarFragment extends Fragment {

    private CalendarView cvCalendario;
    private long fechaSeleccionada;

    public CalendarFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calendar_view, container, false); // Asegúrate de que el layout sea 'calendar_view'

        cvCalendario = view.findViewById(R.id.cvCalendario);
        fechaSeleccionada = System.currentTimeMillis(); // Fecha por defecto al día actual

        // Listener para cuando el usuario selecciona una fecha
        cvCalendario.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            java.util.Calendar calendario = java.util.Calendar.getInstance();
            calendario.set(year, month, dayOfMonth);
            fechaSeleccionada = calendario.getTimeInMillis();
            Toast.makeText(getContext(), "Fecha seleccionada: " + dayOfMonth + "/" + (month + 1) + "/" + year, Toast.LENGTH_SHORT).show();
        });

        // Botón para agregar evento al calendario del dispositivo
        view.findViewById(R.id.btn_add_event).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaSeleccionada)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fechaSeleccionada + (60 * 60 * 1000)) // Añade 1 hora por defecto
                    .putExtra(CalendarContract.Events.TITLE, "Nuevo evento de " + getString(R.string.app_name)) // Usa string resource
                    .putExtra(CalendarContract.Events.DESCRIPTION, "Evento desde mi organizador personal.")
                    .putExtra(CalendarContract.Events.ALL_DAY, false); // No es evento de todo el día

            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No hay aplicación de calendario para agregar el evento.", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para grabar nota de voz
        view.findViewById(R.id.btn_grabar_nota).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VoiceNoteActivity.class);
            startActivity(intent);
            // Aplicar la animación a la Activity que contiene este Fragment al salir
            if (getActivity() != null) {
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left); // Aplica tu animación
            }
        });

        return view;
    }
}