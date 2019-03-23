package de.soundboardcrafter.activity;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Starting Foreground Services with the Help of MediaPlayerService
 */
class MediaPlayerManagerService {

    private final MediaPlayerConnectionService connectionService;
    private static String TAG = MediaPlayerManagerService.class.getName();
    private final Activity activity;

    MediaPlayerManagerService(Activity activity) {
        this.activity = activity;
        Intent intent = new Intent(activity, MediaPlayerService.class);
        connectionService = new MediaPlayerConnectionService(intent);
        activity.bindService(intent, connectionService, Context.BIND_AUTO_CREATE);
        activity.startService(intent);
    }

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
    void playSound(Soundboard soundboard, Sound sound, OnPlay onPlay, OnStop onStop) {
        connectionService.startMediaPlayer(soundboard, sound, onPlay, onStop);
    }

    void stopService() {
        if (connectionService != null) {
            connectionService.stopService();

        }
    }

    boolean shouldBePlaying(@Nonnull Soundboard soundboard, @Nonnull Sound sound) {
        Preconditions.checkNotNull(activity, "activity is null");
        Preconditions.checkNotNull(sound, "sound is null");
        if (connectionService == null) {
            return false;
        }
        return connectionService.shouldBePlaying(soundboard, sound);
    }

    void stopSound(Soundboard soundboard, Sound sound) {
        stopSound(soundboard, sound, null);
    }

    void stopSound(Soundboard soundboard, Sound sound, @Nullable OnStop onStop) {
        Preconditions.checkNotNull(activity, "activity is null");
        Preconditions.checkNotNull(sound, "sound is null");
        if (connectionService != null) {
            connectionService.stopMediaPlayer(soundboard, sound, onStop);
        }

    }

    static class MediaPlayerConnectionService implements ServiceConnection {
        private static final String TAG = MediaPlayerConnectionService.class.getName();
        private final Intent intent;
        private MediaPlayerService service;

        MediaPlayerConnectionService(Intent intent) {
            this.intent = intent;
        }

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

        void startMediaPlayer(Soundboard soundboard, Sound sound, OnPlay onPlay, OnStop onStop) {
            if (service != null) {
                service.addMediaPlayer(soundboard, sound, onPlay, onStop);
            }
        }

        boolean shouldBePlaying(Soundboard soundboard, Sound sound) {
            return service != null && service.shouldBePlaying(soundboard, sound);
        }

        void stopMediaPlayer(Soundboard soundboard, Sound sound, @Nullable OnStop onStop) {
            if (service != null) {
                service.stopPlaying(soundboard, sound, onStop);
            }
        }

        void stopService() {
            if (service != null) {
                service.stopService(intent);
            }
        }
    }

    public static class MediaPlayerService extends Service {
        private final IBinder binder = new Binder();
        static final String EXTRA_SOUND = "Sound";
        static final String EXTRA_SOUNDBOARD = "Soundboard";
        /**
         * pair<SoundboardID, SoundId>
         **/
        private final HashMap<Pair<UUID, UUID>, MediaPlayer> mediaPlayers = new HashMap<>();

        public MediaPlayerService() {

        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return Service.START_NOT_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return binder;
        }

        void stopPlaying(Soundboard soundboard, Sound sound, @Nullable OnStop onStop) {
            MediaPlayer player = mediaPlayers.get(createKey(soundboard, sound));
            if (player != null) {
                if (onStop != null) {
                    onStop.onStop();
                }
                player.stop();
                removeMediaPlayer(player);
            }
        }

        private void removeMediaPlayer(MediaPlayer mediaPlayer) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                Pair<UUID, UUID> key = findKey(mediaPlayer);
                if (key != null) {
                    mediaPlayers.remove(key);
                }
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

        class Binder extends android.os.Binder {
            MediaPlayerService getService() {
                return MediaPlayerService.this;
            }
        }

        void addMediaPlayer(Soundboard soundboard, Sound sound, OnPlay onPlay, OnStop onStop) {
            onPlay.onStartPlaying();
            Pair<UUID, UUID> key = createKey(soundboard, sound);
            MediaPlayer existingMediaPlayer = mediaPlayers.get(key);
            if (existingMediaPlayer == null) {
                final MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setOnErrorListener(this::onError);
                try {
                    mediaPlayer.setDataSource(sound.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
                mediaPlayer.setVolume((float) sound.getVolumePercentage() / 100f, (float) sound.getVolumePercentage() / 100f);
                mediaPlayer.setLooping(sound.isLoop());
                mediaPlayer.setOnPreparedListener(this::onPrepared);
                mediaPlayer.setOnCompletionListener((mbd) -> onCompletion(mbd, onStop));
                mediaPlayer.prepareAsync(); // prepare async to not block main thread
                mediaPlayers.put(key, mediaPlayer);
            } else {
                mediaPlayers.put(key, existingMediaPlayer);
            }

        }

        private Pair<UUID, UUID> createKey(Soundboard soundboard, Sound sound) {
            return Pair.create(soundboard.getId(), sound.getId());
        }

        boolean shouldBePlaying(Soundboard soundboard, Sound sound) {
            return mediaPlayers.get(createKey(soundboard, sound)) != null;
        }

        /**
         * Called when MediaPlayer is ready
         */
        void onPrepared(MediaPlayer player) {
            player.start();
        }

        boolean onError(MediaPlayer player, int what, int extra) {
            removeMediaPlayer(player);
            return true;
        }

        void onCompletion(MediaPlayer player, OnStop onStop) {
            onStop.onStop();
            removeMediaPlayer(player);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            for (MediaPlayer mediaPlayer : mediaPlayers.values()) {
                mediaPlayer.stop();
                removeMediaPlayer(mediaPlayer);
            }
        }
    }
}
