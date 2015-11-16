package at.jku.fim.phonykeyboard.latin.biometrics.data;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class StatisticalClassifierContract extends Contract {
    public static final int DATABASE_VERSION = 1;

    private final String sqlCreateData;

    private String[] sensorColumns;

    public StatisticalClassifierContract(String[] sensorTypes) {
        super();

        StringBuilder sb = new StringBuilder(sensorTypes.length * 3 + 2);
        sb.append("CREATE TABLE " + StatisticalClassifierData.TABLE_NAME + " (" +
                StatisticalClassifierData._ID + " INTEGER PRIMARY KEY, " +
                StatisticalClassifierData.COLUMN_CONTEXT + " INTEGER, " +
                StatisticalClassifierData.COLUMN_SCREEN_ORIENTATION + " INTEGER, " +
                StatisticalClassifierData.COLUMN_KEY_DOWNDOWN + " TEXT, " +
                StatisticalClassifierData.COLUMN_KEY_DOWNUP + " TEXT, " +
                StatisticalClassifierData.COLUMN_POSITION + " TEXT, " +
                StatisticalClassifierData.COLUMN_SIZE + " TEXT, " +
                StatisticalClassifierData.COLUMN_ORIENTATION + " TEXT, " +
                StatisticalClassifierData.COLUMN_PRESSURE + " TEXT");

        sensorColumns = new String[sensorTypes.length];
        int i = 0;
        for (String sensorType : sensorTypes) {
            sensorColumns[i] = sensorType.replace('.', '_');
            sb.append(", ");
            sb.append(sensorColumns[i]);
            sb.append(" TEXT");
            i++;
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
    }

    @Override
    public int getVersion() {
        return DATABASE_VERSION;
    }

    public String[] getSensorColumns() {
        return sensorColumns;
    }

    public static abstract class StatisticalClassifierData implements BaseColumns {
        public static final String TABLE_NAME = "StatisticalClassifierData";
        public static final String COLUMN_CONTEXT = "context";
        public static final String COLUMN_SCREEN_ORIENTATION = "screen_orientation";
        public static final String COLUMN_KEY_DOWNDOWN = "key_downdown";    // n = k-1
        public static final String COLUMN_KEY_DOWNUP = "key_downup";        // n = k
        public static final String COLUMN_POSITION = "position";            // n = k
        public static final String COLUMN_SIZE = "size";            // n = k
        public static final String COLUMN_ORIENTATION = "orientation";            // n = k
        public static final String COLUMN_PRESSURE = "pressure";            // n = k
    }
}
