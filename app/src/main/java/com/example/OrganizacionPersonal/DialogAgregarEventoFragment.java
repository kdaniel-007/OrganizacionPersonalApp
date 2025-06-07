package com.example.OrganizacionPersonal;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class DialogAgregarEventoFragment extends DialogFragment {

    private static final String ARG_EVENTO = "evento";
    private static final String ARG_FECHA = "fecha";
    private Evento eventoEditar;
    private Date fechaSeleccionada;

    private OnEventoGuardadoListener listener;

    public interface OnEventoGuardadoListener {
        void onEventoGuardado();
    }

    public void setOnEventoGuardadoListener(OnEventoGuardadoListener listener) {
        this.listener = listener;
    }

    public static DialogAgregarEventoFragment newInstance(Date fecha) {
        DialogAgregarEventoFragment fragment = new DialogAgregarEventoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FECHA, fecha);
        fragment.setArguments(args);
        return fragment;
    }

    public static DialogAgregarEventoFragment newInstance(Evento evento) {
        DialogAgregarEventoFragment fragment = new DialogAgregarEventoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENTO, evento);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_evento, null);
        EditText etTitulo = view.findViewById(R.id.etTituloEvento);
        EditText etDescripcion = view.findViewById(R.id.etDescripcionEvento);

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_EVENTO)) {
                eventoEditar = (Evento) getArguments().getSerializable(ARG_EVENTO);
                etTitulo.setText(eventoEditar.getTitulo());
                etDescripcion.setText(eventoEditar.getDescripcion());
            } else if (getArguments().containsKey(ARG_FECHA)) {
                fechaSeleccionada = (Date) getArguments().getSerializable(ARG_FECHA);
            }
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(eventoEditar != null ? "Editar Evento" : "Nuevo Evento")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (titulo.isEmpty()) {
                        Toast.makeText(getContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    if (eventoEditar != null) {
                        eventoEditar.setTitulo(titulo);
                        eventoEditar.setDescripcion(descripcion);
                        db.collection("users")
                                .document(uid)
                                .collection("eventos")
                                .document(eventoEditar.getId())
                                .set(eventoEditar)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(getContext(), "Evento actualizado", Toast.LENGTH_SHORT).show();
                                    if (listener != null) listener.onEventoGuardado();
                                });
                    } else {
                        Evento nuevo = new Evento(titulo, descripcion, fechaSeleccionada);
                        db.collection("users")
                                .document(uid)
                                .collection("eventos")
                                .add(nuevo)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(getContext(), "Evento creado", Toast.LENGTH_SHORT).show();
                                    if (listener != null) listener.onEventoGuardado();
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();
    }
}
