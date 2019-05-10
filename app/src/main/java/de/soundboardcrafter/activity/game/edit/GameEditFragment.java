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
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.model.GameWithSoundboards;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;

/**
 * Activity for editing a single gameWithSoundboards (name, volume etc.).
 */
public class GameEditFragment extends Fragment {
    private static final String TAG = GameEditFragment.class.getName();

    private static final String ARG_GAME_ID = "gameId";

    private static final String EXTRA_GAME_ID = "gameId";

    private GameEditView gameEditView;

    private GameWithSoundboards gameWithSoundboards;

    static GameEditFragment newInstance(UUID gameId) {
        Bundle args = new Bundle();
        args.putString(ARG_GAME_ID, gameId.toString());

        GameEditFragment fragment = new GameEditFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final UUID gameId = UUID.fromString(getArguments().getString(ARG_GAME_ID));

        new FindGameTask(getActivity(), gameId).execute();

        // The result will be the game id, so that the calling
        // activity can update its GUI for this gameWithSoundboards.
        Intent intent = new Intent(getActivity(), GameEditActivity.class);
        intent.putExtra(EXTRA_GAME_ID, gameId.toString());
        getActivity().setResult(
                // There is no cancel button - the result is always OK
                Activity.RESULT_OK,
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

        gameEditView.setEnabled(false);

        return rootView;
    }

    @UiThread
    private void updateUI(GameWithSoundboards gameWithSoundboards) {
        this.gameWithSoundboards = gameWithSoundboards;

        gameEditView.setName(gameWithSoundboards.getGame().getName());
        gameEditView.setSoundboards(toSelectableSoundboards(gameWithSoundboards.getSoundboards()));

        gameEditView.setEnabled(true);
    }

    private List<SelectableSoundboard> toSelectableSoundboards(ImmutableList<Soundboard> soundboards) {
        List<SelectableSoundboard> res = new ArrayList<>();
        for (Soundboard soundboard : soundboards) {
            res.add(new SelectableSoundboard(soundboard, true));
        }
        return res;
    }


    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();

        String nameEntered = gameEditView.getName();
        if (!nameEntered.isEmpty()) {
            gameWithSoundboards.getSound().setName(nameEntered);
        }
        new SaveSoundTask(getActivity(), gameWithSoundboards).execute();
    }


    /**
     * A background task, used to load the gameWithSoundboards from the database.
     */
    class FindGameTask extends AsyncTask<Void, Void, GameWithSoundboards> {
        private final String TAG = FindGameTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID soundId;

        FindGameTask(Context context, UUID soundId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundId = soundId;
        }

        @Override
        @WorkerThread
        protected SoundWithSelectableSoundboards doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading gameWithSoundboards....");

            SoundWithSelectableSoundboards res =
                    SoundDao.getInstance(appContext).findSoundWithSelectableSoundboards(soundId);

            Log.d(TAG, "Sound loaded.");

            return res;
        }


        @Override
        @UiThread
        protected void onPostExecute(SoundWithSelectableSoundboards soundWithSelectableSoundboards) {
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

            updateUI(soundWithSelectableSoundboards);
        }
    }

    /**
     * A background task, used to save the gameWithSoundboards
     */
    class SaveSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final SoundWithSelectableSoundboards sound;

        SaveSoundTask(Context context, SoundWithSelectableSoundboards sound) {
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

            Log.d(TAG, "Saving gameWithSoundboards " + sound);

            if (soundboardsEditable) {
                SoundDao.getInstance(appContext).updateSoundAndSounboardLinks(sound);
            } else {
                SoundDao.getInstance(appContext).updateSound(sound.getSound());
            }

            return null;
        }
    }
}
