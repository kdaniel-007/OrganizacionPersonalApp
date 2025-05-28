package com.example.OrganizacionPersonal;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class TareasFragment extends Fragment {

    private ListView listaTareas;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tareas_view, container, false);

        listaTareas = view.findViewById(R.id.listaTareas);
        loadTasks();

        return view;
    }

    private void loadTasks() {
        // Ejemplo: Lista de tareas dummy
        List<String> tasks = new ArrayList<>();
        tasks.add("Entregar informe (Alta prioridad)");
        tasks.add("Comprar materiales");
        tasks.add("Llamar a cliente");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                tasks
        );
        listaTareas.setAdapter(adapter);
    }
}
