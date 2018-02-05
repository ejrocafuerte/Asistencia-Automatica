package com.app.house.asistenciaestudiante;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

import models.Asistencia;
import models.CodigosServer;
import models.Estudiante;
import models.ResponseServer;
import models.Senal;
import models.Token;
import retrofit.Connection;
import retrofit.RestClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import cipher.Crypt;

public class LobbyActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "LobbyActivity";

    private Context mContext;
    private NetworkConReceiver mNetworkConReceiver;
    protected static Retrofit retrofit = null;
    protected static RestClient restClient = null;
    protected static Crypt crypto;
    private ArrayList<Asistencia> asistencias;
    private Asistencia asistenciaActual;
    private Estudiante estudiante;
    private ArrayList<Senal> senales;
    ArrayAdapter<Asistencia> adapter;

    private String mac = "";
    private String imei = "";
    private String nombres = "";
    private String apellidos = "";
    private String matricula = "";
    private String fecha = "";
    private String materia = "";
    private String codigo = "";
    private String distanciaX = "";
    private String distanciaY = "";

    private String delimitador = ";;";
    private String delimitadorNl = "**";
    private String nombreArchivoEstudiante = "/InfoEstudiante";
    private String nombreArchivoAsistencia = "/Asistencia";
    private String nombreArchivoParametros = "/Parametros";
    private int REQUESTCODE_INFO = 1;
    private int REQUESTCODE_CAMERA = 2;
    private ArrayList<String> infoEstudiante;
    private ArrayList<String> infoAsistencias;
    private String infoAsistenciaActual = "";
    private boolean existeInternet = false;
    private boolean enviandoServidor = false;
    private Button btn_enviar;
    private TextView txt_asistencias;
    private ArrayList<String> codigosServer;
    private int tamanioObj = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = this;
        crypto = Crypt.getInstance();

        asistencias = new ArrayList<Asistencia>();
        asistenciaActual = new Asistencia();
        estudiante = new Estudiante();
        senales = new ArrayList<Senal>();

        fileManager(mContext);

        asistenciaActual.setMac(getMacAddr());
        asistenciaActual.setImei(getImei());

        mNetworkConReceiver = new NetworkConReceiver();

        registerReceiver(mNetworkConReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        btn_enviar = (Button) findViewById(R.id.benviar);
        btn_enviar.setOnClickListener(this);


        if (retrofit == null) {
            Connection.init();
            restClient = Connection.createService(RestClient.class); //, username, password);
        }

        senales = scanWifiSignals();

        ListView asistenciasLV = (ListView)findViewById(R.id.listAsistenciasPendientes);
        adapter = new ArrayAdapter<Asistencia>(this, R.layout.simple_list_item, asistencias);

        if(asistencias.size() <= 0){
            asistenciasLV.setEmptyView((View)findViewById(R.id.emptyview));
        }

        asistenciasLV.setAdapter(adapter);

        showDialog(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.benviar: {
                if(validateInfo()) {
                    //enviar asistencia servidor web
                    if (existeInternet) {
                        Log.e(TAG, "Existe internet, enviando server");
                        sendServer();
                    }
                    //guardar info a archivo
                    else {
                        Toast.makeText(mContext, "Sin conexion a internet, guardando a archivo", Toast.LENGTH_SHORT).show();
                        //Log.e(TAG, "No se detecta conexion a internet, guardando a archivo");
                        //saveFile(mContext, asistencias, nombreArchivoAsistencia);
                    }
                }
            }
            default:
                break;
        }
    }

    private void sendServer() {

        if (restClient != null && !enviandoServidor) {

            Call<ResponseServer> request = restClient.sendServer(asistencias);
            enviandoServidor = true;
            request.enqueue(new Callback<ResponseServer>() {
                @Override
                public void onResponse(Call<ResponseServer> call, Response<ResponseServer> response) {
                    if (response.isSuccessful()) {
                        String rsp = response.body().getResponse();
                        Log.e("onResponse", rsp);

                        switch(rsp){
                            case "0": { //OK
                                if(asistencias!=null) asistencias.clear();
                                if(adapter!=null) adapter.notifyDataSetChanged();
                                _deleteFile(nombreArchivoAsistencia);
                                Toast.makeText(mContext, "Server OK",  Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Asistencias enviadas satisfactoriamente");
                            }
                            case "1": { //NOK
                                Toast.makeText(mContext, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Existen problemas al verificar asistencias: "+response.body().getMessage());
                            }
                        }
                    }
                    else{
                        saveFile(mContext, asistencias, nombreArchivoAsistencia);
                        Toast.makeText(mContext, "Server NOK saving file", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Server NOK saving file, "+response.body().getMessage());
                    }

                    enviandoServidor = false;
                }

                @Override
                public void onFailure(Call<ResponseServer> call, Throwable t) {
                    enviandoServidor = false;
                    Toast.makeText(mContext, "Retrofit fail!", Toast.LENGTH_SHORT).show();
                    Log.e("Retrofit fail!", t.getMessage() + " " + t.getLocalizedMessage());
                    //saveFile(mContext, asistencias, nombreArchivoAsistencia);
                }
            });        }
    }



    private String getAsistenciasMessage(ArrayList<Asistencia> asistencias) {
        if(asistencias.size() <= 0) return "";
        StringBuilder message = new StringBuilder("");
        for (int k = 0; k < asistencias.size(); k++) {
            Asistencia a = asistencias.get(k);

            if(a != null) {
                message.append(a.toStringtoFile());
                if (k < asistencias.size() - 1) {
                    message.append(delimitadorNl);
                }
            }
        }
        return message.toString();
    }

    private boolean validateInfo() {
        if (asistenciaActual.getMac().isEmpty()) {
            Toast.makeText(mContext, "Error: MAC", Toast.LENGTH_SHORT).show();
            return false;
        } else if (asistenciaActual.getImei().isEmpty()) {
            Toast.makeText(mContext, "Error: IMEI", Toast.LENGTH_SHORT).show();
            return false;
        } else if (estudiante.getNombres().isEmpty()) {
            Toast.makeText(mContext, "Error: Nombres", Toast.LENGTH_SHORT).show();
            return false;
        }else if (estudiante.getApellidos().isEmpty()) {
            Toast.makeText(mContext, "Error: Apellidos", Toast.LENGTH_SHORT).show();
            return false;
        } else if (estudiante.getMatricula().isEmpty()) {
            Toast.makeText(mContext, "Error: Matricula", Toast.LENGTH_SHORT).show();
            return false;
        } /*else if (asistenciaActual.getCodigo().isEmpty()) {
            Toast.makeText(mContext, "Error: Codigo", Toast.LENGTH_SHORT).show();
            return false;
        } else if (asistenciaActual.getDistanciaX().isEmpty()) {
            Toast.makeText(mContext, "Error: Distancia X", Toast.LENGTH_SHORT).show();
            return false;
        } else if (asistenciaActual.getDistanciaY().isEmpty()) {
            Toast.makeText(mContext, "Error: Distancia Y", Toast.LENGTH_SHORT).show();
            return false;
        } else if (asistenciaActual.getSenales().isEmpty()) {
            Toast.makeText(mContext, "Error: Senales", Toast.LENGTH_SHORT).show();
            return false;
        }*/ else
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
                    res1.append(String.format("%02X:", b));
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

    private String getImei() {
        return ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.camara:
                intent = new Intent(mContext, CameraActivity.class);
                intent.putExtra("Tamanio", tamanioObj);
                startActivityForResult(intent, REQUESTCODE_CAMERA);
                return true;
            case R.id.info: {
                intent = new Intent(mContext, InfoActivity.class);
                intent.putExtra("Estudiante", estudiante);
                intent.putExtra("Tamanio", tamanioObj);
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

        if (requestCode == REQUESTCODE_INFO) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    estudiante = (Estudiante)data.getSerializableExtra("Estudiante");
                    asistenciaActual.setEstudiante(estudiante);
                    tamanioObj = data.getIntExtra("Tamanio", 0);
                }
            }
        } else if (requestCode == REQUESTCODE_CAMERA) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    codigo = data.getStringExtra("codigo");
                    distanciaX = Double.toString(data.getDoubleExtra("distanciaX", 0.0));
                    distanciaY = Double.toString(data.getDoubleExtra("distanciaY", 0.0));
                        Log.e(TAG, "OnActivityResult, Codigo: " + codigo + " , distanceX: " + distanciaX + ", distanceY: " + distanciaY);


                        if (asistenciaActual.getSenales() == null || asistenciaActual.getSenales().size() <= 0)
                            senales = scanWifiSignals();

                        asistenciaActual.setCodigo(codigo);
                        int index = getAsistenciaByCodigo(asistenciaActual);

                        if (index >= 0) {
                            asistencias.get(index).setFecha(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                            asistencias.get(index).setEstudiante(estudiante);
                            asistencias.get(index).setCodigo(codigo);
                            asistencias.get(index).setDistanciaX(distanciaX);
                            asistencias.get(index).setDistanciaY(distanciaY);
                            asistencias.get(index).setSenales(senales);
                            Log.e("onActivityResult", "Asistencia encontrada en pos " + index + ", actualizando...: [" + codigo + ", " + distanciaX + ", " + distanciaY);
                            Toast.makeText(mContext, "Asistencia ya registrada, actualizando info!", Toast.LENGTH_SHORT).show();

                        } else {


                            asistenciaActual.setFecha(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                            asistenciaActual.setEstudiante(estudiante);
                            asistenciaActual.setCodigo(codigo);
                            asistenciaActual.setDistanciaX(distanciaX);
                            asistenciaActual.setDistanciaY(distanciaY);
                            asistenciaActual.setSenales(senales);
                            asistencias.add(asistenciaActual);
                            Log.e("onActivityResult", "Asitencia nueva agregada: [" + codigo + ", " + distanciaX + ", " + distanciaY);
                            Toast.makeText(mContext, "Asitencia nueva agregada!", Toast.LENGTH_SHORT).show();
                        }

                        saveFile(mContext, asistencias, nombreArchivoAsistencia);

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    //}
                }
            }
        }
    }

    @Override
    protected void onResume() {
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
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = conMngr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifi.isConnected()) {
                existeInternet = true;
                //sendServer();
                //Toast.makeText(context, "BroadcastReceiver: WIFI ON", Toast.LENGTH_SHORT).show();
            } else if (mobile.isConnected()) {
                existeInternet = true;
                wifiManager.setWifiEnabled(true);
                //wifiManager.setWifiEnabled(true);
                //Toast.makeText(context, "BroadcastReceiver: MOBILE ON", Toast.LENGTH_SHORT).show();
            } else {
                existeInternet = false;
                wifiManager.setWifiEnabled(true);
                //Toast.makeText(context, "BroadcastReceiver: MOBILE AND WIFI OFF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public ArrayList<Senal> scanWifiSignals() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }

        List<ScanResult> wifiList = wifiManager.getScanResults();
        StringBuilder sb = new StringBuilder("");
        ArrayList<Senal> senales = new ArrayList<>();

        for (int i = 0; i < wifiList.size(); i++) {
            ScanResult scanResult = wifiList.get(i);
            Senal senal = new Senal();
            senal.setBssid(scanResult.BSSID);
            senal.setSsid(scanResult.SSID);
            senal.setLevel(scanResult.level);
            senal.setLevel2(WifiManager.calculateSignalLevel(scanResult.level, 100));
            senales.add(senal);
        }
        return senales;
    }

    int getAsistenciaByCodigo(Asistencia asistenciaActual){
        if(asistenciaActual == null || asistencias == null || asistencias.size() <= 0) return -1;
        for (int i = 0; i < asistencias.size(); i++) {
            Asistencia a = asistencias.get(i);
            if (a.getCodigo().equals(asistenciaActual.getCodigo()) &&
                a.getMac().equals(asistenciaActual.getMac()) &&
                a.getEstudiante().getMatricula().equals(asistenciaActual.getEstudiante().getMatricula()))
                return i;
        }
        return -1;
    }

    void showDialog(Context context){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("1. Ingrese sus datos personales dando clic en el icono del lápiz y oprima guardar.\n\n" +
                "2. Luego abra el decodificador dando clic en el icono de la cámara, apunte a las " +
                "matrices hasta que la cruz roja este centrada con la matriz del medio.\n\n" +
                "3. Espere hasta que se decodifique la información y estime la ubicación.\n\n" +
                "4. De clic en 'Enviar Asistencia' para que su asistencia sea validada.");
        dialogBuilder.setTitle("Instrucciones:");
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void fileManager(Context context) {
        File fileAsistencia = new File(getFilesDir().getPath() + nombreArchivoAsistencia);
        File fileEstudiante = new File(getFilesDir().getPath() + nombreArchivoEstudiante);
        File fileParametros = new File(getFilesDir().getPath() + nombreArchivoParametros);

        if (!fileParametros.exists()) {
            try {
                fileParametros.createNewFile();
            } catch (IOException e) {
            }
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(getFilesDir().getPath() + nombreArchivoParametros);

                if (fileInputStream != null) {
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);//(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "", tam = "";
                    while ((receiveString = bufferedReader.readLine()) != null) {
                        tam += receiveString;
                    }
                    fileInputStream.close();
                    tamanioObj = Integer.valueOf(tam);
                }
            } catch (FileNotFoundException e) {
                tamanioObj = 0;
            } catch (IOException e) {
                tamanioObj = 0;
            }
        }
        if (!fileEstudiante.exists()) {
            try {
                fileEstudiante.createNewFile();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        } else {
            infoEstudiante = readFile(mContext, nombreArchivoEstudiante);

            if (infoEstudiante.size() > 0) {
                String[] info = infoEstudiante.get(0).split(delimitador);
                estudiante.setNombres(info[0]);
                estudiante.setApellidos(info[1]);
                estudiante.setMatricula(info[2]);
                asistenciaActual.setEstudiante(estudiante);
            }
        }

        if (!fileAsistencia.exists()) {
            try {
                fileAsistencia.createNewFile();
            } catch (IOException e) {
            }
        } else {
            infoAsistencias = readFile(context, nombreArchivoAsistencia);

            if (infoAsistencias.size() > 0) {
                for (String info : infoAsistencias) {
                    String[] parametros = info.split(delimitador);
                    Asistencia asistencia = new Asistencia();
                    asistencia.setMac(parametros[0]);
                    asistencia.setImei(parametros[1]);
                    Estudiante estudiante = new Estudiante();
                    estudiante.setNombres(parametros[2]);
                    estudiante.setApellidos(parametros[3]);
                    estudiante.setMatricula(parametros[4]);
                    asistencia.setEstudiante(estudiante);
                    asistencia.setFecha(parametros[5]);
                    asistencia.setMateria(parametros[6]);
                    asistencia.setCodigo(parametros[7]);
                    asistencia.setDistanciaX(parametros[8]);
                    asistencia.setDistanciaY(parametros[9]);
                    String[] senales = new String(parametros[10]).split("&&");

                    ArrayList<Senal> ls = new ArrayList<>();
                    for (String infosenal : senales) {
                        String[] paramsenal = new String(infosenal).split("==");
                        Senal s = new Senal();
                        s.setBssid(paramsenal[0]);
                        s.setSsid(paramsenal[1]);
                        s.setLevel(Integer.parseInt(paramsenal[2]));
                        s.setLevel2(Integer.parseInt(paramsenal[3]));
                        ls.add(s);
                    }
                    asistencia.setSenales(ls);
                    asistencias.add(asistencia);
                }
            }
        }
    }

    private void saveFile(Context context, ArrayList<Asistencia> asistencias, String filename) {
        if (asistencias == null || asistencias.size() <= 0) {
            Toast.makeText(context, "No existen asistencias", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(getFilesDir().getPath() + filename);//, true);
            for (int i = 0; i < asistencias.size(); i++) {
                fos.write((/*encrypt(*/asistencias.get(i).toStringtoFile() + "\n").getBytes());
            }

            Log.e(TAG, "Grabando a asistencias a archivo.");
            fos.close();

        } catch (IOException e) {
        }
    }

    private ArrayList readFile(Context context, String filename) {

        ArrayList<String> dataList = new ArrayList<String>();
        try {
            FileInputStream fileInputStream = new FileInputStream(getFilesDir().getPath() + filename);

            if (fileInputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);//(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                while ((receiveString = bufferedReader.readLine()) != null) {
                    dataList.add(/*crypto.decrypt_string(*/receiveString);
                }

                //Log.e("readFile2 " + filename, receiveString);
                fileInputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException", "File not found: " + e.toString());
            return dataList;
        } catch (IOException e) {
            Log.e("IOException", "Can not read file: " + e.toString());
            return dataList;
        }
        return dataList;
    }

    private void _deleteFile(String filename) {
        File fileAsistencia = new File(getFilesDir().getPath() + filename);
        if (fileAsistencia.exists()) {
            fileAsistencia.delete();
        }
    }

}
