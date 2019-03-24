package de.soundboardcrafter.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.activity.sound.edit.SoundEditActivity;
import de.soundboardcrafter.activity.sound.edit.SoundEditFragment;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment implements ServiceConnection {
    private static final String TAG = SoundboardFragment.class.getName();

    private static final String DIALOG_RESET_ALL = "DialogResetAll";

    private static final int REQUEST_RESET_ALL = 0;
    private static final int REQUEST_EDIT_SOUND = 1;

    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;

    private GridView gridView;
    // TODO Allow for zero or more than one soundboards
    private SoundboardItemAdapter soundboardItemAdapter;
    private MediaPlayerService mediaPlayerService;

    public SoundboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        Log.d(TAG, "MediaPlayerService is connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onBindingDied(ComponentName name) {

    }

    @Override
    public void onNullBinding(ComponentName name) {

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().startService(intent);
        bindService();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(this);
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard,
                container, false);

        gridView = rootView.findViewById(R.id.grid_view_soundboard);
        registerForContextMenu(gridView);

        //TODO start without any soundboard
        Soundboard dummySoundboard = new Soundboard("Dummy", Lists.newArrayList());
        soundboardItemAdapter =
                new SoundboardItemAdapter(newMediaPlayerServiceCallback(), dummySoundboard);
        gridView.setAdapter(soundboardItemAdapter);
        return rootView;
    }

    private SoundBoardItemRow.MediaPlayerServiceCallback newMediaPlayerServiceCallback() {
        SoundBoardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback = new SoundBoardItemRow.MediaPlayerServiceCallback() {
            @Override
            public boolean shouldBePlaying(Soundboard soundboard, Sound sound) {
                MediaPlayerService service = getService();
                if (service == null) {
                    return false;
                }
                return service.shouldBePlaying(soundboard, sound);
            }

            @Override
            public void initMediaPlayer(Soundboard soundboard, Sound sound, SoundboardMediaPlayer.InitializeCallback initializeCallback,
                                        SoundboardMediaPlayer.StartPlayCallback playCallback, SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.initMediaPlayer(soundboard, sound, initializeCallback, playCallback, stopPlayCallback);
                }
            }

            @Override
            public void startPlaying(Soundboard soundboard, Sound sound) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.startPlaying(soundboard, sound);
                }
            }

            @Override
            public void setMediaPlayerCallbacks(Soundboard soundboard, Sound sound, SoundboardMediaPlayer.StartPlayCallback startPlayCallback,
                                                SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.setMediaPlayerCallbacks(soundboard, sound, startPlayCallback, stopPlayCallback);
                }
            }

            @Override
            public void stopPlaying(Soundboard soundboard, Sound sound) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.stopPlaying(soundboard, sound);
                }

            }
        };
        return mediaPlayerServiceCallback;
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
    // Called especially when the SoundEditActivity returns.
    public void onResume() {
        super.onResume();

        updateUI();
    }

    /**
     * Starts reading the data for the UI (first time) or
     * simple ensure that the grid shows the latest information.
     */
    private void updateUI() {
        if (soundboardItemAdapter == null) {
            initSoundboardItemAdapter();
            return;
        }

        soundboardItemAdapter.notifyDataSetChanged();
    }

    private void initSoundboardItemAdapter() {
        // TODO Start without any soundboard
        Soundboard dummySoundboard = new Soundboard("Dummy", Lists.newArrayList());
        soundboardItemAdapter =
                new SoundboardItemAdapter(mediaPlayerManagerService, dummySoundboard);

        gridView.setAdapter(soundboardItemAdapter);

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
                soundboardItemAdapter.remove(menuInfo.position);
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
                soundboardItemAdapter.clear();
                new ResetAllTask(getActivity()).execute();
                break;
            case REQUEST_EDIT_SOUND:
                Log.d(TAG, "Returned from sound edit fragment with OK");

                final UUID soundId = UUID.fromString(
                        data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID));
                new UpdateSoundsTask(getActivity()).execute(soundId);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: 17.03.2019 destroy service save state
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            bindService();
        }
        return mediaPlayerService;
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

            soundboardItemAdapter.setSoundboard(soundboards.iterator().next());
        }
    }

    /**
     * A background task, used to retrieve some sounds from the database and update the GUI.
     */
    public class UpdateSoundsTask extends AsyncTask<UUID, Void, ImmutableCollection<Sound>> {
        private final String TAG = UpdateSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final SoundboardDao soundboardDao = SoundboardDao.getInstance(getActivity());

        UpdateSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        protected ImmutableCollection<Sound> doInBackground(UUID... soundIds) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading sounds: " + Arrays.toString(soundIds));

            return soundboardDao.findSounds(soundIds);
        }

        @Override
        protected void onPostExecute(ImmutableCollection<Sound> sounds) {
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

            soundboardItemAdapter.updateSounds(sounds);
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

                soundboardDao.unlinkSound(soundboardItemAdapter.getSoundboard(), index);
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

            soundboardItemAdapter.setSoundboard(soundboards.iterator().next());
        }
    }
}
