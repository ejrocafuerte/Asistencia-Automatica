package com.asistencia.integradora.espol.emisor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class EstudianteListActivity extends AppCompatActivity {
    ArrayAdapter<String> estudiantesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estudiante_list);
        estudiantesAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        estudiantesAdapter.add("FILA 1");
        estudiantesAdapter.add("Erick Rocafuerte");
        estudiantesAdapter.add("Christian Jaramillo");
        estudiantesAdapter.add("David Castro");
        estudiantesAdapter.add("Hugo Verdesoto");
        estudiantesAdapter.add("Andres Torres");
        estudiantesAdapter.add("FILA 2");
        estudiantesAdapter.add("Carlos Vera");
        estudiantesAdapter.add("Pedro Fernandez");
        estudiantesAdapter.add("FILA 3");
        estudiantesAdapter.add("FILA 4");

        ListView estudiantesList = (ListView)findViewById(R.id.estudiantes);
        estudiantesList.setAdapter(estudiantesAdapter);
    }
}
