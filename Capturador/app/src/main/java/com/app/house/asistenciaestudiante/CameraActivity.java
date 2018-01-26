package com.app.house.asistenciaestudiante;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Camera;
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

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";
    private CameraBridgeViewBase _cameraBridgeViewBase;
    private Mat mRGBA, mResultado, mObjectSize, mParameters;// mEulerAngles;
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
    private float anchoObjetoReal = 750;//1750;// mm
    private double yaw = 0.0f;
    private double tilt = 0.0f;
    private double roll = 0.0f;
    private double initialYaw = 135.0f;
    private  double finalYaw = 0.0f;
    private double flickerTime = 2000;//ms
    private double actualTime = 0;
    private int faseDeco = 0;
    private String[] mensajes = {"", "", ""};
    private String mensajeFinal = "";
    private boolean firstFrame = false;


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
        
        /*if(ori_sensor == null)

            finalYaw = yaw;
        else
            finalYaw = initialYaw - yaw;*/

        mParameters.put(0,0, tilt);
        mParameters.put(0,1, finalYaw);
        mParameters.put(0,2, roll);
        mParameters.put(0,3, horizonalAngle);
        mParameters.put(0,4, verticalAngle);
        mParameters.put(0,5, faseDeco);
        mParameters.put(0,6, focalPx);
///////////////////////
       mensajeResultado = decodificar(mRGBA.getNativeObjAddr(),
                mResultado.getNativeObjAddr(),
                mObjectSize.getNativeObjAddr(),
                mParameters.getNativeObjAddr());

       tilt = mParameters.get(0,0)[0];
       finalYaw = mParameters.get(0,1)[0];
       roll = mParameters.get(0,2)[0];

        anchoObjetoImagen = mObjectSize.get(0, 2)[0];// * Math.abs(((mObjectSize.get(0, 0)[0] / mObjectSize.get(0, 2)[0] )));

        estimatedDist = (anchoObjetoImagen > 0) ? (anchoObjetoReal * focalLength * mRGBA.cols()) / (sensorWidth * anchoObjetoImagen) : 0;//??

        estimatedDistX = -estimatedDist * Math.sin(finalYaw) * Math.cos(tilt);
        estimatedDistY = Math.abs(Math.hypot(estimatedDist, estimatedDistX) /*Math.cos(finalYaw) */* Math.cos(tilt));
////////////////////////
        /*if(System.currentTimeMillis() - actualTime > flickerTime) {
            mensajeResultado = decodificar(mRGBA.getNativeObjAddr(),
                    mResultado.getNativeObjAddr(),
                    mObjectSize.getNativeObjAddr(),
                    mParameters.getNativeObjAddr());

            faseDeco = (int) mParameters.get(0, 5)[0];
            //Log.e(TAG, "Fase deco: " + faseDeco);

            if(faseDeco < 4) {


                if (!mensajeResultado.equals("")) {
                    Log.e(TAG, "onCam Mensaje: " + mensajeResultado + ", fasedeco: "+faseDeco);
                    if (faseDeco > 0) {
                        mensajes[faseDeco-1] = mensajeResultado;
                        Log.e(TAG, "mensajes[faseDeco - 2] =" + mensajeResultado);
                    }

                    //if(faseDeco == 2){ faseDeco = 3; vibrate();}

                    faseDeco++;
                    actualTime = System.currentTimeMillis();
                }

            }
            else if (faseDeco == 4){
                if (mensajes[0].length() == 12 && mensajes[1].length() == 12 && mensajes[2].length() == 12) {

                    Log.e(TAG, "Mensaje 1: " + mensajes[0]);
                    Log.e(TAG, "Mensaje 2: " + mensajes[1]);
                    Log.e(TAG, "Mensaje 3: " + mensajes[2]);

                    if (mensajes[0].equals(mensajes[1]) && mensajes[0].equals(mensajes[2])) {
                        mensajeFinal = mensajes[0];
                        Log.e(TAG, "Mensaje decodificado: 100% accuracy: "+mensajeFinal);
                    } else if (mensajes[0].equals(mensajes[1]) || mensajes[0].equals(mensajes[2])) {
                        mensajeFinal = mensajes[0];
                        Log.e(TAG, "Mensaje decodificado: 66% accuracy: "+mensajeFinal);
                    } else {
                        mensajeFinal = mensajes[0];
                        Log.e(TAG, "Mensaje decodificado: 33% accuracy: "+mensajeFinal);
                    }
                }
                else {Log.e(TAG, "Mensaje 1:" + mensajes[0]);
                    Log.e(TAG, "Mensaje 2:" + mensajes[1]);
                    Log.e(TAG, "Mensaje 3:" + mensajes[2]);
                }

                        anchoObjetoImagen = mObjectSize.get(0, 2)[0] *
                Math.abs(((mObjectSize.get(0, 0)[0] / mObjectSize.get(0, 2)[0] )));

                estimatedDist = (anchoObjetoImagen > 0) ? (anchoObjetoReal * focalLength * mRGBA.cols()) / (sensorWidth * anchoObjetoImagen) : 0;//??

                estimatedDistY = Math.abs(estimatedDist * Math.cos(Math.toRadians(finalYaw)) * Math.cos(Math.toRadians(tilt)));
                estimatedDistX = -estimatedDist * Math.sin(Math.toRadians(finalYaw)) * Math.cos(Math.toRadians(tilt));

                if (anchoObjetoImagen > 1 && estimatedDist > 1) {

                    faseDeco = 0;

                    vibrate();
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
        }*/

        Imgproc.putText(mResultado, "T: " + String.format("%.2f", Math.toDegrees(tilt)) +
                        ", P: " + String.format("%.2f", Math.toDegrees(finalYaw)) +
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
        Imgproc.putText(mResultado, "Fase: " + (int)mParameters.get(0,5)[0],
                new Point(10, 140),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Mensaje: " + mensajeResultado,
                new Point(10, 170),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Tiempo: " + ((actualTime > 0)?(System.currentTimeMillis() - actualTime):0),
                new Point(10, 200),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        return mResultado;
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

    }

    private void vibrate() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150,10));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
        }
    }
}

