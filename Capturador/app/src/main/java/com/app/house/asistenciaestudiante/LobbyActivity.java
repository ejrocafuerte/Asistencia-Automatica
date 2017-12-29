package com.app.house.asistenciaestudiante;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import retrofit.Connection;
import retrofit.RestClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import cipher.Crypt;

public class LobbyActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "LobbyActivity";

    private Context mContext;
    private NetworkConReceiver mNetworkConReceiver;
    private static Retrofit retrofit = null;
    private RestClient restClient = null;
    private Crypt crypto;

    private String mac = "";
    private String nombres = "";
    private String apellidos = "";
    private String matricula = "";
    private String fecha = "";
    private String materia = "CCG01INTEGRADORA";
    private String codigo = "0110101";
    private String distanciaX = "0.0";
    private String distanciaY = "0.0";

    private String delimitador = ";;";
    private String delimitadorNl = "**";
    private String nombreArchivoEstudiante = "/InfoEstudiante";
    private String nombreArchivoAsistencia = "/Asistencia";
    private int REQUESTCODE_INFO = 1;
    private int REQUESTCODE_CAMERA = 2;
    private ArrayList<String> infoEstudiante;
    private ArrayList<String> infoAsistencias;
    private String infoAsistenciaActual;
    private boolean existeInternet = false;
    private Button btn_enviar;
    private TextView txt_asistencias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = this;
        crypto = Crypt.getInstance();

        fileManager(mContext);

        mac = getMacAddr();

        fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        //infoAsistencia.add("Christian A;;Jaramillo E;;200518124;;CCG01INTEGRADORA;;0110101");
        //infoAsistencia.add("CJ;;ME;;200518124;;CCG01INTEGRADORA;;0110101");
        //infoAsistencia.add("AT;;JE;;200518124;;CCG01INTEGRADORA;;0110101");




        mNetworkConReceiver = new NetworkConReceiver();

        registerReceiver(mNetworkConReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        btn_enviar = (Button)findViewById(R.id.benviar);
        btn_enviar.setOnClickListener(this);

        if(retrofit == null){
            Connection.init();
            restClient = Connection.createService(RestClient.class); //, username, password);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.benviar:{
                if(validateInfo()){

                    infoAsistenciaActual = getAsistenciaActualMessage();
                    infoAsistencias.add(infoAsistenciaActual);

                    //enviar asistencia servidor web
                    if(existeInternet){
                        sendMessage(getAsistenciasMessage());
                        infoAsistencias.clear();
                        _deleteFile(nombreArchivoAsistencia);
                        Toast.makeText(mContext, "Asistencias enviadas con exito",
                                Toast.LENGTH_SHORT).show();

                        Log.e("ENVIANDO", getAsistenciaActualMessage());
                    }
                    //guardar info a archivo
                    else{
                        saveFile(mContext, infoAsistencias, nombreArchivoAsistencia);
                    }
                };
            }
            default: break;
        }
    }

    private void sendMessage(String asistenciasMessage) {

        if(restClient != null){
            Log.e("sendMessage", asistenciasMessage);
            Call<String> request = restClient.sendMessage(encrypt(asistenciasMessage));

            request.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if(response.isSuccessful()){
                        infoAsistencias.clear();
                        _deleteFile(nombreArchivoAsistencia);
                        Toast.makeText(mContext, response.body(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                }
            });
        }
    }

    private String getAsistenciasMessage() {
        if(infoAsistencias.size() <= 0) return "";
        StringBuilder message = new StringBuilder("");
        for(int k = 0; k < infoAsistencias.size(); k++){
            message.append(infoAsistencias.get(k));
            if(k < infoAsistencias.size() - 1){
                message.append(delimitadorNl);
            }
        }
        return message.toString();
    }

    private String getAsistenciaActualMessage() {
        StringBuilder sb = new StringBuilder("");
        sb.append(mac).append(delimitador)
            .append(nombres).append(delimitador)
            .append(apellidos).append(delimitador)
            .append(matricula).append(delimitador)
            .append(fecha).append(delimitador)
            .append(materia).append(delimitador)
            .append(codigo).append(delimitador)
            .append(distanciaX).append(delimitador)
            .append(distanciaY);
        return sb.toString();
    }

    private boolean validateInfo() {
        if(mac.isEmpty()){
            Toast.makeText(mContext, "Error: MAC", Toast.LENGTH_SHORT).show();
            return false;
        }else if(nombres.isEmpty()){
            Toast.makeText(mContext, "Error: Nombres", Toast.LENGTH_SHORT).show();
            return false;
        }else if(apellidos.isEmpty()){
            Toast.makeText(mContext, "Error: Apellidos", Toast.LENGTH_SHORT).show();
            return false;
        }else if(matricula.isEmpty()){
            Toast.makeText(mContext, "Error: Matricula", Toast.LENGTH_SHORT).show();
            return false;
        }else if(fecha.isEmpty()){
            Toast.makeText(mContext, "Error: Fecha", Toast.LENGTH_SHORT).show();
            return false;
        }else if(codigo.isEmpty()){
            Toast.makeText(mContext, "Error: Codigo", Toast.LENGTH_SHORT).show();
            return false;
        }else if(materia.isEmpty()) {
            Toast.makeText(mContext, "Error: Materia", Toast.LENGTH_SHORT).show();
            return false;
        }else if(distanciaX.isEmpty()){
            Toast.makeText(mContext, "Error: Distancia X", Toast.LENGTH_SHORT).show();
            return false;
        }else if(distanciaY.isEmpty()){
            Toast.makeText(mContext, "Error: Distancia Y", Toast.LENGTH_SHORT).show();
            return false;
        }else
            return true;
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "";
    }

    private void fileManager(Context context){
        File fileAsistencia = new File(getFilesDir().getPath() + nombreArchivoAsistencia);
        File fileEstudiante = new File(getFilesDir().getPath() + nombreArchivoEstudiante);

        if (!fileEstudiante.exists()) {
            try {
                fileEstudiante.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            infoEstudiante = readFile(mContext, nombreArchivoEstudiante);

            if(infoEstudiante.size() > 0) {
                String[] info = infoEstudiante.get(0).split(delimitador);

                nombres = info[0];
                apellidos = info[1];
                matricula = info[2];
            }
        }

        if (!fileAsistencia.exists()) {
            try {
                fileAsistencia.createNewFile();
            } catch (IOException e) {
            }
        }else{
            infoAsistencias = readFile(context, nombreArchivoAsistencia);
            txt_asistencias = (TextView)findViewById(R.id.lasistencias);
            txt_asistencias.setText(getAsistenciasMessage());
            /*try {

                //fileAsistencia.delete();
                //fileAsistencia.createNewFile();
            } catch (IOException e) {
            }*/

        }
    }

    private void saveFile(Context context, ArrayList<String> infoAsistencia, String filename){
        if(infoAsistencia.size() <= 0){
            Toast.makeText(context, "No infoAsistencia", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(getFilesDir().getPath() + filename);//, true);

            for(int i = 0;  i < infoAsistencia.size(); i++) {
                fos.write((infoAsistencia.get(i).toString() + "\n").getBytes());
                Log.e("saveFile "+filename, (infoAsistencia.get(i).toString() + "\n"));
            }
            fos.close();
            Toast.makeText(context, "Datos guardados", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(context, "saveFile IOException", Toast.LENGTH_SHORT).show();
        }
    }
    private ArrayList readFile(Context context, String filename) {

        ArrayList<String> dataList = new ArrayList<String>();
        try {
            //InputStream inputStream = context.openFileInput("asistencia-config.txt");
            FileInputStream fileInputStream = new FileInputStream(getFilesDir().getPath() + filename);

            if ( fileInputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);//(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    //stringBuilder.append(receiveString);
                    dataList.add(receiveString);
                    Log.e("readFile " + filename, receiveString);
                }
                fileInputStream.close();
                //dataList = (ArrayList<String>)Arrays.asList(stringBuilder.toString().split("\\n"));
            }
        }
        catch (FileNotFoundException e) {
            Log.e("FileNotFoundException", "File not found: " + e.toString());
            return dataList;
        } catch (IOException e) {
            Log.e("IOException", "Can not read file: " + e.toString());
            return dataList;
        }

        return dataList;
    }
    private void _deleteFile(String filename){
        File fileAsistencia = new File(getFilesDir().getPath() + filename);
        if (fileAsistencia.exists()) {
            try {
                fileAsistencia.delete();
                fileAsistencia.createNewFile();
            } catch (IOException e) {
            }
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
        switch (item.getItemId()) {
            case R.id.camara:
                startActivityForResult(new Intent(mContext, CameraActivity.class), REQUESTCODE_CAMERA);
                return true;
            case R.id.info: {
                Intent intent = new Intent(mContext, InfoActivity.class);
                intent.putExtra("intent_nombres", nombres);
                intent.putExtra("intent_apellidos", apellidos);
                intent.putExtra("intent_matricula", matricula);
                startActivityForResult(intent, REQUESTCODE_INFO);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(LobbyActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUESTCODE_INFO) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    nombres = data.getStringExtra("intent_nombres");
                    apellidos = data.getStringExtra("intent_apellidos");
                    matricula = data.getStringExtra("intent_matricula");
                    //Log.e("onActivityResult: ", "OK");
                }
            }
        }
        else if(requestCode == REQUESTCODE_CAMERA) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                }
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mNetworkConReceiver == null)
            registerReceiver(mNetworkConReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNetworkConReceiver != null) {
            unregisterReceiver(mNetworkConReceiver);
            mNetworkConReceiver = null;
        }
    }

    public String encrypt(final String message){
        try {
            String encryptMessage = crypto.encrypt_string(message);
            Log.e("Encrypt Message", encryptMessage);
            return encryptMessage;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public class NetworkConReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMngr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = conMngr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifi.isConnected()) {
                existeInternet = true;
                //sendMessage(getAsistenciasMessage());
                Toast.makeText(context, "BroadcastReceiver: WIFI ON", Toast.LENGTH_SHORT).show();
            } else if (mobile.isConnected()/* && wifi.isConnected()*/) {
                existeInternet = true;
                Toast.makeText(context, "BroadcastReceiver: MOBILE ON", Toast.LENGTH_SHORT).show();
            } else {
                existeInternet = false;
                Toast.makeText(context, "BroadcastReceiver: MOBILE OR WIFI OFF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
