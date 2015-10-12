package at.jku.fim.inputstudy;

import android.provider.BaseColumns;

public final class WordsContract {

    private WordsContract() { }

    public static abstract class Words implements BaseColumns {
        public static final String TABLE_NAME = "words";
        public static final String COLUMN_NAME_WORD = "word";
        public static final String COLUMN_NAME_LENGTH = "length";
        public static final String COLUMN_NAME_ATTRIBUTES = "attributes";
    }
}