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

public class ProyectosFragment extends Fragment {

    private ListView lvProyectos;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.proyectos_view, container, false);

        lvProyectos = view.findViewById(R.id.lvProyectos);
        loadProjects();

        return view;
    }

    private void loadProjects() {
        // Ejemplo: Lista de proyectos dummy
        List<String> projects = new ArrayList<>();
        projects.add("App Organización Personal (En progreso)");
        projects.add("Rediseño Web");
        projects.add("Capacitación equipo");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                projects
        );
        lvProyectos.setAdapter(adapter);
    }
}
