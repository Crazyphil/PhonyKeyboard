package at.jku.fim.inputstudy;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import at.jku.fim.phonykeyboard.latin.R;

public class PasswordGenerator {
    private static final String DB_FILENAME = "words.sqlite";

    SQLiteDatabase db;
    Context context;

    public PasswordGenerator(Context context) {
        this.context = context;
    }

    public String getWordBetweenDigits(int minWordLength, int maxWordLength) {
        ensureDatabase();

        Cursor c = db.query(WordsContract.Words.TABLE_NAME,
                new String[] { WordsContract.Words.COLUMN_NAME_WORD },
                WordsContract.Words.COLUMN_NAME_LENGTH + " BETWEEN ? AND ?",
                new String[] { String.valueOf(minWordLength), String.valueOf(maxWordLength) },
                null, null, null);
        if (c.getCount() == 0) {
            return null;
        }
        c.move((int)(Math.random() * c.getCount()));

        int firstDigit = (int)(Math.random() * 10);
        int lastDigit = (int)(Math.random() * 10);
        return firstDigit + c.getString(c.getColumnIndex(WordsContract.Words.COLUMN_NAME_WORD)) + lastDigit;
    }

    private void ensureDatabase() {
        if (db == null) {
            File dbFile = new File(context.getFilesDir(), DB_FILENAME);
            if (!dbFile.exists()) {
                FileOutputStream output = null;
                try {
                    output = context.openFileOutput(DB_FILENAME, Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {
                    // Cannot occur, see JavaDoc
                }
                InputStream input = context.getResources().openRawResource(R.raw.words);

                byte[] buffer = new byte[4096];
                try {
                    int count = input.read(buffer);
                    while (count > -1) {
                        output.write(buffer, 0, count);
                        count = input.read(buffer);
                    }
                } catch (IOException e) {
                    Log.e("PasswordGenerator", "Exception copying words database to storage", e);
                } finally {
                    try {
                        input.close();
                        output.close();
                    } catch (IOException e) {
                        // What to do when file closing fails?
                    }
                }
            }

            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        }
    }

}
