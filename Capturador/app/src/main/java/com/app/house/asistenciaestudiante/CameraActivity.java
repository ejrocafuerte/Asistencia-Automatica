package com.app.house.asistenciaestudiante;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import models.CodigosServer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";
    private CameraBridgeViewBase _cameraBridgeViewBase;
    private Mat mRGBA, mResultado, mObjectSize, mParameters;// mEulerAngles;
    private FpsMeter fpsMeter = new FpsMeter();
    private ArrayList<String> codigosServer = new ArrayList<>();
    private static final String CODIGO_INICIO = "000000009999";
    private static int amountFrameEstDistance = 0;
    private static int actualGeneralFrame = 0;
    private static int actualFaseFrame = 0;
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
    private float anchoObjetoReal = 215; //1400;//1750;// mm
    private double yaw = 0.0f;
    private double tilt = 0.0f;
    private double roll = 0.0f;
    private double flickerTime = 1000;//ms
    private double actualTime = 0;
    private int faseDeco = 0;
    private int sizeList = 3;
    private String[] mensajes = {"", "", ""};
    private String mensajeFinal = "";
    private String mensajeAnteriorDecodificado = "";
    private ArrayList<String> mensajeListaFase0 = new ArrayList<>(),
                              mensajeListaFase1 = new ArrayList<>(),
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

        //getCodigosServer();

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
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        /*switch (item.getItemId()) {
            case R.id.new_game:
                newGame();
                return true;
            case R.id.help:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }*/
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
        //Log.e(TAG, "Resolution: " + (width + "x" + height));
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        mResultado = new Mat(width, height, CvType.CV_8UC4);


        mObjectSize = new Mat(1, 4, CvType.CV_64FC1);
        mParameters = new Mat(1, 7, CvType.CV_64FC1);
        getCameraParameters(width, height);

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        fpsMeter.measure();
        fps = fpsMeter.getFps();
        //actualGeneralFrame++;
        //actualFaseFrame++;
        //Log.e(TAG, "FPS: " + fpsMeter.getFps());

        //mFrequency = Core.getTickFrequency();//  FRAMERATE
        //mprevFrameTime = Core.getTickCount();

        mParameters.put(0, 0, tilt);
        mParameters.put(0, 1, yaw);
        mParameters.put(0, 2, roll);
        mParameters.put(0, 3, horizonalAngle);
        mParameters.put(0, 4, verticalAngle);
        mParameters.put(0, 5, 4); //faseDeco);
        mParameters.put(0, 6, focalPx);
///////////////////////
       mensajeResultado = decodificar(mRGBA.getNativeObjAddr(),
                mResultado.getNativeObjAddr(),
                mObjectSize.getNativeObjAddr(),
                mParameters.getNativeObjAddr());

       tilt = mParameters.get(0,0)[0];
       yaw = mParameters.get(0,1)[0];
       roll = mParameters.get(0,2)[0];

        anchoObjetoImagen = mObjectSize.get(0, 2)[0];// * Math.abs(((mObjectSize.get(0, 0)[0] / mObjectSize.get(0, 2)[0] )));

        estimatedDist = (anchoObjetoImagen > 0) ? (anchoObjetoReal * focalLength * mRGBA.cols()) / (sensorWidth * anchoObjetoImagen) : 0;//??

        estimatedDistX = -estimatedDist * Math.sin(yaw) * Math.cos(tilt);
        estimatedDistY = Math.abs(estimatedDist * Math.cos(yaw) * Math.cos(tilt));
////////////////////////
        //System.currentTimeMillis() - actualTime > flickerTime) {



        /*mensajeResultado = decodificar(mRGBA.getNativeObjAddr(),
                                        mResultado.getNativeObjAddr(),
                                        mObjectSize.getNativeObjAddr(),
                                        mParameters.getNativeObjAddr());

        faseDeco = (int) mParameters.get(0, 5)[0];

        Log.e(TAG, "Fase deco: " + faseDeco);

        if (faseDeco == 5) {
            mensajeListaFase1.clear();
            mensajeListaFase2.clear();
            mensajeListaFase3.clear();
            actualFaseFrame = 0;
            actualGeneralFrame = 0;
            faseDeco = 0;
        }

        if(!mensajeResultado.equals("") && fps > 2.0f){
            if(faseDeco == 0){
                Log.e(TAG, "Empezando Fase 0, fps: " + fps);
                if(mensajeResultado.equals(CODIGO_INICIO)){
                    //if (avg >= 0.5) {
                    faseDeco++;
                    vibrate();
                    mensajeAnteriorDecodificado = mensajeResultado;
                    actualFaseFrame = 0;
                    actualGeneralFrame = 0;
                    Log.e(TAG, "FASE 0 OK");
                }
                else Log.e(TAG, "MSG FASE 0 NOK: "+mensajeResultado);

                mensajeListaFase1.clear();
                mensajeListaFase2.clear();
                mensajeListaFase3.clear();
            }
            else if (faseDeco == 1) {

                if(mensajeResultado.equals(CODIGO_INICIO)) {
                    faseDeco = 5;
                    Log.e(TAG, "En Fase 1 msg igual a msg Fase 0: " +mensajeResultado);
                }
                else if(!mensajeAnteriorDecodificado.equals(mensajeResultado)) {

                    mensajeListaFase1.add(mensajeResultado);



                    if(actualFaseFrame == 0){

                        //Log.e(TAG, "Empezando Fase 1, fps: " + fpsMeter.getFps());
                        actualGeneralFrame = (int)Math.floor(fpsMeter.getFps() * (flickerTime/1000));//  -1;
                        Log.e(TAG, "Fase 1, Size list: " + mensajeListaFase1.size()+", frame: "+actualGeneralFrame+", "+actualFaseFrame);

                        actualFaseFrame++;
                    }

                    Log.e(TAG, "Mensaje"+mensajeListaFase1.size()+" Fase 1: " +mensajeResultado);
                    if (mensajeListaFase1.size() >= actualGeneralFrame) {
                        mensajes[faseDeco - 1] = mensajeResultado;
                        faseDeco++;
                        actualFaseFrame = 0;
                        actualGeneralFrame = 0;
                        mensajeAnteriorDecodificado = mensajeResultado;
                        vibrate();
                        Log.e(TAG, "Mensaje OK fase 1: , size list: " + mensajeListaFase1.size()+", frame: "+actualGeneralFrame+", "+actualFaseFrame);
                    }



                    }
            } else if (faseDeco == 2) {

                if(mensajeResultado.equals(CODIGO_INICIO)) {
                    faseDeco = 5;
                    Log.e(TAG, "En Fase 2 msg igual a msg Fase 1: " +mensajeResultado);
                }
                else if(!mensajeAnteriorDecodificado.equals(mensajeResultado)) {

                    mensajeListaFase2.add(mensajeResultado);


                    if(actualFaseFrame == 0){
                        //Log.e(TAG, "Empezando Fase 2, fps: " + fpsMeter.getFps());
                        actualGeneralFrame = (int)Math.floor(fpsMeter.getFps() * (flickerTime/1000));//  -1;
                        Log.e(TAG, "Fase 2, Size list: " + mensajeListaFase2.size()+", frame: "+actualGeneralFrame+", "+actualFaseFrame);

                        actualFaseFrame++;
                    }

                    Log.e(TAG, "Mensaje"+mensajeListaFase2.size()+" Fase 2: " +mensajeResultado);
                    if (mensajeListaFase2.size() >= actualGeneralFrame) {
                        mensajes[faseDeco - 1] = mensajeResultado;
                        faseDeco++;
                        actualFaseFrame = 0;
                        actualGeneralFrame = 0;
                        mensajeAnteriorDecodificado = mensajeResultado;
                        vibrate();
                        Log.e(TAG, "Mensaje OK fase 2, size list: " + mensajeListaFase1.size()+", frame: "+actualGeneralFrame+", "+actualFaseFrame);
                    }
                }
            } else if (faseDeco == 3) {
                faseDeco++;

                if(mensajeResultado.equals(CODIGO_INICIO)) {
                    faseDeco = 5;
                    Log.e(TAG, "En Fase 3 msg igual a msg Fase 2: " +mensajeResultado);
                }
                else if(!mensajeAnteriorDecodificado.equals(mensajeResultado)) {

                    mensajeListaFase3.add(mensajeResultado);

                    if(actualFaseFrame == 0){

                        //Log.e(TAG, "Empezando Fase 3, fps: " + fpsMeter.getFps());
                        actualGeneralFrame = (int)Math.floor(fpsMeter.getFps() * (flickerTime/1000));//  -1;
                        Log.e(TAG, "Fase 3, Size list: " + mensajeListaFase3.size()+", frame: "+actualGeneralFrame+", "+actualFaseFrame);

                        actualFaseFrame++;
                    }
                    Log.e(TAG, "Mensaje"+mensajeListaFase3.size()+" Fase 3: " +mensajeResultado);
                    if (mensajeListaFase3.size() >= actualGeneralFrame) {
                        mensajes[faseDeco - 1] = mensajeResultado;
                        faseDeco++;
                        actualFaseFrame = 0;
                        actualGeneralFrame = 0;
                        mensajeAnteriorDecodificado = mensajeResultado;
                        vibrate();
                        Log.e(TAG, "Mensaje OK fase 3, size list: " + mensajeListaFase1.size()+", frame: "+actualGeneralFrame+", "+actualFaseFrame);
                    }
                }
                else{
                    Log.e(TAG, "Mensaje anterior fase 3 igual al anterior de fase 2: " +mensajeResultado);
                }
            }
        }

        if (faseDeco == 4){
            if (mensajeListaFase1.size() > 0  && mensajeListaFase2.size() > 0){ //&& mensajeListaFase3.size() > 0) {

                mensajeFinal = mostCommon(mensajeListaFase1)+mostCommon(mensajeListaFase2); //+mostCommon(mensajeListaFase3);

                Log.e(TAG, "Mensaje Final: " + mensajeResultado);

                if(verificaCodigo(mensajeFinal)) {
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
                            estimatedDistY = Math.abs(Math.hypot(estimatedDist, estimatedDistX) * Math.cos(yaw) * Math.cos(tilt));

                            Log.e(TAG, "Width image: " + anchoObjetoImagen + " , distance: " + estimatedDist);

                            if (estimatedDist > 1) {

                                faseDeco++;

                                vibrate();
                                //mensajeListaFase0.clear();
                                mensajeListaFase1.clear();
                                mensajeListaFase2.clear();
                                mensajeListaFase3.clear();
                                actualFaseFrame = 0;
                                actualGeneralFrame = 0;
                                //Toast.makeText(this, mensajeFinal, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "onCam ancho imagen: " + anchoObjetoImagen);
                                Log.e(TAG, "onCam distancia: " + estimatedDist);
                                Intent intent = new Intent(this, LobbyActivity.class);
                                intent.putExtra("codigo", mensajeFinal);
                                intent.putExtra("distanciaX", estimatedDistX);
                                intent.putExtra("distanciaY", estimatedDistY);
                                setResult(LobbyActivity.RESULT_OK, intent);
                                finish();
                            }
                        }
                    }else{

                        Log.e(TAG, "Frame <= 10, actual: "+ amountFrameEstDistance);
                    }
                }else{

                    faseDeco = 5;
                    Log.e(TAG, "Codigo Final no localizado en server: " + mensajeResultado);
                }
            }
            else{
                Log.e(TAG, "Algunas de las lista de deco esta vacia.");
            }
        }*/

        Imgproc.putText(mResultado, "T: " + String.format("%.2f", Math.toDegrees(tilt)) +
                        ", P: " + String.format("%.2f", Math.toDegrees(yaw)) +
                        ", R: " + String.format("%.2f", 360-Math.toDegrees(roll)),
                new Point(10, 23),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Size Original  : (" +
                        String.format("%.2f", mObjectSize.get(0, 0)[0]) + "px, " +
                        String.format("%.2f", mObjectSize.get(0, 1)[0]) + "px)",
                new Point(10, 50), //140
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Size Corrected: (" +
                        String.format("%.2f", mObjectSize.get(0, 2)[0]) + "px, " +
                        String.format("%.2f", mObjectSize.get(0, 3)[0]) + "px)",
                new Point(10, 80), //140
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Estimated Dist: " +
                        String.format("%.2f", estimatedDist / 10) + " cm (X: " +
                        String.format("%.2f", estimatedDistX / 10) +  ", Y: "+
                        String.format("%.2f", estimatedDistY / 10)+")",
                new Point(10, 110),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Fase: " + faseDeco,
                new Point(10, 140),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Mensaje: " + mensajeResultado,
                new Point(10, 170),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);

        Imgproc.putText(mResultado, "Calc. distancia: " + amountFrameEstDistance,
                new Point(10, 200),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        return mResultado;
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
                //e.printStackTrace();
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

    private void vibrate() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(50,50));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(50);
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
        return max.getKey();
    }

    private void getCodigosServer() {
        if (LobbyActivity.retrofit != null) {
            Call<CodigosServer> request = LobbyActivity.restClient.getCodigosServer();

            request.enqueue(new Callback<CodigosServer>() {
                @Override
                public void onResponse(Call<CodigosServer> call, Response<CodigosServer> response) {

                    if (response.isSuccessful()) {
                        String rsp = response.body().getResponse();

                        Log.e("onResponse codigos: ", rsp);

                        switch(rsp){
                            case "0":{
                                codigosServer = null;
                                codigosServer = response.body().getAsistencias();
                                Toast.makeText(getBaseContext(), "Server OK Codigos",
                                        Toast.LENGTH_SHORT).show();
                            }
                            case "1":{
                                codigosServer.clear();
                                codigosServer = null;
                                Toast.makeText(getBaseContext(), "Codigos NOK",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    else{
                        Toast.makeText(getBaseContext(), "Codigos NOK2",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CodigosServer> call, Throwable t) {
                    Toast.makeText(getBaseContext(), "Retrofit fail!", Toast.LENGTH_SHORT).show();
                }
            });        }
    }
}

