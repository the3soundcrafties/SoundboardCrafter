package de.soundboardcrafter.activity.favorites.edit;

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
import de.soundboardcrafter.activity.soundboard.edit.SoundboardCreateActivity;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardEditActivity;
import de.soundboardcrafter.dao.FavoritesDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.FavoritesWithSoundboards;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Soundboard;

/**
 * Activity for editing favorites.
 */
public class FavoritesEditFragment extends Fragment {
    private static final String ARG_FAVORITES_ID = "favoritesId";

    private static final String EXTRA_FAVORITES_ID = "favoritesId";
    private static final String EXTRA_EDIT_FRAGMENT = "favoritesEditFragment";

    private FavoritesEditView favoritesEditView;
    private FavoritesWithSoundboards favoritesWithSoundboards;
    private boolean isNew;


    static FavoritesEditFragment newInstance(UUID favoritesId) {
        Bundle args = new Bundle();
        args.putString(ARG_FAVORITES_ID, favoritesId.toString());

        FavoritesEditFragment fragment = new FavoritesEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    static FavoritesEditFragment newInstance() {
        return new FavoritesEditFragment();
    }


    @Override
    @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The result will be the id of the favorites, so that the calling
        // activity can update its GUI for this favoritesWithSoundboards.

        if (getArguments() != null) {
            String favoritesIdArg = getArguments().getString(ARG_FAVORITES_ID);
            UUID favoritesId = UUID.fromString(favoritesIdArg);
            new FindFavoritesTask(requireActivity(), favoritesId).execute();
        } else {
            isNew = true;
            favoritesWithSoundboards =
                    new FavoritesWithSoundboards(getString(R.string.new_favorites_name));
            new FindAllSoundboardsTask(requireContext()).execute();
        }

        if (isNew) {
            Intent intent = new Intent(getActivity(), SoundboardCreateActivity.class);
            requireActivity().setResult(
                    Activity.RESULT_CANCELED,
                    intent);
        } else {
            Intent intent = new Intent(getActivity(), SoundboardEditActivity.class);
            requireActivity().setResult(
                    Activity.RESULT_OK,
                    intent);
        }
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
        View rootView = inflater.inflate(R.layout.fragment_favorites_edit,
                container, false);

        favoritesEditView = rootView.findViewById(R.id.edit_view);
        if (isNew) {
            favoritesEditView.setName(favoritesWithSoundboards.getFavorites().getName());
            favoritesEditView.setOnClickListenerSave(
                    () -> {
                        saveNewFavorites();
                        Intent intent = new Intent(getActivity(), SoundboardCreateActivity.class);
                        intent.putExtra(EXTRA_FAVORITES_ID,
                                favoritesWithSoundboards.getFavorites().getId().toString());
                        intent.putExtra(EXTRA_EDIT_FRAGMENT, FavoritesEditFragment.class.getName());
                        requireActivity().setResult(
                                Activity.RESULT_OK,
                                intent);
                        requireActivity().finish();
                    }
            );
            favoritesEditView.setOnClickListenerCancel(
                    () -> {
                        Intent intent = new Intent(getActivity(), SoundboardCreateActivity.class);
                        intent.putExtra(EXTRA_FAVORITES_ID,
                                favoritesWithSoundboards.getFavorites().getId().toString());
                        intent.putExtra(EXTRA_EDIT_FRAGMENT, FavoritesEditFragment.class.getName());
                        requireActivity().setResult(
                                Activity.RESULT_CANCELED,
                                intent);
                        requireActivity().finish();
                    }
            );
        } else {
            favoritesEditView.setButtonsInvisible();
        }


        return rootView;
    }


    @UiThread
    private void updateUIFavoritesInfo(FavoritesWithSoundboards favoritesWithSoundboards) {
        this.favoritesWithSoundboards = favoritesWithSoundboards;
        favoritesEditView.setName(favoritesWithSoundboards.getFavorites().getName());
    }

    @UiThread
    private void updateUISoundboards(List<Soundboard> soundboards) {
        List<SelectableSoundboard> selectableSoundboards = new ArrayList<>();
        ImmutableList<Soundboard> soundboardsInFavorites =
                favoritesWithSoundboards.getSoundboards();
        for (Soundboard soundboard : soundboards) {
            if (soundboardsInFavorites.contains(soundboard)) {
                selectableSoundboards.add(new SelectableSoundboard(soundboard, true));
            } else {
                selectableSoundboards.add(new SelectableSoundboard(soundboard, false));
            }
        }
        favoritesEditView.setSoundboards(selectableSoundboards);


    }

    private void saveNewFavorites() {
        updateFavoritesWithSoundboards();
        new SaveNewFavoritesTask(requireActivity(), favoritesWithSoundboards).execute();
    }

    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();
        if (!isNew && favoritesWithSoundboards != null) {
            updateFavoritesWithSoundboards();
            new UpdateFavoritesTask(requireActivity(), favoritesWithSoundboards).execute();
        }
    }

    private void updateFavoritesWithSoundboards() {
        String nameEntered = favoritesEditView.getName();
        if (!nameEntered.isEmpty()) {
            favoritesWithSoundboards.getFavorites().setName(nameEntered);
        }
        List<SelectableSoundboard> soundboards = favoritesEditView.getSelectableSoundboards();
        favoritesWithSoundboards.clearSoundboards();
        for (SelectableSoundboard soundboard : soundboards) {
            if (soundboard.isSelected()) {
                favoritesWithSoundboards.addSoundboard(soundboard.getSoundboard());
            }
        }
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

            Log.d(TAG, "Favorites loaded.");

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
     * A background task, used to load the favoritesWithSoundboards from the database.
     */
    class FindFavoritesTask extends AsyncTask<Void, Void, FavoritesWithSoundboards> {
        private final String TAG = FindFavoritesTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID favoritesId;

        FindFavoritesTask(Context context, UUID favoritesId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.favoritesId = favoritesId;
        }

        @Override
        @WorkerThread
        protected FavoritesWithSoundboards doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }
            Log.d(TAG, "Loading favoritesWithSoundboards....");

            FavoritesWithSoundboards res =
                    FavoritesDao.getInstance(appContext).findFavoritesWithSoundboards(favoritesId);

            Log.d(TAG, "Favorites loaded.");

            return res;
        }


        @Override
        @UiThread
        protected void onPostExecute(FavoritesWithSoundboards favoritesWithSoundboards) {
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

            updateUIFavoritesInfo(favoritesWithSoundboards);
            new FindAllSoundboardsTask(requireContext()).execute();
        }
    }

    /**
     * A background task, used to save the favoritesWithSoundboards
     */
    class SaveNewFavoritesTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewFavoritesTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final FavoritesWithSoundboards favoritesWithSoundboards;

        SaveNewFavoritesTask(Context context, FavoritesWithSoundboards favoritesWithSoundboards) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.favoritesWithSoundboards = favoritesWithSoundboards;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving favoritesWithSoundboards " + favoritesWithSoundboards);
            FavoritesDao.getInstance(appContext).insertWithSoundboards(favoritesWithSoundboards);

            return null;
        }
    }

    /**
     * A background task, used to save the favoritesWithSoundboards
     */
    class UpdateFavoritesTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = UpdateFavoritesTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final FavoritesWithSoundboards favoritesWithSoundboards;

        UpdateFavoritesTask(Context context, FavoritesWithSoundboards favoritesWithSoundboards) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.favoritesWithSoundboards = favoritesWithSoundboards;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving favoritesWithSoundboards " + favoritesWithSoundboards);
            FavoritesDao.getInstance(appContext).updateWithSoundboards(favoritesWithSoundboards);

            return null;
        }
    }
}
