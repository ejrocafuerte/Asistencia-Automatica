package com.asistencia.integradora.espol.emisor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    public static final String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_DEVICE_TO_CONNECT = 4;
    private static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothSocket btSocket = null;
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothConn conexion;
    byte[] mensaje = new byte[12];


    public BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EmisorSQLHelper db1 = new EmisorSQLHelper(getApplicationContext(), "emisor.db", null, 1);
        SQLiteDatabase dbEmisor = db1.getWritableDatabase();

        if (dbEmisor != null) {
            //dbEmisor.execSQL("insert into core_profesor values(1,\"201021839\",\"erick joel\",\"rocafuerte villon\",\"example@example.com\")");
            Cursor c = dbEmisor.rawQuery("select nombres, apellidos from core_profesor where nombres='erick joel'", null);
            c.moveToFirst();
            System.out.print(c.getString(0).isEmpty());
            System.out.println("el nombre del profesor es: " + c.getString(0) + " " + c.getString(1));
        }
        dbEmisor.close();
        db1.close();
        //TextView txt_bt = (TextView) findViewById(R.id.txt_bt);
        //TextView txt_wifi = (TextView) findViewById(R.id.txt_mac);
        //TextView txt_imei = (TextView)findViewById(R.id.txt_imei);
        //TextView txt_potencia = (TextView)findViewById(R.id.txt_signal);
        //txt_wifi.setText(InfoConn.getMacAddress());
        //txt_imei.setText(InfoConn.getIMEI(getApplicationContext()));
        //txt_bt.setText(InfoConn.getBTMAC());
        //Handler manejador = new Handler();
        //BtConn btconexion = new BtConn();
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
                        mensaje = "12131415\n".getBytes();
                        try{
                            bts.getOutputStream().write(mensaje);
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
                    mBluetoothAdapter.cancelDiscovery();

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

                // Keep listening to the InputStream while connected

                /*try {
                    //buffer[i]=(byte)mmInStream.read();
                    i++;
                    //read the data from socket stream
                    /*int a=mmInStream.available();
                    if(a>0){
                        for(int i=0;i<a;i++){
                            buffer[i]=(byte)mmInStream.read();
                        }

                    }
                    // Send the obtained bytes to the UI Activity
                } catch (IOException e) {
                    //an exception here marks connection loss
                    //send message to UI Activity
                    break;
                }*/
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

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}