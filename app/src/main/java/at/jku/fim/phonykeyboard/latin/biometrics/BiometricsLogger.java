package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.latin.utils.CsvUtils;

public class BiometricsLogger extends BiometricsManager implements SensorEventListener {
    private static final String TAG = "BiometricsLogger";
    private static final String LOG_FILE = "biometrics_log.csv";

    private Writer logStream;

    public void onShowWindow() {
        openOrCreateLog();
    }

    public void onHideWindow() {
        try {
            if (logStream != null) {
                logStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't close biometrics log file", e);
        } finally {
            logStream = null;
        }
    }

    public void onKeyDown(final Key key, final MotionEvent event) {
        writeLogEntry(buildEntry(key, event));
    }

    public void onKeyUp(final Key key, final MotionEvent event) {
        writeLogEntry(buildEntry(key, event));
    }

    @Override
    public double getConfidence() {
        return CONFIDENCE_NOT_ENOUGH_DATA;
    }

    @Override
    public boolean clearData() {
        return true;
    }

    private void openOrCreateLog() {
        if (logStream != null) {
            return;
        }

        String[] privateFiles;
        boolean isExternal = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (isExternal) {
            Log.d(TAG, "Using external storage for biometrics log");
            privateFiles = getContext().getExternalFilesDir(null).list();
        } else {
            Log.d(TAG, "Using internal storage for biometrics log");
            privateFiles = getContext().fileList();
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
                logStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(getContext().getExternalFilesDir(null), LOG_FILE), true)));
            } else {
                logStream = new BufferedWriter(new OutputStreamWriter(getContext().openFileOutput(LOG_FILE, Context.MODE_APPEND)));
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
        Enumeration<Sensor> sensorEnum = getSensors().keys();
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

    private void writeLogEntry(BiometricsEntry entry) {
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
        for (float[] data : entry.getSensorData()) {
            fields.add(arrayToString(data));
        }

        try {
            logStream.write(CsvUtils.join(fields.toArray(new String[fields.size()])));
            logStream.write("\r\n");
        }
        catch (IOException e) {
            Log.e(TAG, String.format("I/O error writing %s event", entry.eventToString()), e);
        }
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder(Math.max(array.length * 2 - 1, 0));
        for (float f : array) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(Float.toString(f));
        }
        return sb.toString();
    }
}
