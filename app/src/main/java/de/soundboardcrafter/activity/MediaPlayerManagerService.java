package de.soundboardcrafter.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Starting Foreground Services with the Help of MediaPlayerService
 */
public class MediaPlayerManagerService {

    private static MediaPlayerConnectionService connectionService;
    private static String TAG = MediaPlayerManagerService.class.getName();

    @FunctionalInterface
    public interface OnPlay extends Serializable {
        void onStartPlaying();
    }

    @FunctionalInterface
    public interface OnStop extends Serializable {
        void onStop();
    }

    /**
     * Play a given sound in a MediaPlayer by starting a MediaPlayerService
     */
    public static void playSound(Activity activity, Soundboard soundboard, Sound sound, OnPlay onPlay) {
        checkPermission(activity);
        if (onPlay != null) {
            onPlay.onStartPlaying();
        }
        if (connectionService == null) {
            connectionService = new MediaPlayerConnectionService();
            Intent intent = new Intent(activity, MediaPlayerService.class);
            //initally we provide soundbar and sound through the intent so it is pass through the connection
            intent.putExtra(MediaPlayerService.EXTRA_SOUNDBOARD, soundboard);
            intent.putExtra(MediaPlayerService.EXTRA_SOUND, sound);
            activity.bindService(intent, connectionService, Context.BIND_AUTO_CREATE);
            activity.startService(intent);
        } else {
            //here we are sure the service must be already connected, so we can start a further media player
            connectionService.startMediaPlayer(soundboard, sound);
        }

    }

    public static boolean shouldBePlaying(@Nonnull Activity activity, @Nonnull Soundboard soundboard, @Nonnull Sound sound) {
        Preconditions.checkNotNull(activity, "activity is null");
        Preconditions.checkNotNull(sound, "sound is null");
        if (connectionService == null) {
            return false;
        }
        return connectionService.shouldBePlaying(soundboard, sound);
    }

    public static void stopSound(Activity activity, Soundboard soundboard, Sound sound, OnStop onStop) {
        Preconditions.checkNotNull(activity, "activity is null");
        Preconditions.checkNotNull(sound, "sound is null");
        if (connectionService != null) {
            onStop.onStop();
            connectionService.stopMediaPlayer(soundboard, sound);
        }

    }

    public static void checkPermission(Activity activity) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);


            // TODO: 16.03.2019 when the user decline the permission, the application should not hung up
        }
    }

    public static class MediaPlayerConnectionService implements ServiceConnection {
        private static String TAG = MediaPlayerConnectionService.class.getName();
        private MediaPlayerService service;

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
            service = b.getService();
            Log.d(TAG, "MediaPlayerService is connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {

        }

        @Override
        public void onNullBinding(ComponentName name) {

        }

        public void startMediaPlayer(Soundboard soundboard, Sound sound) {
            if (service != null) {
                service.addMediaPlayer(soundboard, sound);
            }
        }

        public boolean shouldBePlaying(Soundboard soundboard, Sound sound) {
            return service != null && service.shouldBePlaying(soundboard, sound);
        }

        public void stopMediaPlayer(Soundboard soundboard, Sound sound) {
            if (service != null) {
                service.stopPlaying(soundboard, sound);
            }
        }
    }

    public static class MediaPlayerService extends Service {
        private final IBinder binder = new Binder();
        protected static final String EXTRA_SOUND = "Sound";
        protected static final String EXTRA_SOUNDBOARD = "Soundboard";
        /**
         * pair<SoundboardID, SoundId>
         **/
        private HashMap<Pair<UUID, UUID>, MediaPlayer> mediaPlayers = new HashMap<>();

        public MediaPlayerService() {

        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return Service.START_NOT_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            Soundboard soundboard = (Soundboard) intent.getSerializableExtra(EXTRA_SOUNDBOARD);
            Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
            addMediaPlayer(soundboard, sound);
            return binder;
        }

        public void stopPlaying(Soundboard soundboard, Sound sound) {
            MediaPlayer player = mediaPlayers.get(createKey(soundboard, sound));
            if (player != null) {
                player.stop();
                stopAndRemoveMediaPlayer(player);
            }
        }

        private void stopAndRemoveMediaPlayer(MediaPlayer mediaPlayer) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                Pair<UUID, UUID> key = findKey(mediaPlayer);
                if (key != null) {
                    mediaPlayers.remove(key);
                }
                mediaPlayer = null;

            }
        }

        @Nullable
        private Pair<UUID, UUID> findKey(@Nonnull MediaPlayer toBeFound) {
            Preconditions.checkNotNull(toBeFound, "toBeFound is null");
            Optional<Map.Entry<Pair<UUID, UUID>, MediaPlayer>> foundPlayer =
                    mediaPlayers.entrySet().stream().filter(mediaPlayer -> mediaPlayer.getValue().equals(toBeFound)).findFirst();
            if (foundPlayer.isPresent()) {
                return foundPlayer.get().getKey();
            }
            return null;
        }

        public class Binder extends android.os.Binder {
            MediaPlayerService getService() {
                return MediaPlayerService.this;
            }
        }

        public void addMediaPlayer(Soundboard soundboard, Sound sound) {
            Pair<UUID, UUID> key = createKey(soundboard, sound);
            MediaPlayer existingMediaPlayer = mediaPlayers.get(key);
            if (existingMediaPlayer == null) {
                final MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setOnErrorListener((mbd, what, extra) -> onError(mbd, what, extra));
                try {
                    mediaPlayer.setDataSource(sound.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
                mediaPlayer.setVolume((float) sound.getVolumePercentage() / 100f, (float) sound.getVolumePercentage() / 100f);
                mediaPlayer.setLooping(sound.isLoop());
                mediaPlayer.setOnPreparedListener((mbd) -> onPrepared(mbd));
                mediaPlayer.setOnCompletionListener((mbd) -> onCompletion(mbd));
                mediaPlayer.prepareAsync(); // prepare async to not block main thread
                mediaPlayers.put(key, mediaPlayer);
            } else {
                mediaPlayers.put(key, existingMediaPlayer);
            }

        }

        private Pair<UUID, UUID> createKey(Soundboard soundboard, Sound sound) {
            return Pair.create(soundboard.getId(), sound.getId());
        }

        public boolean shouldBePlaying(Soundboard soundboard, Sound sound) {
            return this.mediaPlayers.get(createKey(soundboard, sound)) != null;
        }

        /**
         * Called when MediaPlayer is ready
         */
        public void onPrepared(MediaPlayer player) {
            player.start();
        }

        public boolean onError(MediaPlayer player, int what, int extra) {
            stopAndRemoveMediaPlayer(player);
            return true;
        }

        public void onCompletion(MediaPlayer player) {
            stopAndRemoveMediaPlayer(player);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            this.mediaPlayers.values().stream().forEach(mediaPlayer -> stopAndRemoveMediaPlayer(mediaPlayer));
        }


    }

}
