package com.example.OrganizacionPersonal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView recyclerViewEventos;
    private LinearLayout emptyStateLayout;
    private TextView tvFechaSeleccionada;

    private FloatingActionButton fabExpandir, fabAgregarEvento, fabNotaVoz;
    private boolean isFabOpen = false;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;

    private EventoAdapter eventoAdapter;
    private List<Evento> listaEventos;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private CollectionReference eventosRef;

    private Date fechaSeleccionada;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calendar_view, container, false);

        calendarView = view.findViewById(R.id.cvCalendario);
        recyclerViewEventos = view.findViewById(R.id.recyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);

        fabExpandir = view.findViewById(R.id.fabExpandir);
        fabAgregarEvento = view.findViewById(R.id.fabAgregarEvento);
        fabNotaVoz = view.findViewById(R.id.fabNotaVoz);

        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        listaEventos = new ArrayList<>();
        eventoAdapter = new EventoAdapter(listaEventos, requireContext(), new EventoAdapter.OnEventoClickListener() {
            @Override
            public void onEventoClick(Evento evento) {
                mostrarDialogoEditarEvento(evento);
            }

            @Override
            public void onEventoLongClick(Evento evento) {
                mostrarDialogoEditarEliminar(evento);
            }
        });

        recyclerViewEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEventos.setAdapter(eventoAdapter);

        fechaSeleccionada = new Date(calendarView.getDate());
        cargarEventosPorFecha(fechaSeleccionada);

        calendarView.setOnDateChangeListener((viewCalendar, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            fechaSeleccionada = cal.getTime();
            cargarEventosPorFecha(fechaSeleccionada);
        });

        fabExpandir.setOnClickListener(v -> toggleFabMenu());
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento());
        fabNotaVoz.setOnClickListener(v -> startActivity(new Intent(getActivity(), VoiceNoteActivity.class)));

        return view;
    }

    private void toggleFabMenu() {
        if (isFabOpen) {
            fabAgregarEvento.startAnimation(fabClose);
            fabNotaVoz.startAnimation(fabClose);
            fabExpandir.startAnimation(rotateBackward);
            fabAgregarEvento.setClickable(false);
            fabNotaVoz.setClickable(false);
            fabAgregarEvento.setVisibility(View.GONE);
            fabNotaVoz.setVisibility(View.GONE);
            isFabOpen = false;
        } else {
            fabAgregarEvento.setVisibility(View.VISIBLE);
            fabNotaVoz.setVisibility(View.VISIBLE);
            fabAgregarEvento.startAnimation(fabOpen);
            fabNotaVoz.startAnimation(fabOpen);
            fabExpandir.startAnimation(rotateForward);
            fabAgregarEvento.setClickable(true);
            fabNotaVoz.setClickable(true);
            isFabOpen = true;
        }
    }

    private void cargarEventosPorFecha(Date fecha) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Inicia sesión para ver eventos", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        eventosRef = firestore.collection("users").document(uid).collection("eventos");

        Date inicioDia = obtenerInicioDelDia(fecha);
        Date finDia = obtenerFinDelDia(fecha);

        tvFechaSeleccionada.setText("Eventos del " + dateFormat.format(fecha));

        eventosRef
                .whereGreaterThanOrEqualTo("fecha", inicioDia)
                .whereLessThan("fecha", finDia)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaEventos.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Evento evento = doc.toObject(Evento.class);
                            evento.setId(doc.getId());
                            listaEventos.add(evento);
                        }
                        eventoAdapter.updateEventos(listaEventos);

                        emptyStateLayout.setVisibility(listaEventos.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerViewEventos.setVisibility(listaEventos.isEmpty() ? View.GONE : View.VISIBLE);
                    } else {
                        Toast.makeText(getContext(), "Error al cargar eventos: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarDialogoAgregarEvento() {
        FragmentManager fragmentManager = getParentFragmentManager();
        DialogAgregarEventoFragment dialog = DialogAgregarEventoFragment.newInstance(fechaSeleccionada);
        dialog.setOnEventoGuardadoListener(() -> cargarEventosPorFecha(fechaSeleccionada));
        dialog.show(fragmentManager, "DialogAgregarEventoFragment");
    }

    private void mostrarDialogoEditarEliminar(Evento evento) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Selecciona una opción")
                .setItems(new String[]{"Editar", "Eliminar"}, (dialog, which) -> {
                    if (which == 0) mostrarDialogoEditarEvento(evento);
                    else eliminarEvento(evento);
                }).show();
    }

    private void mostrarDialogoEditarEvento(Evento evento) {
        FragmentManager fragmentManager = getParentFragmentManager();
        DialogAgregarEventoFragment dialog = DialogAgregarEventoFragment.newInstance(evento);
        dialog.setOnEventoGuardadoListener(() -> cargarEventosPorFecha(fechaSeleccionada));
        dialog.show(fragmentManager, "DialogEditarEventoFragment");
    }

    private void eliminarEvento(Evento evento) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .collection("eventos")
                .document(evento.getId())
                .delete()
                .addOnSuccessListener(unused -> cargarEventosPorFecha(fechaSeleccionada))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private Date obtenerInicioDelDia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date obtenerFinDelDia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}
