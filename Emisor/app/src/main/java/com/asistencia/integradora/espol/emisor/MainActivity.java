package com.asistencia.integradora.espol.emisor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public BluetoothSocket mmSocket;
    public BluetoothDevice mmDevice;
    public InputStream mmInStream;
    public OutputStream mmOutStream;

    //Donde se cargan los elementos detectados y pareados
    ListView lv,lv2;
    //Conjunto que guarda los dispositivos pareados
    ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //Conjunto que guarda los dispositivos encontrados
    ArrayAdapter<String> mNewDevicesArrayAdapter;
    //El bluetooth de mi dispositivo
    BluetoothAdapter btAdap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmisorSQLHelper db1 = new EmisorSQLHelper(getApplicationContext());
        System.out.println("el nombre del profesor es: "+db1.getProfesor());

        setContentView(R.layout.activity_main);

        TextView txt_bt = (TextView) findViewById(R.id.txt_bt);
        TextView txt_wifi = (TextView) findViewById(R.id.txt_mac);
        TextView txt_imei = (TextView)findViewById(R.id.txt_imei);
        TextView txt_potencia = (TextView)findViewById(R.id.txt_signal);
        lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(mPairedDevicesArrayAdapter);
        lv2 = (ListView)findViewById(R.id.lvNew);
        lv2.setAdapter(mNewDevicesArrayAdapter);
        //Cuando ha iniciado la busqueda
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        //Cuando ha terminado la busqueda
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        txt_wifi.setText(InfoConn.getMacAddress());
        txt_imei.setText(InfoConn.getIMEI(getApplicationContext()));
        txt_bt.setText(InfoConn.getBTMAC());

        encenderBT();
        Set<BluetoothDevice> pairedDev = btAdap.getBondedDevices();

        mostrarPareados();

        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
        //btAdap.startDiscovery();
        doDiscovery();
        Toast.makeText(getApplicationContext(), "Showing Nearest Devices",Toast.LENGTH_SHORT).show();


    }
    public void encenderBT() {
        this.btAdap = BluetoothAdapter.getDefaultAdapter();
        if (btAdap != null) {
            if (!btAdap.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                //return enableBtIntent;//
            }
        }
    }
    public void mostrarPareados(){
        Set<BluetoothDevice> pairedDevices = btAdap.getBondedDevices();
        if(pairedDevices.size()>0){
            for(BluetoothDevice btdev : pairedDevices){
                mPairedDevicesArrayAdapter.add(btdev.getName()+"\n"+btdev.getAddress());
            }
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice btDev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                View v = new View();
                v.se
                mNewDevicesArrayAdapter.add(btDev.getName() + "\n" + btDev.getAddress());
                //debemos tener un listado de las macs de los bluetooth para que no cargue todos los pareados sino
                // solamente el del bloque o aula especifico
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (btAdap != null) {
            btAdap.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Toast.makeText(this.getApplicationContext(),"doDiscovery",Toast.LENGTH_SHORT);
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        // If we're already discovering, stop it
        if (btAdap.isDiscovering()) {
            btAdap.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        btAdap.startDiscovery();
    }
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int i, long l) {
            // Cancel discovery because it's costly and we're about to connect
            btAdap.cancelDiscovery();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            System.out.println(address);
        }
    };

    private class ConnectThread extends Thread {
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }
        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdap.cancelDiscovery();
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
