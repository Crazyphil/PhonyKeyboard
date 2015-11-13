package at.jku.fim.phonykeyboard.latin.biometrics.data;

import android.database.sqlite.SQLiteDatabase;

public abstract class Contract {
    protected Contract() {
    }

    public int getVersion() {
        return 1;
    }

    public abstract void onCreate(SQLiteDatabase db);
    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
}
