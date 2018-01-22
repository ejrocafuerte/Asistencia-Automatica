package com.asistencia.integradora.espol.emisor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends Activity{

    public static final String TAG = "MainActivity";

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_DEVICE_TO_CONNECT=4;
    private static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothSocket btSocket=null;
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public BluetoothAdapter mBluetoothAdapter;
    public StringBuffer mStringBuffer=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*if (savedInstanceState == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            BtConn fragment = new BtConn();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }*/
        EmisorSQLHelper db1 = new EmisorSQLHelper(getApplicationContext(),"emisor.db",null,1);
        SQLiteDatabase dbEmisor = db1.getWritableDatabase();

        if(dbEmisor != null){
            dbEmisor.execSQL("insert into core_profesor values(1,\"201021839\",\"erick joel\",\"rocafuerte villon\",\"example@example.com\")");
            Cursor c = dbEmisor.rawQuery("select nombres, apellidos from core_profesor where nombres='erick joel'",null);
            c.moveToFirst();
            System.out.print(c.getString(0).isEmpty());
            System.out.println("el nombre del profesor es: " + c.getString(0)+" "+c.getString(1));
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
        }else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }else{
            Intent selectDevice = new Intent(this.getApplication(),DeviceListActivity.class);
            startActivityForResult(selectDevice,REQUEST_DEVICE_TO_CONNECT);
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
            /*case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;*/
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //BtConn conn= new BtConn();
                    //BtConn.setupChat();
                    Intent selectDevice = new Intent(this.getApplication(),DeviceListActivity.class);
                    startActivityForResult(selectDevice,REQUEST_DEVICE_TO_CONNECT);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    //Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            case REQUEST_DEVICE_TO_CONNECT:
                if(resultCode==Activity.RESULT_OK){
                    Bundle bundle = data.getExtras();
                    Handler manejadorUi= new Handler();
                    String address=bundle.getString(EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice btDev = mBluetoothAdapter.getRemoteDevice(address);

                    //BtService conexion = new BtService(getApplicationContext(),manejadorUi);
                    //conexion.getState();
                    try {
                        btSocket = btDev.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                        if(btSocket!= null){
                            System.out.println("socket creado, crear thread");
                            System.out.println(btSocket.isConnected());
                            btSocket.connect();
                            System.out.println("conectado:" +btSocket.isConnected());
                            System.out.print("123456".getBytes());
                            byte[] a = new byte['1','2','3','4','5','6','\n'];
                            //byte[6] a =['1','2','3','4','5','6']
                            btSocket.getOutputStream().write('1');
                            btSocket.getOutputStream().write('2');
                            btSocket.getOutputStream().write('1');
                            btSocket.getOutputStream().write('3');
                            btSocket.getOutputStream().write('1');
                            btSocket.getOutputStream().write('4');
                            btSocket.getOutputStream().write('1');
                            btSocket.getOutputStream().write('5');
                            btSocket.getOutputStream().write('\n');

                            //byte[] response= new byte[3];
                            //btSocket.getInputStream().read(response);
                            //System.out.println(response.toString());
                            System.out.println("cerrando conexion");
                            btSocket.close();

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }
}