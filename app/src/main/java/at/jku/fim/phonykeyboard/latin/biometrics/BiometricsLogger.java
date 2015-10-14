package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.latin.utils.CsvUtils;

public class BiometricsLogger implements SensorEventListener {
    private Context context;
    private SensorManager sensorManager;
    private final int[] sensorTypes;
    private Dictionary<Sensor, float[]> sensors = new Hashtable<>();
    private Writer logStream;

    private static final String TAG = "BiometricsLogger";
    private static final String LOG_FILE = "biometrics_log.csv";
    private static final float[] EMPTY_SENSOR_DATA = new float[0];

    public BiometricsLogger() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }
        else {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }
    }

    public void init(Context context) {
        this.context = context;
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        for (int sensorType : sensorTypes){
            if (sensorManager.getDefaultSensor(sensorType) != null) {
                sensors.put(sensorManager.getDefaultSensor(sensorType), EMPTY_SENSOR_DATA);
            }
        }
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
    }

    public void onDestroy() {
        sensorManager.unregisterListener(this);
    }

    public void onShowWindow() {
        openOrCreateLog(context);
    }

    public void onHideWindow() {
        try {
            logStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Couldn't close biometrics log file", e);
        } finally {
            logStream = null;
        }
    }

    public void onKeyDown(final Key key, final MotionEvent event) {
        onKeyEvent(BiometricsLogEntry.EVENT_DOWN, key, event);
    }

    public void onKeyUp(final Key key, final MotionEvent event) {
        onKeyEvent(BiometricsLogEntry.EVENT_UP, key, event);
    }

    private void onKeyEvent(int eventType, Key key, MotionEvent event) {
        BiometricsLogEntry entry = new BiometricsLogEntry(sensors.size());
        entry.setProperties(eventType, key, event, getScreenOrientation());

        Enumeration<Sensor> sensorEnum = sensors.keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            entry.addSensorData(sensors.get(sensor));
        }

        writeLogEntry(entry);
    }

    private void openOrCreateLog(Context context) {
        if (logStream != null) {
            return;
        }

        String[] privateFiles;
        boolean isExternal = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (isExternal) {
            Log.d(TAG, "Using external storage for biometrics log");
            privateFiles = context.getExternalFilesDir(null).list();
        } else {
            Log.d(TAG, "Using internal storage for biometrics log");
            privateFiles = context.fileList();
        }

        boolean exists = false;
        for (String filename : privateFiles) {
            if (filename.equals(LOG_FILE)) {
                Log.d(TAG, "Biometrics log already exists, appending");
                exists = true;
            }
        }

        try {
            if (isExternal) {
                logStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(context.getExternalFilesDir(null), LOG_FILE), true)));
            } else {
                logStream = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(LOG_FILE, Context.MODE_APPEND)));
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "Biometrics log output file not found", e);
        }
        if (!exists) {
            writeCSVHeader();
        }
    }

    private void writeCSVHeader() {
        List<String> fields = new LinkedList<>();
        fields.add("Timestamp");
        fields.add("ScreenOrientation");
        fields.add("Key");
        fields.add("Event");
        fields.add("X");
        fields.add("Y");
        fields.add("Size");
        fields.add("Orientation");
        fields.add("Pressure");
        Enumeration<Sensor> sensorEnum = sensors.keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            fields.add(getSensorType(sensor));
        }
        try {
            logStream.write(CsvUtils.join(fields.toArray(new String[fields.size()])));
            logStream.write("\r\n");
        }
        catch (IOException e) {
            Log.e(TAG, "I/O error writing CSV header", e);
        }
    }

    private void writeLogEntry(BiometricsLogEntry entry) {
        List<String> fields = new LinkedList<>();
        fields.add(Long.toString(entry.getTimestamp()));
        fields.add(Integer.toString(entry.getScreenOrientation()));
        fields.add(entry.getKey());
        fields.add(Integer.toString(entry.getEvent()));
        fields.add(Float.toString(entry.getX()));
        fields.add(Float.toString(entry.getY()));
        fields.add(Float.toString(entry.getSize()));
        fields.add(Float.toString(entry.getOrientation()));
        fields.add(Float.toString(entry.getPressure()));
        Enumeration<Sensor> sensorEnum = sensors.keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            fields.add(arrayToString(sensors.get(sensor)));
        }
        try {
            logStream.write(CsvUtils.join(fields.toArray(new String[fields.size()])));
            logStream.write("\r\n");
        }
        catch (IOException e) {
            Log.e(TAG, String.format("I/O error writing %s event", entry.eventToString()), e);
        }
    }

    private String getSensorType(Sensor sensor) {
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

    private int getScreenOrientation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getRotation();
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder(1 + Math.max(array.length * 2, 1));
        sb.append("[");
        for (float f : array) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(Float.toString(f));
        }
        sb.append("]");
        return sb.toString();
    }
}
