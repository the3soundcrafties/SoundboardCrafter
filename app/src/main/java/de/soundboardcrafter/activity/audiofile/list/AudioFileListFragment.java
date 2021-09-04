package de.soundboardcrafter.activity.audiofile.list;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.soundboardcrafter.activity.common.TutorialUtil.createClickTutorialListener;
import static de.soundboardcrafter.dao.TutorialDao.Key.AUDIO_FILE_LIST_EDIT;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.TutorialUtil;
import de.soundboardcrafter.activity.common.audiofile.list.AudioSubfolderRow;
import de.soundboardcrafter.activity.common.audioloader.AudioLoader;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.activity.sound.edit.audiofile.list.AudiofileListSoundEditActivity;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AnywhereInTheFileSystemAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.AudioModelAndSound;
import de.soundboardcrafter.model.audio.FullAudioModel;

/**
 * Shows Soundboard in a Grid
 */
public class AudioFileListFragment extends Fragment implements
        ServiceConnection,
        AudioFileRow.Callback,
        SoundEventListener {

    private static final String TAG = AudioFileListFragment.class.getName();

    private static final int TAP_TARGET_RADIUS_DP = 33;

    private static final String STATE_SORT_ORDER = "sortOrder";
    private static final String STATE_SELECTION = "selection";

    /**
     * Request code used whenever this activity starts a sound edit
     * fragment
     */
    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    @Nullable
    private MenuItem selectionMenuItem;
    private ListView listView;
    private ConstraintLayout folderLayout;
    private ImageView iconFolderUp;
    private TextView folderPath;
    private AudioFileListItemAdapter adapter;
    private MediaPlayerService mediaPlayerService;
    @Nullable
    private SoundboardMediaPlayer mediaPlayer;

    @Nullable
    private SoundEventListener soundEventListenerActivity;

    private AudioModelAndSound.SortOrder sortOrder;

    private IAudioFileSelection selection;

    /**
     * Creates an <code>AudioFileListFragment</code>.
     */
    @NonNull
    public static AudioFileListFragment newInstance() {
        return new AudioFileListFragment();
    }

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        // As soon the media player service is connected, the play/stop icons can be set correctly
        updateUI();
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
        Intent intent = new Intent(requireActivity(), MediaPlayerService.class);
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
                             @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_audiofile_list,
                container, false);

        folderLayout = rootView.findViewById(R.id.folderLayout);
        iconFolderUp = rootView.findViewById(R.id.icon_folder_up);
        folderPath = rootView.findViewById(R.id.folder_path);
        listView = rootView.findViewById(R.id.list_view_audiofile);

        if (savedInstanceState != null) {
            sortOrder = (AudioModelAndSound.SortOrder) savedInstanceState
                    .getSerializable(STATE_SORT_ORDER);
            setSelection(savedInstanceState.getParcelable(STATE_SELECTION));
        } else {
            sortOrder = AudioModelAndSound.SortOrder.BY_NAME;
            setSelection(new FileSystemFolderAudioLocation("/"));
        }

        selectionMenuItem = null;

        initAudioFileListItemAdapter();

        iconFolderUp.setOnClickListener(v -> {
                    if (!(selection instanceof AbstractAudioLocation) || selection.isRoot()) {
                        return;
                    }

                    changeFolder(((AbstractAudioLocation) selection).folderUp());
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
        startFindingAudioFilesIfPermitted();

        return rootView;
    }

    private void updateSelectionMenuItem() {
        if (selectionMenuItem == null) {
            return;
        }

        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            selectionMenuItem.setTitle(R.string.toolbar_menu_audiofiles_folders_all_on_device);
            selectionMenuItem.setIcon(R.drawable.ic_long_list);
        } else if (selection instanceof FileSystemFolderAudioLocation) {
            selectionMenuItem.setTitle(R.string.toolbar_menu_audiofiles_folders_single);
            selectionMenuItem.setIcon(R.drawable.ic_folder);
        } else if (selection instanceof AssetFolderAudioLocation) {
            selectionMenuItem.setTitle(R.string.toolbar_menu_audiofiles_assets);
            selectionMenuItem.setIcon(R.drawable.ic_included);
        } else {
            throw new IllegalStateException(
                    "Unexpected type of selection: " + selection.getClass());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_audiofile_file, menu);

        selectionMenuItem = menu.findItem(R.id.toolbar_menu_audiofiles_selection);
        updateSelectionMenuItem();
    }

    private void showTutorialHintForEdit() {
        showTutorialHint(
                R.string.tutorial_audio_file_list_edit,
                createClickTutorialListener(() -> {
                    @Nullable View itemView =
                            listView.getChildAt(listView.getFirstVisiblePosition());
                    if (itemView != null) {
                        TutorialDao.getInstance(requireContext()).check(AUDIO_FILE_LIST_EDIT);
                        itemView.performClick();
                    }
                }));
    }

    @UiThread
    private void showTutorialHint(
            int descriptionId, @NonNull TapTargetView.Listener tapTargetViewListener) {
        @Nullable Activity activity = getActivity();

        if (activity != null) {
            TutorialUtil.showTutorialHint(activity, listView, 20,
                    30, TAP_TARGET_RADIUS_DP, true,
                    descriptionId, tapTargetViewListener);
        }
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
        if (id == R.id.toolbar_menu_audiofiles_selection) {
            toggleSelection();
            return true;
        } else if (id == R.id.toolbar_menu_audiofiles_sort_alpha) {
            sort(AudioModelAndSound.SortOrder.BY_NAME);
            return true;
        } else if (id == R.id.toolbar_menu_audiofiles_sort_date) {
            sort(AudioModelAndSound.SortOrder.BY_DATE);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleSelection() {
        final boolean readExternalPermissionNecessary;
        final IAudioFileSelection newSelection;

        if (selection instanceof FileSystemFolderAudioLocation) {
            readExternalPermissionNecessary = true;
            newSelection = AnywhereInTheFileSystemAudioLocation.INSTANCE;
        } else if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            readExternalPermissionNecessary = false;
            newSelection = new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH);
        } else {
            readExternalPermissionNecessary = true;
            newSelection = new FileSystemFolderAudioLocation("/");
        }

        if (!readExternalPermissionNecessary || permissionReadExternalStorageIsGranted()) {
            setSelection(newSelection);
            setAudioFolderEntries(ImmutableList.of());
            loadAudioFiles();
        } // Otherwise, the fragment will receive an event later.
    }

    private void setSelection(IAudioFileSelection selection) {
        this.selection = selection;
        folderPath.setText(this.selection.getDisplayPath());
        setVisibilityFolder(selection instanceof AnywhereInTheFileSystemAudioLocation ? View.GONE :
                View.VISIBLE);
        if (this.selection.isRoot()) {
            iconFolderUp.setVisibility(View.INVISIBLE);
        }
    }

    private void setVisibilityFolder(int visibility) {
        folderLayout.setVisibility(visibility);
        iconFolderUp.setVisibility(visibility);
        folderPath.setVisibility(visibility);
    }

    private void sort(AudioModelAndSound.SortOrder sortOrder) {
        this.sortOrder = sortOrder;

        startFindingAudioFilesIfPermitted();
    }

    private void onClickAudioSubfolder(@NonNull AudioSubfolderRow audioSubfolderRow) {
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

    private void changeFolder(@NonNull String newFolder) {
        checkNotNull(selection, "newFolder");

        if (selection instanceof AbstractAudioLocation) {
            changeFolder(((AbstractAudioLocation) selection).changeFolder(newFolder));
        } else {
            throw new IllegalStateException(
                    "Unexpected selection type: " + selection.getClass());
        }
    }

    private void changeFolder(@NonNull AbstractAudioLocation newFolder) {
        if (selection instanceof AssetFolderAudioLocation
                || permissionReadExternalStorageIsGranted()) {
            setSelection(newFolder);
            loadAudioFiles();
        } // Otherwise, the fragment will receive an event later.
    }

    public void loadAudioFiles() {
        new FindAudioFilesTask(this, selection, sortOrder).execute();
    }

    private void onClickAudioFile(@NonNull AudioFileRow audioFileItemRow, int position) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        boolean positionWasPlaying = adapter.isPlaying(position);
        stopPlaying();

        if (!positionWasPlaying) {
            AudioModelAndSound audioModelAndSound = (AudioModelAndSound) adapter.getItem(position);
            final AbstractAudioLocation audioLocation =
                    audioModelAndSound.getAudioModel().getAudioLocation();

            if (audioLocation instanceof AssetFolderAudioLocation
                    || permissionReadExternalStorageIsGranted()) {
                adapter.setPositionPlaying(position);

                audioFileItemRow.setImage(R.drawable.ic_stop);
                try {
                    mediaPlayer = service.play(audioModelAndSound.getName(),
                            audioLocation,
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
                                            new DeleteSoundTask(this, soundId)
                                                    .execute();
                                        } else if (soundEventListenerActivity != null) {
                                            soundEventListenerActivity.somethingMightHaveChanged();
                                        }
                                    });
                    snackbar.show();
                }
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

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }

    @Override
    @UiThread
    public void onEditAudioFile(@NonNull AudioModelAndSound audioModelAndSound) {
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

        @Nullable final Context context = getContext();
        if (context != null) {
            TutorialDao.getInstance(context).check(AUDIO_FILE_LIST_EDIT);
        }

        Intent intent = AudiofileListSoundEditActivity.newIntent(requireContext(), sound);
        startActivityForResult(intent, EDIT_SOUND_REQUEST_CODE);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
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
                            data != null ?
                                    data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID) : null;
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

        startFindingAudioFilesIfPermitted();
    }

    @Override
    public void soundChanged(UUID soundId) {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        // The sound NAME may have been changed.
        startFindingAudioFilesIfPermitted();
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new AudioFileListItemAdapter(this);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void setAudioFolderEntries(
            List<? extends AbstractAudioFolderEntry> audioFolderEntries) {
        stopPlaying();
        adapter.setAudioFolderEntries(audioFolderEntries);

        @Nullable final Context context = getContext();

        if (context != null) {
            TutorialDao tutorialDao = TutorialDao.getInstance(context);
            if (!tutorialDao.isChecked(AUDIO_FILE_LIST_EDIT)
                    && !adapter.isEmpty()
                    && (adapter.getItem(0) instanceof AudioModelAndSound)) {
                showTutorialHintForEdit();
            }
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

        bindService();
    }

    @UiThread
    private void startFindingAudioFilesIfPermitted() {
        if (selection instanceof AssetFolderAudioLocation
                || permissionReadExternalStorageIsGranted()) {
            loadAudioFiles();
        } // Otherwise, the fragment will receive an event later.
    }

    private boolean permissionReadExternalStorageIsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(STATE_SORT_ORDER, sortOrder);
        outState.putParcelable(STATE_SELECTION, selection);
        super.onSaveInstanceState(outState);
    }

    /**
     * A background task, used to retrieve audio files (and audio folders)
     * and corresponding sounds from the database.
     */
    static class FindAudioFilesTask extends AsyncTask<Void, Void,
            ImmutableList<? extends AbstractAudioFolderEntry>> {
        @NonNull
        private final WeakReference<AudioFileListFragment> fragmentRef;

        private final IAudioFileSelection selection;

        @NonNull
        private final AudioModelAndSound.SortOrder sortOrder;

        FindAudioFilesTask(@NonNull AudioFileListFragment fragment, IAudioFileSelection selection,
                           @NonNull AudioModelAndSound.SortOrder sortOrder) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.selection = selection;
            this.sortOrder = sortOrder;
        }

        @Nullable
        @Override
        @WorkerThread
        protected ImmutableList<? extends AbstractAudioFolderEntry> doInBackground(Void... voids) {
            @Nullable AudioFileListFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                cancel(true);
                return null;
            }

            return loadAudioFolderEntries(fragment.getContext());
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

            Map<IAudioFileSelection, Sound> soundMap =
                    SoundDao.getInstance(appContext).findAllByAudioLocation();

            return joinAndSort(audioModelsAndFolders, soundMap);
        }

        private ImmutableList<AbstractAudioFolderEntry> joinAndSort(
                Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> audioModelsAndFolders,
                Map<IAudioFileSelection, Sound> soundMap) {
            ArrayList<AbstractAudioFolderEntry> res =
                    new ArrayList<>(audioModelsAndFolders.first.size() +
                            audioModelsAndFolders.second.size());
            res.addAll(audioModelsAndFolders.second);
            for (FullAudioModel audioModel : audioModelsAndFolders.first) {
                res.add(new AudioModelAndSound(
                        audioModel,
                        soundMap.get(audioModel.getAudioLocation())));
            }

            res.sort(AbstractAudioFolderEntry.byTypeAnd(sortOrder));

            return ImmutableList.copyOf(res);
        }

        @Override
        @UiThread
        protected void onPostExecute(
                ImmutableList<? extends AbstractAudioFolderEntry> audioFolderEntries) {
            @Nullable AudioFileListFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                // fragment (or context) no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            fragment.updateSelectionMenuItem();
            fragment.setAudioFolderEntries(audioFolderEntries);
        }
    }

    /**
     * A background task, used to save the sound
     */
    static class SaveNewSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundTask.class.getName();

        @NonNull
        private final WeakReference<Context> appContextRef;
        private final Sound sound;

        SaveNewSoundTask(@NonNull Context context, Sound sound) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.sound = sound;
        }

        @Nullable
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
    static class DeleteSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = DeleteSoundTask.class.getName();

        @NonNull
        private final WeakReference<AudioFileListFragment> fragmentRef;
        private final UUID soundId;

        DeleteSoundTask(@NonNull AudioFileListFragment fragment, UUID soundId) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.soundId = soundId;
        }

        @Nullable
        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            AudioFileListFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Deleting sound " + soundId);

            SoundDao.getInstance(fragment.requireContext()).delete(soundId);
            return null;
        }

        @Override
        @UiThread
        protected void onPostExecute(Void nothing) {
            AudioFileListFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            if (fragment.soundEventListenerActivity != null) {
                fragment.soundEventListenerActivity.somethingMightHaveChanged();
            }
        }
    }
}
