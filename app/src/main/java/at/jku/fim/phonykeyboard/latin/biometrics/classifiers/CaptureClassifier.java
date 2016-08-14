package at.jku.fim.phonykeyboard.latin.biometrics.classifiers;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.app.AlertDialog;
import android.util.Log;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import at.jku.fim.phonykeyboard.latin.Constants;
import at.jku.fim.phonykeyboard.latin.R;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsEntry;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManager;
import at.jku.fim.phonykeyboard.latin.biometrics.BiometricsManagerImpl;
import at.jku.fim.phonykeyboard.latin.biometrics.data.CaptureClassifierContract;
import at.jku.fim.phonykeyboard.latin.biometrics.data.Contract;
import at.jku.fim.phonykeyboard.latin.utils.CsvUtils;

public class CaptureClassifier extends Classifier {
    private static final String TAG = "CaptureClassifier";
    private static final int INDEX_DOWNDOWN = 0, INDEX_DOWNUP = 1, INDEX_SIZE =  2, INDEX_ORIENTATION = 3, INDEX_PRESSURE = 4, INDEX_POSITION = 5, INDEX_SENSOR_START = 6;
    private static final String MULTI_VALUE_SEPARATOR = "|";

    private final CaptureClassifierContract dbContract;
    private final Pattern multiValueRegex = Pattern.compile("\\" + MULTI_VALUE_SEPARATOR);

    private int screenOrientation;
    private int inputmethod;
    private int situation;
    private boolean invalidData;
    private ActiveBiometricsEntries activeEntries = new ActiveBiometricsEntries();
    private List<List<double[]>> currentData;   // datapoint<col<[values]>>
    private List<String> pressedKeys;

    private OnUserDataGatheredListener listener;
    /** Set to true when the user clicked the Next, Previous or Enter button and therefore submitted the input to the app **/
    private boolean submittedInput;
    /** Set to true when calcScore() was successful **/
    private boolean calculatedScore;
    private double score = BiometricsManager.SCORE_NOT_ENOUGH_DATA;

    public CaptureClassifier(BiometricsManagerImpl manager) {
        super(manager);

        String[] sensorTypes = new String[manager.getSensors().size()];
        Enumeration<Sensor> sensors = manager.getSensors().keys();
        int i = 0;
        for (Sensor sensor : IterableEnumeration.asIterable(sensors)) {
            sensorTypes[i] = manager.getSensorType(sensor);
            i++;
        }
        dbContract = new CaptureClassifierContract(sensorTypes);
    }


    @Override
    public Contract getDatabaseContract() {
        return dbContract;
    }

    @Override
    public double getScore(double laxness) {
        if (!calculatedScore) {
            if (invalidData) {
                score = BiometricsManager.SCORE_CAPTURING_ERROR;
            }
            saveBiometricData();
            resetData();
            calculatedScore = true;
        }
        return score;
    }

    public long getCaptureCount() {
        return DatabaseUtils.queryNumEntries(manager.getDb(), CaptureClassifierContract.CaptureClassifierData.TABLE_NAME);
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
                resetData();
            }
            return;
        }

        screenOrientation = manager.getScreenOrientation();

        int columnCount = dbContract.getSensorColumns().length + INDEX_SENSOR_START;
        currentData = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            currentData.add(new LinkedList<double[]>());
        }
        pressedKeys = new ArrayList<>();

        invalidData = false;
        submittedInput = false;
        calculatedScore = false;
        score = BiometricsManager.SCORE_NOT_ENOUGH_DATA;
    }

    @Override
    public void onKeyEvent(BiometricsEntry entry) {
        if (submittedInput) return;
        if (invalidData) {
            CharSequence text = manager.getInputText();
            if (text == null || text.length() == 0) {
                resetData();
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
                ACRA.getErrorReporter().handleSilentException(new IllegalStateException("No matching DOWN event found"));
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
            pressedKeys.add(entry.getKey());
        }
    }

    @Override
    public void onFinishInput(boolean done) {
        if (!done) {
            CharSequence text = manager.getInputText();
            if (text != null && text.length() != 0) {
                invalidData = true;
            }
        }
    }

    @Override
    public void onDestroy() {
    }

    public void setOnUserDataGatheredListener(OnUserDataGatheredListener listener) {
        this.listener = listener;
    }

    public void gatherUserData(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.study_capture_userinfo_hand_title).setItems(R.array.study_capture_userinfo_hand_choices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                inputmethod = which;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.study_capture_userinfo_situation_title).setItems(R.array.study_capture_userinfo_situation_choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        situation = which;
                        getScore(0.5);

                        if (listener != null) {
                            listener.onUserDataCollected();
                        }
                    }
                }).show();
            }
        }).show();
    }

    private void resetData() {
        invalidData = false;
        submittedInput = false;
        for (List<double[]> data : currentData) {
            data.clear();
        }
        activeEntries.clear();
    }

    private void saveBiometricData() {
        if (invalidData || calculatedScore) return;
        if (currentData.get(INDEX_DOWNDOWN).size() == 0) {
            Log.e(TAG, "No data available, was it already cleared?");
            ACRA.getErrorReporter().handleSilentException(new IllegalStateException("No data available"));
            return;
        }

        SQLiteDatabase db = manager.getDb();
        ContentValues values = new ContentValues(INDEX_SENSOR_START + dbContract.getSensorColumns().length + 3);
        values.put(CaptureClassifierContract.CaptureClassifierData.COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_SCREEN_ORIENTATION, screenOrientation);
        values.put(CaptureClassifierContract.CaptureClassifierData.COLUMN_INPUTMETHOD, inputmethod);
        values.put(CaptureClassifierContract.CaptureClassifierData.COLUMN_SITUATION, situation);
        values.put(CaptureClassifierContract.CaptureClassifierData.COLUMN_KEY, CsvUtils.join(pressedKeys.toArray(new String[pressedKeys.size()])));
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_KEY_DOWNDOWN, CsvUtils.join(toCsvStrings(currentData.get(INDEX_DOWNDOWN))));
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_KEY_DOWNUP, CsvUtils.join(toCsvStrings(currentData.get(INDEX_DOWNUP))));
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_SIZE, CsvUtils.join(toCsvStrings(currentData.get(INDEX_SIZE))));
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_ORIENTATION, CsvUtils.join(toCsvStrings(currentData.get(INDEX_ORIENTATION))));
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_PRESSURE, CsvUtils.join(toCsvStrings(currentData.get(INDEX_PRESSURE))));
        values.put(CaptureClassifierContract.StatisticalClassifierData.COLUMN_POSITION, CsvUtils.join(toCsvStrings(currentData.get(INDEX_POSITION))));
        for (int i = 0; i < dbContract.getSensorColumns().length; i++) {
            values.put(dbContract.getSensorColumns()[i], CsvUtils.join(toCsvStrings(currentData.get(INDEX_SENSOR_START + i))));
        }
        db.insert(CaptureClassifierContract.CaptureClassifierData.TABLE_NAME, null, values);
    }

    public interface OnUserDataGatheredListener {
        void onUserDataCollected();
    }
}
