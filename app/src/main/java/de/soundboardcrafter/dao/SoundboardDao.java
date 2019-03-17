package de.soundboardcrafter.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Database Access Object for accessing Soundboards in the database
 */
public class SoundboardDao {
    private static SoundboardDao instance;

    private SQLiteDatabase database;

    public static SoundboardDao getInstance(final Context context) {
        if (instance == null) {
            instance = new SoundboardDao(context);
        }

        return instance;
    }

    private SoundboardDao(Context context) {
        database = new DBHelper(context.getApplicationContext()).getWritableDatabase();
    }

    public ImmutableList<Soundboard> findAll() {
        Sound livinOnAPrayer = new Sound("/storage/emulated/0/soundboard crafter test songs/Bon Jovi-Livin On A Prayer.mp3",
                "Livin On A Prayer", 50, true);
        Sound stayAnotherDay = new Sound("/storage/emulated/0/soundboard crafter test songs/Stay Another Day.mp3",
                "Stay Another Day", 50, true);
        Sound trailer2 = new Sound("/storage/emulated/0/soundboard crafter test songs/trailer2.wav",
                "Trailer2", 90, false);
        Soundboard board = new Soundboard("my new Soundboard", Lists.newArrayList(livinOnAPrayer, stayAnotherDay, trailer2));

        return ImmutableList.of(board);
    }
}
