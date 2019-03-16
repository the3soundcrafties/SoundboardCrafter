package de.soundboardcrafter.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.nfc.Tag;
import android.util.Log;

import de.soundboardcrafter.dao.DBSchema.GameTable;
import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;

/**
 * Helper class for SQL database access
 */
class DBHelper extends SQLiteOpenHelper {
    /**
     * Database version
     */
    private static final int VERSION = 1;

    /**
     * Database name
     */
    private static final String DATABASE_NAME = "soundboard.db";

    private static final String CREATE_TABLE_GAME = //
            "CREATE TABLE " + GameTable.NAME + " (" + //
                    GameTable.Cols.ID + " TEXT NOT NULL, " + //
                    GameTable.Cols.NAME + " TEXT NOT NULL, " + //
                    "PRIMARY KEY (" + GameTable.Cols.ID + "));";

    private static final String CREATE_TABLE_SOUNDBOARD = //
            "CREATE TABLE " + SoundboardTable.NAME + " (" + //
                    SoundboardTable.Cols.ID + " TEXT NOT NULL, " + //
                    SoundboardTable.Cols.NAME + " TEXT NOT NULL, " + //
                    "PRIMARY KEY (" + SoundboardTable.Cols.ID + "));";

    private static final String CREATE_TABLE_SOUNDBOARD_GAME = //
            "CREATE TABLE " + SoundboardGameTable.NAME + " (" + //
                    SoundboardGameTable.Cols.SOUNDBOARD_ID + " TEXT NOT NULL, " + //
                    SoundboardGameTable.Cols.GAME_ID + " TEXT NOT NULL, " + //
                    "PRIMARY KEY (" + SoundboardGameTable.Cols.SOUNDBOARD_ID + ", " + //
                    SoundboardGameTable.Cols.GAME_ID + "));";

    private static final String CREATE_TABLE_SOUND = //
            "CREATE TABLE " + SoundTable.NAME + " (" + //
                    SoundTable.Cols.ID + " TEXT NOT NULL, " + //
                    SoundTable.Cols.PATH + " TEXT NOT NULL, " + //
                    SoundTable.Cols.NAME + " TEXT NOT NULL, " + //
                    SoundTable.Cols.VOLUME_PERCENTAGE + " INTEGER NOT NULL, " +
                    // Boolean. 0 == false, 1 == true
                    SoundTable.Cols.LOOP + " INTEGER NOT NULL, " + //
                    "PRIMARY KEY (" + SoundTable.Cols.ID + "));";

    private static final String CREATE_TABLE_SOUNDBOARD_SOUND = //
            "CREATE TABLE " + SoundboardSoundTable.NAME + " (" + //
                    SoundboardSoundTable.Cols.SOUNDBOARD_ID + " TEXT NOT NULL, " + //
                    SoundboardSoundTable.Cols.SOUND_ID + " TEXT NOT NULL, " + //
                    "PRIMARY KEY (" + SoundboardSoundTable.Cols.SOUNDBOARD_ID + ", " + //
                    SoundboardSoundTable.Cols.SOUND_ID + "));";

    private static final String TAG = DBHelper.class.getName();

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database...");

        db.execSQL(CREATE_TABLE_GAME);
        db.execSQL(CREATE_TABLE_SOUNDBOARD);
        db.execSQL(CREATE_TABLE_SOUNDBOARD_GAME);
        db.execSQL(CREATE_TABLE_SOUND);
        db.execSQL(CREATE_TABLE_SOUNDBOARD_SOUND);

        // TODO extra index on primary keys necessary?

        Log.d(TAG, "Database created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // update logic, if any
    }
}
