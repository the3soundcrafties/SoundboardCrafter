package de.soundboardcrafter.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment {
    private GridView gridView;
    private MediaPlayerManagerService mediaPlayerManagerService;

    public SoundboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        checkPermission(getActivity());
        new FindSoundboardsTask(getActivity()).execute();
        mediaPlayerManagerService = new MediaPlayerManagerService(getActivity());
        // Will call setupAdapter() later.
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private static void checkPermission(Activity activity) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);


            // TODO: 16.03.2019 when the user decline the permission, the application should not hung up
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: 17.03.2019 destroy service save state
        // TODO: 17.03.2019 icon change on finish song
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
                new SoundboardItemAdapter(getContext(), mediaPlayerManagerService, someSoundboard);

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
