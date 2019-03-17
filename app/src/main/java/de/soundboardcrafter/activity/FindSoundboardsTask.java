package de.soundboardcrafter.activity;

import android.os.AsyncTask;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * A background task, used to retrieve soundboards from the database.
 */
public class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
    public FindSoundboardsTask() {
        super();
    }

    @Override
    protected ImmutableList<Soundboard> doInBackground(Void... voids) {
        final ImmutableList<Soundboard> res = SoundboardDao.findAll();

        return res;
    }
}
