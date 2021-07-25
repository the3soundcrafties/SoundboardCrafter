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

import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audioloader.AudioLoader;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.activity.sound.edit.audiofile.list.AudiofileListSoundEditActivity;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.AnywhereInTheFileSystemAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.AudioModelAndSound;
import de.soundboardcrafter.model.audio.FullAudioModel;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Shows Soundboard in a Grid
 */
public class AudioFileListFragment extends Fragment implements
        ServiceConnection,
        AudioFileRow.Callback,
        SoundEventListener {
    private enum SortOrder {
        BY_NAME(Comparator.comparing(AudioModelAndSound::getCollationKey)),
        BY_DATE(Comparator.comparing(AudioModelAndSound::getDateAdded,
                Comparator.nullsFirst(Comparator.reverseOrder())));

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
    private static final String STATE_FOLDER_TYPE = "folderType";
    private static final String STATE_FOLDER_TYPE_FILE = "file";
    private static final String STATE_FOLDER_TYPE_ASSET = "asset";
    private static final String STATE_FOLDER_PATH = "folderPath";

    /**
     * Request code used whenever this activity starts a sound edit
     * fragment
     */
    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    private MenuItem byFolderMenuItem;
    private ListView listView;
    private ConstraintLayout folderLayout;
    private ImageView iconFolder;
    private TextView folderPath;
    private AudioFileListItemAdapter adapter;
    private MediaPlayerService mediaPlayerService;
    @Nullable
    private SoundboardMediaPlayer mediaPlayer;

    @Nullable
    private SoundEventListener soundEventListenerActivity;

    private SortOrder sortOrder;

    private IAudioFileSelection selection;

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
        // As soon the media player service is connected, the play/stop icons can be set correctly
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
            setSelection(getFolder(savedInstanceState));
        } else {
            sortOrder = SortOrder.BY_NAME;
            setSelection(new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH));
        }

        byFolderMenuItem = null;

        initAudioFileListItemAdapter();

        iconFolder.setOnClickListener(v -> {
                    if (selection instanceof AnywhereInTheFileSystemAudioLocation || isRootFolder()) {
                        return;
                    }

                    String folderPath = getFolderPath();
                    int lastIndexOfSlash = requireNonNull(folderPath).lastIndexOf("/");
                    if (lastIndexOfSlash < 0) {
                        return;
                    }

                    String newFolder = folderPath.substring(0, lastIndexOfSlash);
                    if (newFolder.isEmpty() && selection instanceof FileSystemFolderAudioLocation) {
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

        if (ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            new FindAudioFilesTask(requireContext(), selection, sortOrder).execute();
        } // Otherwise, we will receive an event later.

        return rootView;
    }

    private @Nullable
    String getFolderPath() {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            return null;
        }

        if (selection instanceof FileSystemFolderAudioLocation) {
            return ((FileSystemFolderAudioLocation) selection).getPath();
        }

        if (selection instanceof AssetFolderAudioLocation) {
            return ((AssetFolderAudioLocation) selection).getAssetPath();
        }

        throw new IllegalStateException("Unexpected folder type: " + selection.getClass());
    }

    private void changeFolder(String newFolder) {
        checkNotNull(selection, "newFolder");

        if (selection instanceof FileSystemFolderAudioLocation) {
            changeFolder(new FileSystemFolderAudioLocation(newFolder));
        } else if (selection instanceof AssetFolderAudioLocation) {
            changeFolder(new AssetFolderAudioLocation(newFolder));
        } else {
            throw new IllegalStateException(
                    "Unexpected selection type: " + selection.getClass());
        }
    }

    private IAudioFileSelection getFolder(Bundle savedInstanceState) {
        @Nullable String type = savedInstanceState.getString(STATE_FOLDER_TYPE);
        @Nullable String path = savedInstanceState.getString(STATE_FOLDER_PATH);
        if (type == null || path == null) {
            return AnywhereInTheFileSystemAudioLocation.INSTANCE;
        }

        if (type.equals(STATE_FOLDER_TYPE_FILE)) {
            return new FileSystemFolderAudioLocation(path);
        }

        if (type.equals(STATE_FOLDER_TYPE_ASSET)) {
            return new AssetFolderAudioLocation(path);
        }

        throw new IllegalStateException("Unexpected folder type " + type);
    }

    private void updateByFolderMenuItem() {
        if (byFolderMenuItem == null) {
            return;
        }

        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            byFolderMenuItem.setTitle(R.string.toolbar_menu_audiofiles_folders_all_on_device);
            byFolderMenuItem.setIcon(R.drawable.ic_long_list);
        } else if (selection instanceof FileSystemFolderAudioLocation) {
            byFolderMenuItem.setTitle(R.string.toolbar_menu_audiofiles_folders_single);
            byFolderMenuItem.setIcon(R.drawable.ic_by_folder);
        } else if (selection instanceof AssetFolderAudioLocation) {
            byFolderMenuItem.setTitle(R.string.toolbar_menu_audiofiles_assets);
            byFolderMenuItem.setIcon(R.drawable.ic_included);
        } else {
            throw new IllegalStateException(
                    "Unexpected type of selection: " + selection.getClass());
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
        final int id = item.getItemId();
        if (id == R.id.toolbar_menu_audiofiles_by_folder) {
            toggleByFolder();
            return true;
        } else if (id == R.id.toolbar_menu_audiofiles_sort_alpha) {
            sort(SortOrder.BY_NAME);
            return true;
        } else if (id == R.id.toolbar_menu_audiofiles_sort_date) {
            sort(SortOrder.BY_DATE);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleByFolder() {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            setSelection(new FileSystemFolderAudioLocation("/"));
        } else if (selection instanceof FileSystemFolderAudioLocation) {
            setSelection(new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH));
        } else {
            setSelection(AnywhereInTheFileSystemAudioLocation.INSTANCE);
        }

        setAudioFolderEntries(ImmutableList.of());

        new FindAudioFilesTask(requireContext(), selection, sortOrder).execute();
    }

    private void setSelection(IAudioFileSelection selection) {
        this.selection = selection;
        folderPath.setText(calcFolderDisplayPath());
        setVisibilityFolder(selection instanceof AnywhereInTheFileSystemAudioLocation ? View.GONE :
                View.VISIBLE);
        if (isRootFolder()) {
            iconFolder.setVisibility(View.INVISIBLE);
        }
    }

    private @Nullable
    String calcFolderDisplayPath() {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            return null;
        }

        if (selection instanceof FileSystemFolderAudioLocation) {
            return calcFolderDisplayPath((FileSystemFolderAudioLocation) selection);
        }

        if (selection instanceof AssetFolderAudioLocation) {
            return calcFolderDisplayPath((AssetFolderAudioLocation) selection);
        }

        throw new IllegalStateException("Unexpected type of folder: " + selection.getClass());
    }

    private static String calcFolderDisplayPath(@NonNull FileSystemFolderAudioLocation folder) {
        return folder.getPath();
    }

    private static String calcFolderDisplayPath(@NonNull AssetFolderAudioLocation folder) {
        if (isRootFolder(folder)) {
            return "/";
        }

        return folder.getAssetPath().substring(AudioLoader.ASSET_SOUND_PATH.length());
    }

    private boolean isRootFolder() {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            return false;
        }

        if (selection instanceof FileSystemFolderAudioLocation) {
            return isRootFolder((FileSystemFolderAudioLocation) selection);
        }

        if (selection instanceof AssetFolderAudioLocation) {
            return isRootFolder((AssetFolderAudioLocation) selection);
        }

        throw new IllegalStateException("Unexpected type of folder: " + selection.getClass());
    }

    private static boolean isRootFolder(@NonNull FileSystemFolderAudioLocation folder) {
        return "/".equals(folder.getPath());
    }

    private static boolean isRootFolder(@NonNull AssetFolderAudioLocation folder) {
        return AudioLoader.ASSET_SOUND_PATH.equals(folder.getAssetPath());
    }

    private void setVisibilityFolder(int visibility) {
        folderLayout.setVisibility(visibility);
        iconFolder.setVisibility(visibility);
        folderPath.setVisibility(visibility);
    }

    private void sort(SortOrder sortOrder) {
        this.sortOrder = sortOrder;

        new FindAudioFilesTask(requireContext(), selection, sortOrder).execute();
    }

    private void onClickAudioSubfolder(AudioSubfolderRow audioSubfolderRow) {
        @Nullable String subfolderPath = audioSubfolderRow.getPath();
        if (subfolderPath == null) {
            return;
        }

        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            // (some race condition, perhaps?!)
            return;
        }

        changeFolder(subfolderPath);
    }

    private void changeFolder(@NonNull IAudioLocation newFolder) {
        setSelection(newFolder);

        new FindAudioFilesTask(requireContext(), newFolder, sortOrder).execute();
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
            try {
                mediaPlayer = service.play(audioModelAndSound.getName(),
                        audioModelAndSound.getAudioModel().getAudioLocation(),
                        () -> {
                            adapter.setPositionPlaying(null);
                            mediaPlayer = null;
                        });
            } catch (IOException e) {
                @Nullable UUID soundId = audioModelAndSound.getSoundId();

                adapter.setPositionPlaying(null);
                mediaPlayer = null;
                Snackbar snackbar = Snackbar
                        .make(listView, getString(R.string.audiofile_not_found),
                                Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.update_all_soundboards_and_sounds),
                                view -> {
                                    if (soundId != null) {
                                        new DeleteSoundTask(requireActivity(), soundId).execute();
                                    } else if (soundEventListenerActivity != null) {
                                        soundEventListenerActivity.somethingMightHaveChanged();
                                    }
                                });
                snackbar.show();
            }
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
            sound = new Sound(audioModelAndSound.getAudioModel().getAudioLocation(),
                    audioModelAndSound.getAudioModel().getName());
            new AudioFileListFragment.SaveNewSoundTask(requireActivity(), sound).execute();
        } else {
            // Use existing sound
            sound = audioModelAndSound.getSound();
        }

        Log.d(TAG, "Editing sound for audio file " +
                audioModelAndSound.getAudioModel().getAudioLocation());

        TutorialDao.getInstance(requireContext()).check(TutorialDao.Key.AUDIO_FILE_LIST_EDIT);

        Intent intent = AudiofileListSoundEditActivity.newIntent(requireContext(), sound);
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
                    @Nullable String soundIdString =
                            data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID);
                    if (soundIdString != null) {
                        final UUID soundId = UUID.fromString(soundIdString);
                        soundEventListenerActivity.soundChanged(soundId);
                    } else {
                        // Update everything
                        soundEventListenerActivity.somethingMightHaveChanged();
                    }
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

        new FindAudioFilesTask(context, selection, sortOrder).execute();
    }

    @Override
    public void soundChanged(UUID soundId) {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        // The sound NAME may have been changed.
        new FindAudioFilesTask(context, selection, sortOrder).execute();
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new AudioFileListItemAdapter(this);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void setAudioFolderEntries(
            ImmutableList<? extends AbstractAudioFolderEntry> audioFolderEntries) {
        stopPlaying();
        adapter.setAudioFolderEntries(audioFolderEntries);

        if (getUserVisibleHint()) {
            adapter.markAsRightPlaceToShowTutorialHints();
        }
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
        putFolder(outState);
        super.onSaveInstanceState(outState);
    }

    private void putFolder(@NonNull Bundle outState) {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            outState.putString(STATE_FOLDER_TYPE, null);
            outState.putString(STATE_FOLDER_PATH, null);
        } else if (selection instanceof FileSystemFolderAudioLocation) {
            outState.putString(STATE_FOLDER_TYPE, STATE_FOLDER_TYPE_FILE);
            outState.putString(STATE_FOLDER_PATH,
                    ((FileSystemFolderAudioLocation) selection).getPath());
        } else if (selection instanceof AssetFolderAudioLocation) {
            outState.putString(STATE_FOLDER_TYPE, STATE_FOLDER_TYPE_ASSET);
            outState.putString(STATE_FOLDER_PATH,
                    ((AssetFolderAudioLocation) selection).getAssetPath());
        } else {
            throw new IllegalStateException("Unexpected folder type " + selection.getClass());
        }
    }

    /**
     * A background task, used to retrieve audio files (and audio folders)
     * and corresponding sounds from the database.
     */
    class FindAudioFilesTask extends AsyncTask<Void, Void,
            ImmutableList<? extends AbstractAudioFolderEntry>> {
        private final String TAG = FindAudioFilesTask.class.getName();

        private final WeakReference<Context> appContextRef;

        private final IAudioFileSelection selection;

        private final Comparator<AbstractAudioFolderEntry> entryComparator =
                new Comparator<AbstractAudioFolderEntry>() {
                    @Override
                    public int compare(AbstractAudioFolderEntry one,
                                       AbstractAudioFolderEntry other) {
                        if (one instanceof AudioFolder) {
                            // one instanceof AudioFolder
                            if (!(other instanceof AudioFolder)) {
                                return -1;
                            }

                            // one and other instanceof AudioFolder
                            return FOLDER_COMPARATOR
                                    .compare((AudioFolder) one, (AudioFolder) other);
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

        FindAudioFilesTask(Context context, IAudioFileSelection selection,
                           @NonNull SortOrder sortOrder) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.selection = selection;
            this.sortOrder = sortOrder;
        }

        @Override
        @WorkerThread
        protected ImmutableList<? extends AbstractAudioFolderEntry> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading audio files from file system...");

            return loadAudioFolderEntries(appContext);
        }

        /**
         * Retrieves the audio files (and audio folders) according to the
         * selection as well as the corresponding sounds from the database.
         */
        @WorkerThread
        private ImmutableList<AbstractAudioFolderEntry> loadAudioFolderEntries(
                Context appContext) {
            Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> audioModelsAndFolders =
                    new AudioLoader().loadAudioFolderEntriesWithoutSounds(appContext, selection);

            Log.d(TAG, "Audio files loaded.");

            Log.d(TAG, "Loading sounds from database...");

            Map<IAudioFileSelection, Sound> soundMap =
                    SoundDao.getInstance(appContext).findAllByAudioLocation();

            Log.d(TAG, "Sounds loaded.");

            ArrayList<AbstractAudioFolderEntry> res =
                    new ArrayList<>(
                            audioModelsAndFolders.first.size() +
                                    audioModelsAndFolders.second.size());
            res.addAll(audioModelsAndFolders.second);
            for (FullAudioModel audioModel : audioModelsAndFolders.first) {
                res.add(new AudioModelAndSound(
                        audioModel,
                        soundMap.get(audioModel.getAudioLocation())));
            }

            res.sort(entryComparator);

            return ImmutableList.copyOf(res);
        }

        @Override
        @UiThread
        protected void onPostExecute(
                ImmutableList<? extends AbstractAudioFolderEntry> audioFolderEntries) {
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
    static class SaveNewSoundTask extends AsyncTask<Void, Void, Void> {
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

    /**
     * A background task, used to delete the sound
     */
    class DeleteSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = DeleteSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID soundId;

        DeleteSoundTask(Context context, UUID soundId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundId = soundId;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Deleting sound " + soundId);

            SoundDao.getInstance(appContext).delete(soundId);
            return null;
        }

        @Override
        @UiThread
        protected void onPostExecute(Void nothing) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            if (soundEventListenerActivity != null) {
                soundEventListenerActivity.somethingMightHaveChanged();
            }
        }
    }
}
