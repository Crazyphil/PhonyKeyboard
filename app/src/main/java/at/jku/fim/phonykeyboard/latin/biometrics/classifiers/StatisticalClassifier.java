package at.jku.fim.phonykeyboard.latin.biometrics.classifiers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import at.jku.fim.phonykeyboard.latin.Constants;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsEntry;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManager;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManagerImpl;
import at.jku.fim.phonykeyboard.latin.biometrics.data.Contract;
import at.jku.fim.phonykeyboard.latin.biometrics.data.StatisticalClassifierContract;
import at.jku.fim.phonykeyboard.latin.utils.CsvUtils;

/**
 * This Classifier implements the statistical classifier proposed by Maiorana, et al. 2011, extended by further features.
 */
public class StatisticalClassifier extends Classifier {
    private static final String TAG = "StatisticalClassifier";
    private static final int INDEX_DOWNDOWN = 0, INDEX_DOWNUP = 1, INDEX_SIZE =  2, INDEX_ORIENTATION = 3, INDEX_PRESSURE = 4, INDEX_POSITION = 5, INDEX_SENSOR_START = 6;
    private static final String MULTI_VALUE_SEPARATOR = "|";
    private static final int TEMPLATE_SET_SIZE = 10;

    private final StatisticalClassifierContract dbContract;
    private final Pattern multiValueRegex = Pattern.compile(MULTI_VALUE_SEPARATOR);

    private int screenOrientation;
    private List<double[][][]> acquisitions;  // datapoint<[row][col][values]>
    private double[] means;
    private boolean invalidData;
    private ActiveBiometricsEntries activeEntries = new ActiveBiometricsEntries();
    private List<List<double[]>> currentData;   // datapoint<col<[values]>>

    /** Set to true when the user clicked the Next, Previous or Enter button and therefore submitted the input to the app **/
    private boolean submittedInput;
    /** Set to true when calcConfidence() was successful **/
    private boolean calculatedConfidence;
    private double confidence = BiometricsManager.CONFIDENCE_NOT_ENOUGH_DATA;

    public StatisticalClassifier(BiometricsManagerImpl manager) {
        super(manager);

        String[] sensorTypes = new String[manager.getSensors().size()];
        Enumeration<Sensor> sensors = manager.getSensors().keys();
        int i = 0;
        for (Sensor sensor : IterableEnumeration.asIterable(sensors)) {
            sensorTypes[i] = manager.getSensorType(sensor);
            i++;
        }
        dbContract = new StatisticalClassifierContract(sensorTypes);
    }

    @Override
    public Contract getDatabaseContract() {
        return dbContract;
    }

    @Override
    public double getConfidence() {
        if (!calculatedConfidence) {
            calcConfidence();
            saveBiometricData();
        }
        return invalidData ? BiometricsManager.CONFIDENCE_CAPTURING_ERROR : confidence;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onStartInput(long context, boolean restarting) {
        if (restarting || invalidData) {
            if (submittedInput) return;

            CharSequence text = manager.getInputText();
            if (text != null && text.length() != 0) {
                invalidData = true;
            } else {
                recoverFromInvalidData();
            }
            return;
        }

        SQLiteDatabase db = manager.getDb();
        screenOrientation = manager.getScreenOrientation();

        List<String> columns = new ArrayList<>(dbContract.getSensorColumns().length + INDEX_SENSOR_START);
        columns.add(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_KEY_DOWNDOWN);
        columns.add(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_KEY_DOWNUP);
        columns.add(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_SIZE);
        columns.add(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_ORIENTATION);
        columns.add(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_PRESSURE);
        columns.add(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_POSITION);
        for (String sensor : dbContract.getSensorColumns()) {
            columns.add(sensor);
        }

        Cursor c = db.query(StatisticalClassifierContract.StatisticalClassifierData.TABLE_NAME,
                columns.toArray(new String[columns.size()]),
                StatisticalClassifierContract.StatisticalClassifierData.COLUMN_CONTEXT + " = ? AND " + StatisticalClassifierContract.StatisticalClassifierData.COLUMN_SCREEN_ORIENTATION + " = ?",
                new String[] { String.valueOf(context), String.valueOf(screenOrientation) },
                null, null, null);

        currentData = new ArrayList<>(c.getColumnCount());
        for (int i = 0; i < c.getColumnCount(); i++) {
            currentData.add(new LinkedList<double[]>());
        }

        submittedInput = false;
        calculatedConfidence = false;
        confidence = BiometricsManager.CONFIDENCE_NOT_ENOUGH_DATA;

        means = new double[c.getColumnCount()];
        acquisitions = new ArrayList<>(c.getColumnCount());
        for (int i = 0; i < c.getColumnCount(); i++) {
            acquisitions.add(new double[c.getCount()][0][0]);
            calcStatistics(c, i);
        }
        c.close();
    }

    @Override
    public void onFinishInput(boolean done) {
        if (!done) {
            invalidData = true;
        } else {
            calcConfidence();
        }
    }

    @Override
    public void onKeyEvent(BiometricsEntry entry) {
        if (invalidData) {
            if (manager.getInputText().length() == 0) {
                recoverFromInvalidData();
            } else {
                return;
            }
        }
        if (entry.getKeyCode() == Constants.CODE_DELETE || entry.getKeyCode() == Constants.CODE_SETTINGS || entry.getScreenOrientation() != screenOrientation) {
            invalidData = true;
            return;
        }
        if (entry.getKeyCode() == Constants.CODE_ACTION_NEXT || entry.getKeyCode() == Constants.CODE_ACTION_PREVIOUS || entry.getKeyCode() == Constants.CODE_ENTER) {
            submittedInput = true;
            return;
        }

        if (entry.getEvent() == BiometricsEntry.EVENT_UP) {
            BiometricsEntry downEntry = activeEntries.getDownEntry(entry.getPointerId());
            if (downEntry == null) {
                Log.e(TAG, "BUG: Got UP event, but no matching DOWN event found");
            } else {
                currentData.get(INDEX_DOWNUP).add(new double[] { entry.getTimestamp() - downEntry.getTimestamp() });
            }
            currentData.get(INDEX_POSITION).add(new double[] { entry.getX(), entry.getY() });
            activeEntries.removeById(entry.getPointerId());
        } else {
            BiometricsEntry prevEntry = activeEntries.getLastDownEntry(entry.getTimestamp());
            if (prevEntry != null) {
                currentData.get(INDEX_DOWNDOWN).add(new double[] { entry.getTimestamp() - prevEntry.getTimestamp() });
                for (int i = 0; i < entry.getSensorData().size(); i++) {
                    float[] prevData = prevEntry.getSensorData().get(i);
                    double[] sensorData = new double[prevData.length];
                    for (int j = 0; j < sensorData.length; j++) {
                        sensorData[j] = entry.getSensorData().get(i)[j] - prevData[j];
                    }
                    currentData.get(INDEX_SENSOR_START + i).add(sensorData);
                }
            }
            currentData.get(INDEX_SIZE).add(new double[] { entry.getSize() });
            currentData.get(INDEX_ORIENTATION).add(new double[] { entry.getOrientation() });
            currentData.get(INDEX_PRESSURE).add(new double[]{entry.getPressure()});
            activeEntries.add(entry);
        }
    }

    @Override
    public void onDestroy() {
    }

    private void calcStatistics(Cursor c, int columnIndex) {
        if (c.getCount() == 0) return;
        double[][][] values = acquisitions.get(columnIndex);

        // Load rows from DB to array
        c.moveToPosition(-1);
        int row = 0;
        while (c.moveToNext()) {
            String[] rowValues = CsvUtils.split(c.getString(columnIndex));
            for (int col = 0; col < rowValues.length; col++) {
                values[row][col] = stringsToDoubles(multiValueRegex.split(rowValues[col]));
            }
            row++;
        }

        double mean = 0;
        // Calculate mean of distance between each and every row
        for (int e = 0; e < c.getCount(); e++) {
            for (int i = 0; i < c.getCount(); i++) {
                if (e == i) continue;
                mean += getDistance(values[e], values[i]);
            }
        }
        means[columnIndex] = mean / c.getCount() - 1;
    }

    /**
     * Implementation of the D(.,.) function in Maiorana, et al. 2011
     * @param f1 Fu,e
     * @param f2 Fu,i
     * @return D(.,.)
     */
    private double getDistance(double[][] f1, double[][] f2) {
        double distance = 0;
        // Calculate Manhattan distance for all datapoints
        for (int k = 0; k < f1.length; k++) {
            distance += manhattanDistance(f1[k], f2[k]);
        }
        distance /= f1.length;
        return distance;
    }

    private void recoverFromInvalidData() {
        invalidData = false;
        for (List<double[]> data : currentData) {
            data.clear();
        }
    }

    private void calcConfidence() {
        if (confidence != BiometricsManager.CONFIDENCE_NOT_ENOUGH_DATA) return;

        if (acquisitions.get(0).length < TEMPLATE_SET_SIZE) {
            Log.i(TAG, "Template set too small (" + acquisitions.get(0).length + ") for authentication");
            calculatedConfidence = true;
            return;
        }

        if (currentData.size() != acquisitions.size()) {
            Log.e(TAG, "Authentication data has " + currentData.size() + " datapoints, needs " + acquisitions.size());
            invalidData = true;
            return;
        }

        // Calculate mean of distance to template acquisitions
        double mean = 0;
        for (int i = 0; i < acquisitions.size(); i++) {
            double distance = 0;
            for (int row = 0; row < acquisitions.get(i).length; row++) {
                if (currentData.get(i).size() != acquisitions.get(i)[row].length) {
                    Log.e(TAG, "Authentication data has " + currentData.get(i).size() + " samples, needs " + acquisitions.get(i)[row].length);
                    invalidData = true;
                    return;
                }

                double[][] rowData = new double[currentData.get(i).size()][currentData.get(i).get(0).length];
                distance += getDistance(acquisitions.get(i)[row], currentData.get(i).toArray(rowData));
            }
            distance /= means[i];
            mean += distance;
        }
        mean /= acquisitions.size();
        confidence = mean;
        calculatedConfidence = true;
        submittedInput = false;
    }

    private double[] stringsToDoubles(String[] strings) {
        double[] doubles = new double[strings.length];
        for (int i = 0; i < strings.length; i++) {
            doubles[i] = Double.valueOf(strings[i]);
        }
        return doubles;
    }

    private double manhattanDistance(double[] f1, double[] f2) {
        double result = 0;
        for (int i = 0; i < f1.length; i++) {
            result += Math.abs(f1[i] - f2[i]);
        }
        return result;
    }

    private void saveBiometricData() {
        if (invalidData || !calculatedConfidence) return;

        SQLiteDatabase db = manager.getDb();
        ContentValues values = new ContentValues(INDEX_SENSOR_START + dbContract.getSensorColumns().length + 2);
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_CONTEXT, manager.getBiometricsContext());
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_KEY_DOWNDOWN, CsvUtils.join(toCsvStrings(currentData.get(INDEX_DOWNDOWN))));
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_KEY_DOWNUP, CsvUtils.join(toCsvStrings(currentData.get(INDEX_DOWNUP))));
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_SIZE, CsvUtils.join(toCsvStrings(currentData.get(INDEX_SIZE))));
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_ORIENTATION, CsvUtils.join(toCsvStrings(currentData.get(INDEX_ORIENTATION))));
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_PRESSURE, CsvUtils.join(toCsvStrings(currentData.get(INDEX_PRESSURE))));
        values.put(StatisticalClassifierContract.StatisticalClassifierData.COLUMN_POSITION, CsvUtils.join(toCsvStrings(currentData.get(INDEX_POSITION))));
        db.insert(StatisticalClassifierContract.StatisticalClassifierData.TABLE_NAME, null, values);
    }

    private String[] toCsvStrings(List<double[]> values) {
        String[] strings = new String[values.size()];

        StringBuilder sb = new StringBuilder(3 + 2);
        for (int i = 0; i < values.size(); i++) {
            for (int j = 0; j < values.get(i).length; j++) {
                if (j > 0) {
                    sb.append(MULTI_VALUE_SEPARATOR);
                }
                sb.append(values.get(i)[j]);
            }
            strings[i] = sb.toString();
            sb.delete(0, sb.length());
        }
        return strings;
    }

    private static class ActiveBiometricsEntries extends LinkedList<BiometricsEntry> {
        List<BiometricsEntry> pendingRemoval = new LinkedList<>();

        public BiometricsEntry getDownEntry(int pointerId) {
            for (BiometricsEntry entry : this) {
                if (entry.getPointerId() == pointerId) {
                    return entry;
                }
            }
            return null;
        }

        public BiometricsEntry getLastDownEntry(long timestamp) {
            long maxTimestamp = 0;
            BiometricsEntry foundEntry = null;
            for (BiometricsEntry entry : this) {
                if (entry.getEvent() == BiometricsEntry.EVENT_DOWN && entry.getTimestamp() < timestamp && entry.getTimestamp() > maxTimestamp) {
                    maxTimestamp = entry.getTimestamp();
                    foundEntry = entry;
                }
            }
            return foundEntry;
        }

        public void removeById(int pointerId) {
            removePending();

            ListIterator<BiometricsEntry> iter = listIterator();
            while (iter.hasNext()) {
                BiometricsEntry entry = iter.next();
                if (entry.getPointerId() == pointerId) {
                    if (size() > 1) {
                        iter.remove();
                    } else {
                        pendingRemoval.add(entry);
                    }
                }
            }
        }

        private void removePending() {
            if (size() <= 1) return;
            ListIterator<BiometricsEntry> iter = pendingRemoval.listIterator();
            while (iter.hasNext()) {
                BiometricsEntry entry = iter.next();
                if (size() > 1) {
                    remove(entry);
                    iter.remove();
                }
            }
        }
    }

    // Source: http://stackoverflow.com/questions/1240077/why-cant-i-use-foreach-on-java-enumeration/17960641#17960641
    private static class IterableEnumeration<T> implements Iterable<T>, Iterator<T>
    {
        private final Enumeration<T> enumeration;
        private boolean used = false;

        IterableEnumeration(final Enumeration<T> enm) {
            enumeration = enm;
        }

        public Iterator<T> iterator() {
            if (used) throw new IllegalStateException("Cannot use iterator from asIterable wrapper more than once");
            used = true;
            return this;
        }

        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        public T next() {
            return enumeration.nextElement();
        }

        public void remove() {
            throw new UnsupportedOperationException("Cannot remove elements from AsIterator wrapper around Enumeration");
        }

        public static <T> Iterable<T> asIterable(final Enumeration<T> enm) {
            return new IterableEnumeration<>(enm);
        }
    }
}
