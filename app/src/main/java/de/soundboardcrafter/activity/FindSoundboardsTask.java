package de.soundboardcrafter.activity;

import android.content.Context;
import android.os.AsyncTask;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * A background task, used to retrieve soundboards from the database.
 */
public class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
    private final Context appContext;

    public FindSoundboardsTask(Context context) {
        super();
        appContext = context.getApplicationContext();
    }

    @Override
    protected ImmutableList<Soundboard> doInBackground(Void... voids) {
        final ImmutableList<Soundboard> res = SoundboardDao.getInstance(appContext).findAll();

        return res;
    }
}
