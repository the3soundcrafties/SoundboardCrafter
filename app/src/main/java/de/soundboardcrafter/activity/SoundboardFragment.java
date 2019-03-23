package de.soundboardcrafter.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.sound.edit.SoundEditActivity;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment {
    private static final String TAG = SoundboardDao.class.getName();

    private static final String DIALOG_RESET_ALL = "DialogResetAll";

    private static final int REQUEST_RESET_ALL = 0;
    private static final int REQUEST_EDIT_SOUND = 1;

    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;

    // TODO Allow for zero or more than one soundboards
    private SoundboardItemAdapter soundBoardItemAdapter;
    private MediaPlayerManagerService mediaPlayerManagerService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mediaPlayerManagerService = new MediaPlayerManagerService(getActivity());
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard,
                container, false);

        GridView gridView = rootView.findViewById(R.id.grid_view_soundboard);
        registerForContextMenu(gridView);

        // TODO Start without any soundboard
        Soundboard dummySoundboard = new Soundboard("Dummy", Lists.newArrayList());
        soundBoardItemAdapter =
                new SoundboardItemAdapter(mediaPlayerManagerService, dummySoundboard);

        gridView.setAdapter(soundBoardItemAdapter);

        // Here, activity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE);
            // From here on see #onRequestPermissionsResult()
        } else {
            new FindSoundboardsTask(getActivity()).execute();
        }

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // User denied. Stop the app.
                getActivity().finishAndRemoveTask();
            } else {
                new FindSoundboardsTask(getActivity()).execute();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@Nonnull Menu menu, @Nonnull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@Nonnull MenuItem item) {
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
    public void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_main_context, menu);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        SoundBoardItemRow itemRow = (SoundBoardItemRow) adapterContextMenuInfo.targetView;

        menu.setHeaderTitle(itemRow.getSoundName());
    }

    @Override
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.context_menu_edit_sound:
                AdapterView.AdapterContextMenuInfo menuInfo =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                SoundBoardItemRow itemRow = (SoundBoardItemRow) menuInfo.targetView;
                Sound sound = itemRow.getSound();

                Log.d(TAG, "Editing sound \"" + sound.getName() + "\"");

                Intent intent = SoundEditActivity.newIntent(getActivity(), sound);
                startActivityForResult(intent, REQUEST_EDIT_SOUND);
                return true;
            case R.id.context_menu_remove_sound:
                menuInfo =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

                Log.d(TAG, "Removing sound " + menuInfo.position);
                soundBoardItemAdapter.remove(menuInfo.position);
                new RemoveSoundsTask(getActivity()).execute(menuInfo.position);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_RESET_ALL:
                Log.i(TAG, "Resetting sound data");
                soundBoardItemAdapter.clear();
                new ResetAllTask(getActivity()).execute();
                break;
            case REQUEST_EDIT_SOUND:
                Log.d(TAG, "Returned from sound edit fragment with OK");
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: 17.03.2019 destroy service save state
    }

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    public class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;
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

            ImmutableList<Soundboard> res = soundboardDao.findAll();

            if (res.isEmpty()) {
                Log.d(TAG, "No soundboards found.");
                Log.d(TAG, "Insert some dummy data...");

                soundboardDao.insertDummyData();

                Log.d(TAG, "...and load the soundboards again");
                res = soundboardDao.findAll();
            }

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

            soundBoardItemAdapter.setSoundboard(soundboards.iterator().next());
        }
    }

    /**
     * A background task, used to remove sounds with the given indexes from the soundboard
     */
    public class RemoveSoundsTask extends AsyncTask<Integer, Void, Void> {
        private final String TAG = RemoveSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final SoundboardDao soundboardDao = SoundboardDao.getInstance(getActivity());

        RemoveSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        protected Void doInBackground(Integer... indexes) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            for (int index : indexes) {
                Log.d(TAG, "Removing sound + " + index + " from soundboard");

                soundboardDao.unlinkSound(soundBoardItemAdapter.getSoundboard(), index);
            }

            return null;
        }
    }

    /**
     * A background task, used to reset the soundboards and retrieve them from the database.
     */
    public class ResetAllTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = ResetAllTask.class.getName();

        private final WeakReference<Context> appContextRef;
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

            soundBoardItemAdapter.setSoundboard(soundboards.iterator().next());
        }
    }
}
