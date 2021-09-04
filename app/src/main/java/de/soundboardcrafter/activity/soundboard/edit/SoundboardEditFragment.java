package de.soundboardcrafter.activity.soundboard.edit;

import static com.google.common.base.Preconditions.checkNotNull;

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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.AbstractPermissionFragment;
import de.soundboardcrafter.activity.common.audiofile.list.AudioSubfolderRow;
import de.soundboardcrafter.activity.common.audioloader.AudioLoader;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AnywhereInTheFileSystemAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.AudioModelAndSound;
import de.soundboardcrafter.model.audio.FullAudioModel;

/**
 * Activity for editing a single soundboard (name, volume etc.).
 */
public class SoundboardEditFragment extends AbstractPermissionFragment
        implements ServiceConnection {
    private static final String ARG_SOUNDBOARD_ID = "soundboardId";

    private static final String EXTRA_SOUNDBOARD_ID = "soundboardId";
    private static final String EXTRA_EDIT_FRAGMENT = "soundboardEditFragment";

    private static final String STATE_SELECTION = "selection";
    private static final String STATE_AUDIO_SELECTION_CHANGES = "audioSelectionChanges";

    private SoundboardEditView editView;

    private Soundboard soundboard;
    private boolean isNew;
    private IAudioFileSelection selection;
    private AudioSelectionChanges audioSelectionChanges;

    private MediaPlayerService mediaPlayerService;
    @Nullable
    private SoundboardMediaPlayer mediaPlayer;

    static SoundboardEditFragment newInstance(UUID soundboardId) {
        Bundle args = new Bundle();
        args.putString(ARG_SOUNDBOARD_ID, soundboardId.toString());

        SoundboardEditFragment fragment = new SoundboardEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    static SoundboardEditFragment newInstance() {
        return new SoundboardEditFragment();
    }

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        // As soon the media player service is connected, the play/stop icons can be set correctly
        if (editView != null) {
            editView.notifyListDataSetChanged();
        }
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

        // The result will be the soundboard id, so that the calling
        // activity can update its GUI for this Soundboard.

        if (getArguments() != null) {
            UUID soundboardId = UUID.fromString(getArguments().getString(ARG_SOUNDBOARD_ID));
            new FindSoundboardTask(this, soundboardId).execute();
        } else {
            isNew = true;
            soundboard = new Soundboard(getString(R.string.new_soundboard_name), false);
        }

        audioSelectionChanges = new AudioSelectionChanges();

        if (isNew) {
            Intent intent = new Intent(requireActivity(), SoundboardCreateActivity.class);
            requireActivity().setResult(Activity.RESULT_CANCELED, intent);
        } else {
            Intent intent = new Intent(requireActivity(), SoundboardEditActivity.class);
            requireActivity().setResult(Activity.RESULT_OK, intent);
        }

        // TODO Necessary?! Also done in onResume()
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent(requireActivity(), MediaPlayerService.class);
        requireActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard_edit,
                container, false);

        editView = rootView.findViewById(R.id.edit_view);

        if (isNew) {
            editView.setName(soundboard.getName());
            editView.setOnClickListenerSave(
                    () -> {
                        saveNewSoundboard();
                        Intent intent = new Intent(getActivity(), SoundboardCreateActivity.class);
                        intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
                        intent.putExtra(EXTRA_EDIT_FRAGMENT,
                                SoundboardEditFragment.class.getName());
                        requireActivity().setResult(
                                Activity.RESULT_OK,
                                intent);
                        requireActivity().finish();
                    }
            );
            editView.setOnClickListenerCancel(
                    () -> {
                        Intent intent =
                                new Intent(requireActivity(), SoundboardCreateActivity.class);
                        intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
                        intent.putExtra(EXTRA_EDIT_FRAGMENT,
                                SoundboardEditFragment.class.getName());
                        requireActivity().setResult(
                                Activity.RESULT_CANCELED,
                                intent);
                        requireActivity().finish();
                    }
            );
        } else {
            editView.setButtonsInvisible();
        }

        editView.setOnClickListenerSelection(this::toggleSelection);

        if (savedInstanceState != null) {
            setSelection(savedInstanceState.getParcelable(STATE_SELECTION));
            audioSelectionChanges =
                    savedInstanceState.getParcelable(STATE_AUDIO_SELECTION_CHANGES);
        } else {
            setSelection(new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH));
            audioSelectionChanges = new AudioSelectionChanges();
        }

        editView.setOnIconFolderUpClickListener(() -> {
                    if (!(selection instanceof AbstractAudioLocation) || selection.isRoot()) {
                        return;
                    }

                    changeFolder(((AbstractAudioLocation) selection).folderUp());
                }
        );

        editView.setOnListItemClickListener(
                (parent, view, position, id) -> {
                    if (view instanceof SoundboardEditSelectableAudioRow) {
                        SoundboardEditSelectableAudioRow audioFileItemRow =
                                (SoundboardEditSelectableAudioRow) view;
                        onClickAudioFile(audioFileItemRow, position);
                    } else if (view instanceof AudioSubfolderRow) {
                        AudioSubfolderRow audioSubfolderRow = (AudioSubfolderRow) view;
                        onClickAudioSubfolder(audioSubfolderRow);
                    }
                });

        startFindingAudioFilesIfPermitted();

        return rootView;
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
            throw new IllegalStateException("Unexpected selection type: " + selection.getClass());
        }
    }

    private void changeFolder(@NonNull AbstractAudioLocation newFolder) {
        stopPlaying();

        rememberAudioSelectionChanges();

        if (selection instanceof AssetFolderAudioLocation
                || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
            setSelection(newFolder);
            loadAudioFiles();
        } // Otherwise, the fragment will receive an event later.
    }

    private void onClickAudioFile(@NonNull SoundboardEditSelectableAudioRow audioFileItemRow,
                                  int position) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        boolean positionWasPlaying = editView.isPlaying(position);
        stopPlaying();

        if (!positionWasPlaying) {
            AudioModelAndSound audioModelAndSound =
                    (AudioModelAndSound) editView.getAudioFolderEntry(position);
            final AbstractAudioLocation audioLocation =
                    audioModelAndSound.getAudioModel().getAudioLocation();

            if (audioLocation instanceof AssetFolderAudioLocation
                    || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
                editView.setPositionPlaying(position);

                audioFileItemRow.setImage(R.drawable.ic_stop);
                try {
                    mediaPlayer = service.play(audioModelAndSound.getName(),
                            audioLocation,
                            () -> {
                                editView.setPositionPlaying(null);
                                mediaPlayer = null;
                            });
                } catch (IOException e) {
                    editView.setPositionPlaying(null);
                    mediaPlayer = null;
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


    @UiThread
    private void startFindingAudioFilesIfPermitted() {
        clearSelectableAudioFolderEntries();

        if (selection instanceof AssetFolderAudioLocation
                || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
            if (soundboard != null) {
                loadAudioFiles();
            }
        } // Otherwise, we will receive an event later.
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

        if (!readExternalPermissionNecessary
                || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
            stopPlaying();
            rememberAudioSelectionChanges();
            setSelection(newSelection);
            if (soundboard != null) {
                loadAudioFiles();
            }
        } // Otherwise, we will receive an event later.
    }

    @Override
    protected void onPermissionReadExternalStorageGranted() {
        // We don't need any other permissions
        // FIXME Does this work?
        toggleSelection();
    }

    @Override
    protected void onPermissionReadExternalStorageNotGrantedUserGivesUp() {
        stopPlaying();
        rememberAudioSelectionChanges();
        setSelection(new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH));
        if (soundboard != null) {
            loadAudioFiles();
        } else {
            clearSelectableAudioFolderEntries();
        }
    }

    private void rememberAudioSelectionChanges() {
        audioSelectionChanges.addAll(editView.getAudioLocationsSelected());
        audioSelectionChanges.removeAll(editView.getAudioLocationsNotSelected());

        // TODO save result to database later - like in Favorites Edit Fragment
        //  (Save new sounds when necessary)
    }

    public void loadAudioFiles() {
        clearSelectableAudioFolderEntries();

        new FindAudioFilesTask(this, soundboard.getId(), selection,
                audioSelectionChanges).execute();
    }

    @UiThread
    private void updateUI(Soundboard soundboard) {
        this.soundboard = soundboard;
        editView.setName(soundboard.getName());
        loadAudioFiles();
    }

    private void clearSelectableAudioFolderEntries() {
        stopPlaying();
        editView.clearSelectableAudioFolderEntries();
    }

    private void setSelection(IAudioFileSelection selection) {
        this.selection = selection;
        editView.setSelection(selection);
    }

    @Override
    @UiThread
    // Called especially when the edit activity returns.
    public void onResume() {
        super.onResume();

        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        bindService();
    }

    private void saveNewSoundboard() {
        String nameEntered = editView.getName();
        if (!nameEntered.isEmpty()) {
            soundboard.setName(nameEntered);
        }
        new SaveNewSoundboardTask(this, soundboard).execute();
    }

    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();

        stopPlaying();

        requireActivity().unbindService(this);

        rememberAudioSelectionChanges();

        if (!isNew && soundboard != null) {
            String nameEntered = editView.getName();
            if (!nameEntered.isEmpty()) {
                soundboard.setName(nameEntered);
            }

            new UpdateSoundboardTask(this, soundboard).execute();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(STATE_SELECTION, selection);
        outState.putParcelable(STATE_AUDIO_SELECTION_CHANGES, audioSelectionChanges);

        super.onSaveInstanceState(outState);
    }

    /**
     * A background task, used to load the soundboard from the database.
     */
    static class FindSoundboardTask extends AsyncTask<Void, Void, Soundboard> {
        private final String TAG = FindSoundboardTask.class.getName();

        private final WeakReference<SoundboardEditFragment> fragmentRef;
        private final UUID soundboardId;

        FindSoundboardTask(SoundboardEditFragment fragment, UUID soundboardId) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.soundboardId = soundboardId;
        }

        @Override
        @WorkerThread
        protected Soundboard doInBackground(Void... voids) {
            @Nullable SoundboardEditFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading soundboard....");

            Soundboard res =
                    SoundboardDao.getInstance(fragment.requireContext()).find(soundboardId);

            Log.d(TAG, "Soundboard loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(Soundboard soundboard) {
            @Nullable SoundboardEditFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                // fragment (or context) no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            fragment.updateUI(soundboard);
        }
    }


    /**
     * A background task, used to retrieve audio files (and audio folders)
     * and corresponding sounds from the database.
     */
    static class FindAudioFilesTask extends AsyncTask<Void, Void,
            ImmutableList<SelectableModel<AbstractAudioFolderEntry>>> {
        @NonNull
        private final WeakReference<SoundboardEditFragment> fragmentRef;

        private final UUID soundboardId;
        private final IAudioFileSelection selection;

        /**
         * The audios the user has already added to the soundboard - or
         * removed from the soundboard. (These changes have not yet been
         * persisted to the database and must be applied after loading.)
         */
        private final AudioSelectionChanges audioSelectionChanges;

        FindAudioFilesTask(@NonNull SoundboardEditFragment fragment,
                           UUID soundboardId,
                           IAudioFileSelection selection,
                           AudioSelectionChanges audioSelectionChanges) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.soundboardId = soundboardId;
            this.selection = selection;
            this.audioSelectionChanges = audioSelectionChanges;
        }

        @Nullable
        @Override
        @WorkerThread
        protected ImmutableList<SelectableModel<AbstractAudioFolderEntry>> doInBackground(
                Void... voids) {
            @Nullable SoundboardEditFragment fragment = fragmentRef.get();
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
        private ImmutableList<SelectableModel<AbstractAudioFolderEntry>> loadAudioFolderEntries(
                Context appContext) {
            Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> audioModelsAndFolders =
                    new AudioLoader().loadAudioFolderEntriesWithoutSounds(appContext, selection);

            Map<IAudioFileSelection, SelectableModel<Sound>> soundMap =
                    SoundDao.getInstance(appContext).findAllSelectableByAudioLocation(soundboardId);

            return joinAndSort(audioModelsAndFolders, soundMap);
        }

        private ImmutableList<SelectableModel<AbstractAudioFolderEntry>> joinAndSort(
                Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> audioModelsAndFolders,
                Map<IAudioFileSelection, SelectableModel<Sound>> soundMap) {
            ArrayList<SelectableModel<AbstractAudioFolderEntry>> res =
                    new ArrayList<>(audioModelsAndFolders.first.size() +
                            audioModelsAndFolders.second.size());

            res.addAll(SelectableModel.uncheckAll(audioModelsAndFolders.second));

            for (FullAudioModel audioModel : audioModelsAndFolders.first) {
                final SelectableModel<Sound> selectableSound =
                        soundMap.get(audioModel.getAudioLocation());

                if (selectableSound == null) {
                    res.add(new SelectableModel<>(
                            new AudioModelAndSound(audioModel, null),
                            audioSelectionChanges.isAdded(audioModel.getAudioLocation())));
                } else {
                    res.add(new SelectableModel<>(
                            new AudioModelAndSound(audioModel, selectableSound.getModel()),
                            audioSelectionChanges.isAdded(audioModel.getAudioLocation())
                                    || (selectableSound.isSelected()
                                    && !audioSelectionChanges
                                    .isRemoved(audioModel.getAudioLocation()))));
                }
            }

            res.sort(SelectableModel.byModel(
                    AbstractAudioFolderEntry.byTypeAnd(AudioModelAndSound.SortOrder.BY_NAME)));

            return ImmutableList.copyOf(res);
        }

        @Override
        @UiThread
        protected void onPostExecute(
                ImmutableList<SelectableModel<AbstractAudioFolderEntry>> audioFolderEntries) {
            @Nullable SoundboardEditFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                // fragment (or context) no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            // TODO stopPlaying();

            fragment.editView.setSelectionIcon(fragment.requireContext(), fragment.selection);
            fragment.editView.setSelectableAudioFolderEntries(audioFolderEntries);
        }
    }

    /**
     * A background task, used to save the soundboard
     */
    static class SaveNewSoundboardTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundboardTask.class.getName();

        private final WeakReference<SoundboardEditFragment> fragmentRef;
        private final Soundboard soundboard;

        SaveNewSoundboardTask(SoundboardEditFragment fragment, Soundboard soundboard) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.soundboard = soundboard;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            @Nullable SoundboardEditFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving soundboard " + soundboard);
            SoundboardDao.getInstance(fragment.requireContext()).insert(soundboard);

            return null;
        }
    }

    /**
     * A background task, used to save the soundboard
     */
    static class UpdateSoundboardTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundboardTask.class.getName();

        private final WeakReference<SoundboardEditFragment> fragmentRef;
        private final Soundboard soundboard;

        UpdateSoundboardTask(SoundboardEditFragment fragment, Soundboard soundboard) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.soundboard = soundboard;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            @Nullable SoundboardEditFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving soundboard " + soundboard);
            SoundboardDao.getInstance(fragment.requireContext()).update(soundboard);

            return null;
        }
    }
}
