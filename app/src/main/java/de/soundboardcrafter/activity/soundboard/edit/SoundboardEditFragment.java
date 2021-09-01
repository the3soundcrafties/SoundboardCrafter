package de.soundboardcrafter.activity.soundboard.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.AbstractPermissionFragment;
import de.soundboardcrafter.activity.common.audioloader.AudioLoader;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.SoundboardDao;
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
public class SoundboardEditFragment extends AbstractPermissionFragment {
    private static final String ARG_SOUNDBOARD_ID = "soundboardId";

    private static final String EXTRA_SOUNDBOARD_ID = "soundboardId";
    private static final String EXTRA_EDIT_FRAGMENT = "soundboardEditFragment";

    private SoundboardEditView editView;

    private Soundboard soundboard;
    private boolean isNew;

    private IAudioFileSelection selection;

    private static final String STATE_FOLDER_TYPE = "folderType";
    private static final String STATE_FOLDER_TYPE_FILE = "file";
    private static final String STATE_FOLDER_TYPE_ASSET = "asset";
    private static final String STATE_FOLDER_PATH = "folderPath";

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
        if (isNew) {
            Intent intent = new Intent(requireActivity(), SoundboardCreateActivity.class);
            requireActivity().setResult(
                    Activity.RESULT_CANCELED,
                    intent);
        } else {
            Intent intent = new Intent(requireActivity(), SoundboardEditActivity.class);
            requireActivity().setResult(
                    Activity.RESULT_OK,
                    intent);
        }
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
            setSelection(getFolder(savedInstanceState));
        } else {
            setSelection(new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH));
        }

        // FIXME iconFolderUp.setOnClickListener(...

        // FIXME listView.setOnItemClickListener(...

        startFindingAudioFilesIfPermitted();

        return rootView;
    }

    @UiThread
    private void startFindingAudioFilesIfPermitted() {
        if (selection instanceof AssetFolderAudioLocation
                || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
            editView.setAudioFolderEntries(ImmutableList.of());
            if (soundboard != null) {
                loadAudioFiles();
            }
        } // Otherwise, we will receive an event later.
    }

    private IAudioFileSelection getFolder(@NonNull Bundle savedInstanceState) {
        @Nullable String type = savedInstanceState.getString(STATE_FOLDER_TYPE);
        @Nullable String path = savedInstanceState.getString(STATE_FOLDER_PATH);
        if (type == null || path == null) {
            return AnywhereInTheFileSystemAudioLocation.INSTANCE;

            // FIXME Does this work even when the user has redrawn the relevant
            //  permission?
        }

        if (type.equals(STATE_FOLDER_TYPE_FILE)) {
            return new FileSystemFolderAudioLocation(path);
        }

        if (type.equals(STATE_FOLDER_TYPE_ASSET)) {
            return new AssetFolderAudioLocation(path);
        }

        throw new IllegalStateException("Unexpected folder type " + type);
    }

    private void updateSelectionMenuItem() {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            editView.setSelectionIcon(requireContext(), R.drawable.ic_long_list);
        } else if (selection instanceof FileSystemFolderAudioLocation) {
            editView.setSelectionIcon(requireContext(), R.drawable.ic_folder);
        } else if (selection instanceof AssetFolderAudioLocation) {
            editView.setSelectionIcon(requireContext(), R.drawable.ic_included);
        } else {
            throw new IllegalStateException(
                    "Unexpected type of selection: " + selection.getClass());
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

        if (!readExternalPermissionNecessary
                || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
            setSelection(newSelection);
            editView.setAudioFolderEntries(ImmutableList.of());
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
        setSelection(new AssetFolderAudioLocation(AudioLoader.ASSET_SOUND_PATH));
        editView.setAudioFolderEntries(ImmutableList.of());
        if (soundboard != null) {
            loadAudioFiles();
        }
    }

    public void loadAudioFiles() {
        new FindAudioFilesTask(this, soundboard.getId(), selection).execute();
    }

    private void setSelection(IAudioFileSelection selection) {
        this.selection = selection;
        editView.setSelection(selection);
    }

    @UiThread
    private void updateUI(Soundboard soundboard) {
        this.soundboard = soundboard;
        editView.setName(soundboard.getName());
        loadAudioFiles();
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
                    ((FileSystemFolderAudioLocation) selection).getInternalPath());
        } else if (selection instanceof AssetFolderAudioLocation) {
            outState.putString(STATE_FOLDER_TYPE, STATE_FOLDER_TYPE_ASSET);
            outState.putString(STATE_FOLDER_PATH,
                    ((AssetFolderAudioLocation) selection).getInternalPath());
        } else {
            throw new IllegalStateException("Unexpected folder type " + selection.getClass());
        }
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

        FindAudioFilesTask(@NonNull SoundboardEditFragment fragment,
                           UUID soundboardId,
                           IAudioFileSelection selection) {
            super();
            fragmentRef = new WeakReference<>(fragment);
            this.soundboardId = soundboardId;
            this.selection = selection;
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

            // FIXME add added-and-not-yet-saved
            // FIXME delete deleted-and-not-yet-saved

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
                    // FIXME Selected Audio Models without sound?!
                    res.add(new SelectableModel<>(
                            new AudioModelAndSound(audioModel, null), false));
                } else {
                    res.add(new SelectableModel<>(
                            new AudioModelAndSound(audioModel, selectableSound.getModel()),
                            selectableSound.isSelected()));
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
            fragment.updateSelectionMenuItem();
            fragment.editView.setAudioFolderEntries(audioFolderEntries);
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
