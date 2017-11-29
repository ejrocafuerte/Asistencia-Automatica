package com.asistencia.integradora.espol.emisor;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView txt_bt = (TextView) findViewById(R.id.txt_bt);
        TextView txt_wifi = (TextView) findViewById(R.id.txt_mac);
        TextView txt_imei = (TextView)findViewById(R.id.txt_imei);

        txt_wifi.setText(BtConn.getMacAddress());
        txt_imei.setText(BtConn.getIMEI(getApplicationContext()));

        BluetoothAdapter btAdap;

        btAdap = BluetoothAdapter.getDefaultAdapter();
        if(btAdap!=null){
            txt_bt.setText(btAdap.getAddress());
            if(!btAdap.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }

        }

    }
}
