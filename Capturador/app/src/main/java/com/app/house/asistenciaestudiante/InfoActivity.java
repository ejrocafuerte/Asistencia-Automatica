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

import models.Estudiante;

public class InfoActivity extends AppCompatActivity {
    private Context mContext;
    private Button btn_guardar;
    private Button btn_borrar;
    private TextView txt_nombres;
    private TextView txt_apellidos;
    private TextView txt_matricula;
    private TextView txt_tamanio;
    private File miArchivo;
    private File miArchivoParam;
    private String delimitador = ";;";

    private String nombres;
    private String apellidos;
    private String matricula;
    private String tamanio;
    private String nombreArchivoEstudiante = "InfoEstudiante";
    private String nombreArchivoParametros = "Parametros";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Información");
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mContext = this;
        btn_guardar = (Button) findViewById(R.id.bguardar);
        btn_borrar =  (Button) findViewById(R.id.btnborrarid);
        txt_nombres = (TextView) findViewById(R.id.tnombres);
        txt_apellidos = (TextView) findViewById(R.id.tapellidos);
        txt_matricula = (TextView) findViewById(R.id.tmatricula);
        txt_tamanio = (TextView) findViewById(R.id.ttamanio);

        btn_guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                nombres = txt_nombres.getText().toString().trim();
                apellidos = txt_apellidos.getText().toString().trim();
                matricula = txt_matricula.getText().toString().trim();
                tamanio = txt_tamanio.getText().toString().trim();
                miArchivo = new File(getFilesDir().getPath(), nombreArchivoEstudiante);
                miArchivoParam = new File(getFilesDir().getPath(), nombreArchivoParametros);


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

                Estudiante estudiante = new Estudiante(nombres, apellidos, matricula);

                try {
                    FileOutputStream fos = new FileOutputStream(miArchivo);
                    fos.write((/*LobbyActivity.encrypt(*/estudiante.toString()).getBytes());
                    fos.close();
                } catch (IOException e) {
                    //setResult(InfoActivity.RESULT_CANCELED, null);
                    Toast.makeText(v.getContext(), "IOException", Toast.LENGTH_SHORT).show();
                }

                try {
                    FileOutputStream fos = new FileOutputStream(miArchivoParam);
                    fos.write((String.valueOf(tamanio)).getBytes());
                    fos.close();

                } catch (IOException e) {
                    Toast.makeText(v.getContext(), "IOException", Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(mContext, LobbyActivity.class);
                intent.putExtra("Estudiante", estudiante);
                //Log.e("InfoActivity: tamanio: ", tamanio);
                intent.putExtra("Tamanio", Integer.valueOf(tamanio));
                setResult(LobbyActivity.RESULT_OK, intent);
                finish();

            }
        });

        btn_borrar.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              _deleteFile("/Asistencia");
          }
      });

        Intent intent = getIntent();

        if(intent != null) {
            Estudiante estudiante = (Estudiante)intent.getSerializableExtra("Estudiante");
            txt_nombres.setText(estudiante.getNombres());
            txt_apellidos.setText(estudiante.getApellidos());
            txt_matricula.setText(estudiante.getMatricula());
            txt_tamanio.setText(String.valueOf(intent.getIntExtra("Tamanio", 0)));
        }
    }
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }


    private void _deleteFile(String filename) {
        File fileAsistencia = new File(getFilesDir().getPath() + filename);
        if (fileAsistencia.exists()) {
            fileAsistencia.delete();
        }
    }
}
