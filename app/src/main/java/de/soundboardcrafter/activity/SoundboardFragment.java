package de.soundboardcrafter.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.fragment.app.FragmentManager;
import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment {
    private static final String TAG = SoundboardDao.class.getName();

    private static final String DIALOG_RESET_ALL = "DialogResetAll";
    private static final int REQUEST_RESET_ALL = 0;

    private GridView gridView;
    private MediaPlayerManagerService mediaPlayerManagerService;

    public SoundboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mediaPlayerManagerService = new MediaPlayerManagerService(getActivity());
        // Will call setupAdapter() later.
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private static void checkPermission(Activity activity) {
        // Here, activity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);

            // TODO: 16.03.2019 when the user declines the permission, the application must
            // not hang up
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard,
                container, false);

        gridView = (GridView) rootView.findViewById(R.id.gridview_soundboard);

        checkPermission(getActivity());
        new FindSoundboardsTask(getActivity()).execute();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_menu_reset_all:
                resetAllOrCancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void resetAllOrCancel() {
        FragmentManager manager = getFragmentManager();
        ResetAllDialogFragment dialog = new ResetAllDialogFragment();
        dialog.setTargetFragment(this, REQUEST_RESET_ALL);
        dialog.show(manager, DIALOG_RESET_ALL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_RESET_ALL) {
            Log.i(TAG, "Resetting sound data");

            // FIXME 1. Disable options button (necessary?)
            // FIXME 2. Stop all sound and remove Sounds from GUI


            new ResetAllTask(getActivity()).execute();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: 17.03.2019 destroy service save state
        // TODO: 17.03.2019 icon change on finish song
    }

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    public class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private WeakReference<Context> appContextRef;
        private final SoundboardDao soundboardDao = SoundboardDao.getInstance(getActivity());

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

            final ImmutableList<Soundboard> res = soundboardDao.findAll();

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

    /**
     * A background task, used to reset the soundboards and retrieve them from the database.
     */
    public class ResetAllTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private WeakReference<Context> appContextRef;
        private final SoundboardDao soundboardDao = SoundboardDao.getInstance(getActivity());

        ResetAllTask(Context context) {
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

            Log.d(TAG, "Resetting soundboards.");

            // FIXME 3. Clear and reset database
            // FIXME 4. re-read sounds
            // FIXME 5. enable button

            // (3 separate AsyncTasks?)

            soundboardDao.clearDatabase();
            soundboardDao.insertDummyData();

            Log.d(TAG, "Loading soundboards...");

            final ImmutableList<Soundboard> res = soundboardDao.findAll();

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
