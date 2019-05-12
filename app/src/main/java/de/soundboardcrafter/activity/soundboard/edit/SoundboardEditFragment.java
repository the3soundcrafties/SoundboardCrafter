package de.soundboardcrafter.activity.soundboard.edit;

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

import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.main.MainActivity;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * Activity for editing a single soundboard (name, volume etc.).
 */
public class SoundboardEditFragment extends Fragment {
    private static final String TAG = SoundboardEditFragment.class.getName();

    private static final String ARG_SOUNDBOARD_ID = "soundboardId";

    private static final String EXTRA_SOUNDBOARD_ID = "soundboardId";
    private static final String EXTRA_EDIT_FRAGMENT = "soundboardEditFragment";

    private SoundboardEditView soundboardEditView;

    private Soundboard soundboard;
    private boolean isNew;


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
            new FindSoundboardTask(getActivity(), soundboardId).execute();
        } else {
            isNew = true;
            soundboard = new Soundboard(getString(R.string.new_soundboard_name));
        }
        Intent intent = new Intent(getActivity(), SoundboardCreateActivity.class);
        if (isNew) {
            getActivity().setResult(
                    Activity.RESULT_CANCELED,
                    intent);
        } else {
            getActivity().setResult(
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
        View rootView = inflater.inflate(R.layout.fragment_soundboard_edit,
                container, false);

        soundboardEditView = rootView.findViewById(R.id.edit_view);

        if (isNew) {
            soundboardEditView.setOnClickListenerSave(
                    () -> {
                        saveNewSoundboard();
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
                        intent.putExtra(EXTRA_EDIT_FRAGMENT, SoundboardEditFragment.class.getName());
                        getActivity().setResult(
                                Activity.RESULT_OK,
                                intent);
                        getActivity().finish();
                    }
            );
            soundboardEditView.setOnClickListenerCancel(
                    () -> {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
                        intent.putExtra(EXTRA_EDIT_FRAGMENT, SoundboardEditFragment.class.getName());
                        getActivity().setResult(
                                Activity.RESULT_CANCELED,
                                intent);
                        getActivity().finish();
                    }
            );
        } else {
            soundboardEditView.setButtonsInvisible();
        }
        if (isNew) {
            soundboardEditView.setName(soundboard.getName());
        }


        return rootView;
    }


    @UiThread
    private void updateUI(Soundboard soundboard) {
        this.soundboard = soundboard;
        soundboardEditView.setName(soundboard.getName());
    }


    private void saveNewSoundboard() {
        String nameEntered = soundboardEditView.getName();
        if (!nameEntered.isEmpty()) {
            soundboard.setName(nameEntered);
        }
        new SaveNewSoundboardTask(getActivity(), soundboard).execute();
    }


    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();
        if (!isNew) {
            String nameEntered = soundboardEditView.getName();
            if (!nameEntered.isEmpty()) {
                soundboard.setName(nameEntered);
            }

            new UpdateSoundboardTask(getActivity(), soundboard).execute();
        }

    }


    /**
     * A background task, used to load the soundboard from the database.
     */
    class FindSoundboardTask extends AsyncTask<Void, Void, Soundboard> {
        private final String TAG = FindSoundboardTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID soundboardId;

        FindSoundboardTask(Context context, UUID soundboardId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundboardId = soundboardId;
        }

        @Override
        @WorkerThread
        protected Soundboard doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }
            Log.d(TAG, "Loading soundboard....");

            Soundboard res =
                    SoundboardDao.getInstance(appContext).find(soundboardId);

            Log.d(TAG, "Soundboard loaded.");

            return res;
        }


        @Override
        @UiThread
        protected void onPostExecute(Soundboard soundboard) {
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

            updateUI(soundboard);
        }
    }

    /**
     * A background task, used to save the soundboard
     */
    class SaveNewSoundboardTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundboardTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final Soundboard soundboard;

        SaveNewSoundboardTask(Context context, Soundboard soundboard) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundboard = soundboard;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving soundboard " + soundboard);
            SoundboardDao.getInstance(appContext).insert(soundboard);

            return null;
        }
    }

    /**
     * A background task, used to save the soundboard
     */
    class UpdateSoundboardTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundboardTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final Soundboard soundboard;

        UpdateSoundboardTask(Context context, Soundboard soundboard) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundboard = soundboard;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving soundboard " + soundboard);
            SoundboardDao.getInstance(appContext).update(soundboard);

            return null;
        }
    }
}
