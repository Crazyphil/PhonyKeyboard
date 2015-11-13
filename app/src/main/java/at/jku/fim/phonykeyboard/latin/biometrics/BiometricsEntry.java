package at.jku.fim.phonykeyboard.latin.biometrics;

import android.view.MotionEvent;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.keyboard.KeyDetector;

public class BiometricsEntry {
    public static final int EVENT_DOWN = 0, EVENT_UP = 1;

    private int pointerId;
    private long timestamp;
    private int screenOrientation;
    private String key;
    private int event;
    private float x, y, size, orientation, pressure;
    private List<float[]> sensorData;

    public BiometricsEntry(final int sensorCount) {
        sensorData = new ArrayList<>(sensorCount);
    }

    public void setProperties(int eventType, Key key, MotionEvent event, int screenOrientation) {
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            pointerId = event.getPointerId(event.getActionIndex());
        } else {
            Assert.assertEquals(event.getPointerCount(), 1);
            pointerId = event.getPointerId(0);
        }

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

    public void addSensorData(float[] data) {
        sensorData.add(data);
    }

    public int getPointerId() {
        return pointerId;
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

    public List<float[]> getSensorData() {
        return sensorData;
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
