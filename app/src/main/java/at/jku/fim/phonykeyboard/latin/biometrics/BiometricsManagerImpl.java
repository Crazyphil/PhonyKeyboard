package at.jku.fim.phonykeyboard.latin.biometrics;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.os.Build;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;

import at.jku.fim.phonykeyboard.keyboard.Key;
import at.jku.fim.phonykeyboard.latin.Constants;
import at.jku.fim.phonykeyboard.latin.PhonyKeyboard;
import at.jku.fim.phonykeyboard.latin.biometrics.classifiers.Classifier;
import at.jku.fim.phonykeyboard.latin.biometrics.classifiers.StatisticalClassifier;
import at.jku.fim.phonykeyboard.latin.biometrics.data.BiometricsContract;
import at.jku.fim.phonykeyboard.latin.biometrics.data.BiometricsDbHelper;
import at.jku.fim.phonykeyboard.latin.utils.StringUtils;

public class BiometricsManagerImpl extends BiometricsManager {
    private Classifier classifier;
    private BiometricsDbHelper dbHelper;
    private SQLiteDatabase db;
    private long currentBiometricsContext;

    @Override
    public void init(PhonyKeyboard context) {
        super.init(context);
        classifier = new StatisticalClassifier(this);
        dbHelper = new BiometricsDbHelper(context, classifier.getDatabaseContract());
        db = dbHelper.getWritableDatabase();
    }

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        String context = StringUtils.valueOfCommaSplittableKeyValueText(Constants.ImeOption.BIOMETRICS_CONTEXT, editorInfo.privateImeOptions);
        if (context == null || context.isEmpty()) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                context = getContext().getPackageManager().getNameForUid(((PhonyKeyboard)getContext()).getCurrentInputBinding().getUid());
            } else {
                context = editorInfo.packageName;    // Package name is system verified since Android M
            }
        }
        currentBiometricsContext = getBiometricsContext(context);
        classifier.onStartInput(currentBiometricsContext, restarting);
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
    public double getConfidence() {
        return classifier.getConfidence();
    }

    @Override
    public boolean clearData() {
        return classifier.clearData();
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
}
