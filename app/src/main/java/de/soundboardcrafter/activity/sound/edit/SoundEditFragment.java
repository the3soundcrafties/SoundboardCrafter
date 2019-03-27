package de.soundboardcrafter.activity.sound.edit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Sound;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditFragment extends Fragment implements ServiceConnection {
    private static final String TAG = SoundEditFragment.class.getName();

    private static final String ARG_SOUND_ID = "soundId";

    public static final String EXTRA_SOUND_ID = "soundId";

    private Sound sound;

    private TextView nameTextView;
    private SeekBar volumePercentageSeekBar;
    private Switch loopSwitch;

    private MediaPlayerService mediaPlayerService;

    static SoundEditFragment newInstance(UUID soundId) {
        Bundle args = new Bundle();
        args.putString(ARG_SOUND_ID, soundId.toString());

        SoundEditFragment fragment = new SoundEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        Log.d(TAG, "MediaPlayerService is connected");
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
        final UUID soundId = UUID.fromString(getArguments().getString(ARG_SOUND_ID));

        new FindSoundTask(getActivity(), soundId).execute();

        bindService();

        // The result will be the sound id, so that the calling
        // activity can update its GUI for this sound.
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SOUND_ID, soundId.toString());

        getActivity().setResult(
                // There is no cancel button - the result is always OK
                Activity.RESULT_OK,
                intent);
    }

    private void bindService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
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
        volumePercentageSeekBar.setOnSeekBarChangeListener(new SeekBarVolumeUpdater());

        loopSwitch = rootView.findViewById(R.id.loopSwitch);
        loopSwitch.setEnabled(false);

        return rootView;
    }

    @UiThread
    private void updateUI(Sound sound) {
        this.sound = sound;

        nameTextView.setTextKeepState(sound.getName());
        nameTextView.setEnabled(true);

        volumePercentageSeekBar.setProgress(sound.getVolumePercentage());
        volumePercentageSeekBar.setEnabled(true);

        loopSwitch.setChecked(sound.isLoop());
        loopSwitch.setEnabled(true);
    }

    // TODO Show Path

    private void setVolume(int volumePercentage) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }
        service.setVolumePercentage(sound.getId(), volumePercentage);
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            bindService();
        }
        return mediaPlayerService;
    }

    @Override
    @UiThread
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();

        getActivity().unbindService(this);

        String nameEntered = nameTextView.getText().toString();
        if (!nameEntered.isEmpty()) {
            sound.setName(nameEntered);
        }

        sound.setVolumePercentage(volumePercentageSeekBar.getProgress());
        // TODO Scale volume logarithmically

        sound.setLoop(loopSwitch.isChecked());

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

    private class SeekBarVolumeUpdater implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            setVolume(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
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
