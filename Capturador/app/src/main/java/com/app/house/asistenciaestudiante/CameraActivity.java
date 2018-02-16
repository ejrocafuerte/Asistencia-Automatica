package com.app.house.asistenciaestudiante;

import android.Manifest;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SizeF;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Codigo;
import models.CodigosServer;
import retrofit.Connection;
import retrofit.RestClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";
    private CameraBridgeViewBase _cameraBridgeViewBase;
    private Mat mRGBA, mResultado, mObjectSize, mParameters;
    private FpsMeter fpsMeter = new FpsMeter();
    private ArrayList<Codigo> codigosServer = new ArrayList<>();
    private boolean consultandoServer = false;
    private static final String CODIGO_INICIO = "999999999999";
    private static final String CODIGO_OFF = "000000000000";
    private static int amountFrameEstDistance = 0;
    private static double fps = 0.0f;
    private double anchoObjetoImagen = 0.0f;
    private double estimatedDist = 0.0f;
    private double estimatedDistX = 0.0f;
    private double estimatedDistY = 0.0f;
    private String mensajeResultado = "";
    private double focalLength = 0.0f;
    private float horizonalAngle = 0.0f;
    private float verticalAngle = 0.0f;
    private double sensorWidth = 0.0f;
    private double sensorHeight = 0.0f;
    private double focalPx = 0.0f;
    private double anchoObjetoReal = 1660;//1184;//1720
    private double yaw = 0.0f;
    private double tilt = 0.0f;
    private double roll = 0.0f;
    private double flickerTime = 1000;//ms
    private double actualTime = 0;
    private int faseDeco = 0;
    private int threshold = 0;
    private String[] mensajeComun = {"", "", ""};
    private String mensajeFinal = "";
    private int BUSCAR = 0;
    private int DECODIFICACION = 1;
    private int POSICION = 2;
    private int MAX_WIDTH = 0;
    private int MAX_HEIGHT = 0;
    private boolean first_frame = true;

    private ArrayList<String> mensajeListaFase1 = new ArrayList<>(),
                              mensajeListaFase2 = new ArrayList<>(),
                              mensajeListaFase3 = new ArrayList<>();


    private BaseLoaderCallback _baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.e(TAG, "OpenCV loaded successfully");
                    // Load ndk built module, as specified in moduleName in build.gradle
                    // after opencv initialization
                    System.loadLibrary("native-lib");
                    _cameraBridgeViewBase.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
            }
        }
};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Decodificador");
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Permissions for Android 6+
        ActivityCompat.requestPermissions(CameraActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);

        _cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.main_surface);
        _cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        _cameraBridgeViewBase.setCvCameraViewListener(this);
        _cameraBridgeViewBase.setMaxFrameSize(640, 640);
        _cameraBridgeViewBase.enableFpsMeter();

        if (LobbyActivity.retrofit == null) {
            Connection.init();
            LobbyActivity.restClient = Connection.createService(RestClient.class); //, username, password);
        }

        if(codigosServer.size()<= 0)
            getCodigosServer();

        Intent intent = getIntent();
        if(intent != null) {
            anchoObjetoReal = intent.getIntExtra("Tamanio", 0);
            Log.e(TAG, "Ancho Objecto Real: "+anchoObjetoReal);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    public void disableCamera() {
        if (_cameraBridgeViewBase != null)
            _cameraBridgeViewBase.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        mResultado = new Mat(width, height, CvType.CV_8UC4);

        MAX_WIDTH = width;
        MAX_HEIGHT = height;

        mObjectSize = new Mat(1, 4, CvType.CV_64FC1);
        mParameters = new Mat(1, 8, CvType.CV_64FC1);
        getCameraParameters(width, height);

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        fpsMeter.measure();
        fps = fpsMeter.getFps();

        mParameters.put(0, 0, tilt);
        mParameters.put(0, 1, yaw);
        mParameters.put(0, 2, roll);
        mParameters.put(0, 3, horizonalAngle);
        mParameters.put(0, 4, verticalAngle);
        mParameters.put(0, 5, 4);//faseDeco);
        mParameters.put(0, 6, focalPx);
        mParameters.put(0, 7, threshold);

        mensajeResultado = decodificar(mRGBA.getNativeObjAddr(),
                                        mResultado.getNativeObjAddr(),
                                        mObjectSize.getNativeObjAddr(),
                                        mParameters.getNativeObjAddr());

        tilt = mParameters.get(0, 0)[0];
        yaw = mParameters.get(0, 1)[0];
        roll = mParameters.get(0, 2)[0];

        anchoObjetoImagen = mObjectSize.get(0, 2)[0];

            estimatedDist = (anchoObjetoImagen > 0) ? ((anchoObjetoReal * focalLength * mRGBA.cols()) / (sensorWidth * anchoObjetoImagen)) : 0;//??

            estimatedDistX = -estimatedDist * Math.sin(yaw) * Math.cos(tilt);
            estimatedDistY = Math.abs(estimatedDist * Math.cos(yaw) * Math.cos(tilt));
        Log.e(TAG, "d "+anchoObjetoReal+ " "+anchoObjetoImagen+" "+estimatedDist);


        faseDeco = (int) mParameters.get(0, 5)[0];

        /*if (faseDeco == -1) {
            mensajeListaFase1.clear();
            mensajeListaFase2.clear();
            mensajeListaFase3.clear();
            faseDeco = 0;
            first_frame = true;
            amountFrameEstDistance = 0;
        }

        if(faseDeco == -2){
            amountFrameEstDistance = 1;
            faseDeco = 4;
        }

        mensajeResultado = checkOff(mensajeResultado);

        if(!mensajeResultado.equals(CODIGO_OFF)) {

            if(fps > 1.0f){

                if (faseDeco == 0) {

                    //Log.e(TAG, "Empezando Fase 0, fps: " + fps);
                    if (mensajeResultado.equals(CODIGO_INICIO)) {
                        faseDeco++;
                        if(first_frame){
                            vibrate(50);
                            first_frame = false;
                        }
                        //Log.e(TAG, "FASE 0 OK");
                    }

                    mensajeListaFase1.clear();
                    mensajeListaFase2.clear();
                    mensajeListaFase3.clear();

                } else if (faseDeco == 1) {

                    if (mensajeResultado.equals(CODIGO_INICIO)) {
                        faseDeco = -1;
                    } else{

                        mensajeListaFase1.add(mensajeResultado);
                        Log.e(TAG, "Mensaje " + mensajeListaFase1.size() + " Fase 1: " + mensajeResultado);
                    }
                } else if (faseDeco == 2) {

                    if (mensajeResultado.equals(CODIGO_INICIO)) {
                        faseDeco = -1;
                    } else{

                        mensajeListaFase2.add(mensajeResultado);
                        Log.e(TAG, "Mensaje " + mensajeListaFase2.size() + " Fase 2: " + mensajeResultado);
                    }
                } else if (faseDeco == 3) {

                    if (mensajeResultado.equals(CODIGO_INICIO)) {
                        faseDeco = -1;
                    } else{
                        mensajeListaFase3.add(mensajeResultado);
                        Log.e(TAG, "Mensaje " + mensajeListaFase3.size() + " Fase 3: " + mensajeResultado);
                    }
                }
            }
        }
        else{
            if(faseDeco == 1 && mensajeListaFase1.size() > 0) {
                faseDeco = 2;
                Log.e(TAG, "Mensajes FASE 1 terminó ");
            }
            else if(faseDeco == 2 && mensajeListaFase2.size() > 0) {
                faseDeco = 3;
                Log.e(TAG, "Mensajes FASE 2 terminó ");
            }
            if(faseDeco == 3 && mensajeListaFase3.size() > 0) {
                faseDeco = 4;
                Log.e(TAG, "Mensajes FASE 3 terminó ");
            }
            //showText(DECODIFICACION);
        }

        if (faseDeco == 4){

            if (mensajeListaFase1.size() > 0  && mensajeListaFase2.size() > 0 && mensajeListaFase3.size() > 0) {

                if(amountFrameEstDistance == 0) {
                    mensajeComun[0] = mostCommon(mensajeListaFase1);
                    mensajeComun[1] = mostCommon(mensajeListaFase2);
                    mensajeComun[2] = mostCommon(mensajeListaFase3);

                    Log.e(TAG, "Mensaje FASE 1 mas comun: " + mensajeComun[0]);
                    Log.e(TAG, "Mensaje FASE 2 mas comun: " + mensajeComun[1]);
                    Log.e(TAG, "Mensaje FASE 3 mas comun: " + mensajeComun[2]);

                    if (mensajeComun[0].equals(mensajeComun[1]) && mensajeComun[0].equals(mensajeComun[2])) {
                        Log.e(TAG, "Mensaje Final Precisión 100%: " + mensajeFinal);
                        mensajeFinal = mensajeComun[0];

                    } else if ((mensajeComun[0].equals(mensajeComun[1]) && !mensajeComun[0].equals(mensajeComun[2])) ||
                            (mensajeComun[0].equals(mensajeComun[2]) && !mensajeComun[0].equals(mensajeComun[1])) ||
                            (mensajeComun[1].equals(mensajeComun[2]) && !mensajeComun[0].equals(mensajeComun[1]))) {
                        Log.e(TAG, "Mensaje Final Precisión 66%: " + mensajeFinal);
                        mensajeFinal = mensajeComun[0];


                    } else if (!mensajeComun[0].equals(mensajeComun[1]) && !mensajeComun[0].equals(mensajeComun[2])) {
                        Log.e(TAG, "Precisión 0%, no es posible determinar codigo, reiniciando...");
                        faseDeco = -1;
                    }

                    if(!mensajeFinal.isEmpty() && serverContains(mensajeFinal)){
                        Log.e(TAG, "Codigo coincide con Server!");
                    }

                }
                amountFrameEstDistance++;

                if(amountFrameEstDistance > 10) {
                    amountFrameEstDistance = 0;
                    tilt = mParameters.get(0, 0)[0];
                    yaw = mParameters.get(0, 1)[0];
                    roll = mParameters.get(0, 2)[0];

                    anchoObjetoImagen = mObjectSize.get(0, 2)[0];// * Math.abs(((mObjectSize.get(0, 0)[0] / mObjectSize.get(0, 2)[0] )));

                    if (anchoObjetoImagen > 1) {

                        estimatedDist = (anchoObjetoImagen > 0) ? (anchoObjetoReal * focalLength * mRGBA.cols()) / (sensorWidth * anchoObjetoImagen) : 0;//??

                        estimatedDistX = -estimatedDist * Math.sin(yaw) * Math.cos(tilt);
                        estimatedDistY = Math.abs(estimatedDist * Math.cos(yaw) * Math.cos(tilt));

                        if (estimatedDist > 1) {

                            faseDeco = -1;
                            vibrate(150);
                            mensajeListaFase1.clear();
                            mensajeListaFase2.clear();
                            mensajeListaFase3.clear();
                            Intent intent = new Intent(this, LobbyActivity.class);
                            intent.putExtra("codigo", mensajeFinal);
                            intent.putExtra("distanciaX", estimatedDistX/10);
                            intent.putExtra("distanciaY", estimatedDistY/10);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "X: " +
                                            String.format("%.2f", estimatedDistX / 10) +  ", Y: " +
                                            String.format("%.2f", estimatedDistY / 10), Toast.LENGTH_LONG).show();
                                }
                            });

                            Log.e(TAG, "Codigo: " + mensajeFinal+ " , distanceX: " + estimatedDistX/10 + ", distanceY: "+estimatedDistY/10);
                            setResult(LobbyActivity.RESULT_OK, intent);
                            finish();
                        }


                    }
                }else{
                    if(faseDeco == 4)
                        Log.e(TAG, "Frame <= 20, actual: "+ amountFrameEstDistance);
                }
            }
            else{
                Log.e(TAG, "Algunas de las listas de cods esta vacia.");
            }

            showText(POSICION);

        }*/

        if(faseDeco >= 1 && faseDeco <= 3){
            showText(DECODIFICACION);
        }else if(faseDeco <= 0){
            showText(BUSCAR);
        }else if(faseDeco == 4){
            showText(POSICION);
        }


        return mResultado;
    }

    private String checkOff(String mensajeResultado) {
        if(mensajeResultado == null || mensajeResultado.equals("")) return CODIGO_OFF;
        String msg1 = mensajeResultado.substring(0,4);
        String msg2 = mensajeResultado.substring(4,8);
        String msg3 = mensajeResultado.substring(8,12);
        if(msg1.equals("0000") ||
           msg2.equals("0000") ||
           msg3.equals("0000")) {
           //Log.e(TAG, "Discarding: " + mensajeResultado);

            return CODIGO_OFF;
        }
        else {
            //Log.e(TAG, "Secciones: (" + msg1+", "+msg2+", "+msg3+")");
            return mensajeResultado;
        }
    }

    private boolean verificaCodigo(String codigoFinal) {
        if(codigosServer == null || codigosServer.size() <= 0) return false;

        return codigosServer.contains(codigoFinal);
    }

    public void getCameraParameters(int maxwidth, int maxheight) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

            try {
                String cameraId = "0";
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
                float[] focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                SizeF sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

                sensorWidth = sensorSize.getWidth();
                sensorHeight = sensorSize.getHeight();
                int focusId = 0;
                focalLength = focalLengths[focusId];
                horizonalAngle = (float) Math.toDegrees(2 * Math.atan(0.5f * sensorWidth / focalLength));
                verticalAngle = (float) Math.toDegrees(2 * Math.atan(0.5f * sensorHeight / focalLength));
                focalPx = (focalLength / sensorWidth) * maxwidth;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            // do something for phones running an SDK before lollipop
        }
    }

    public native static String decodificar(long mRGBA, long mResultado, long mObjectSize, long mParameters);

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, _baseLoaderCallback);
        } else {

            Log.e(TAG, "OpenCV library found inside package. Using it!");
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        getCodigosServer();
    }

    private void vibrate(int mili) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(mili,mili));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(mili);
        }
    }

    public class FpsMeter {
        private static final int STEP = 20;
        private final DecimalFormat FPS_FORMAT = new DecimalFormat("0.00");

        private int mFramesCounter;
        private double mFrequency;
        private long mprevFrameTime;
        private double fps;
        boolean mIsInitialized = false;

        public void init() {
            mFramesCounter = 0;
            mFrequency = Core.getTickFrequency();
            mprevFrameTime = Core.getTickCount();
        }

        public void measure() {
            if (!mIsInitialized) {
                init();
                mIsInitialized = true;
            } else {
                mFramesCounter++;
                if (mFramesCounter % STEP == 0) {
                    long time = Core.getTickCount();
                    fps = STEP * mFrequency / (time - mprevFrameTime);
                    mprevFrameTime = time;
                }
            }
        }

        public double getFps() {
            return fps;
        }
    }

    public static String mostCommon(List<String> list) {
        Map<String, Integer> map = new HashMap<>();

        for (String t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        Map.Entry<String, Integer> max = null;

        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }
        /*if(map.size() == list.size()){
            return "";
        }*/
        return (max != null) ? max.getKey() : "";
    }

    private void getCodigosServer() {
        if (LobbyActivity.restClient != null && !consultandoServer) {
            consultandoServer = true;
            Call<CodigosServer> request = LobbyActivity.restClient.getCodigosServer();

            if(!request.isExecuted()) {
                request.enqueue(new Callback<CodigosServer>() {
                    @Override
                    public void onResponse(Call<CodigosServer> call, Response<CodigosServer> response) {

                        if (response.isSuccessful()) {
                            String rsp = response.body().getResponse();

                            switch (rsp) {
                                case "0": {

                                    //codigosServer = null;
                                    codigosServer = response.body().getCodigos();
                                    Toast.makeText(getBaseContext(), "Server OK Codigos", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Server OK Codigos");
                                    break;
                                }
                                case "1": {
                                    //codigosServer.clear();
                                    //codigosServer = null;
                                    //Toast.makeText(getBaseContext(), "Codigos NOK", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Server NOK Codigos");
                                    break;
                                }
                                default: {
                                    //Toast.makeText(getBaseContext(), "Default Codigos NOK", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                            }
                        } else {
                            //Toast.makeText(getBaseContext(), "Codigos NOK2, no succs req", Toast.LENGTH_SHORT).show();
                        }
                        consultandoServer=false;
                    }

                    @Override
                    public void onFailure(Call<CodigosServer> call, Throwable t) {
                        Toast.makeText(getBaseContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                        consultandoServer=false;
                    }
                });
            }
        }
    }

    public void showText(int proceso){
        if(proceso == BUSCAR) {
            Imgproc.putText(mResultado, "Buscando...",
                    new Point(10, 24),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
            Imgproc.putText(mResultado, "Fase: " + ((faseDeco == -1 || faseDeco == -2) ? 0 : faseDeco) + " de 4",
                    new Point(10, 50),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        }
        else if(proceso == DECODIFICACION){
            Imgproc.putText(mResultado, "Decodificando...",
                    new Point(10, 24),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
            Imgproc.putText(mResultado, "Fase: " + ((faseDeco == -1 || faseDeco == -2) ? 0 : faseDeco) + " de 4",
                    new Point(10, 50),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        }
        else if(proceso == POSICION){
            Imgproc.putText(mResultado, "Estimando Posicion...",
                    new Point(10, 24),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
            Imgproc.putText(mResultado, "Fase: 4 de 4",
                    new Point(10, 50),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
            Imgproc.putText(mResultado, "Distancias: ("+String.format("%.2f", estimatedDistX/10)+", "+String.format("%.2f", estimatedDistY/10)+")",
                    new Point(10, 80),
                    Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
            Imgproc.putText(mResultado, "Espere: "+ ((amountFrameEstDistance == 0) ? 0 : (10 - amountFrameEstDistance)),
                    new Point(10, 110), Core.FONT_HERSHEY_SIMPLEX, .85, new Scalar(255, 0, 0), 2);

        }

        Imgproc.putText(mResultado, "T: " + String.format("%.2f", Math.toDegrees(tilt)) +
                        ", P: " + String.format("%.2f", Math.toDegrees(yaw)) +
                        ", R: " + String.format("%.2f", Math.toDegrees(roll)),
                new Point(10, MAX_HEIGHT-30),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Threshold: " + (int)mParameters.get(0, 7)[0],
                new Point(10, MAX_HEIGHT-60),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
    }

    private boolean serverContains(String mensaje){
        if(codigosServer == null || codigosServer.size() <= 0) return false;

        for(int i = 0; i < codigosServer.size(); i++){
            if(codigosServer.get(i).getCodigo().equals(mensaje))
                return true;
        }
        return  false;
    }
}
