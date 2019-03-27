package de.soundboardcrafter.activity.sound.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Sound;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditFragment extends Fragment {
    private static final String ARG_SOUND_ID = "soundId";

    public static final String EXTRA_SOUND_ID = "soundId";

    private Sound sound;

    private TextView nameTextView;
    private SeekBar volumePercentageSeekBar;

    static SoundEditFragment newInstance(UUID soundId) {
        Bundle args = new Bundle();
        args.putString(ARG_SOUND_ID, soundId.toString());

        SoundEditFragment fragment = new SoundEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final UUID soundId = UUID.fromString(getArguments().getString(ARG_SOUND_ID));

        new FindSoundTask(getActivity(), soundId).execute();

        // The result will be the sound id, so that the calling
        // activity can update its GUI for this sound.
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SOUND_ID, soundId.toString());

        getActivity().setResult(
                // There is no cancel button - the result is always OK
                Activity.RESULT_OK,
                intent);
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sound_edit,
                container, false);

        nameTextView = rootView.findViewById(R.id.nameText);
        nameTextView.setEnabled(false);
        volumePercentageSeekBar = rootView.findViewById(R.id.volumePercentageSeekBar);
        volumePercentageSeekBar.setMax(Sound.MAX_VOLUME_PERCENTAGE);
        volumePercentageSeekBar.setEnabled(false);

        return rootView;
    }

    @UiThread
    private void updateUI(Sound sound) {
        this.sound = sound;

        nameTextView.setTextKeepState(sound.getName());
        nameTextView.setEnabled(true);

        volumePercentageSeekBar.setProgress(sound.getVolumePercentage());
        volumePercentageSeekBar.setEnabled(true);
    }

    // TODO Show and edit other parts of the sound

    // TODO "When interacting with a slider, changes should be represented immediately."

    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();

        String nameEntered = nameTextView.getText().toString();
        if (!nameEntered.isEmpty()) {
            sound.setName(nameEntered);
        }

        sound.setVolumePercentage(volumePercentageSeekBar.getProgress());

        // TODO Take other values from the GUI

        new SaveSoundTask(getActivity(), sound).execute();
    }

    /**
     * A background task, used to load the sound from the database.
     */
    public class FindSoundTask extends AsyncTask<Void, Void, Sound> {
        private final String TAG = FindSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID soundId;
        private final SoundboardDao soundboardDao = SoundboardDao.getInstance(getActivity());

        FindSoundTask(Context context, UUID soundId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundId = soundId;
        }

        @Override
        @WorkerThread
        protected Sound doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading sound....");

            Sound sound = soundboardDao.getInstance(getActivity()).findSound(soundId);

            Log.d(TAG, "Sound loaded.");

            return sound;
        }

        @Override
        @UiThread
        protected void onPostExecute(Sound sound) {
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

            updateUI(sound);
        }
    }

    /**
     * A background task, used to save the sound
     */
    public class SaveSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final Sound sound;
        private final SoundboardDao soundboardDao = SoundboardDao.getInstance(getActivity());

        SaveSoundTask(Context context, Sound sound) {
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

            Log.d(TAG, "Saving sound " + sound);

            soundboardDao.updateSound(sound);

            return null;
        }
    }
}
