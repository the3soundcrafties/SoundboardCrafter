package de.soundboardcrafter.activity.game.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.main.MainActivity;
import de.soundboardcrafter.dao.GameDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.GameWithSoundboards;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Soundboard;

/**
 * Activity for editing a single gameWithSoundboards (name, volume etc.).
 */
public class GameEditFragment extends Fragment {
    private static final String TAG = GameEditFragment.class.getName();

    private static final String ARG_GAME_ID = "gameId";

    private static final String EXTRA_GAME_ID = "gameId";
    private static final String EXTRA_EDIT_FRAGMENT = "gameEditFragment";

    private GameEditView gameEditView;

    private GameWithSoundboards gameWithSoundboards;


    static GameEditFragment newInstance(UUID gameId) {
        Bundle args = new Bundle();
        args.putString(ARG_GAME_ID, gameId.toString());

        GameEditFragment fragment = new GameEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    static GameEditFragment newInstance() {
        return new GameEditFragment();
    }


    @Override
    @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The result will be the game id, so that the calling
        // activity can update its GUI for this gameWithSoundboards.

        if (getArguments() != null) {
            String gameIdArg = getArguments().getString(ARG_GAME_ID);
            UUID gameId = UUID.fromString(gameIdArg);
            new FindGameTask(getActivity(), gameId).execute();
        } else {
            gameWithSoundboards = new GameWithSoundboards(getString(R.string.new_game_name));
            new FindAllSoundboardsTask(getContext()).execute();

        }
        Intent intent = new Intent(getActivity(), GameCreateActivity.class);
        getActivity().setResult(
                Activity.RESULT_CANCELED,
                intent);


    }

    @Override
    @UiThread
    // Called especially when the SoundboardPlaySoundEditActivity returns.
    public void onResume() {
        super.onResume();
    }


    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game_edit,
                container, false);

        gameEditView = rootView.findViewById(R.id.edit_view);
        gameEditView.setName(gameWithSoundboards.getGame().getName());


        gameEditView.setOnClickListenerSave(
                () -> {
                    saveGame();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra(EXTRA_GAME_ID, gameWithSoundboards.getGame().getId().toString());
                    intent.putExtra(EXTRA_EDIT_FRAGMENT, GameEditFragment.class.getName());
                    getActivity().setResult(
                            Activity.RESULT_OK,
                            intent);
                    getActivity().finish();
                }
        );
        gameEditView.setOnClickListenerCancel(
                () -> {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra(EXTRA_GAME_ID, gameWithSoundboards.getGame().getId().toString());
                    intent.putExtra(EXTRA_EDIT_FRAGMENT, GameEditFragment.class.getName());
                    getActivity().setResult(
                            Activity.RESULT_CANCELED,
                            intent);
                    getActivity().finish();
                }
        );

        return rootView;
    }


    @UiThread
    private void updateUIGameInfo(GameWithSoundboards gameWithSoundboards) {
        this.gameWithSoundboards = gameWithSoundboards;
        gameEditView.setName(gameWithSoundboards.getGame().getName());
    }

    @UiThread
    private void updateUISoundboards(List<Soundboard> soundboards) {
        List<SelectableSoundboard> selectableSoundboards = new ArrayList<>();
        ImmutableList<Soundboard> soundboardsInGame = gameWithSoundboards.getSoundboards();
        for (Soundboard soundboard : soundboards) {
            if (soundboardsInGame.contains(soundboard)) {
                selectableSoundboards.add(new SelectableSoundboard(soundboard, true));
            } else {
                selectableSoundboards.add(new SelectableSoundboard(soundboard, false));
            }
        }
        gameEditView.setSoundboards(selectableSoundboards);


    }

    private void saveGame() {
        String nameEntered = gameEditView.getName();
        if (!nameEntered.isEmpty()) {
            gameWithSoundboards.getGame().setName(nameEntered);
        }
        List<SelectableSoundboard> soundboards = gameEditView.getSelectableSoundboards();
        gameWithSoundboards.clearSoundboards();
        for (SelectableSoundboard soundboard : soundboards) {
            if (soundboard.isSelected()) {
                gameWithSoundboards.addSoundboard(soundboard.getSoundboard());
            }
        }
        new SaveNewGameTask(getActivity(), gameWithSoundboards).execute();
    }


    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();
    }


    /**
     * A background task, used to load all soundboards from the database.
     */
    class FindAllSoundboardsTask extends AsyncTask<Void, Void, List<Soundboard>> {
        private final String TAG = FindAllSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindAllSoundboardsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected List<Soundboard> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }
            Log.d(TAG, "Loading soundboards....");

            List<Soundboard> res =
                    SoundboardDao.getInstance(appContext).findAll();

            Log.d(TAG, "Game loaded.");

            return res;
        }


        @Override
        @UiThread
        protected void onPostExecute(List<Soundboard> soundboards) {
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

            updateUISoundboards(soundboards);
        }
    }


    /**
     * A background task, used to load the gameWithSoundboards from the database.
     */
    class FindGameTask extends AsyncTask<Void, Void, GameWithSoundboards> {
        private final String TAG = FindGameTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID gameId;

        FindGameTask(Context context, UUID gameId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.gameId = gameId;
        }

        @Override
        @WorkerThread
        protected GameWithSoundboards doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }
            Log.d(TAG, "Loading gameWithSoundboards....");

            GameWithSoundboards res =
                    GameDao.getInstance(appContext).findGameWithSoundboards(gameId);

            Log.d(TAG, "Game loaded.");

            return res;
        }


        @Override
        @UiThread
        protected void onPostExecute(GameWithSoundboards gameWithSoundboards) {
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

            updateUIGameInfo(gameWithSoundboards);
            new FindAllSoundboardsTask(getContext()).execute();
        }
    }

    /**
     * A background task, used to save the gameWithSoundboards
     */
    class SaveNewGameTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewGameTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final GameWithSoundboards gameWithSoundboards;

        SaveNewGameTask(Context context, GameWithSoundboards gameWithSoundboards) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.gameWithSoundboards = gameWithSoundboards;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving gameWithSoundboards " + gameWithSoundboards);
            GameDao.getInstance(appContext).insertWithSoundboards(gameWithSoundboards);

            return null;
        }
    }
}
