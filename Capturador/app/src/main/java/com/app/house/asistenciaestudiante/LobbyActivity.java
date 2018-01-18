package com.app.house.asistenciaestudiante;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
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

import models.Asistencia;
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
    private static Retrofit retrofit = null;
    private RestClient restClient = null;
    protected static Crypt crypto;

    //private ListaAsistencias asistencias;
    private ArrayList<Asistencia> asistencias;
    private Asistencia asistenciaActual;
    private Estudiante estudiante;
    private ArrayList<Senal> senales;

    private String mac = "";
    private String imei = "";
    private String nombres = "";
    private String apellidos = "";
    private String matricula = "";
    private String fecha = "";
    private String materia = "CCG01INTEGRADORA";
    private String codigo = "0110101";
    private String distanciaX = "0.0";
    private String distanciaY = "0.0";
    //private String senales = "";

    private String delimitador = ";;";
    private String delimitadorNl = "**";
    private String nombreArchivoEstudiante = "/InfoEstudiante";
    private String nombreArchivoAsistencia = "/Asistencia";
    private int REQUESTCODE_INFO = 1;
    private int REQUESTCODE_CAMERA = 2;
    private ArrayList<String> infoEstudiante;
    private ArrayList<String> infoAsistencias;
    private String infoAsistenciaActual = "";
    private boolean existeInternet = false;
    private boolean asistenciaEnlistada = false;
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

        asistencias = new ArrayList<Asistencia>();
        //asistencias = new ListaAsistencias();
        asistenciaActual = new Asistencia();
        estudiante = new Estudiante();
        senales = new ArrayList<Senal>();
        //_deleteFile(nombreArchivoEstudiante);
        //_deleteFile(nombreArchivoAsistencia);

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

        scanWifiSignals();

        txt_asistencias = (TextView) findViewById(R.id.lasistencias);
        txt_asistencias.setText(getAsistenciasMessage(asistencias));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.benviar: {
                if (validateInfo()) {
                        //infoAsistenciaActual = getAsistenciaActualMessage();
                    asistenciaActual.setFecha(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    asistenciaActual.setEstudiante(estudiante);
                    //asistencias.setAsistencias(asistenciaActual);

                    if(!asistenciaEnlistada) {
                        //infoAsistencias.add(infoAsistenciaActual);
                        asistencias.add(asistenciaActual);
                        Log.e("Existe internet, enviando server: ", getAsistenciasMessage(asistencias));
                        asistenciaEnlistada = true;
                    }
                    else{
                        //infoAsistencias.remove(infoAsistencias.size()-1);
                        //infoAsistencias.add(infoAsistenciaActual);
                        if(asistencias.size() > 0)
                            asistencias.remove(asistencias.size()-1);
                        asistencias.add(asistenciaActual);
                    }

                    //enviar asistencia servidor web
                    if (existeInternet) {
                        Log.e("Existe internet, enviando server: ", "3");
                        sendMessage(asistencias/*getAsistenciasMessage()*/);

                        Log.e("Existe internet, enviando server", infoAsistenciaActual);
                    }
                    //guardar info a archivo
                    else {
                        saveFile(mContext, asistencias, nombreArchivoAsistencia);
                    }
                }
                ;
            }
            default:
                break;
        }
    }

    /*private void sendMessage(final String msg) {

        if (restClient != null) {
            Call<String> request = restClient.sendMessage2(msg);

            request.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        ArrayList<Asistencia> rsp = response.body();
                        Log.e("onResponse", rsp.toString());
                        switch(rsp.toString()){
                            case "0":{ //OK
                                asistencias.clear();
                                //infoAsistenciaActual = "";
                                _deleteFile(nombreArchivoAsistencia);
                                Toast.makeText(mContext, "Server OK",
                                        Toast.LENGTH_SHORT).show();
                            }
                            case "1":{

                            }
                        }
                    }
                    else{
                        saveFile(mContext, asistencias, nombreArchivoAsistencia);
                        Toast.makeText(mContext, "Server NOK saving file...", Toast.LENGTH_SHORT).show();
                        Log.e("Server NOK saving file...", getAsistenciasMessage(asistencias));
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(mContext, "Retrofit fail!", Toast.LENGTH_SHORT).show();
                    Log.e("Retrofit fail!", t.getMessage());
                    saveFile(mContext, asistencias, nombreArchivoAsistencia);
                }
            });        }
    }*/

    private void sendMessage(final ArrayList<Asistencia> asistencias/*String asistenciasMessage*/) {

        if (restClient != null) {
            Call<ResponseServer> request = restClient.sendMessage(asistencias);

            request.enqueue(new Callback<ResponseServer>() {
                @Override
                public void onResponse(Call<ResponseServer> call, Response<ResponseServer> response) {
                    if (response.isSuccessful()) {
                        String rsp = response.body().getResponse();
                        Log.e("onResponse", rsp);
                        switch(rsp.toString()){
                            case "0":{ //OK
                                asistencias.clear();
                                //infoAsistenciaActual = "";
                                _deleteFile(nombreArchivoAsistencia);
                                Toast.makeText(mContext, "Server OK",
                                        Toast.LENGTH_SHORT).show();
                            }
                            case "1":{

                            }
                        }
                    }
                    else{
                        saveFile(mContext, asistencias, nombreArchivoAsistencia);
                        Toast.makeText(mContext, "Server NOK saving file...", Toast.LENGTH_SHORT).show();
                        Log.e("Server NOK saving file...", getAsistenciasMessage(asistencias));
                    }
                }

                @Override
                public void onFailure(Call<ResponseServer> call, Throwable t) {
                    Toast.makeText(mContext, "Retrofit fail!", Toast.LENGTH_SHORT).show();
                    Log.e("Retrofit fail!", t.getMessage() + " " + t.getLocalizedMessage() + " "+getAsistenciasMessage(asistencias));
                    saveFile(mContext, asistencias, nombreArchivoAsistencia);
                }
            });        }
    }

    private String getAsistenciasMessage(ArrayList<Asistencia> asistencias) {
        if(asistencias.size() <= 0) return "";
        StringBuilder message = new StringBuilder("");
        for (int k = 0; k < asistencias.size(); k++) {
            Asistencia a = asistencias.get(k);

            if(a != null) {
                message.append(a.toString());
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
        } else if (asistenciaActual.getCodigo().isEmpty()) {
            Toast.makeText(mContext, "Error: Codigo", Toast.LENGTH_SHORT).show();
            return false;
        } else if (asistenciaActual.getMateria().isEmpty()) {
            Toast.makeText(mContext, "Error: Materia", Toast.LENGTH_SHORT).show();
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
        } else
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

    private void fileManager(Context context) {
        File fileAsistencia = new File(getFilesDir().getPath() + nombreArchivoAsistencia);
        File fileEstudiante = new File(getFilesDir().getPath() + nombreArchivoEstudiante);

        if (!fileEstudiante.exists()) {
            try {
                fileEstudiante.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            infoEstudiante = readFile(mContext, nombreArchivoEstudiante);

            if (infoEstudiante.size() > 0) {
                String[] info = infoEstudiante.get(0).split(delimitador);
                estudiante.setNombres(info[0]);
                estudiante.setApellidos(info[1]);
                estudiante.setMatricula(info[2]);
                //nombres = info[0];
                //apellidos = info[1];
                //matricula = info[2];
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
                    Log.e("senales " , senales[0]);

                    ArrayList<Senal> ls = new ArrayList<>();
                    for (String infosenal : senales) {
                        String[] paramsenal = new String(infosenal).split("==");

                        Log.e("senalesc " , paramsenal[0]);
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
        if (asistencias.size() <= 0) {
            Toast.makeText(context, "No infoAsistencia", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(getFilesDir().getPath() + filename);//, true);

            /*for (int i = 0; i < infoAsistencia.size(); i++) {
                fos.write((encrypt(infoAsistencia.get(i).toString()) + "\n").getBytes());
                Log.e("saveFile " + filename, (infoAsistencia.get(i).toString() + "\n"));
            }*/
            for (int i = 0; i < asistencias.size(); i++) {
                fos.write((/*encrypt(*/asistencias.get(i).toString() + "\n").getBytes());
                //Log.e("saveFile " + filename, (infoAsistencia.get(i).toString() + "\n"));
                Log.e("saveFile " + filename, (asistencias.get(i).toString()) + "\n");
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

            if (fileInputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);//(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                while ((receiveString = bufferedReader.readLine()) != null) {
                    //stringBuilder.append(receiveString);
                    dataList.add(/*crypto.decrypt_string(*/receiveString);
                    Log.e("readFile2 " + filename, receiveString);
                }
                fileInputStream.close();
                //dataList = (ArrayList<String>)Arrays.asList(stringBuilder.toString().split("\\n"));
            }
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException", "File not found: " + e.toString());
            return dataList;
        } catch (IOException e) {
            Log.e("IOException", "Can not read file: " + e.toString());
            return dataList;
        }
        //Log.e("Read File", dataList.get(0).toString());
        return dataList;
    }

    private void _deleteFile(String filename) {
        File fileAsistencia = new File(getFilesDir().getPath() + filename);
        if (fileAsistencia.exists()) {
            fileAsistencia.delete();
                //fileAsistencia.createNewFile();
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
                intent.putExtra("Estudiante", estudiante);
                //intent.putExtra("intent_nombres", nombres);
                //intent.putExtra("intent_apellidos", apellidos);
                //intent.putExtra("intent_matricula", matricula);
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
                    //nombres = data.getStringExtra("intent_nombres");
                    //apellidos = data.getStringExtra("intent_apellidos");
                    // = data.getStringExtra("intent_matricula");
                    //Log.e("onActivityResult: ", "OK");
                }
            }
        } else if (requestCode == REQUESTCODE_CAMERA) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
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
                Toast.makeText(context, "BroadcastReceiver: MOBILE AND WIFI OFF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void scanWifiSignals() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> wifiList = wifiManager.getScanResults();
        StringBuilder sb = new StringBuilder("");
        senales.clear();

        for (int i = 0; i < wifiList.size(); i++) {
            ScanResult scanResult = wifiList.get(i);
            Senal senal = new Senal();
            senal.setBssid(scanResult.BSSID);
            senal.setSsid(scanResult.SSID);
            senal.setLevel(scanResult.level);
            senal.setLevel2(WifiManager.calculateSignalLevel(scanResult.level, 100));
            senales.add(senal);
            Log.e(TAG, "scanWifiSignals: " + senal.toString());
            /*sb.append(scanResult.BSSID)
                    .append("==")
                    .append(scanResult.level)
                    .append("==")
                    .append(WifiManager.calculateSignalLevel(scanResult.level, 100));
            if (i < wifiList.size() - 1) {
                sb.append("&&");
            }*/
        }

        asistenciaActual.setSenales(senales);
        //senales = sb.toString();

    }

    public static String encrypt(final String message) {
        try {
            String encryptMessage = crypto.encrypt_string(message);
            Log.e("encrypt", encryptMessage);
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
}

