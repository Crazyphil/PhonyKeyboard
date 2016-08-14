package at.jku.fim.phonykeyboard.latin.biometrics;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.latin.PhonyKeyboard;
import at.usmile.cormorant.api.AbstractConfidenceService;

public abstract class BiometricsManager implements SensorEventListener {
    private static final String TAG = "BiometricsManager";

    public static final String BROADCAST_ACTION_GET_SCORE = "at.jku.fim.phonykeyboard.BIOMETRICS_GET_SCORE";
    public static final String BROADCAST_EXTRA_LAXNESS = "at.jku.fim.phonykeyboard.BIOMETRICS_LAXNESS";
    public static final String BROADCAST_EXTRA_RESULT = "at.jku.fim.phonykeyboard.BIOMETRICS_RESULT";
    public static final String BROADCAST_ACTION_CLEAR_DATA = "at.jku.fim.phonykeyboard.BIOMETRICS_CLEAR_DATA";
    public static final double SCORE_NOT_ENOUGH_DATA = -1, SCORE_CAPTURING_ERROR = -2, SCORE_CAPTURING_DISABLED = -3;

    private static BiometricsManager instance;

    private PhonyKeyboard keyboard;
    private SensorManager sensorManager;
    private final int[] sensorTypes;
    private Dictionary<Sensor, float[]> sensors = new Hashtable<>();
    private BiometricsReceiver receiver;
    private CormorantAuthenticationService cormorantService;

    protected static final float[] EMPTY_SENSOR_DATA = new float[0];

    protected BiometricsManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        } else {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }
    }

    public void init(PhonyKeyboard keyboard) {
        this.keyboard = keyboard;
        instance = this;

        sensorManager = (SensorManager)keyboard.getSystemService(Context.SENSOR_SERVICE);
        for (int sensorType : sensorTypes) {
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

    public void onCreate() {
        Enumeration<Sensor> sensorEnum = sensors.keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }

        receiver = new BiometricsReceiver();
        IntentFilter filter = new IntentFilter(BROADCAST_ACTION_GET_SCORE);
        filter.addAction(BROADCAST_ACTION_CLEAR_DATA);
        filter.setPriority(1);
        keyboard.registerReceiver(receiver, filter);

        Intent intent = new Intent(keyboard, CormorantAuthenticationService.class);
        keyboard.bindService(intent, cormorantConnection, Context.BIND_AUTO_CREATE);
    }

    public void onDestroy() {
        keyboard.unbindService(cormorantConnection);
        sensorManager.unregisterListener(this);

        keyboard.unregisterReceiver(receiver);
        receiver = null;
    }

    public abstract void onStartInputView(EditorInfo editorInfo, boolean restarting);
    public abstract void onFinishInputView(boolean finishInput);

    public abstract void onKeyDown(final Key key, final MotionEvent event);
    public abstract void onKeyUp(final Key key, final MotionEvent event);

    public abstract double getScore(double laxness);
    public abstract double getLastScore();
    public abstract boolean clearData();

    public String getSensorType(Sensor sensor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return sensor.getStringType().substring(sensor.getStringType().lastIndexOf('.') + 1);
        }
        else {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    return "accelerometer";
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    return "ambient_temperature";
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    return "game_rotation_vector";
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    return "geomagnetic_rotation_vector";
                case Sensor.TYPE_GRAVITY:
                    return "gravity";
                case Sensor.TYPE_GYROSCOPE:
                    return "gyroscope";
                case Sensor.TYPE_HEART_RATE:
                    return "heart_rate";
                case Sensor.TYPE_LIGHT:
                    return "light";
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    return "linear_acceleration";
                case Sensor.TYPE_MAGNETIC_FIELD:
                    return "magnetic_field";
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    return "magnetic_field_uncalibrated";
                case Sensor.TYPE_PRESSURE:
                    return "pressure";
                case Sensor.TYPE_PROXIMITY:
                    return "proximity";
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    return "relative_humidity";
                case Sensor.TYPE_ROTATION_VECTOR:
                    return "rotation_vector";
                case Sensor.TYPE_SIGNIFICANT_MOTION:
                    return "significant_motion";
                case Sensor.TYPE_STEP_COUNTER:
                    return "step_counter";
                case Sensor.TYPE_STEP_DETECTOR:
                    return "step_detector";
            }
            Log.w(TAG, "Couldn't resolve sensor to type: " + sensor.toString());
            return null;
        }
    }

    public int getScreenOrientation() {
        WindowManager wm = (WindowManager)keyboard.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getRotation();
    }

    public CharSequence getInputText() {
        return keyboard.getText();
    }

    public String getVerifiedPackageName(EditorInfo editorInfo) {
        InputBinding binding = ((PhonyKeyboard)getContext()).getCurrentInputBinding();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 && binding != null) {
            PackageManager pm = getContext().getPackageManager();
            return pm.getNameForUid(binding.getUid());
        } else {
            return editorInfo.packageName;    // Package name is system verified since Android M
        }
    }

    protected BiometricsEntry buildEntry(Key key, MotionEvent event) {
        int eventType = BiometricsEntry.EVENT_DOWN;
        if ((event.getAction() & (MotionEvent.ACTION_POINTER_UP | MotionEvent.ACTION_UP)) > 0) {
            eventType = BiometricsEntry.EVENT_UP;
        }

        BiometricsEntry entry = new BiometricsEntry(getSensors().size());
        entry.setProperties(eventType, key, event, getScreenOrientation());

        Enumeration<Sensor> sensorEnum = getSensors().keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            entry.addSensorData(getSensors().get(sensor));
        }
        return entry;
    }

    protected void addExtraScoreData(Bundle result) { }

    public Context getContext() {
        return keyboard;
    }

    private class BiometricsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BROADCAST_ACTION_GET_SCORE:
                    double securityLevel = intent.getDoubleExtra(BROADCAST_EXTRA_LAXNESS, 0.5);
                    getScore(securityLevel);
                    break;
                case BROADCAST_ACTION_CLEAR_DATA:
                    clearData();
                    break;
            }
        }

        public void getScore(double laxness) {
            if (!isOrderedBroadcast()) return;

            Bundle result = new Bundle(1);
            double score;
            if (BiometricsPolicy.getInstance().isBiometricsAllowed()) {
                score = BiometricsManager.this.getScore(laxness);
                BiometricsManager.this.addExtraScoreData(result);
            } else {
                score = SCORE_CAPTURING_DISABLED;
            }
            result.putInt(BROADCAST_EXTRA_RESULT, score > 0 ? 1 : (int)score);

            setResultCode((score <= SCORE_NOT_ENOUGH_DATA) ? Activity.RESULT_CANCELED : Activity.RESULT_OK);
            setResultExtras(result);

            if (cormorantService != null) {
                cormorantService.publishConfidenceUpdate(score);
            }
        }

        public void clearData() {
            boolean result = true;
            if (BiometricsPolicy.getInstance().isBiometricsAllowed()) {
                result = BiometricsManager.this.clearData();
            }
            setResultCode(result ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
        }
    }


    public ServiceConnection cormorantConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            AbstractConfidenceService.ConfidencePluginServiceBinder binder = (AbstractConfidenceService.ConfidencePluginServiceBinder)service;
            cormorantService = (CormorantAuthenticationService)binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            cormorantService = null;
        }
    };
}
