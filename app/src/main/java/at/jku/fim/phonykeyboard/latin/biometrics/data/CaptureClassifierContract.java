package at.jku.fim.phonykeyboard.latin.biometrics.data;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import at.jku.fim.phonykeyboard.latin.biometrics.classifiers.StatisticalClassifier;

public class CaptureClassifierContract extends StatisticalClassifierContract {
    public static final int DATABASE_VERSION = 1;

    private final String sqlCreateData;

    public CaptureClassifierContract(String[] sensorTypes) {
        super(sensorTypes);

        StringBuilder sb = new StringBuilder(sensorTypes.length * 3 + 2);
        sb.append("CREATE TABLE " + CaptureClassifierData.TABLE_NAME + " (" +
                StatisticalClassifierData._ID + " INTEGER PRIMARY KEY, " +
                CaptureClassifierData.COLUMN_TIMESTAMP + " INTEGER, " +
                StatisticalClassifierData.COLUMN_SCREEN_ORIENTATION + " INTEGER, " +
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
    public int getVersion() {
        return DATABASE_VERSION;
    }

    public static abstract class CaptureClassifierData implements BaseColumns {
        public static final String TABLE_NAME = "CaptureClassifierData";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }
}
