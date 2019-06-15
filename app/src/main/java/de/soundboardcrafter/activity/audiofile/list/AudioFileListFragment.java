package de.soundboardcrafter.activity.audiofile.list;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.activity.sound.edit.audiofile.list.AudiofileListSoundEditActivity;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.model.Sound;

/**
 * Shows Soundboard in a Grid
 */
public class AudioFileListFragment extends Fragment implements
        ServiceConnection,
        AudioFileItemRow.Callback,
        SoundEventListener {
    private enum SortOrder {
        BY_NAME(Comparator.comparing(AudioModelAndSound::getName)),
        BY_DATE(Comparator.comparing(AudioModelAndSound::getDateAdded).reversed());
        private final Comparator<AudioModelAndSound> comparator;

        SortOrder(Comparator<AudioModelAndSound> comparator) {
            this.comparator = comparator;
        }

        public Comparator<AudioModelAndSound> getComparator() {
            return comparator;
        }
    }

    private static final String TAG = AudioFileListFragment.class.getName();

    private static final String STATE_SORT_ORDER = "sortOrder";
    private static final String STATE_FOLDER = "folder";

    /**
     * Request code used whenever a sound edit
     * fragment is started from this activity
     */
    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    private ListView listView;
    private AudioFileListItemAdapter adapter;
    private MediaPlayerService mediaPlayerService;
    private @Nullable
    SoundboardMediaPlayer mediaPlayer;

    private @Nullable
    SoundEventListener soundEventListenerActivity;

    private SortOrder sortOrder;

    private @Nullable
    String folder;

    /**
     * Creates an <code>AudioFileListFragment</code>.
     */
    public static AudioFileListFragment createFragment() {
        AudioFileListFragment fragment = new AudioFileListFragment();
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // TODO Necessary?! Also done in onResume()
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    @UiThread
    public void onPause() {
        super.onPause();
        stopPlaying();

        getActivity().unbindService(this);
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            sortOrder = (SortOrder) savedInstanceState.getSerializable(STATE_SORT_ORDER);
            folder = savedInstanceState.getString(STATE_FOLDER);
        } else {
            sortOrder = SortOrder.BY_NAME;
            folder = null;
        }

        View rootView = inflater.inflate(R.layout.fragment_audiofile_list,
                container, false);
        listView = rootView.findViewById(R.id.listview_audiofile);

        initAudioFileListItemAdapter();

        listView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (!(view instanceof AudioFileItemRow)) {
                        return;
                    }
                    AudioFileItemRow audioFileItemRow = (AudioFileItemRow) view;
                    onClickAudioFile(audioFileItemRow, position);
                });

        new FindAudioFileTask(getContext(), sortOrder).execute();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_audiofile_file, menu);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof SoundEventListener) {
            soundEventListenerActivity = (SoundEventListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        soundEventListenerActivity = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_menu_audiofiles_sort_alpha:
                sort(SortOrder.BY_NAME);
                return true;
            case R.id.toolbar_menu_audiofiles_sort_date:
                sort(SortOrder.BY_DATE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sort(SortOrder sortOrder) {
        this.sortOrder = sortOrder;

        new FindAudioFileTask(getContext(), sortOrder).execute();
    }

    private void onClickAudioFile(AudioFileItemRow audioFileItemRow,
                                  int position) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        @Nullable boolean positionWasPlaying = adapter.isPlaying(position);
        stopPlaying();

        if (!positionWasPlaying) {
            adapter.setPositionPlaying(position);

            AudioModelAndSound audioModelAndSound = adapter.getItem(position);
            audioFileItemRow.setImage(R.drawable.ic_stop);
            mediaPlayer = service.play(audioModelAndSound.getName(),
                    audioModelAndSound.getAudioModel().getPath(),
                    () -> {
                        adapter.setPositionPlaying(null);
                        mediaPlayer = null;
                    });
        }
    }

    public void stopPlaying() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
        mediaPlayer = null;
    }

    private void removeMediaPlayer(@NonNull SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.release();

    }

    // TODO Inherit from some MediaPlayerFragment?! Use some MediaPlayerSupport??!
    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }


    @Override
    @UiThread
    public void onEditAudioFile(AudioModelAndSound audioModelAndSound) {
        final Sound sound;
        if (audioModelAndSound.getSound() == null) {
            // Create and save new sound
            sound = new Sound(audioModelAndSound.getAudioModel().getPath(),
                    audioModelAndSound.getAudioModel().getName());
            new AudioFileListFragment.SaveNewSoundTask(getActivity(), sound).execute();
        } else {
            // Use existing sound
            sound = audioModelAndSound.getSound();
        }

        Log.d(TAG, "Editing sound for audio file " +
                audioModelAndSound.getAudioModel().getPath());

        Intent intent = AudiofileListSoundEditActivity.newIntent(getContext(), sound);
        startActivityForResult(intent, EDIT_SOUND_REQUEST_CODE);
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

                if (soundEventListenerActivity != null) {
                    final UUID soundId = UUID.fromString(
                            data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID));

                    soundEventListenerActivity.soundChanged(soundId);
                }
                break;
        }
    }

    @Override
    public void soundChanged(UUID soundId) {
        // The sound NAME may have been changed.
        new FindAudioFileTask(getContext(), sortOrder).execute();
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new AudioFileListItemAdapter(this);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void setAudioFiles(ImmutableList<AudioModelAndSound> audioFilesAndSounds) {
        stopPlaying();
        adapter.setAudioFiles(audioFilesAndSounds);
    }


    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    @UiThread
    // Called especially when the edit activity returns.
    public void onResume() {
        super.onResume();

        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // updateUI();
        bindService();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(STATE_SORT_ORDER, sortOrder);
        outState.putString(STATE_FOLDER, folder);
        super.onSaveInstanceState(outState);
    }

    /**
     * A background task, used to retrieve audio files from the file system
     * and corresponding sounds from the database.
     */
    class FindAudioFileTask extends AsyncTask<Void, Void, ImmutableList<AudioModelAndSound>> {
        private final String TAG = FindAudioFileTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private SortOrder sortOrder;

        FindAudioFileTask(Context context, SortOrder sortOrder) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.sortOrder = sortOrder;
        }

        @Override
        @WorkerThread
        protected ImmutableList<AudioModelAndSound> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            AudioLoader audioLoader = new AudioLoader();

            Log.d(TAG, "Loading audio files from file system...");

            ImmutableList<AudioModel> audioModels =
                    audioLoader.getAllAudioFromDevice(appContext);

            Log.d(TAG, "Audio files loaded.");

            Log.d(TAG, "Loading sounds from database...");

            SoundDao soundDao = SoundDao.getInstance(appContext);
            Map<String, Sound> soundMap = soundDao.findAllByPath();

            Log.d(TAG, "Sounds loaded.");

            ArrayList<AudioModelAndSound> res = new ArrayList<>(audioModels.size());
            for (AudioModel audioModel : audioModels) {
                res.add(new AudioModelAndSound(audioModel, soundMap.get(audioModel.getPath())));
            }

            res.sort(sortOrder.getComparator());

            return ImmutableList.copyOf(res);
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<AudioModelAndSound> audioFilesAndSounds) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            setAudioFiles(audioFilesAndSounds);
        }
    }

    /**
     * A background task, used to save the sound
     */
    class SaveNewSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final Sound sound;

        SaveNewSoundTask(Context context, Sound sound) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.sound = sound;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving sound " + sound);

            // TODO If name already exists, choose another name - like ... "Name1", "Name2"

            SoundDao.getInstance(appContext).insert(sound);

            return null;
        }
    }
}
