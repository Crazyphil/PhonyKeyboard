package at.jku.fim.phonykeyboard.latin.biometrics;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.keyboard.KeyDetector;

public class BiometricsLogEntry {
    public static final int EVENT_DOWN = 0, EVENT_UP = 1;

    private long timestamp;
    private int screenOrientation;
    private String key;
    private int event;
    private float x, y, size, orientation, pressure;
    private List<float[]> sensorData;
    private int sensorCount;

    public BiometricsLogEntry(final int sensorCount) {
        this.sensorCount = sensorCount;
        sensorData = new ArrayList<>(sensorCount);
    }

    public void setProperties(int eventType, Key key, MotionEvent event, int screenOrientation) {
        timestamp = event.getEventTime();
        this.screenOrientation = screenOrientation;
        this.key = KeyDetector.printableCode(key);
        this.event = eventType;
        x = event.getX() - key.getX();
        y = event.getY() - key.getY();
        size = event.getSize();
        orientation = event.getOrientation();
        pressure = event.getPressure();
    }

    public void setSensorData(int index, float[] data) {
        if (index < 0 || index > sensorCount - 1) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        sensorData.set(index, data);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getScreenOrientation() {
        return screenOrientation;
    }

    public void setScreenOrientation(int screenOrientation) {
        this.screenOrientation = screenOrientation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getOrientation() {
        return orientation;
    }

    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public String eventToString() {
        switch (event) {
            case EVENT_DOWN:
                return "KeyDown";
            case EVENT_UP:
                return "KeyUp";
            default:
                return null;
        }
    }
}
