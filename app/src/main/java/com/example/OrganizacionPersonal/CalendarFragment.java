package com.example.OrganizacionPersonal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class CalendarFragment extends Fragment {

    private CalendarView cvCalendario;
    private long fechaSeleccionada;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calendar_view, container, false);

        cvCalendario = view.findViewById(R.id.cvCalendario);
        fechaSeleccionada = System.currentTimeMillis(); // Fecha por defecto

        cvCalendario.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            java.util.Calendar calendario = java.util.Calendar.getInstance();
            calendario.set(year, month, dayOfMonth);
            fechaSeleccionada = calendario.getTimeInMillis();
        });

        view.findViewById(R.id.btn_add_event).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaSeleccionada)
                    .putExtra(android.provider.CalendarContract.Events.TITLE, "Nuevo evento");
            // Nota: No aplicamos overridePendingTransition aquí porque es una Intent implícita
            startActivity(intent);
        });

        view.findViewById(R.id.btn_grabar_nota).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VoiceNoteActivity.class);
            startActivity(intent);
            // Aplicar la animación a la Activity que contiene este Fragment
            // getHost() puede ser null si el fragmento no está adjunto, getActivity() es más seguro.
            if (getActivity() != null) {
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        return view;
    }
}