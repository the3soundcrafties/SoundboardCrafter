package de.soundboardcrafter.activity.audiofile.list;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
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
        AudioFileRow.Callback,
        SoundEventListener {
    private enum SortOrder {
        BY_NAME(Comparator.comparing(AudioModelAndSound::getCollationKey)),
        BY_DATE(Comparator.comparing(AudioModelAndSound::getDateAdded).reversed());
        private final Comparator<AudioModelAndSound> comparator;

        SortOrder(Comparator<AudioModelAndSound> comparator) {
            this.comparator = comparator;
        }

        Comparator<AudioModelAndSound> getComparator() {
            return comparator;
        }
    }

    private static final Comparator<AudioFolder> FOLDER_COMPARATOR =
            Comparator.comparing(AudioFolder::getPath)
                    .thenComparing(AudioFolder::getNumAudioFiles);

    private static final String TAG = AudioFileListFragment.class.getName();

    private static final String STATE_SORT_ORDER = "sortOrder";
    private static final String STATE_FOLDER = "folder";

    /**
     * Request code used whenever a sound edit
     * fragment is started from this activity
     */
    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    private MenuItem byFolderMenuItem;
    private ListView listView;
    private ConstraintLayout folderLayout;
    private ImageView iconFolder;
    private TextView folderPath;
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
        return new AudioFileListFragment();
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
        requireActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    @UiThread
    public void onPause() {
        super.onPause();
        stopPlaying();

        requireActivity().unbindService(this);
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_audiofile_list,
                container, false);

        /*
        Animator scaleUp = ObjectAnimator.ofPropertyValuesHolder((Object) null,
                PropertyValuesHolder.ofFloat("translateY", 0, -300));
        scaleUp.setDuration(3000);
        scaleUp.setStartDelay(3000);
        scaleUp.setInterpolator(new OvershootInterpolator());

        Animator scaleDown = ObjectAnimator.ofPropertyValuesHolder((Object) null,
                PropertyValuesHolder.ofFloat("translateY", -300, 0));
        scaleDown.setDuration(3000);
        scaleDown.setInterpolator(new OvershootInterpolator());


        LayoutTransition itemLayoutTransition = new LayoutTransition();
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp);
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, scaleDown);

        rootView.setLayoutTransition(itemLayoutTransition);
        */

        folderLayout = rootView.findViewById(R.id.folderLayout);
        iconFolder = rootView.findViewById(R.id.icon_folder);
        folderPath = rootView.findViewById(R.id.folder_path);
        listView = rootView.findViewById(R.id.list_view_audiofile);

        if (savedInstanceState != null) {
            sortOrder = (SortOrder) savedInstanceState.getSerializable(STATE_SORT_ORDER);
            setFolder(savedInstanceState.getString(STATE_FOLDER));
        } else {
            sortOrder = SortOrder.BY_NAME;
            setFolder(null);
        }

        byFolderMenuItem = null;

        initAudioFileListItemAdapter();

        iconFolder.setOnClickListener(v -> {
                    if (folder == null) {
                        return;
                    }

                    int lastIndexOfSlash = folder.lastIndexOf("/");
                    if (lastIndexOfSlash < 0) {
                        return;
                    }

                    String newFolder = folder.substring(0, lastIndexOfSlash);
                    if (newFolder.isEmpty()) {
                        newFolder = "/";
                    }
                    changeFolder(newFolder);
                }
        );

        listView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (view instanceof AudioFileRow) {
                        AudioFileRow audioFileItemRow = (AudioFileRow) view;
                        onClickAudioFile(audioFileItemRow, position);
                    } else if (view instanceof AudioSubfolderRow) {
                        AudioSubfolderRow audioSubfolderRow = (AudioSubfolderRow) view;
                        onClickAudioSubfolder(audioSubfolderRow);
                    }
                });

        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            new FindAudioFileTask(requireContext(), folder, sortOrder).execute();
        } // otherwise we will receive an event later

        return rootView;
    }

    private void updateByFolderMenuItem() {
        if (byFolderMenuItem == null) {
            return;
        }

        if (folder == null) {
            byFolderMenuItem.setTitle(R.string.toolbar_menu_audiofiles_folders_all);
            byFolderMenuItem.setIcon(R.drawable.ic_long_list);
        } else {
            byFolderMenuItem.setTitle(R.string.toolbar_menu_audiofiles_folders_single);
            byFolderMenuItem.setIcon(R.drawable.ic_by_folder);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_audiofile_file, menu);

        byFolderMenuItem = menu.findItem(R.id.toolbar_menu_audiofiles_by_folder);
        updateByFolderMenuItem();
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
            case R.id.toolbar_menu_audiofiles_by_folder:
                toggleByFolder();
                return true;
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

    private void toggleByFolder() {
        if (folder == null) {
            setFolder("/");
        } else {
            setFolder(null);
        }

        setAudioFolderEntries(ImmutableList.of());

        new FindAudioFileTask(requireContext(), folder, sortOrder).execute();
    }

    private void setFolder(@Nullable String folder) {
        this.folder = folder;
        folderPath.setText(folder);
        setVisibilityFolder(folder != null ? View.VISIBLE : View.GONE);
        if ("/".equals(folder)) {
            iconFolder.setVisibility(View.INVISIBLE);
        }
    }

    private void setVisibilityFolder(int visibility) {
        folderLayout.setVisibility(visibility);
        iconFolder.setVisibility(visibility);
        folderPath.setVisibility(visibility);
    }

    private void sort(SortOrder sortOrder) {
        this.sortOrder = sortOrder;

        new FindAudioFileTask(requireContext(), folder, sortOrder).execute();
    }

    private void onClickAudioSubfolder(AudioSubfolderRow audioSubfolderRow) {
        @Nullable String subfolderPath = audioSubfolderRow.getPath();
        if (subfolderPath == null) {
            return;
        }

        changeFolder(subfolderPath);
    }

    private void changeFolder(@NonNull String newFolder) {
        if (folder == null) {
            sortOrder = SortOrder.BY_NAME;
        }

        setFolder(newFolder);

        new FindAudioFileTask(requireContext(), newFolder, sortOrder).execute();
    }

    private void onClickAudioFile(AudioFileRow audioFileItemRow,
                                  int position) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        @Nullable boolean positionWasPlaying = adapter.isPlaying(position);
        stopPlaying();

        if (!positionWasPlaying) {
            adapter.setPositionPlaying(position);

            AudioModelAndSound audioModelAndSound = (AudioModelAndSound) adapter.getItem(position);
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
            new AudioFileListFragment.SaveNewSoundTask(requireActivity(), sound).execute();
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
                if (soundEventListenerActivity != null) {
                    final UUID soundId = UUID.fromString(
                            data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID));

                    soundEventListenerActivity.soundChanged(soundId);
                }
                break;
        }
    }

    @Override
    public void somethingMightHaveChanged() {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        new FindAudioFileTask(context, folder, sortOrder).execute();
    }

    @Override
    public void soundChanged(UUID soundId) {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        // The sound NAME may have been changed.
        new FindAudioFileTask(context, folder, sortOrder).execute();
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new AudioFileListItemAdapter(this);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void setAudioFolderEntries(ImmutableList<? extends AbstractAudioFolderEntry> audioFolderEntries) {
        stopPlaying();
        adapter.setAudioFolderEntries(audioFolderEntries);
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

        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

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
     * A background task, used to retrieve audio files (and audio folders) from the file system
     * and corresponding sounds from the database.
     */
    class FindAudioFileTask extends AsyncTask<Void, Void, ImmutableList<AbstractAudioFolderEntry>> {
        private final String TAG = FindAudioFileTask.class.getName();

        private final WeakReference<Context> appContextRef;

        @Nullable
        private final String folder;

        private final Comparator<AbstractAudioFolderEntry> entryComparator =
                new Comparator<AbstractAudioFolderEntry>() {
                    @Override
                    public int compare(AbstractAudioFolderEntry one, AbstractAudioFolderEntry other) {
                        if (one instanceof AudioFolder) {
                            // one instanceof AudioFolder
                            if (!(other instanceof AudioFolder)) {
                                return -1;
                            }

                            // one and other instanceof AudioFolder
                            return FOLDER_COMPARATOR.compare((AudioFolder) one, (AudioFolder) other);
                        }

                        // one instanceof AudioModelAndSound
                        if (!(other instanceof AudioModelAndSound)) {
                            return 1;
                        }

                        // one and other instanceof AudioModelAndSound
                        return sortOrder.getComparator()
                                .compare((AudioModelAndSound) one, (AudioModelAndSound) other);
                    }
                };
        private final SortOrder sortOrder;

        FindAudioFileTask(Context context, @Nullable String folder, @NonNull SortOrder sortOrder) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.folder = folder;
            this.sortOrder = sortOrder;
        }

        @Override
        @WorkerThread
        protected ImmutableList<AbstractAudioFolderEntry> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            AudioLoader audioLoader = new AudioLoader();

            Log.d(TAG, "Loading audio files from file system...");

            Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>> audioModelsAndFolders =
                    audioLoader.getAudioFromDevice(appContext, folder);

            Log.d(TAG, "Audio files loaded.");

            Log.d(TAG, "Loading sounds from database...");

            SoundDao soundDao = SoundDao.getInstance(appContext);
            Map<String, Sound> soundMap = soundDao.findAllByPath();

            Log.d(TAG, "Sounds loaded.");

            ArrayList<AbstractAudioFolderEntry> res =
                    new ArrayList<>(
                            audioModelsAndFolders.first.size() +
                                    audioModelsAndFolders.second.size());
            res.addAll(audioModelsAndFolders.second);
            for (AudioModel audioModel : audioModelsAndFolders.first) {
                res.add(new AudioModelAndSound(audioModel, soundMap.get(audioModel.getPath())));
            }

            res.sort(entryComparator);

            return ImmutableList.copyOf(res);
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<AbstractAudioFolderEntry> audioFolderEntries) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            updateByFolderMenuItem();
            setAudioFolderEntries(audioFolderEntries);
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
