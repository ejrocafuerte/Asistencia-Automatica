package com.asistencia.integradora.espol.emisor;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
/**
 * Created by erick on 28/11/2017.
 */

public class InfoConn {

    public static String getIMEI(Context ctx) {
        String myIMEI = "error";
        myIMEI = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        return myIMEI;
    }
    private static boolean conectarArduino(){
        BluetoothAdapter btAdap = BluetoothAdapter.getDefaultAdapter();
        return true;
    }
    public static String getBTMAC(){
        return BluetoothAdapter.getDefaultAdapter().getAddress();
    }
    public static String getMacAddress() {
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
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            Log.e("Error", ex.getMessage());
        }
        return "";
    }




    /*
    public static String getMobileSignal(Context ctx) {
        TelephonyManager telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> lista_cell;
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try{
                lista_cell = telephonyManager.getAllCellInfo();
                CellInfoWcdma cellinfogsm = (CellInfoWcdma)telephonyManager.getAllCellInfo().get(0);
                CellSignalStrengthWcdma potencia= cellinfogsm.getCellSignalStrength();
                cellSignalStrengthGsm.getDbm();
                return "OK";
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return "Error";
    }
*/
}