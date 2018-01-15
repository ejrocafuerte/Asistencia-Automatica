package com.app.house.asistenciaestudiante;

import android.Manifest;
import android.content.Context;
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
    private double altoObjetoImagen = 0.0f;
    private double estimatedDist = 0.0f;
    private double estimatedDistX = 0.0f;
    private double estimatedDistY = 0.0f;
    private String mensajeResultado = "";
    private double focalLength = 0.0f;
    private double focalLengthPixelX = 0.0f;
    private double focalLengthPixelY = 0.0f;
    private float horizonalAngle = 0.0f;
    private float verticalAngle = 0.0f;
    private double sensorWidth = 0.0f;
    private double sensorHeight = 0.0f;
    private float anchoObjetoReal = 1775.0f;
    private int max_width = 0;
    private int max_height = 0;
    private double pan = 0.0f;
    private double tilt = 0.0f;
    private double roll = 0.0f;
    private double initialpan = 135.0f;
    private boolean opencvLoaded = false;

    private WindowManager mWindowManager;

    //Setup variables for the SensorManger, the SensorEventListeners,
    // the Sensors, and the arrays to hold the resultant sensor values
    private SensorManager mSensorManager;
    private MySensorEventListener oriSensorEventListener;
    Sensor ori_sensor, sensorAccelerometer, sensorMagnetometer, sensorGyroscope ;
    float[] ori_values = new float[3];


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
        mWindowManager = getWindow().getWindowManager();
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
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(oriSensorEventListener);
            mSensorManager = null;
        }
        super.onPause();
        disableCamera();
    }

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
        if(mSensorManager == null) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            // Build a SensorEventListener for each type of sensor (just one here):
            oriSensorEventListener = new MySensorEventListener();
            // Get each of our Sensors (just one here):

            sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (sensorAccelerometer != null && sensorMagnetometer != null && sensorGyroscope != null){
                // Success! There's a magnetometer.
                ori_sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                Log.e(TAG,  "Encontrado acelerometro, magnetometro y giroscopio");
            }
            else if(sensorAccelerometer != null && sensorGyroscope != null){
                // Failure! No magnetometer.
                ori_sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                Log.e(TAG,  "Encontrado acelerometro y giroscopio");
            }
            else if(sensorAccelerometer != null && sensorMagnetometer != null){
                ori_sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
                Log.e(TAG,  "Encontrado acelerometro y magnetometro " + ((ori_sensor == null)?". Sensor null":""));
            }
            else{
                Log.e(TAG,  "Sensores necesitados, imposible calcular angulos euler");
            }

            // Register the SensorEventListeners with their Sensor, and their SensorManager (just one here):
            //mSensorManager.registerListener(oriSensorEventListener, sensorMagnetometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(oriSensorEventListener, ori_sensor, SensorManager.SENSOR_DELAY_GAME);

        }
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
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(oriSensorEventListener);
            mSensorManager = null;
        }
        super.onDestroy();
        disableCamera();
    }

    public void disableCamera() {
        if (_cameraBridgeViewBase != null)
            _cameraBridgeViewBase.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        Log.e(TAG, "Resolution: " + (width + "x" + height));
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        mResultado = new Mat(width, height, CvType.CV_8UC4);


        mObjectSize = new Mat(1, 4, CvType.CV_64FC1);
        mParameters = new Mat(1, 5, CvType.CV_64FC1);
        max_width = width;
        max_height = height;
        getCameraParameters();

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();

        mParameters.put(0,0, tilt);//tilt);
        mParameters.put(0,1, initialpan - pan);//;
        mParameters.put(0,2, roll);//;
        mParameters.put(0,3, horizonalAngle);
        mParameters.put(0,4, verticalAngle);

        mensajeResultado = decodificar(mRGBA.getNativeObjAddr(), mResultado.getNativeObjAddr(), mObjectSize.getNativeObjAddr(), mParameters.getNativeObjAddr());

        anchoObjetoImagen = (float) mObjectSize.get(0, 2)[0];
        altoObjetoImagen = (float) mObjectSize.get(0, 3)[0];

        estimatedDist = (anchoObjetoReal * focalLength * mRGBA.cols()) / (sensorWidth * anchoObjetoImagen);//??

        estimatedDistY = Math.abs(estimatedDist  * (float)Math.cos(Math.toRadians(initialpan - pan)) * (float)Math.cos(Math.toRadians(tilt)));
        estimatedDistX = -estimatedDistY * (float)Math.sin(Math.toRadians(initialpan - pan));


        Imgproc.putText(mResultado, "T: " + String.format("%.2f", tilt) +
                        ", P: " + String.format("%.2f", initialpan - pan) +
                        ", R: " + String.format("%.2f", roll),
                new Point(10, 23),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        Imgproc.putText(mResultado, "Size Camera  : (" +
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
        Imgproc.putText(mResultado, "Mensaje: " + mensajeResultado,
                new Point(10, 140),
                Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(255, 0, 0), 2);
        return mResultado;
    }

    public void getCameraParameters() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

            try {

                //for (String cameraId : manager.getCameraIdList()) {
                String cameraId = "0";
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
                float[] focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                SizeF sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

                sensorWidth = sensorSize.getWidth();
                sensorHeight = sensorSize.getHeight();

                //Log.e("mr", "Camera " + cameraId + " has sensorSize == " + Float.toString(2.0f*w) + ", " + Float.toString(2.0f*h));

                //for (int focusId=0; focusId<focalLengths.length; focusId++) {
                int focusId = 0;
                focalLength = focalLengths[focusId];
                focalLengthPixelX = (focalLength / sensorWidth) * max_width;
                focalLengthPixelY = (focalLength / sensorHeight) * max_height;
                horizonalAngle = (float) Math.toDegrees(2 * Math.atan(0.5f * sensorWidth / focalLength));
                verticalAngle = (float) Math.toDegrees(2 * Math.atan(0.5f * sensorHeight / focalLength));
                Log.e(TAG, horizonalAngle + " : " + verticalAngle);
                //}
                //}
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            // do something for phones running an SDK before lollipop
        }
    }

    public native static String decodificar(long mRGBA, long mResultado, long mObjectSize, long mParameters);

    // Setup our SensorEventListener
    public class MySensorEventListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            int eventType = event.sensor.getType();

            if (eventType == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                ori_values = event.values.clone();
                updateOrientation(event.values);
            }
            else if (eventType == Sensor.TYPE_ROTATION_VECTOR) {
                ori_values = event.values.clone();
                updateOrientation(event.values);
            }
            else if (eventType == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
                ori_values = event.values.clone();
                updateOrientation(event.values);
            }

            //float[] mRotationMatrixCurrent = new float[9];
            //SensorManager.getRotationMatrixFromVector(mRotationMatrixCurrent, ori_values);

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private void updateOrientation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix);

        // Transform rotation matrix into azimuth/pitch/roll
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);

        // Convert radians to degrees
        tilt = orientation[1] * -180.0f / (float)Math.PI;
        roll = orientation[2] * -180.0f / (float)Math.PI;
        pan = orientation[0] * -180.0f / (float)Math.PI;

        //if(Math.abs(pan) > 5.0) finish();
    }
}

