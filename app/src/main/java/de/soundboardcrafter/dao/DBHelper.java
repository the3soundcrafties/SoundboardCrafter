package de.soundboardcrafter.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.DBSchema.FavoritesTable;
import de.soundboardcrafter.dao.DBSchema.GamesTable;
import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardFavoritesTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGamesTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;

/**
 * Helper class for SQL database access
 */
@ParametersAreNonnullByDefault
class DBHelper extends SQLiteOpenHelper {
    /**
     * Database version
     */
    private static final int VERSION = 19;

    private static final String DROP_TABLE_GAMES = //
            "DROP TABLE IF EXISTS " + GamesTable.NAME + ";";

    private static final String DROP_TABLE_SOUNDBOARD_GAMES = //
            "DROP TABLE IF EXISTS " + SoundboardGamesTable.NAME + ";";

    private static final String CREATE_TABLE_FAVORITES = //
            "CREATE TABLE " + FavoritesTable.NAME + " (" + //
                    FavoritesTable.Cols.ID + " TEXT NOT NULL, " + //
                    FavoritesTable.Cols.NAME + " TEXT NOT NULL, " + //
                    "PRIMARY KEY (" + FavoritesTable.Cols.ID + "));";

    private static final String DROP_TABLE_FAVORITES = //
            "DROP TABLE IF EXISTS " + FavoritesTable.NAME + ";";

    private static final String CREATE_TABLE_SOUNDBOARD = //
            "CREATE TABLE " + SoundboardTable.NAME + " (" + //
                    SoundboardTable.Cols.ID + " TEXT NOT NULL, " + //
                    SoundboardTable.Cols.NAME + " TEXT NOT NULL, " + //
                    SoundboardTable.Cols.PROVIDED + " INTEGER NOT NULL " //
                    + "CHECK (" + SoundboardTable.Cols.PROVIDED + " IN (0, 1)), " + //
                    "PRIMARY KEY (" + SoundboardTable.Cols.ID + "));";

    private static final String DROP_TABLE_SOUNDBOARD = //
            "DROP TABLE IF EXISTS " + SoundboardTable.NAME + ";";

    private static final String CREATE_TABLE_SOUNDBOARD_FAVORITES = //
            "CREATE TABLE " + SoundboardFavoritesTable.NAME + " (" + //
                    SoundboardFavoritesTable.Cols.SOUNDBOARD_ID + " TEXT NOT NULL, " + //
                    SoundboardFavoritesTable.Cols.FAVORITES_ID + " TEXT NOT NULL, " + //
                    "PRIMARY KEY (" + SoundboardFavoritesTable.Cols.SOUNDBOARD_ID + ", " + //
                    SoundboardFavoritesTable.Cols.FAVORITES_ID + "));";

    private static final String DROP_TABLE_SOUNDBOARD_FAVORITES = //
            "DROP TABLE IF EXISTS " + SoundboardFavoritesTable.NAME + ";";

    private static final String CREATE_TABLE_SOUND = //
            "CREATE TABLE " + SoundTable.NAME + " (" + //
                    SoundTable.Cols.ID + " TEXT NOT NULL, " + //
                    SoundTable.Cols.LOCATION_TYPE + " TEXT NOT NULL, " + //
                    SoundTable.Cols.PATH + " TEXT NOT NULL, " + //
                    SoundTable.Cols.NAME + " TEXT NOT NULL, " + //
                    SoundTable.Cols.VOLUME_PERCENTAGE + " INTEGER NOT NULL, " +
                    // Boolean. 0 == false, 1 == true
                    SoundTable.Cols.LOOP + " INTEGER NOT NULL, " + //
                    "PRIMARY KEY (" + SoundTable.Cols.ID + "));";

    private static final String DROP_TABLE_SOUND = //
            "DROP TABLE IF EXISTS " + SoundTable.NAME + ";";

    private static final String CREATE_TABLE_SOUNDBOARD_SOUND = //
            "CREATE TABLE " + SoundboardSoundTable.NAME + " (" + //
                    SoundboardSoundTable.Cols.SOUNDBOARD_ID + " TEXT NOT NULL, " + //
                    SoundboardSoundTable.Cols.SOUND_ID + " TEXT NOT NULL, " + //
                    SoundboardSoundTable.Cols.POS_INDEX + " INTEGER NOT NULL, " + //
                    "PRIMARY KEY (" + SoundboardSoundTable.Cols.SOUNDBOARD_ID + ", " + //
                    SoundboardSoundTable.Cols.SOUND_ID + "));";

    private static final String DROP_TABLE_SOUNDBOARD_SOUND = //
            "DROP TABLE IF EXISTS " + SoundboardSoundTable.NAME + ";";

    private static final String TAG = DBHelper.class.getName();

    DBHelper(Context context) {
        super(
                context,
                // The database is chosen by the BUILD TYPE.
                // There are three build types available:
                // debug, staging and release.
                // debug has its special database, staging a release use the same.
                context.getResources().getString(R.string.database_name),
                null,
                VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database...");

        // This will create all tables.
        onUpgrade(db, 0, VERSION);

        Log.d(TAG, "Database created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(TAG,
                "Upgrading database from version " + oldVersion + " to version " + newVersion);

        if (oldVersion < VERSION) {
            dropTables(db);
            createInitialTables(db);
        }
    }

    private void createInitialTables(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SOUNDBOARD);

        db.execSQL(CREATE_TABLE_SOUND);
        db.execSQL(CREATE_TABLE_SOUNDBOARD_SOUND);

        db.execSQL(CREATE_TABLE_FAVORITES);
        db.execSQL(CREATE_TABLE_SOUNDBOARD_FAVORITES);

        // TODO extra index on primary keys necessary / useful?
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE_GAMES);

        db.execSQL(DROP_TABLE_FAVORITES);

        db.execSQL(DROP_TABLE_SOUNDBOARD);
        db.execSQL(DROP_TABLE_SOUNDBOARD_GAMES);
        db.execSQL(DROP_TABLE_SOUNDBOARD_FAVORITES);

        db.execSQL(DROP_TABLE_SOUND);
        db.execSQL(DROP_TABLE_SOUNDBOARD_SOUND);
    }
}
