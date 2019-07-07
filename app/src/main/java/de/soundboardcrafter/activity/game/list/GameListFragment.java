package de.soundboardcrafter.activity.game.list;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.game.edit.GameCreateActivity;
import de.soundboardcrafter.activity.game.edit.GameEditActivity;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.dao.GameDao;
import de.soundboardcrafter.model.GameWithSoundboards;

/**
 * Shows Games in a Grid
 */
public class GameListFragment extends Fragment
        implements SoundEventListener {
    private static final String TAG = GameListFragment.class.getName();

    /**
     * Request code used whenever the soundboard playing view
     * is started from this activity
     */
    private static final int SOUNDBOARD_PLAY_REQUEST_CODE = 1;

    private static final int CREATE_SOUNDBOARD_REQUEST_CODE = 27;
    private static final int EDIT_GAME_REQUEST_CODE = 26;

    private @Nullable
    SoundEventListener soundEventListenerActivity;

    private ListView listView;
    private Button addNewGame;
    private GameListItemAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            new GameListFragment.FindGamesTask(requireContext()).execute();
        } // otherwise we will receive an event later
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game_list,
                container, false);
        listView = rootView.findViewById(R.id.list_view_games);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter == null) {
                return;
            }

            onClickGame(adapter.getItem(position));
        });

        addNewGame = rootView.findViewById(R.id.new_game);
        addNewGame.setOnClickListener(e ->
                startActivityForResult(
                        GameCreateActivity.newIntent(getContext()),
                        CREATE_SOUNDBOARD_REQUEST_CODE));
        registerForContextMenu(listView);

        return rootView;
    }

    private void onClickGame(GameWithSoundboards gameWithSoundboards) {
        Intent intent = new Intent(getContext(), SoundboardPlayActivity.class);
        intent.putExtra(
                SoundboardPlayActivity.EXTRA_GAME_ID,
                gameWithSoundboards.getGame().getId().toString());

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
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_game_list_context, menu);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        GameListItemRow itemRow = (GameListItemRow) adapterContextMenuInfo.targetView;

        menu.setHeaderTitle(itemRow.getGameWithSoundboards().getGame().getName());
    }

    @Override
    @UiThread
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        if (!getUserVisibleHint()) {
            // The wrong fragment got the event.
            // See https://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager-receives-oncontextitemselected-call
            return false; // Pass the event to the next fragment
        }
        AdapterView.AdapterContextMenuInfo menuInfo =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        GameListItemRow itemRow = (GameListItemRow) menuInfo.targetView;
        GameWithSoundboards gameWithSoundboards = itemRow.getGameWithSoundboards();
        switch (item.getItemId()) {
            case R.id.context_menu_edit_game:
                Intent intent = GameEditActivity.newIntent(requireActivity(), gameWithSoundboards.getGame());
                startActivityForResult(intent, EDIT_GAME_REQUEST_CODE);
                return true;
            case R.id.context_menu_remove_game:
                new RemoveGameTask(requireActivity(), gameWithSoundboards).execute();
                adapter.remove(gameWithSoundboards);
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
            case SOUNDBOARD_PLAY_REQUEST_CODE:
                if (soundEventListenerActivity != null) {
                    soundEventListenerActivity.somethingMightHaveChanged();
                }
                break;
            case CREATE_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "created new game " + this);
                new FindGamesTask(requireContext()).execute();
                break;
            case EDIT_GAME_REQUEST_CODE:
                Log.d(TAG, "Editing game " + this + ": Returned from game edit fragment with OK");
                new FindGamesTask(requireContext()).execute();
                break;
        }
    }

    @Override
    public void somethingMightHaveChanged() {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        new GameListFragment.FindGamesTask(context).execute();
    }

    @Override
    public void soundChanged(UUID soundId) {
        // This does not make a difference for the list of games
    }

    @UiThread
    private void initGameItemAdapter(ImmutableList<GameWithSoundboards> games) {
        List<GameWithSoundboards> list = Lists.newArrayList(games);
        list.sort((g1, g2) -> g1.getGame().getName().compareTo(g2.getGame().getName()));
        adapter = new GameListItemAdapter(list);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * A background task, used to remove soundboard with the given indexes from the soundboard
     */
    class RemoveGameTask extends AsyncTask<Integer, Void, Void> {
        private final String TAG = RemoveGameTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID gameId;

        RemoveGameTask(Context context, GameWithSoundboards gameWithSoundboards) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            gameId = gameWithSoundboards.getGame().getId();
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Integer... indexes) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            GameDao gameDao = GameDao.getInstance(appContext);
            gameDao.remove(gameId);
            return null;
        }

    }


    /**
     * A background task, used to retrieve games from the database.
     */
    class FindGamesTask extends AsyncTask<Void, Void, ImmutableList<GameWithSoundboards>> {
        private final String TAG = GameListFragment.FindGamesTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindGamesTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<GameWithSoundboards> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            GameDao gameDao = GameDao.getInstance(appContext);

            Log.d(TAG, "Loading games...");

            ImmutableList<GameWithSoundboards> res = gameDao.findAllGamesWithSoundboards();

            Log.d(TAG, "Games loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<GameWithSoundboards> gameWithSoundboards) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            initGameItemAdapter(gameWithSoundboards);
        }
    }
}
