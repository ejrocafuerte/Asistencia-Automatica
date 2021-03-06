package com.asistencia.integradora.espol.emisor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.asistencia.integradora.espol.cipher.Crypt;
import com.asistencia.integradora.espol.models.Asistencia;
import com.asistencia.integradora.espol.models.Profesor;
import com.asistencia.integradora.espol.models.Senal;
import com.asistencia.integradora.espol.retrofit.Connection;
import com.asistencia.integradora.espol.retrofit.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import retrofit2.Retrofit;

public class MainActivity extends Activity {
    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_DEVICE_TO_CONNECT = 4;
    private static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static Retrofit retrofit = null;
    protected static RestClient restClient = null;
    protected static Crypt crypto;
    BluetoothConn conexion;
    public BluetoothAdapter mBluetoothAdapter;

    String mensaje="";
    private Profesor profesor;
    private Asistencia asistenciaActual;
    private ArrayList<Senal> senales;


    private String mac = "";
    private String imei = "";
    private String nombres = "";
    private String apellidos = "";
    private String matricula = "";
    private String fecha = "";
    private String materia = "";
    private String codigo = "";
    private String distanciaX = "0";
    private String distanciaY = "0";

    private String delimitador = ";;";
    private String delimitadorNl = "**";
    private String nombreArchivoEstudiante = "/Info";
    private String nombreArchivoAsistencia = "/Asistencia";
    private ArrayList<String> infoProfesor;
    private String infoAsistenciaActual = "";
    private boolean existeInternet = false;
    private boolean asistenciaEnlistada = false;
    private ArrayList<String> codigosServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String id_materia = null, numero_paralelo = null, id_prof = null;
        super.onCreate(savedInstanceState);
        crypto = Crypt.getInstance();
        asistenciaActual = new Asistencia();
        profesor = new Profesor();
        senales = new ArrayList<Senal>();

        //asistenciaActual.setMac(getMacAddr());
        //asistenciaActual.setImei(getImei());

        if (retrofit == null) {
            Connection.init();
            restClient = Connection.createService(RestClient.class); //, username, password);
        }
        EmisorSQLHelper db1 = new EmisorSQLHelper(getApplicationContext(), "emisor.db", null, 1);
        SQLiteDatabase dbEmisor = db1.getWritableDatabase();
        //  CONSULTANDO DATOS DE PROFESOR
        Cursor c = dbEmisor.rawQuery("select identificador from core_profesor where nombres='erick joel'", null);
        c.moveToFirst();
        if (!c.getString(0).isEmpty()) {
            id_prof = c.getString(0);
            //corregimos el string de idprofesor conforme se debe mostrar en matriz
            if(id_prof.length()<4){
                if(id_prof.length()<2){
                    String resultado = "000";
                    resultado="000".concat(id_prof);
                    id_prof=resultado;
                }
                if(id_prof.length()<3){
                    String resultado = "00";
                    resultado= "00".concat(id_prof);
                    id_prof=resultado;
                }
                if(id_prof.length()<4){
                    String resultado = "0";
                    resultado="0".concat(id_prof);
                    id_prof=resultado;
                }
            }
        }
        //CONSULTO DATOS DE PARALELO que toca hoy ahi consigo idprofesor id materia id aula
        int dia = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int rangoHora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String aComparar="";
        switch (dia) {
            case 2:
                aComparar = "LUN";
                break;
            case 3:
                aComparar = "MAR";
                break;
            case 4:
                aComparar = "MIE";
                break;
            case 5:
                aComparar = "JUE";
                break;
            case 6:
                aComparar = "VIE";
                break;
            case 7:
                aComparar = "SAB";
                break;
        }
        c = dbEmisor.rawQuery("select id_materia_id, numero_paralelo from core_paralelo ",null);
        c = dbEmisor.rawQuery("select id_materia_id, numero_paralelo from core_paralelo where (dia1='"+aComparar+"'"+" or dia2='"+aComparar+"'"+" or dia3='"+aComparar+"')",null);
        c.moveToFirst();
        if (!c.getString(0).isEmpty()) {
            id_materia = c.getString(0);
            numero_paralelo = c.getString(1);
            if(numero_paralelo.length()<2){
                String resultado = "";
                resultado= "0".concat(numero_paralelo);
                numero_paralelo=resultado;
            }
        } else {
            Toast.makeText(this, "Error fatal",Toast.LENGTH_LONG).show();
        }
        //el id de la materia a mostrar es el registro que muestro en matriz
        //aula siempre 1
        if(id_materia.length()<4){
            if(id_materia.length()<2){
                String resultado = "000";
                resultado="000".concat(id_materia);
                id_materia=resultado;
            }
            if(id_materia.length()<3){
                String resultado = "00";
                resultado = "00".concat(id_materia);
                id_materia=resultado;
            }
            if(id_materia.length()<4){
                String resultado = "0";
                resultado = "0".concat(id_materia);
                id_materia=resultado;
            }
        }
        String datoAMostrar = "";
        numero_paralelo = numero_paralelo.concat("01");
        id_prof = id_prof.concat(numero_paralelo);
        id_materia = id_materia.concat(id_prof);
        datoAMostrar = id_materia;
        mensaje = datoAMostrar.concat("\n");
        setContentView(R.layout.activity_main);

        //TextView txt_bt = (TextView) findViewById(R.id.txt_bt);
        //TextView txt_wifi = (TextView) findViewById(R.id.txt_mac);
        //TextView txt_imei = (TextView)findViewById(R.id.txt_imei);
        //TextView txt_potencia = (TextView)findViewById(R.id.txt_signal);
        //txt_wifi.setText(InfoConn.getMacAddress());
        //txt_imei.setText(InfoConn.getIMEI(getApplicationContext()));
        //txt_bt.setText(InfoConn.getBTMAC());
        //Handler manejador = new Handler();
        /*Intent intent = new Intent(this.getApplication(),DeviceListActivity.class);
        startActivity(intent);*/
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Intent selectDevice = new Intent(this.getApplication(), DeviceListActivity.class);
            startActivityForResult(selectDevice, REQUEST_DEVICE_TO_CONNECT);
        }
    }
/*
    private String getImei() {
        return ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    }*/
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Intent selectDevice = new Intent(this.getApplication(), DeviceListActivity.class);
                    startActivityForResult(selectDevice, REQUEST_DEVICE_TO_CONNECT);
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            case REQUEST_DEVICE_TO_CONNECT:
                //mBluetoothAdapter.cancelDiscovery();
                //mBluetoothAdapter.startDiscovery();
                if (resultCode == Activity.RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String address = bundle.getString(EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice btDev = mBluetoothAdapter.getRemoteDevice(address);
                    BluetoothSocket bts = null;
                    try {
                        System.out.println("Creando socket");
                        bts = btDev.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(bts!=null){
                        try {
                            bts.connect();
                            System.out.println("estableciendo conexion");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("conexion fallida");
                        }
                    }
                    if (bts.isConnected()) {
                        System.out.println("conectado");
                        System.out.println("enviando mensaje");
                        try{
                            bts.getOutputStream().write(mensaje.getBytes());
                            //AQUI DEBO DE ENVIAR EL MENSAJE AL SERVIDOR
                            System.out.println("mensaje enviado");
                            //System.out.println(bts.getInputStream().read());
                        }catch (IOException e){
                            e.printStackTrace();
                        }finally {
                            try {
                                bts.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                }
        }
    }



    private class BluetoothConn extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        int i=0;
        byte[] buffer;

        // Unique UUID for this application, you may use different
        public BluetoothConn(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
            //now make the socket connection in separate thread to avoid FC
            Thread connectionThread  = new Thread(new Runnable() {

                @Override
                public void run() {
                    // Always cancel discovery because it will slow down a connection
                    // mBluetoothAdapter.cancelDiscovery();
                    // Make a connection to the BluetoothSocket
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        mmSocket.connect();
                    } catch (IOException e) {
                        //connection to device failed so close the socket
                        try {
                            mmSocket.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            });
            connectionThread.start();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
                buffer = new byte[5];
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            while (true) {
                if (mmSocket.isConnected()){
                    byte[] a =new byte[8];
                    a="12131415\n".getBytes();
                    conexion.write(a);
                    System.out.println("Se escribio en el arduino");
                    //conexion.cancel();
                    System.out.println("cerrando conexion");
                }else{
                    System.out.println("espeerando conexion");
                }
            }
        }
        public void write(byte[] buffer) {
            try {
                //write the data to socket stream
                mmOutStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void scanWifiSignals() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){
            //wifiManager.setWifiEnabled(true);
        }
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
        }
        asistenciaActual.setSenales(senales);
    }
}