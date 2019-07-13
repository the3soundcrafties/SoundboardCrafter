package de.soundboardcrafter.activity.sound.edit.common;

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

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.sound.edit.soundboard.play.SoundboardPlaySoundEditActivity;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;

import static androidx.core.util.Preconditions.checkNotNull;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditFragment extends Fragment implements ServiceConnection {
    private static final String TAG = SoundEditFragment.class.getName();

    private static final String ARG_SOUND_ID = "soundId";
    public static final String EXTRA_SOUND_ID = "soundId";


    private SoundEditView soundEditView;

    private SoundWithSelectableSoundboards sound;

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
        Bundle arguments = getArguments();
        checkNotNull(arguments, "Sound edit fragment started without arguments");

        final UUID soundId = UUID.fromString(arguments.getString(ARG_SOUND_ID));
        new FindSoundTask(requireActivity(), soundId).execute();

        startService();
        // TODO Necessary?! Also done in onResume()
        bindService();

        // The result will be the sound id, so that the calling
        // activity can update its GUI for this sound.
        Intent intent = new Intent(getActivity(), SoundboardPlaySoundEditActivity.class);
        intent.putExtra(EXTRA_SOUND_ID, soundId.toString());
        requireActivity().setResult(
                // There is no cancel button - the result is always OK
                Activity.RESULT_OK,
                intent);

    }

    @Override
    @UiThread
    // Called especially when the SoundboardPlaySoundEditActivity returns.
    public void onResume() {
        super.onResume();
        bindService();
    }

    private void startService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().startService(intent);
    }

    private void bindService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sound_edit,
                container, false);

        soundEditView = rootView.findViewById(R.id.edit_view);

        soundEditView.setMaxVolumePercentage(Sound.MAX_VOLUME_PERCENTAGE);
        soundEditView.setOnVolumePercentageChangeListener(new VolumeUpdater());
        soundEditView.setEnabled(false);

        return rootView;
    }

    @UiThread
    private void updateUI(SoundWithSelectableSoundboards soundWithSelectableSoundboards) {
        sound = soundWithSelectableSoundboards;

        soundEditView.setName(soundWithSelectableSoundboards.getSound().getName());
        soundEditView.setVolumePercentage(soundWithSelectableSoundboards.getSound().getVolumePercentage());
        soundEditView.setLoop(soundWithSelectableSoundboards.getSound().isLoop());

        soundEditView.setSoundboards(soundWithSelectableSoundboards.getSoundboards());

        soundEditView.setEnabled(true);
    }

    /**
     * Sets the volume; also starts the sound is so desired.
     */
    private void setVolumePercentage(int volumePercentage, boolean playIfNotPlaying) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        if (playIfNotPlaying && !service.isPlaying(sound.getSound())) {
            service.play(null, sound.getSound(), null);
        }

        service.setVolumePercentage(sound.getSound().getId(), volumePercentage);
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

        stopPlaying();

        requireActivity().unbindService(this);

        if (sound == null) {
            // Sound not yet loaded
            return;
        }

        String nameEntered = soundEditView.getName();
        if (!nameEntered.isEmpty()) {
            sound.getSound().setName(nameEntered);
        }

        sound.getSound().setVolumePercentage(soundEditView.getVolumePercentage());
        sound.getSound().setLoop(soundEditView.isLoop());

        new SaveSoundTask(requireActivity(), sound).execute();
    }

    private void stopPlaying() {
        if (sound == null) {
            // not yet loaded
            return;
        }

        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        service.stopPlaying(null, sound.getSound());
    }

    /**
     * Callback to set the volume when the volume slider ist change; also starts the sound is
     * so desired.
     */
    private class VolumeUpdater implements SoundEditView.OnVolumePercentageChangeListener {
        @Override
        public void onVolumePercentageChanged(int volumePercentage, boolean fromUser) {
            setVolumePercentage(volumePercentage, fromUser);
        }
    }

    /**
     * A background task, used to load the sound from the database.
     */
    class FindSoundTask extends AsyncTask<Void, Void, SoundWithSelectableSoundboards> {
        private final String TAG = FindSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID soundId;

        FindSoundTask(Context context, UUID soundId) {
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

            Log.d(TAG, "Loading sound....");

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
     * A background task, used to save the sound
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

            Log.d(TAG, "Saving sound " + sound);

            SoundDao.getInstance(appContext).updateSoundAndSoundboardLinks(sound);
            return null;
        }
    }
}
