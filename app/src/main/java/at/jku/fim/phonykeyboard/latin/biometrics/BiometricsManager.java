package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import at.jku.fim.phonykeyboard.keyboard.Key;

public abstract class BiometricsManager implements SensorEventListener {
    private static final String TAG = "BiometricsManager";
    private static final String BROADCAST_ACTION_GET_CONFIDENCE = "at.jku.fim.phonykeyboard.BIOMETRICS_GET_CONFIDENCE";
    private static final String BROADCAST_ACTION_CLEAR_DATA = "at.jku.fim.phonykeyboard.BIOMETRICS_CLEAR_DATA";
    public static final int EVENT_DOWN = 0, EVENT_UP = 1;

    private static BiometricsManager instance;

    private SensorManager sensorManager;
    private final int[] sensorTypes;
    private Dictionary<Sensor, float[]> sensors = new Hashtable<>();
    private BiometricsReceiver receiver;

    protected static final float[] EMPTY_SENSOR_DATA = new float[0];
    protected Context context;

    public BiometricsManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }
        else {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }
    }

    public void init(Context context) {
        this.context = context;
        instance = this;

        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        for (int sensorType : sensorTypes){
            if (sensorManager.getDefaultSensor(sensorType) != null) {
                sensors.put(sensorManager.getDefaultSensor(sensorType), EMPTY_SENSOR_DATA);
            }
        }
    }

    public static BiometricsManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Must init() before getting instance");
        }
        return instance;
    }

    public Dictionary<Sensor, float[]> getSensors() {
        return sensors;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensors.put(event.sensor, event.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onStartInput(EditorInfo editorInfo) {
    }

    public void onCreate() {
        Enumeration<Sensor> sensorEnum = sensors.keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }

        receiver = new BiometricsReceiver();
        IntentFilter filter = new IntentFilter(BROADCAST_ACTION_GET_CONFIDENCE);
        context.registerReceiver(receiver, filter);
    }

    public void onDestroy() {
        sensorManager.unregisterListener(this);

        context.unregisterReceiver(receiver);
        receiver = null;
    }

    public abstract void onShowWindow();
    public abstract void onHideWindow();

    public abstract void onKeyDown(final Key key, final MotionEvent event);
    public abstract void onKeyUp(final Key key, final MotionEvent event);

    public String getSensorType(Sensor sensor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return sensor.getStringType();
        }
        else {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    return "Accelerometer";
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    return "AmbientTemperature";
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    return "GameRotationVector";
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    return "GeomagneticRotationVector";
                case Sensor.TYPE_GRAVITY:
                    return "Gravity";
                case Sensor.TYPE_GYROSCOPE:
                    return "Gyroscope";
                case Sensor.TYPE_HEART_RATE:
                    return "Heartrate";
                case Sensor.TYPE_LIGHT:
                    return "Light";
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    return "LinearAcceleration";
                case Sensor.TYPE_MAGNETIC_FIELD:
                    return "MagneticField";
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    return "MagneticFieldUncalibrated";
                case Sensor.TYPE_PRESSURE:
                    return "Pressure";
                case Sensor.TYPE_PROXIMITY:
                    return "Proximity";
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    return "RelativeHumidity";
                case Sensor.TYPE_ROTATION_VECTOR:
                    return "RotationVector";
                case Sensor.TYPE_SIGNIFICANT_MOTION:
                    return "SignificantMotion";
                case Sensor.TYPE_STEP_COUNTER:
                    return "StepCounter";
                case Sensor.TYPE_STEP_DETECTOR:
                    return "StepDetector";
            }
            Log.w(TAG, "Couldn't resolve sensor to type: " + sensor.toString());
            return null;
        }
    }

    protected int getScreenOrientation() {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getRotation();
    }

    private class BiometricsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BROADCAST_ACTION_GET_CONFIDENCE:
                    getConfidence(intent);
                    break;
                case BROADCAST_ACTION_CLEAR_DATA:
                    clearData(intent);
                    break;
            }
        }

        public void getConfidence(Intent intent) {

        }

        public void clearData(Intent intent) {

        }
    }
}
