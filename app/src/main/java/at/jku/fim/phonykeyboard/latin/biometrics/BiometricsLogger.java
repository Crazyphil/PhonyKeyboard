package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.latin.utils.CsvUtils;

public class BiometricsLogger implements SensorEventListener {
    private final Context context;
    private SensorManager sensorManager;
    private Dictionary<Sensor, float[]> sensors = new Hashtable<>();
    private Writer logStream;

    private static final String TAG = "at.jku.fim.phonykbd";
    private static final String LOG_FILE = "biometrics_log.csv";

    public BiometricsLogger(Context context) {
        int[] sensorTypes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }
        else {
            sensorTypes = new int[] { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR };
        }

        this.context = context;
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        for (int sensorType : sensorTypes){
            if (sensorManager.getDefaultSensor(sensorType) != null) {
                sensors.put(sensorManager.getDefaultSensor(sensorType), null);
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
        openOrCreateLog(context);

        Enumeration<Sensor> sensorEnum = sensors.keys();
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void onDestroy() {
        try {
            logStream.close();
        }
        catch (IOException e) {
            // Nothing to do if closing a stream fails
        }
        sensorManager.unregisterListener(this);
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
        int i = 0;
        while (sensorEnum.hasMoreElements()) {
            Sensor sensor = sensorEnum.nextElement();
            entry.setSensorData(i, sensors.get(sensor));
            i++;
        }

        writeLogEntry(entry);
    }

    private void openOrCreateLog(Context context) {
        String[] privateFiles = context.fileList();
        boolean exists = false;
        for (String filename : privateFiles) {
            if (filename.equals(LOG_FILE)) {
                exists = true;
            }
        }

        try {
            logStream = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(LOG_FILE, Context.MODE_APPEND)));
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "Log output file not found", e);
        }
        if (!exists) {
            writeCSVHeader();
        }
    }

    private void writeCSVHeader() {
        if (logStream == null) return;

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
            return null;
        }
    }

    private int getScreenOrientation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getRotation();
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2 + 1);
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
