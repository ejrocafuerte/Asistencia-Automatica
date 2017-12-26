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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LobbyActivity extends AppCompatActivity {
    private static final String TAG = "LobbyActivity";

    private Context mContext;
    private NetworkConReceiver mNetworkConReceiver;
    private String mac = "";
    private String nombres = "";
    private String apellidos = "";
    private String matricula = "";
    private String materia = "CCG01INTEGRADORA";
    private String codigo = "0110101";
    private String fecha = "";
    private String delimitador = ";;";
    private String nombreArchivoEstudiante = "/InfoEstudiante";
    private String nombreArchivoAsistencia = "/Asistencia";
    private int REQUESTCODE_INFO = 1;
    private int REQUESTCODE_CAMERA = 2;
    private ArrayList<String> infoEstudiante;
    private ArrayList<String> infoAsistencia;
    private boolean existeInternet = false;
    private Button btn_enviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = this;

        fileManager(mContext);


        mac = getMacAddr();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH24:mm:ss");
        fecha = formatter.format(new Date());

        infoAsistencia.add("Christian Alejandro;;Jaramillo Espinoza;;200518124;;CCG01INTEGRADORA;;0110101");
        infoAsistencia.add("Carlos Javier;;Moran Espinoza;;200518124;;CCG01INTEGRADORA;;0110101");
        infoAsistencia.add("Anahi Thalia;;Jaramillo Espinoza;;200518124;;CCG01INTEGRADORA;;0110101");

        mNetworkConReceiver = new NetworkConReceiver();

        registerReceiver(mNetworkConReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        btn_enviar = (Button)findViewById(R.id.benviar);
        btn_enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //enviar asistencia servidor web
                if(existeInternet){
                    saveFile(mContext, infoAsistencia, nombreArchivoAsistencia);
                }
                //guardar info a archivo
                else{

                }
            }
        });

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
            infoAsistencia = readFile(context, nombreArchivoAsistencia);
            try {

                fileAsistencia.delete();
                fileAsistencia.createNewFile();
            } catch (IOException e) {
            }

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
                Log.e("saveFile", (infoAsistencia.get(i).toString() + "\n"));
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
                    Log.e("readFile: 4", receiveString);
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

    public class NetworkConReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMngr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = conMngr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if(wifi.isConnected()){
                existeInternet = true;
                Toast.makeText(context, "BroadcastReceiver: WIFI ON", Toast.LENGTH_SHORT).show();
            }else if(mobile.isConnected() && wifi.isConnected()){
                existeInternet = true;
                Toast.makeText(context, "BroadcastReceiver: MOBILE ON", Toast.LENGTH_SHORT).show();
            }else{
                existeInternet = false;
                Toast.makeText(context, "BroadcastReceiver: MOBILE OR WIFI OFF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
