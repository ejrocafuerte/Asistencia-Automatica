package com.app.house.asistenciaestudiante;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class InfoActivity extends AppCompatActivity {
    private Button btn_guardar;
    private TextView txt_nombres;
    private TextView txt_apellidos;
    private TextView txt_matricula;
    private File miArchivo;

    private String nombres;
    private String apellidos;
    private String matricula;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Información");
        setSupportActionBar(toolbar);

        btn_guardar = (Button) findViewById(R.id.bguardar);
        txt_nombres = (TextView) findViewById(R.id.tnombres);
        txt_apellidos = (TextView) findViewById(R.id.tapellidos);
        txt_matricula = (TextView) findViewById(R.id.tmatricula);

        btn_guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                nombres = txt_nombres.getText().toString().trim();
                apellidos = txt_apellidos.getText().toString().trim();
                matricula = txt_matricula.getText().toString().trim();
                miArchivo = new File(getFilesDir().getPath(),"asistencia-info.txt");


                if(nombres.length() <= 0){
                    Toast.makeText(v.getContext(), "Completar nombres", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(apellidos.length() <= 0){
                    Toast.makeText(v.getContext(), "Completar apellidos", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(matricula.length() <= 0){
                    Toast.makeText(v.getContext(), "Completar matricula", Toast.LENGTH_SHORT).show();
                    return;
                }

                String info = nombres + ";;" + apellidos + ";;" + matricula;

                try {
                    FileOutputStream fos = new FileOutputStream(miArchivo);
                    fos.write(info.getBytes());
                    fos.close();
                    Toast.makeText(v.getContext(), "Datos guardados", Toast.LENGTH_SHORT).show();
                    finishActivity(0);
                } catch (IOException e) {
                    Toast.makeText(v.getContext(), "IOException", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Intent intent = getIntent();

        if(intent != null) {
            txt_nombres.setText(intent.getStringExtra("intent_nombres"));
            txt_apellidos.setText(intent.getStringExtra("intent_apellidos"));
            txt_matricula.setText(intent.getStringExtra("intent_matricula"));
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        /*switch (item.getItemId()) {
            case R.id.new_game:
                newGame();
                return true;
            case R.id.help:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }*/
        return false;
    }



}
