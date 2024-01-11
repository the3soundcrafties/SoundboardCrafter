package de.soundboardcrafter.activity.favorites.list;

import static de.soundboardcrafter.activity.common.TutorialUtil.createLongClickTutorialListener;
import static de.soundboardcrafter.activity.common.ViewUtil.dpToPx;
import static de.soundboardcrafter.dao.TutorialDao.Key.FAVORITES_LIST_CONTEXT_MENU;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.TutorialUtil;
import de.soundboardcrafter.activity.favorites.edit.FavoritesCreateActivity;
import de.soundboardcrafter.activity.favorites.edit.FavoritesEditActivity;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.dao.FavoritesDao;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.FavoritesWithSoundboards;

/**
 * Shows favorites in a Grid
 */
public class FavoritesListFragment extends Fragment
        implements SoundEventListener {
    private static final String TAG = FavoritesListFragment.class.getName();

    private static final int TAP_TARGET_RADIUS_DP = 44;

    private static final int FIRST_ITEM_X_DP = 25;
    private static final int FIRST_ITEM_Y_DP = 25;

    private static final int UNIQUE_TAB_ID = 1;

    /**
     * Request code used whenever the soundboard playing view
     * is started from this activity
     */
    private static final int SOUNDBOARD_PLAY_REQUEST_CODE = 1;

    private static final int CREATE_SOUNDBOARD_REQUEST_CODE = 27;
    private static final int EDIT_FAVORITES_REQUEST_CODE = 26;

    private @Nullable
    SoundEventListener soundEventListenerActivity;

    private ListView listView;
    private FavoritesListItemAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new FindFavoritesTask(requireContext()).execute();
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorites_list,
                container, false);
        listView = rootView.findViewById(R.id.list_view_favorites);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter == null) {
                return;
            }

            onClickFavorites(adapter.getItem(position));
        });

        Button addNewFavorites = rootView.findViewById(R.id.new_favorites);
        addNewFavorites.setOnClickListener(e ->
                startActivityForResult(
                        FavoritesCreateActivity.newIntent(getContext()),
                        CREATE_SOUNDBOARD_REQUEST_CODE));
        registerForContextMenu(listView);

        return rootView;
    }

    @Override
    @UiThread
    public void onResume() {
        super.onResume();

        showTutorialHintIfNecessary();
    }

    private void showTutorialHintIfNecessary() {
        final TutorialDao tutorialDao = TutorialDao.getInstance(requireContext());
        if (!tutorialDao.isChecked(FAVORITES_LIST_CONTEXT_MENU)
                && adapter != null && !adapter.isEmpty()) {
            showTutorialHint();
        }
    }

    private void showTutorialHint() {
        showTutorialHint(
                R.string.tutorial_favorites_list_context_menu_description,
                createLongClickTutorialListener(
                        () -> {
                            @Nullable View itemView =
                                    listView.getChildAt(listView.getFirstVisiblePosition());
                            if (itemView != null) {
                                TutorialDao.getInstance(requireContext())
                                        .check(FAVORITES_LIST_CONTEXT_MENU);
                                itemView.performLongClick(dpToPx(requireContext(), FIRST_ITEM_X_DP),
                                        dpToPx(requireContext(), FIRST_ITEM_Y_DP));
                            }
                        }));
    }

    @UiThread
    private void showTutorialHint(
            int descriptionId, TapTargetView.Listener tapTargetViewListener) {
        TutorialUtil.showTutorialHint(getActivity(), listView, 50, 33,
                TAP_TARGET_RADIUS_DP, false, descriptionId,
                tapTargetViewListener);
    }

    private void onClickFavorites(FavoritesWithSoundboards favoritesWithSoundboards) {
        Intent intent = new Intent(getContext(), SoundboardPlayActivity.class);
        intent.putExtra(
                SoundboardPlayActivity.EXTRA_FAVORITES_ID,
                favoritesWithSoundboards.getFavorites().getId().toString());

        startActivityForResult(intent, SOUNDBOARD_PLAY_REQUEST_CODE);
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

        menu.add(UNIQUE_TAB_ID,
                1,
                0,
                R.string.context_menu_edit_favorites);
        menu.add(UNIQUE_TAB_ID,
                2,
                1,
                R.string.context_menu_delete_favorites);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        FavoritesListItemRow itemRow = (FavoritesListItemRow) adapterContextMenuInfo.targetView;

        menu.setHeaderTitle(itemRow.getFavoritesWithSoundboards().getFavorites().getName());

        @Nullable final Context context = getContext();
        if (context != null) {
            TutorialDao.getInstance(context).check(TutorialDao.Key.FAVORITES_LIST_CONTEXT_MENU);
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
        FavoritesListItemRow itemRow = (FavoritesListItemRow) menuInfo.targetView;
        FavoritesWithSoundboards favoritesWithSoundboards = itemRow.getFavoritesWithSoundboards();
        final int id = item.getItemId();
        if (id == 1) {
            Intent intent = FavoritesEditActivity
                    .newIntent(requireActivity(), favoritesWithSoundboards.getFavorites());
            startActivityForResult(intent, EDIT_FAVORITES_REQUEST_CODE);
            return true;
        } else if (id == 2) {
            new DeleteFavoritesTask(requireActivity(), favoritesWithSoundboards).execute();
            adapter.remove(favoritesWithSoundboards);
            return true;
        } else {
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
            case SOUNDBOARD_PLAY_REQUEST_CODE:
                fireSomethingMightHaveChanged();
                break;
            case CREATE_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "created new favorites " + this);
                new FindFavoritesTask(requireContext()).execute();
                break;
            case EDIT_FAVORITES_REQUEST_CODE:
                Log.d(TAG, "Editing favorites " + this
                        + ": Returned from favorites edit fragment with OK");
                new FindFavoritesTask(requireContext()).execute();
                break;
        }
    }

    private void fireSomethingMightHaveChanged() {
        if (soundEventListenerActivity != null) {
            soundEventListenerActivity.somethingMightHaveChanged();
        }
    }

    @Override
    public void somethingMightHaveChanged() {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        new FindFavoritesTask(context).execute();
    }

    @Override
    public void soundChanged(UUID soundId) {
        // This does not make a difference for the favorites
    }

    @UiThread
    private void initFavoritesItemAdapter(ImmutableList<FavoritesWithSoundboards> favorites) {
        List<FavoritesWithSoundboards> list = Lists.newArrayList(favorites);
        list.sort(Comparator.comparing(g -> g.getFavorites().getCollationKey()));
        adapter = new FavoritesListItemAdapter(list);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    static class DeleteFavoritesTask extends AsyncTask<Integer, Void, Void> {
        private final WeakReference<Context> appContextRef;
        private final UUID favoritesId;

        DeleteFavoritesTask(Context context, FavoritesWithSoundboards favoritesWithSoundboards) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            favoritesId = favoritesWithSoundboards.getFavorites().getId();
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Integer... indexes) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            FavoritesDao favoritesDao = FavoritesDao.getInstance(appContext);
            favoritesDao.delete(favoritesId);
            return null;
        }

    }


    /**
     * A background task, used to retrieve favorites from the database.
     */
    class FindFavoritesTask extends AsyncTask<Void, Void, ImmutableList<FavoritesWithSoundboards>> {
        private final String TAG = FindFavoritesTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindFavoritesTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<FavoritesWithSoundboards> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            FavoritesDao favoritesDao = FavoritesDao.getInstance(appContext);

            Log.d(TAG, "Loading favorites...");

            ImmutableList<FavoritesWithSoundboards> res =
                    favoritesDao.findAllFavoritesWithSoundboards();

            Log.d(TAG, "Favorites loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(
                ImmutableList<FavoritesWithSoundboards> favoritesWithSoundboards) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            initFavoritesItemAdapter(favoritesWithSoundboards);
        }
    }
}
