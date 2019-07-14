package de.soundboardcrafter.activity.soundboard.play;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.activity.sound.edit.soundboard.play.SoundboardPlaySoundEditActivity;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment implements ServiceConnection {
    private static final String TAG = SoundboardFragment.class.getName();

    private static final String ARG_SOUNDBOARD = "Soundboard";

    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    private enum SortOrder {
        BY_NAME(Comparator.comparing(Sound::getCollationKey));
        // TODO Have other sort orders?

        private final Comparator<Sound> comparator;

        SortOrder(Comparator<Sound> comparator) {
            this.comparator = comparator;
        }
    }

    private GridView gridView;
    // TODO Allow for zero soundboards
    private SoundboardItemAdapter soundboardItemAdapter;
    private MediaPlayerService mediaPlayerService;
    private SoundboardWithSounds soundboard;
    private static final String ARG_SORT_ORDER = "sortOrder";
    private SortOrder sortOrder;

    /**
     * Creates a <code>SoundboardFragment</code> for this soundboard.
     */
    static SoundboardFragment createFragment(SoundboardWithSounds soundboard) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_SOUNDBOARD, soundboard);
        SoundboardFragment fragment = new SoundboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        //as soon the media player service is connected, the play/stop icons can be set correctly
        updateUI();

        Log.d(TAG, "MediaPlayerService is connected");
    }

    @Override
    @UiThread
    public void onServiceDisconnected(ComponentName name) {
        // TODO What to do on Service Disconnected?
    }

    @Override
    @UiThread
    public void onBindingDied(ComponentName name) {
        // TODO What to do on Service Died?
    }

    @Override
    @UiThread
    public void onNullBinding(ComponentName name) {
        // TODO What to do on Null Binding?
    }

    @Override
    @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("SoundboardFragment without arguments");
        }
        soundboard = (SoundboardWithSounds) arguments.getSerializable(ARG_SOUNDBOARD);

        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().startService(intent);
        // TODO Necessary?! Also done in onResume()
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    @UiThread
    public void onPause() {
        super.onPause();
        requireActivity().unbindService(this);
    }


    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard,
                container, false);

        Bundle args = getArguments();
        if (args != null) {
            sortOrder = (SortOrder) args.getSerializable(ARG_SORT_ORDER);
        }
        if (sortOrder == null) {
            sortOrder = SortOrder.BY_NAME;
        }
        gridView = rootView.findViewById(R.id.grid_view_soundboard);
        initSoundboardItemAdapter();
        registerForContextMenu(gridView);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (!(view instanceof SoundboardItemRow)) {
                return;
            }
            SoundboardItemRow soundboardItemRow = (SoundboardItemRow) view;

            onClickSoundboard(soundboardItemRow, soundboardItemAdapter.getItem(position));
        });
        return rootView;
    }

    private void onClickSoundboard(SoundboardItemRow soundboardItemRow, Sound sound) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        if (!service.isActivelyPlaying(soundboard.getSoundboard(), sound)) {
            soundboardItemRow.setImage(R.drawable.ic_stop);
            service.play(soundboard.getSoundboard(), sound,
                    soundboardItemAdapter::notifyDataSetChanged);
        } else {
            service.stopPlaying(soundboard.getSoundboard(), sound, true);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_menu_sound_sort_alpha:
                new SoundSortInSoundboardTask(requireContext(), soundboard, SortOrder.BY_NAME).execute();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private SoundboardItemRow.MediaPlayerServiceCallback newMediaPlayerServiceCallback() {
        return new SoundboardItemRow.MediaPlayerServiceCallback() {
            @Override
            public boolean isConnected() {
                return getService() != null;
            }

            @Override
            public boolean isActivelyPlaying(Soundboard soundboard, Sound sound) {
                MediaPlayerService service = getService();
                if (service == null) {
                    return false;
                }
                return service.isActivelyPlaying(soundboard, sound);
            }

            @Override
            public void setOnPlayingStopped(Soundboard soundboard, Sound sound,
                                            SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.setOnPlayingStopped(soundboard, sound, onPlayingStopped);
                }
            }

            @Override
            public void stopPlaying(Soundboard soundboard, Sound sound, boolean fadeOut) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.stopPlaying(soundboard, sound, fadeOut);
                }
            }
        };
    }


    /**
     * Starts reading the data for the UI (first time) or
     * simply ensure that the grid shows the latest information.
     */
    @UiThread
    private void updateUI() {
        if (soundboardItemAdapter != null) {
            soundboardItemAdapter.notifyDataSetChanged();
        }
    }

    @Override
    @UiThread
    // Called especially when the SoundboardPlaySoundEditActivity returns.
    public void onResume() {
        super.onResume();
        updateUI();
        bindService();
    }

    @UiThread
    private void initSoundboardItemAdapter() {
        soundboardItemAdapter =
                new SoundboardItemAdapter(newMediaPlayerServiceCallback(), soundboard);
        gridView.setAdapter(soundboardItemAdapter);
        updateUI();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_sound, menu);
    }


    @Override
    @UiThread
    public void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_soundboard_play_context, menu);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        SoundboardItemRow itemRow = (SoundboardItemRow) adapterContextMenuInfo.targetView;

        menu.setHeaderTitle(itemRow.getSoundName());
    }

    @Override
    @UiThread
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        if (!getUserVisibleHint()) {
            // The wrong fragment got the event.
            // See https://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager-receives-oncontextitemselected-call
            return false; // Pass the event to the next fragment
        }

        switch (item.getItemId()) {
            case R.id.context_menu_edit_sound:
                AdapterView.AdapterContextMenuInfo menuInfo =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                SoundboardItemRow itemRow = (SoundboardItemRow) menuInfo.targetView;
                Sound sound = itemRow.getSound();

                Log.d(TAG, "Editing sound " + this + " \"" + sound.getName() + "\"");

                Intent intent = SoundboardPlaySoundEditActivity.newIntent(getActivity(), sound);
                startActivityForResult(intent, EDIT_SOUND_REQUEST_CODE);
                return true;
            case R.id.context_menu_remove_sound:
                menuInfo =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

                Log.d(TAG, "Removing sound " + menuInfo.position);
                soundboardItemAdapter.remove(menuInfo.position);
                new RemoveSoundsTask(requireActivity()).execute(menuInfo.position);
                return true;
            default:
                return false;
        }
    }

    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case EDIT_SOUND_REQUEST_CODE:
                Log.d(TAG, "Editing sound " + this + ": Returned from sound edit fragment with OK");

                final UUID soundId = UUID.fromString(
                        data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID));
                // The sound details may have been changed, but not its soundboards!
                new UpdateSoundsTask(requireActivity()).execute(soundId);
                break;
        }
    }

    @Override
    @UiThread
    public void onDestroy() {
        // TODO: 17.03.2019 destroy service save state

        super.onDestroy();
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }


    class SoundSortInSoundboardTask extends AsyncTask<Void, Void, SoundboardWithSounds> {
        private final String TAG = UpdateSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;
        final SoundboardWithSounds soundboardWithSounds;
        final SortOrder order;

        SoundSortInSoundboardTask(Context context, SoundboardWithSounds soundboardWithSounds, SortOrder order) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundboardWithSounds = soundboardWithSounds;
            this.order = order;
        }

        @Override
        @WorkerThread
        protected SoundboardWithSounds doInBackground(Void... v) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }


            Log.d(TAG, "Sorting sounds: " + soundboardWithSounds);
            soundboardWithSounds.sortSoundsBy(order.comparator);

            SoundboardDao.getInstance(appContext).linkSoundsInOrder(soundboardWithSounds);

            return soundboardWithSounds;
        }

        @Override
        @UiThread
        protected void onPostExecute(SoundboardWithSounds soundboardWithSounds) {
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

            soundboardItemAdapter.updateSounds(soundboardWithSounds.getSounds());
        }
    }

    /**
     * A background task, used to retrieve some sounds from the database and update the GUI.
     */
    class UpdateSoundsTask extends AsyncTask<UUID, Void, Map<Sound, Boolean>> {
        private final String TAG = UpdateSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        UpdateSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected Map<Sound, Boolean> doInBackground(UUID... soundIds) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading sounds: " + Arrays.toString(soundIds));
            Map<Sound, Boolean> sounds = new HashMap<>();
            for (UUID soundId : soundIds) {
                SoundWithSelectableSoundboards soundWithSelectableSoundboards =
                        SoundDao.getInstance(appContext).findSoundWithSelectableSoundboards(soundId);
                List<SelectableSoundboard> soundboards = soundWithSelectableSoundboards.getSoundboards();
                SelectableSoundboard foundSoundboard = soundboards.stream().filter(s -> s.getSoundboard().getId().equals(soundboard.getId()))
                        .findFirst().get();
                if (!foundSoundboard.isSelected()) {
                    sounds.put(soundWithSelectableSoundboards.getSound(), false);
                } else {
                    sounds.put(soundWithSelectableSoundboards.getSound(), true);
                }

            }

            return sounds;
        }

        @Override
        @UiThread
        protected void onPostExecute(Map<Sound, Boolean> sounds) {
            stopSoundsNotInSoundboard(sounds);
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

        void stopSoundsNotInSoundboard(Map<Sound, Boolean> sounds) {
            for (Map.Entry<Sound, Boolean> soundEntry : sounds.entrySet()) {
                if (!soundEntry.getValue()) {
                    mediaPlayerService.stopPlaying(soundboard.getSoundboard(), soundEntry.getKey(), true);
                }
            }
        }
    }

    /**
     * A background task, used to remove sounds with the given indexes from the soundboard
     */
    class RemoveSoundsTask extends AsyncTask<Integer, Void, Void> {
        private final String TAG = RemoveSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        RemoveSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Integer... indexes) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);

            for (int index : indexes) {
                Log.d(TAG, "Removing sound + " + index + " from soundboard");

                soundboardDao.unlinkSound(soundboardItemAdapter.getSoundboard().getSoundboard(), index);
            }

            return null;
        }
    }
}
