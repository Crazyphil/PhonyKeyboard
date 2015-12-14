package at.jku.fim.phonykeyboard.latin.biometrics.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

public class BiometricsDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "biometrics.db";

    private List<Contract> contracts;
    private boolean wasCreated;

    public BiometricsDbHelper(Context context, Contract contract) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.contracts = new LinkedList<>();
        if (contract != null) {
            this.contracts.add(contract);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(BiometricsContract.SQL_CREATE_CONTRACT_VERSIONS);
        db.execSQL(BiometricsContract.SQL_CREATE_CONTEXTS);

        ContentValues values = new ContentValues(2);
        for (Contract contract : contracts) {
            createContractTables(db, contract, values);
        }
        wasCreated = true;
    }

    private void createContractTables(SQLiteDatabase db, Contract contract, ContentValues values) {
        if (values == null) {
            values = new ContentValues(2);
        }
        values.put(BiometricsContract.ContractVersions.COLUMN_CONTRACT, contract.getClass().getSimpleName());
        values.put(BiometricsContract.ContractVersions.COLUMN_VERSION, contract.getVersion());
        db.insert(BiometricsContract.ContractVersions.TABLE_NAME, null, values);
        contract.onCreate(db);
    }

    private void upgradeContractTables(SQLiteDatabase db, Contract contract, int oldVersion, ContentValues values) {
        if (values == null) {
            values = new ContentValues(2);
        }
        values.put(BiometricsContract.ContractVersions.COLUMN_CONTRACT, contract.getClass().getSimpleName());
        values.put(BiometricsContract.ContractVersions.COLUMN_VERSION, contract.getVersion());
        db.update(BiometricsContract.ContractVersions.TABLE_NAME, values, BiometricsContract.ContractVersions.COLUMN_CONTRACT + " = ?", new String[] { contract.getClass().getSimpleName() });
        contract.onUpgrade(db, oldVersion, contract.getVersion());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        if (wasCreated) return;

        StringBuilder selection = new StringBuilder(contracts.size() + 2);
        String[] selectionArgs = new String[contracts.size()];
        selection.append(BiometricsContract.ContractVersions.COLUMN_CONTRACT + " IN (");
        int i = 0;
        for (Contract contract : contracts) {
            if (i == 0) {
                selection.append("?");
            } else {
                selection.append(", ?");
            }
            selectionArgs[i] = contract.getClass().getSimpleName();
            i++;
        }
        selection.append(")");

        Cursor c = db.query(BiometricsContract.ContractVersions.TABLE_NAME,
                new String[] { BiometricsContract.ContractVersions.COLUMN_CONTRACT, BiometricsContract.ContractVersions.COLUMN_VERSION },
                selection.toString(), selectionArgs, null, null, null);
        for (Contract contract : contracts) {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                if (c.getString(c.getColumnIndex(BiometricsContract.ContractVersions.COLUMN_CONTRACT)).equals(contract.getClass().getSimpleName())) {
                    int version = c.getInt(c.getColumnIndex(BiometricsContract.ContractVersions.COLUMN_VERSION));
                    if (contract.getVersion() > version) {
                        upgradeContractTables(db, contract, version, null);
                    }
                    break;
                }
            }
            if (c.isAfterLast()) {
                createContractTables(db, contract, null);
            }
        }
        c.close();
    }

    public void addContract(Contract contract) {
        contracts.add(contract);
        if (wasCreated) {
            createContractTables(getWritableDatabase(), contract, null);
        } else {
            Cursor c = getWritableDatabase().query(BiometricsContract.ContractVersions.TABLE_NAME,
                    new String[] { BiometricsContract.ContractVersions.COLUMN_CONTRACT, BiometricsContract.ContractVersions.COLUMN_VERSION },
                    BiometricsContract.ContractVersions.COLUMN_CONTRACT + " = ?", new String[] { contract.getClass().getSimpleName() }, null, null, null);
            if (c.getCount() != 1) {
                createContractTables(getWritableDatabase(), contract, null);
            } else {
                c.moveToFirst();
                upgradeContractTables(getWritableDatabase(), contract, c.getInt(c.getColumnIndex(BiometricsContract.ContractVersions.COLUMN_CONTRACT)), null);
            }
            c.close();
        }
    }

    public void removeContract(Contract contract) {
        contracts.remove(contract);
    }
}
