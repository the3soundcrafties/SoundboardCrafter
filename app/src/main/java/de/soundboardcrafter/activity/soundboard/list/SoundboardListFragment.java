package de.soundboardcrafter.activity.soundboard.list;

import static android.content.Context.MODE_PRIVATE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static de.soundboardcrafter.activity.common.TutorialUtil.createLongClickTutorialListener;
import static de.soundboardcrafter.activity.common.ViewUtil.dpToPx;
import static de.soundboardcrafter.dao.TutorialDao.Key.SOUNDBOARD_LIST_CUSTOM_SOUNDBOARD_CONTEXT_MENU;
import static de.soundboardcrafter.dao.TutorialDao.Key.SOUNDBOARD_LIST_SOUNDBOARD_CONTEXT_MENU;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.TutorialUtil;
import de.soundboardcrafter.activity.common.audioloader.AssetsAudioLoader;
import de.soundboardcrafter.activity.common.audioloader.AudioLoader;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardCreateActivity;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardEditOrCopyActivity;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.dao.DBHelper;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;
import de.soundboardcrafter.model.audio.BasicAudioModel;

/**
 * Shows Soundboards in a list
 */
public class SoundboardListFragment extends Fragment
        implements SoundEventListener {
    private static final String TAG = SoundboardListFragment.class.getName();

    private static final int TAP_TARGET_RADIUS_DP = 44;

    private static final int FIRST_ITEM_X_DP = 25;
    private static final int FIRST_ITEM_Y_DP = 25;

    private static final int UNIQUE_TAB_ID = 2;

    private static final String EXTRA_SOUNDBOARD_ID = "SoundboardId";

    /**
     * Request code used whenever the soundboard playing view
     * is started from this activity
     */
    private static final int SOUNDBOARD_PLAY_REQUEST_CODE = 1;

    private static final int NEW_SOUNDBOARD_REQUEST_CODE = 25;
    private static final int EDIT_SOUNDBOARD_REQUEST_CODE = 26;

    private static final int CONTEXT_MENU_EDIT_ITEM_ID = 1;
    private static final int CONTEXT_MENU_COPY_ITEM_ID = 2;
    private static final int CONTEXT_MENU_DELETE_ITEM_ID = 3;

    private @Nullable
    SoundEventListener soundEventListenerActivity;

    private ListView listView;
    private View loadingFooterView;
    private ProgressBar loadingProgressBar;
    private SoundboardListItemAdapter adapter;

    /**
     * Creates a <code>SoundboardListFragment</code>.
     */
    public static SoundboardListFragment createFragment() {
        return new SoundboardListFragment();
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard_list,
                container, false);

        buildAddButton(rootView);

        buildListView(inflater, rootView);

        somethingMightHaveChanged();

        return rootView;
    }

    private void buildAddButton(View rootView) {
        Button addNewSoundboard = rootView.findViewById(R.id.new_soundboard);
        addNewSoundboard.setOnClickListener(e ->
                startActivityForResult(
                        SoundboardCreateActivity.newIntent(
                                getContext()), NEW_SOUNDBOARD_REQUEST_CODE));
    }

    private void buildListView(@NonNull LayoutInflater inflater, View rootView) {
        listView = rootView.findViewById(R.id.list_view_soundboard);
        loadingFooterView =
                inflater.inflate(R.layout.fragment_soundboard_list_loading, listView, false);
        loadingProgressBar = loadingFooterView.findViewById(R.id.footerProgressBar);

        initSoundboardItemAdapter();
        registerForContextMenu(listView);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter == null || adapter.isEmpty()) {
                return;
            }

            SoundboardWithSounds soundboard = adapter.getItem(position);

            Intent intent = new Intent(getContext(), SoundboardPlayActivity.class);
            intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());

            startActivityForResult(intent, SOUNDBOARD_PLAY_REQUEST_CODE);
        });
    }

    @Override
    @UiThread
    public void onResume() {
        super.onResume();

        showTutorialHintIfNecessary();
    }

    private void showTutorialHintIfNecessary() {
        final TutorialDao tutorialDao = TutorialDao.getInstance(requireContext());

        if (!tutorialDao.isChecked(SOUNDBOARD_LIST_SOUNDBOARD_CONTEXT_MENU)
                && adapter != null && !adapter.isEmpty()) {
            showContextMenuTutorialHint(
                    R.string.tutorial_soundboard_list_context_menu_description,
                    SOUNDBOARD_LIST_SOUNDBOARD_CONTEXT_MENU);
        } else if (!tutorialDao.isChecked(SOUNDBOARD_LIST_CUSTOM_SOUNDBOARD_CONTEXT_MENU)
                && adapter != null && !adapter.isEmpty()
                && !adapter.areAllSoundboardsProvided()) {
            showContextMenuTutorialHint(
                    R.string.tutorial_soundboard_list_custom_context_menu_description,
                    SOUNDBOARD_LIST_CUSTOM_SOUNDBOARD_CONTEXT_MENU);
        }
    }

    private void showContextMenuTutorialHint(@StringRes final int descriptionId,
                                             final TutorialDao.Key tutorialKey) {
        showListViewFirstItemTutorialHint(
                descriptionId,
                createLongClickTutorialListener(
                        () -> {
                            @Nullable View itemView =
                                    listView.getChildAt(listView.getFirstVisiblePosition());
                            if (itemView != null) {
                                TutorialDao.getInstance(requireContext())
                                        .check(tutorialKey);
                                itemView.performLongClick(dpToPx(requireContext(), FIRST_ITEM_X_DP),
                                        dpToPx(requireContext(), FIRST_ITEM_Y_DP));
                            }
                        }));
    }

    @UiThread
    private void showListViewFirstItemTutorialHint(
            @StringRes int descriptionId, TapTargetView.Listener tapTargetViewListener) {
        @Nullable Activity activity = getActivity();

        if (activity != null) {
            TutorialUtil.showTutorialHint(activity,
                    listView, 50, 33, TAP_TARGET_RADIUS_DP,
                    false, descriptionId,
                    tapTargetViewListener);
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
    @UiThread
    public void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        SoundboardListItemRow itemRow = (SoundboardListItemRow) adapterContextMenuInfo.targetView;

        final Soundboard soundboard = requireNonNull(itemRow.getSoundboard());

        int order = 0;
        if (!soundboard.isProvided()) {
            menu.add(UNIQUE_TAB_ID,
                    CONTEXT_MENU_EDIT_ITEM_ID,
                    order++,
                    R.string.context_menu_edit_soundboard);
        }

        menu.add(UNIQUE_TAB_ID,
                CONTEXT_MENU_COPY_ITEM_ID,
                order++,
                R.string.context_menu_copy_soundboard);

        if (!soundboard.isProvided()) {
            menu.add(UNIQUE_TAB_ID,
                    CONTEXT_MENU_DELETE_ITEM_ID,
                    order,
                    R.string.context_menu_remove_soundboard);
        }

        menu.setHeaderTitle(soundboard.getDisplayName());

        checkTutorialDaoForContextMenu(soundboard);
    }

    private void checkTutorialDaoForContextMenu(Soundboard soundboard) {
        @Nullable final Context context = getContext();
        if (context == null) {
            return;
        }

        final TutorialDao tutorialDao = TutorialDao.getInstance(context);

        tutorialDao.check(TutorialDao.Key.SOUNDBOARD_LIST_SOUNDBOARD_CONTEXT_MENU);

        if (!soundboard.isProvided()) {
            tutorialDao.check(TutorialDao.Key.SOUNDBOARD_LIST_CUSTOM_SOUNDBOARD_CONTEXT_MENU);
        }
    }

    @Override
    @UiThread
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        if (item.getGroupId() != UNIQUE_TAB_ID) {
            // The wrong fragment got the event.
            // See https://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager
            // -receives-oncontextitemselected-call
            return false; // Pass the event to the next fragment
        }
        AdapterView.AdapterContextMenuInfo menuInfo =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        SoundboardListItemRow itemRow = (SoundboardListItemRow) menuInfo.targetView;
        SoundboardWithSounds soundboardWithSounds =
                requireNonNull(itemRow.getSoundboardWithSounds());
        final int id = item.getItemId();
        if (id == CONTEXT_MENU_EDIT_ITEM_ID) {
            Intent intent = SoundboardEditOrCopyActivity
                    .newIntent(getActivity(), soundboardWithSounds.getSoundboard(), false);
            startActivityForResult(intent, EDIT_SOUNDBOARD_REQUEST_CODE);
            return true;
        } else if (id == CONTEXT_MENU_COPY_ITEM_ID) {
            Intent intent = SoundboardEditOrCopyActivity
                    .newIntent(getActivity(), soundboardWithSounds.getSoundboard(), true);
            startActivityForResult(intent, NEW_SOUNDBOARD_REQUEST_CODE);
            return true;
        } else if (id == CONTEXT_MENU_DELETE_ITEM_ID) {
            new DeleteSoundboardTask(requireActivity(), soundboardWithSounds).execute();
            adapter.remove(soundboardWithSounds);
            fireSomethingMightHaveChanged();
            return true;
        } else {
            return false;
        }
    }

    private void fireSomethingMightHaveChanged() {
        if (soundEventListenerActivity != null) {
            soundEventListenerActivity.somethingMightHaveChanged();
        }
    }

    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case SOUNDBOARD_PLAY_REQUEST_CODE:
                fireSomethingMightHaveChanged();
                break;
            case NEW_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "created new soundboard " + this);
                new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
                break;
            case EDIT_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "updated soundboard " + this);
                new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
                break;
        }
    }

    @Override
    public void somethingMightHaveChanged() {
        @Nullable Context context = getContext();
        if (context == null || getActivity() == null) {
            return;
        }

        if (noSoundboards(requireActivity())
                || providedSoundboardsNeedToBeUpdated(requireActivity())) {
            setLoadingProgress(0);
            listView.addFooterView(loadingFooterView);
        } else {
            listView.removeFooterView(loadingFooterView);
        }

        new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
    }

    @Override
    public void soundChanged(UUID soundId) {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
    }

    @UiThread
    private void initSoundboardItemAdapter() {
        adapter = new SoundboardListItemAdapter();
        listView.setAdapter(adapter);
        updateUI();
    }

    private void setLoadingProgress(int percent) {
        loadingProgressBar.setProgress(percent);
    }

    @UiThread
    private void setSoundboards(ImmutableList<SoundboardWithSounds> soundboards) {
        loadingProgressBar.setProgress(100);
        listView.removeFooterView(loadingFooterView);
        List<SoundboardWithSounds> list = Lists.newArrayList(soundboards);
        list.sort(SoundboardWithSounds.PROVIDED_LAST_THEN_BY_COLLATION_KEY);
        adapter.setSoundboards(list);
    }

    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static boolean noSoundboards(Context context) {
        return !SoundboardDao.getInstance(context).areAny();
    }

    private boolean providedSoundboardsNeedToBeUpdated(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(
                DBHelper.DB_SHARED_PREFERENCES, MODE_PRIVATE);
        return sharedPreferences.getBoolean(DBHelper.PREF_KEY_CHECK_SOUNDBOARDS, true);
    }

    /**
     * A background task, used to delete the soundboards with the given indexes from the soundboard
     */
    static class DeleteSoundboardTask extends AsyncTask<Integer, Void, Void> {
        private final WeakReference<Context> appContextRef;
        private final UUID soundboardId;

        DeleteSoundboardTask(Context context, SoundboardWithSounds soundboard) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            soundboardId = soundboard.getId();
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
            soundboardDao.delete(soundboardId);
            return null;
        }
    }

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    class FindSoundboardsTask
            extends AsyncTask<Void, Integer, ImmutableList<SoundboardWithSounds>> {
        private final String TAG = SoundboardListFragment.FindSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindSoundboardsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<SoundboardWithSounds> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            updateSoundboardsIfNecessary(appContext);

            Log.d(TAG, "Loading soundboards...");

            ImmutableList<SoundboardWithSounds> res =
                    SoundboardDao.getInstance(appContext).findAllWithSounds();

            Log.d(TAG, "Soundboards loaded.");

            return res;
        }

        /**
         * If necessary, generates the provided soundboards (from the assets) or updates
         * the existing provided soundboards and sounds (based on the asset sounds).
         */
        private void updateSoundboardsIfNecessary(Context appContext) {
            if (noSoundboards(appContext)) {
                generateProvidedSoundboards(appContext);
            } else if (providedSoundboardsNeedToBeUpdated(appContext)) {
                updateProvidedSoundboardsAndSounds(appContext);
            }

            DBHelper.setProvidedSoundboardsNeedToBeChecked(appContext, false);
        }

        /**
         * Generates the provided soundboards from the assets.
         */
        private void generateProvidedSoundboards(Context appContext) {
            Log.d(TAG, "Generating soundboards from included audio files...");
            publishProgress(10);

            // We must be sure not to insert the same sounds into the database again - so we
            // remove all sounds before inserting the new soundboards.
            SoundDao.getInstance(appContext).deleteAllSounds();

            publishProgress(20);

            AudioLoader audioLoader = new AudioLoader();
            Map<String, List<BasicAudioModel>> audioModelsByTopFolderName =
                    audioLoader.getAllAudiosFromAssetsByTopFolderName(appContext);

            int numSoundboards = audioModelsByTopFolderName.size();
            int i = 0;

            for (Map.Entry<String, List<BasicAudioModel>> entry :
                    audioModelsByTopFolderName.entrySet()) {
                generateProvidedSoundboard(appContext, entry.getKey(), entry.getValue());
                publishProgress(20 + 70 * i / numSoundboards);

                i++;
            }

            publishProgress(90);
            Log.d(TAG, "Soundboards generated.");
        }

        /**
         * Generates a provided soundboard.
         */
        private void generateProvidedSoundboard(Context appContext, String name,
                                                List<BasicAudioModel> audioModels) {
            Soundboard soundboard = new Soundboard(name, true);

            ImmutableList.Builder<Sound> sounds = ImmutableList.builder();
            for (BasicAudioModel audioModel : audioModels) {
                sounds.add(toSound(audioModel));
            }

            SoundboardWithSounds soundboardWithSounds =
                    new SoundboardWithSounds(soundboard, sounds.build());

            SoundboardDao.getInstance(appContext)
                    .insertSoundboardAndInsertAllSounds(soundboardWithSounds);
        }

        /**
         * Migrates (updates, complements, deletes) the existing provided soundboards and sounds,
         * based on the current assets.
         */
        private void updateProvidedSoundboardsAndSounds(Context appContext) {
            Log.d(TAG, "Updating soundboards from included audio files...");
            publishProgress(10);

            AudioLoader audioLoader = new AudioLoader();
            Map<String, List<BasicAudioModel>> audioModelsByTopFolderName =
                    audioLoader.getAllAudiosFromAssetsByTopFolderName(appContext);

            final ImmutableList<Soundboard> oldSoundboards =
                    SoundboardDao.getInstance(appContext).findAllProvided();

            final ImmutableSet.Builder<String> newSoundboardNames =
                    updateNewSoundboards(appContext, audioModelsByTopFolderName);

            deleteObsoleteSoundboards(appContext, oldSoundboards, newSoundboardNames);

            updateProvidedSounds(appContext);

            publishProgress(90);
            Log.d(TAG, "Soundboards updated.");
        }

        /**
         * Updates or complements the existing provided soundboards,
         * based on the current assets. Does not do deletions.
         *
         * @return The names of the soundboards according to the assets.
         */
        @NonNull
        private ImmutableSet.Builder<String> updateNewSoundboards(
                Context appContext,
                Map<String, List<BasicAudioModel>> audioModelsByTopFolderName) {
            final ImmutableSet.Builder<String> newSoundboardNames = ImmutableSet.builder();

            int numNewSoundboards = audioModelsByTopFolderName.size();
            int newSoundboardCount = 0;

            for (Map.Entry<String, List<BasicAudioModel>> entry :
                    audioModelsByTopFolderName.entrySet()) {
                updateProvidedSoundboard(appContext, entry.getKey(), entry.getValue());

                newSoundboardNames.add(entry.getKey());

                publishProgress(10 + 30 * newSoundboardCount / numNewSoundboards);

                newSoundboardCount++;
            }
            return newSoundboardNames;
        }

        /**
         * Deletes the old provided Soundboards whenever they are not part of the new provided
         * soundboards.
         */
        private void deleteObsoleteSoundboards(Context appContext,
                                               ImmutableList<Soundboard> oldSoundboards,
                                               ImmutableSet.Builder<String> newSoundboardNames) {
            Set<String> oldSoundboardNames =
                    oldSoundboards.stream().map(Soundboard::getFullName)
                            .collect(toSet());

            final Sets.SetView<String> removedSoundboardNames = Sets
                    .difference(oldSoundboardNames, newSoundboardNames.build());

            int numRemovedSoundboards = removedSoundboardNames.size();
            int removedSoundboardCount = 0;

            for (String removedSoundboardName : removedSoundboardNames) {
                deleteProvidedSoundboard(appContext, removedSoundboardName);

                removedSoundboardCount++;

                publishProgress(40 + 30 * removedSoundboardCount / numRemovedSoundboards);
            }
        }

        /**
         * Updates or deletes sounds, based on the current assets:
         * <ul>
         * <li>If a sound is no longer part of the assets, the sound will be deleted.</li>
         * <li>If a sound is still part of the assets and still has its international (english)
         * name,
         * and now we provide a localized name for the sound, update the name to the localized
         * name.</li>
         * </ul>
         */
        private void updateProvidedSounds(Context appContext) {
            ImmutableMap<String, String> allAssetsAudioNamesByPath =
                    new AssetsAudioLoader().getAllAudioNamesByPath(appContext);

            SoundDao soundDao = SoundDao.getInstance(appContext);

            final ImmutableList<Sound> providedSounds = soundDao.findAllProvided();

            int numProvidedSounds = providedSounds.size();
            int providedSoundsCount = 0;

            for (Sound sound : providedSounds) {
                final String path = sound.getAudioLocation().getInternalPath();
                @Nullable String audioName = allAssetsAudioNamesByPath.get(path);

                if (audioName == null) {
                    // Audio file has been removed from the assets.
                    soundDao.delete(sound.getId());
                } else if (sound.getName().equals(
                        AssetsAudioLoader.pathOrFileNameToInternationalName(path))
                        && !sound.getName().equals(audioName)) {
                    // Sound name has not been changed manually, but now
                    // we have provided a translated name - use ths translated name.
                    sound.setName(audioName);
                    soundDao.update(sound);
                }

                providedSoundsCount++;

                publishProgress(70 + 20 * providedSoundsCount / numProvidedSounds);
            }
        }

        /**
         * Saves or updates this soundboard with these audio files.
         */
        private void updateProvidedSoundboard(Context appContext, String name,
                                              List<BasicAudioModel> audioModels) {
            SoundboardDao.getInstance(appContext)
                    .updateProvidedSoundboardWithAudios(name, audioModels);
        }

        /**
         * Deletes this provided soundboard.
         */
        private void deleteProvidedSoundboard(Context appContext, String name) {
            SoundboardDao.getInstance(appContext).deleteProvidedSoundboard(name);
        }

        private Sound toSound(BasicAudioModel audioModel) {
            return new Sound(audioModel.getAudioLocation(), audioModel.getName());
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that progress
                // will be of no use to anyone.
                // (Anyway - there is no reason to cancel preparing the soundboards.)
                return;
            }

            setLoadingProgress(values[0]);
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<SoundboardWithSounds> soundboards) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            setSoundboards(soundboards);
        }
    }
}
