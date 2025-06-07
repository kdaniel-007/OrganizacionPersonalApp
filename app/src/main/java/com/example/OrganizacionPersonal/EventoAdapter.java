package com.example.OrganizacionPersonal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    private List<Evento> eventos;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final Context context;
    private final OnEventoClickListener clickListener;
    private final Animation fadeInAnimation;

    public interface OnEventoClickListener {
        void onEventoClick(Evento evento);
        void onEventoLongClick(Evento evento);
    }

    public EventoAdapter(List<Evento> eventos, Context context, OnEventoClickListener clickListener) {
        this.eventos = eventos;
        this.context = context;
        this.clickListener = clickListener;
        this.fadeInAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = eventos.get(position);
        holder.tvTitulo.setText(evento.getTitulo());

        Date fecha = evento.getFecha();
        String textoDescripcion = evento.getDescripcion();
        if (fecha != null) {
            textoDescripcion += "\nFecha: " + dateFormat.format(fecha);
        }
        holder.tvDescripcion.setText(textoDescripcion);

        holder.itemView.setOnClickListener(v -> clickListener.onEventoClick(evento));

        holder.itemView.setOnLongClickListener(v -> {
            clickListener.onEventoLongClick(evento);
            return true;
        });

        holder.itemView.startAnimation(fadeInAnimation);
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    public void updateEventos(List<Evento> nuevosEventos) {
        this.eventos = nuevosEventos;
        notifyDataSetChanged();
    }

    public static class EventoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloEvento);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionEvento);
        }
    }
}
