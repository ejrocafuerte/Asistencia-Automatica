package com.asistencia.integradora.espol.emisor;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.asistencia.integradora.espol.cipher.Crypt;
import com.asistencia.integradora.espol.models.Profesor;
import com.asistencia.integradora.espol.models.ResponseServer;
import com.asistencia.integradora.espol.models.Usuario;
import com.asistencia.integradora.espol.retrofit.RestClient;
import com.asistencia.integradora.espol.retrofit.Connection;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class InitActivity extends AppCompatActivity {
    protected static Retrofit retrofit = null;
    protected static RestClient restClient = null;
    protected Profesor profesor=null;
    protected static Crypt crypto;
    private ArrayList<String> codigosServer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TextView txtUser = (TextView) findViewById(R.id.txtUser);
        final TextView txtPass = (TextView) findViewById(R.id.txtPass);
        setContentView(R.layout.activity_init);

        EmisorSQLHelper db1 = new EmisorSQLHelper(getApplicationContext(), "emisor.db", null, 1);
        SQLiteDatabase dbEmisor = db1.getWritableDatabase();
        Toast.makeText(this.getApplicationContext(), "Buscando datos de usuario", Toast.LENGTH_LONG);
        //si es diferente de null pregunto si tiene datos

        if (dbEmisor != null) {
            //dbEmisor.execSQL("insert into core_profesor values(1,\"201021839\",\"erick joel\",\"rocafuerte villon\",\"example@example.com\")");
            Cursor c = dbEmisor.rawQuery("select nombres, apellidos from core_profesor where nombres='erick joel'", null);
            c.moveToFirst();
            if (!c.getString(0).isEmpty()) {
                Intent intent = new Intent(this, MainActivity.class);
                this.startActivity(intent);
            } else {
                login();
            }
            System.out.println(c.getString(0).isEmpty());
            System.out.println("el nombre del profesor es: " + c.getString(0) + " " + c.getString(1));
        } else{
            dbEmisor.close();
            login();
        }

    }
    private void login(){
        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        final TextView txtUser = (TextView) findViewById(R.id.txtUser);
        final TextView txtPass = (TextView) findViewById(R.id.txtPass);
        Toast.makeText(this.getApplicationContext(), "No se encontraron datos, inicie sesion", Toast.LENGTH_LONG);
        findViewById(R.id.txtInit).setVisibility(View.GONE);
        findViewById(R.id.lyt_welcome).setVisibility(View.VISIBLE);
        findViewById(R.id.lyt_content).setVisibility(View.VISIBLE);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = txtUser.getText().toString();
                String pass = txtPass.getText().toString();
                if (user != null) {
                    if (pass != null) {
                        Usuario usr = new Usuario(user, pass);
                        sendMessage(usr);
                        
                    }
                }
            }
        });
    }

    private void sendMessage(Usuario usr) {
        if (retrofit == null) {
            Connection.init();
            restClient = Connection.createService(RestClient.class); //, username, password);
        }

        if (retrofit != null) {
            System.out.println(retrofit);
            final Call<ResponseServer> request = restClient.sendMessage(usr);
            request.enqueue(new Callback<ResponseServer>() {
                @Override
                public void onResponse(Call<ResponseServer> call, Response<ResponseServer> response) {
                    if (response.isSuccessful()) {
                        String rsp = response.body().getResponse();
                        switch (rsp) {
                            case "0": {
                                //codigosUser = response.body().getMessage();
                                Toast.makeText(getBaseContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                            }
                            case "1": {
                                profesor = response.body().getProfesor();
                                System.out.println(profesor);
                                Toast.makeText(getBaseContext(), "Usuario encontrado, descargando informacion", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call<ResponseServer> call, Throwable t) {
                    Toast.makeText(getBaseContext(), "Retrofit fail!", Toast.LENGTH_SHORT).show();
                }
            }
            );
        }
    }
}

