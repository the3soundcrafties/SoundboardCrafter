package de.soundboardcrafter.activity.sound.edit.common;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.sound.edit.soundboard.play.SoundboardPlaySoundEditActivity;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditFragment extends Fragment implements ServiceConnection {
    private static final String TAG = SoundEditFragment.class.getName();

    private static final String ARG_SOUND_ID = "soundId";
    public static final String EXTRA_SOUND_ID = "soundId";

    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 1024;

    private SoundEditView soundEditView;

    private SoundWithSelectableSoundboards sound;

    private SoundEditChangeListener soundEditChangeListener = new SoundEditChangeListener();

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

        soundEditView.setOnVolumePercentageChangeListener(soundEditChangeListener);
        soundEditView.setOnLoopChangeListener(soundEditChangeListener);

        soundEditView.setEnabled(false);

        return rootView;
    }

    @UiThread
    private void updateUI(SoundWithSelectableSoundboards soundWithSelectableSoundboards) {
        sound = soundWithSelectableSoundboards;

        soundEditView.setName(sound.getSound().getName());
        soundEditView.setVolumePercentage(sound.getSound().getVolumePercentage());
        soundEditView.setLoop(sound.getSound().isLoop());
        soundEditChangeListener.setLoop(sound.getSound().isLoop());

        soundEditView.setSoundboards(sound.getSoundboards());

        soundEditView.setEnabled(true);
    }

    /**
     * Starts playing the sound, if it is not already playing.
     */
    private void ensurePlaying() {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        if (sound == null) {
            return;
        }

        if (!service.isActivelyPlaying(sound.getSound())) {
            if (sound.getSound().getAudioLocation() instanceof AssetFolderAudioLocation
                    || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
                try {
                    service.play(null, sound.getSound(), null);
                } catch (IOException e) {
                    handleSoundFileNotFound();
                }
            }
        }
    }

    private void handleSoundFileNotFound() {
        Snackbar snackbar = Snackbar
                .make(soundEditView, getString(R.string.audiofile_not_found),
                        Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.update_all_soundboards_and_sounds),
                        view -> {
                            new DeleteSoundTask(requireActivity(), sound.getSound().getId())
                                    .execute();
                            sound = null;

                            Intent intent = new Intent(getActivity(),
                                    SoundboardPlaySoundEditActivity.class);
                            // EXTRA_SOUND_ID stays null!
                            requireActivity().setResult(
                                    // There is no cancel button - the result is always OK
                                    Activity.RESULT_OK,
                                    intent);
                            getActivity().finish();
                        });
        snackbar.show();
    }

    /**
     * Sets the volume.
     */
    private void setVolumePercentage(int volumePercentage, boolean playIfNotPlaying) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        if (sound == null) {
            return;
        }

        service.setVolumePercentage(sound.getSound().getId(), volumePercentage);
    }

    /**
     * Sets whether the sound shall be played in a loop.
     */
    private void setLoop(boolean loop) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        if (sound == null) {
            return;
        }

        service.setLoop(sound.getSound().getId(), loop);
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

        stopPlaying(false);

        requireActivity().unbindService(this);

        if (sound == null) {
            // Sound not yet loaded - or has been deleted from the database
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

    private void stopPlaying(boolean fadeOut) {
        if (sound == null) {
            // sound not yet loaded - or has been deleted from the database
            return;
        }

        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        service.stopPlaying(null, sound.getSound(), fadeOut);
    }

    /**
     * Listens to changes while editing - might also start playing the sound.
     */
    private class SoundEditChangeListener implements
            SoundEditView.OnVolumePercentageChangeListener, SoundEditView.OnLoopChangeListener {
        private boolean loop;

        public void setLoop(boolean loop) {
            this.loop = loop;
        }

        @Override
        public void onVolumePercentageChanged(int volumePercentage, boolean fromUser) {
            if (fromUser) {
                ensurePlaying();
            }
            setVolumePercentage(volumePercentage, fromUser);
            SoundEditFragment.this.setLoop(loop);
        }

        @Override
        public void onLoopChanged(boolean loop) {
            this.loop = loop;
            SoundEditFragment.this.setLoop(loop);
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
        protected void onPostExecute(
                SoundWithSelectableSoundboards soundWithSelectableSoundboards) {
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

            final IAudioLocation audioLocation =
                    soundWithSelectableSoundboards.getSound().getAudioLocation();

            if (audioLocation instanceof AssetFolderAudioLocation
                    || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
                updateUI(soundWithSelectableSoundboards);
            } // Otherwise, the fragment will receive an event later.
        }
    }

    @UiThread
    private boolean isPermissionReadExternalStorageGrantedIfNotAskForIt() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalPermission();

            return false;
        }
        return true;
    }

    // This works, because the fragment ist not nested. And we *have* to do this here,
    // because our activity won't get the correct requestCode.
    // See https://stackoverflow.com/questions/36170324/receive-incorrect-resultcode-in-activitys
    // -onrequestpermissionsresult-when-reque/36186666 .
    @Override
    @UiThread
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (getActivity() == null) {
            return;
        }

        if (requestCode == REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // if (shouldShowRequestPermissionRationale(Manifest.permission
                // .READ_EXTERNAL_STORAGE)) {
                showPermissionRationale();
                //}
                return;
            }

            // We don't need any other permissions, so start preparing the sound
            Bundle arguments = getArguments();
            if (arguments != null) {
                final UUID soundId = UUID.fromString(arguments.getString(ARG_SOUND_ID));
                new FindSoundTask(requireActivity(), soundId).execute();
            }
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.yourSoundsPermissionRationaleTitle)
                .setMessage(R.string.yourSoundsPermissionRationaleMsg)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> requestReadExternalPermission())
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> requireActivity().finish())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void requestReadExternalPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE);
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

    /**
     * A background task, used to delete the sound
     */
    class DeleteSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = DeleteSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final UUID soundId;

        DeleteSoundTask(Context context, UUID soundId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundId = soundId;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Deleting sound " + soundId);

            SoundDao.getInstance(appContext).delete(soundId);
            return null;
        }
    }
}
