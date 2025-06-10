package com.example.OrganizacionPersonal;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;

import com.google.api.services.calendar.model.EventDateTime;

import com.google.api.services.calendar.model.EventDateTime;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.metrics.Event;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
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
import com.kizitonwose.calendar.core.CalendarDay;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

        Switch switchVista = view.findViewById(R.id.switchVistaCalendario);
        FrameLayout contenedorKizitonwose = view.findViewById(R.id.contenedorKizitonwose);

        switchVista.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                if (isChecked) {
                    calendarView.setVisibility(View.GONE);
                    contenedorKizitonwose.setVisibility(View.VISIBLE);
                    cargarVistaMensual(contenedorKizitonwose);
                } else {
                    calendarView.setVisibility(View.VISIBLE);
                    contenedorKizitonwose.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                buttonView.setChecked(!isChecked); // Revertir el cambio si hay error
                Toast.makeText(getContext(), "Error al cambiar vista: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    public class DayViewContainer extends ViewContainer {
        final TextView textView;

        public DayViewContainer(@NonNull View view) {
            super(view);
            textView = view.findViewById(R.id.dayText);
        }
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
                            programarNotificacion(requireContext(), evento.getFecha(), "Recordatorio: " + evento.getTitulo());
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

    private void cargarVistaMensual(FrameLayout container) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View monthView = inflater.inflate(R.layout.calendar_month_view, container, false);
        container.removeAllViews();
        container.addView(monthView);

        com.kizitonwose.calendar.view.CalendarView calendarViewKiz = monthView.findViewById(R.id.calendarView);

        YearMonth currentCalendarMonth = YearMonth.now();

        calendarViewKiz.setDayViewResource(R.layout.calendar_day_view); // NUEVO

        calendarViewKiz.setup(
                YearMonth.now().minusMonths(12),
                YearMonth.now().plusMonths(12),
                java.time.DayOfWeek.MONDAY
        );

        calendarViewKiz.scrollToMonth(currentCalendarMonth);

        calendarViewKiz.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));

                container.getView().setOnClickListener(v -> {
                    //Date selectedDate = java.sql.Date.valueOf(String.valueOf(day.getDate()));
                    //fechaSeleccionada = selectedDate;
                    //cargarEventosPorFecha(fechaSeleccionada);
                    //Toast.makeText(getContext(), "Seleccionado: " + dateFormat.format(fechaSeleccionada), Toast.LENGTH_SHORT).show();
                    try {
                        // Convertir LocalDate a Date
                        LocalDate localDate = day.getDate();
                        Date selectedDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        fechaSeleccionada = selectedDate;
                        cargarEventosPorFecha(fechaSeleccionada);
                        Toast.makeText(getContext(), "Seleccionado: " + dateFormat.format(fechaSeleccionada), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("CalendarFragment", "Error al convertir fecha", e);
                    }
                });
            }
        });
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

    private void programarNotificacion(Context context, Date fecha, String mensaje) {
        Intent intent = new Intent(context, NotificacionReceiver.class);
        intent.putExtra("mensaje", mensaje);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, fecha.getTime(), pendingIntent);
        }
    }
}
