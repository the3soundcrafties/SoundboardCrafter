package de.soundboardcrafter.activity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment {
    private GridView gridView;

    public SoundboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        new FindSoundboardsTask(getActivity()).execute();
        // Will call setupAdapter() later.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard,
                container, false);

        gridView = (GridView) rootView.findViewById(R.id.gridview_soundboard);

        return rootView;
    }

    private void setupAdapter(ImmutableList<Soundboard> soundboards) {
        Soundboard someSoundboard = soundboards.iterator().next();

        SoundboardItemAdapter soundBoardItemAdapter =
                new SoundboardItemAdapter(getActivity(), someSoundboard);

        gridView.setAdapter(soundBoardItemAdapter);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_main, menu);
    }

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    public class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private WeakReference<Context> appContextRef;

        FindSoundboardsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        protected ImmutableList<Soundboard> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading soundboards...");

            final ImmutableList<Soundboard> res = SoundboardDao.getInstance(appContext).findAll();

            Log.d(TAG, "Soundboards loaded.");

            return res;
        }

        @Override
        protected void onPostExecute(ImmutableList<Soundboard> soundboards) {
            if (!isAdded()) {
                // fragment is no longer linked to an activity
                return;
            }
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            setupAdapter(soundboards);
        }
    }
}
