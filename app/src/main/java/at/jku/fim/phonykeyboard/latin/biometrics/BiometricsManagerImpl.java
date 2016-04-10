package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.latin.Constants;
import at.jku.fim.phonykeyboard.latin.PhonyKeyboard;
import at.jku.fim.phonykeyboard.latin.biometrics.classifiers.CaptureClassifier;
import at.jku.fim.phonykeyboard.latin.biometrics.classifiers.Classifier;
import at.jku.fim.phonykeyboard.latin.biometrics.classifiers.StatisticalClassifier;
import at.jku.fim.phonykeyboard.latin.biometrics.data.BiometricsContract;
import at.jku.fim.phonykeyboard.latin.biometrics.data.BiometricsDbHelper;
import at.jku.fim.phonykeyboard.latin.utils.StringUtils;

public class BiometricsManagerImpl extends BiometricsManager {
    public static final String TAG = "BiometricsManagerImpl";
    public static final String INTERNAL_BROADCAST_EXTRA_CAPTURE_COUNT = "at.jku.fim.phonykeyboard.internal.BIOMETRICS_CAPTURE_COUNT";

    private Classifier classifier;
    private BiometricsDbHelper dbHelper;
    private SQLiteDatabase db;
    private long currentBiometricsContext;
    private boolean hasCustomClassifier;

    @Override
    public void init(PhonyKeyboard context) {
        super.init(context);
        classifier = new StatisticalClassifier(this);
        dbHelper = new BiometricsDbHelper(context, classifier.getDatabaseContract());
        db = dbHelper.getWritableDatabase();
    }

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        if (!restarting) {
            String packageName = getVerifiedPackageName(editorInfo);
            String classifierName = null;
            if (packageName.equals(getContext().getPackageName())) {
                // Evaluate internal options
                classifierName = StringUtils.valueOfCommaSplittableKeyValueText(Constants.ImeOption.INTERNAL_BIOMETRICS_CLASSIFIER, editorInfo.privateImeOptions);
                if (classifierName != null && !classifier.getClass().equals(getClassifierByName(classifierName))) {
                    try {
                        Classifier newClassifier = getClassifierByName(classifierName).getConstructor(BiometricsManagerImpl.class).newInstance(this);
                        swapClassifier(newClassifier);
                    } catch (Exception e) {
                        // Must not occur - else check conformity of reflection code with real class constructor
                        Log.wtf(TAG, "Could not instantiate internal classifier", e);
                    }
                    hasCustomClassifier = true;
                }
            }
            if (classifierName == null && hasCustomClassifier) {
                // Revert to default class if internal override is not set
                if (!classifier.getClass().equals(StatisticalClassifier.class)) {
                    swapClassifier(new StatisticalClassifier(this));
                }
                hasCustomClassifier = false;
            }

            String context = StringUtils.valueOfCommaSplittableKeyValueText(Constants.ImeOption.BIOMETRICS_CONTEXT, editorInfo.privateImeOptions);
            if (context == null || context.isEmpty()) {
                context = packageName;
            }
            currentBiometricsContext = getBiometricsContext(context);
        }
        classifier.onStartInput(currentBiometricsContext, restarting);
    }

    private void swapClassifier(Classifier newClassifier) {
        dbHelper.removeContract(classifier.getDatabaseContract());
        classifier.onDestroy();
        classifier = newClassifier;
        classifier.onCreate();
        dbHelper.addContract(classifier.getDatabaseContract());
    }

    @Override
    public void onFinishInputView(boolean finishInput) {
        classifier.onFinishInput(finishInput);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        classifier.onCreate();
    }

    @Override
    public void onDestroy() {
        classifier.onDestroy();
        db.close();
        super.onDestroy();
    }

    @Override
    public void onKeyDown(Key key, MotionEvent event) {
        classifier.onKeyEvent(buildEntry(key, event));
    }

    @Override
    public void onKeyUp(Key key, MotionEvent event) {
        classifier.onKeyEvent(buildEntry(key, event));
    }

    @Override
    public double getScore() {
        return classifier.getScore();
    }

    @Override
    protected void addExtraScoreData(Bundle result) {
        super.addExtraScoreData(result);

        if (classifier instanceof CaptureClassifier) {
            result.putLong(INTERNAL_BROADCAST_EXTRA_CAPTURE_COUNT, ((CaptureClassifier)classifier).getCaptureCount());
        }
    }

    @Override
    public boolean clearData() {
        return classifier.clearData();
    }

    public void setOnUserDataGatheredListener(CaptureClassifier.OnUserDataGatheredListener listener) {
        if (classifier instanceof CaptureClassifier) {
            ((CaptureClassifier)classifier).setOnUserDataGatheredListener(listener);
        }
    }

    public void gatherUserData(Context context) {
        if (classifier instanceof CaptureClassifier) {
            ((CaptureClassifier)classifier).gatherUserData(context);
        } else {
            throw new UnsupportedOperationException("Gathering user data is only supported by Capture Classifier");
        }
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public long getBiometricsContext() {
        return currentBiometricsContext;
    }

    private long getBiometricsContext(String context) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long id;
        try {
            id = DatabaseUtils.longForQuery(db, "SELECT " + BiometricsContract.Contexts._ID + " FROM " + BiometricsContract.Contexts.TABLE_NAME + " WHERE " + BiometricsContract.Contexts.COLUMN_CONTEXT + " = ?", new String[] { context });
        } catch (SQLiteDoneException e) {
            ContentValues values = new ContentValues(1);
            values.put(BiometricsContract.Contexts.COLUMN_CONTEXT, context);
            id = db.insert(BiometricsContract.Contexts.TABLE_NAME, null, values);
        }
        return id;
    }

    private Class<? extends Classifier> getClassifierByName(String name) {
        switch (name) {
            case "CaptureClassifier":
                return CaptureClassifier.class;
            default:
                return StatisticalClassifier.class;
        }
    }
}
