package at.jku.fim.phonykeyboard.latin.biometrics.data;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class CaptureClassifierContract extends StatisticalClassifierContract {
    public static final int DATABASE_VERSION = 2;

    private final String sqlCreateData;

    public CaptureClassifierContract(String[] sensorTypes) {
        super(sensorTypes);

        StringBuilder sb = new StringBuilder(sensorTypes.length * 3 + 2);
        sb.append("CREATE TABLE " + CaptureClassifierData.TABLE_NAME + " (" +
                StatisticalClassifierData._ID + " INTEGER PRIMARY KEY, " +
                CaptureClassifierData.COLUMN_TIMESTAMP + " INTEGER, " +
                StatisticalClassifierData.COLUMN_SCREEN_ORIENTATION + " INTEGER, " +
                CaptureClassifierData.COLUMN_INPUTMETHOD + " INTEGER, " +
                CaptureClassifierData.COLUMN_SITUATION + " INTEGER, " +
                CaptureClassifierData.COLUMN_KEY + " TEXT, " +
                StatisticalClassifierData.COLUMN_KEY_DOWNDOWN + " TEXT, " +
                StatisticalClassifierData.COLUMN_KEY_DOWNUP + " TEXT, " +
                StatisticalClassifierData.COLUMN_POSITION + " TEXT, " +
                StatisticalClassifierData.COLUMN_SIZE + " TEXT, " +
                StatisticalClassifierData.COLUMN_ORIENTATION + " TEXT, " +
                StatisticalClassifierData.COLUMN_PRESSURE + " TEXT");

        String[] sensorColumns = getSensorColumns();
        for (String sensorColumn : sensorColumns) {
            sb.append(", ");
            sb.append(sensorColumn);
            sb.append(" TEXT");
        }
        sb.append(")");
        sqlCreateData = sb.toString();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCreateData);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);

        if (oldVersion < DATABASE_VERSION) {
            // Drop old data on every upgrade (must inform study participants about this to avoid data loss!)
            db.execSQL("DROP TABLE " + CaptureClassifierData.TABLE_NAME);
            db.execSQL(sqlCreateData);
        }
    }

    @Override
    public int getVersion() {
        return DATABASE_VERSION;
    }

    public static abstract class CaptureClassifierData implements BaseColumns {
        public static final String TABLE_NAME = "CaptureClassifierData";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_INPUTMETHOD = "inputmethod";
        public static final String COLUMN_SITUATION = "situation";
    }
}
