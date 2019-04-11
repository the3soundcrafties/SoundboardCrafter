package de.soundboardcrafter.activity.common.mediaplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Android service that keeps track off all the media players that are playing sounds in the app.
 */
public class MediaPlayerService extends Service {
    private static final String TAG = MediaPlayerService.class.getName();

    private final IBinder binder = new Binder();
    private final HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> mediaPlayers = new HashMap<>();

    @MainThread
    public MediaPlayerService() {
        Log.d(TAG, "MediaPlayerService is created");
    }

    @Override
    @UiThread
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    @UiThread
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @UiThread
    public void setOnPlayingStopped(Soundboard soundboard, Sound sound,
                                    SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.setOnPlayingStopped(onPlayingStopped);
        }
    }

    /**
     * Sets the volume for this sound.
     */
    @UiThread
    public void setVolumePercentage(UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        setVolume(soundId, percentageToVolume(volumePercentage));
    }

    /**
     * Sets the volume for this sound.
     */
    @UiThread
    private void setVolume(UUID soundId, float volume) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setVolume(m, volume));
    }

    /**
     * Stops all playing sounds in these soundboards
     */
    @UiThread
    public void stopPlaying(Iterable<Soundboard> soundboards) {
        for (Soundboard soundboard : soundboards) {
            stopPlaying(soundboard);
        }
    }

    /**
     * Stops all playing sounds in this soundboard
     */
    @UiThread
    private void stopPlaying(@NonNull Soundboard soundboard) {
        for (Iterator<Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer>> entryIt =
             mediaPlayers.entrySet().iterator(); entryIt.hasNext(); ) {
            Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer> entry = entryIt.next();
            if (soundboard.getId().equals(entry.getKey().getSoundboardId())) {
                SoundboardMediaPlayer player = entry.getValue();
                player.stop();
                player.release();
                entryIt.remove();
            }
        }
    }

    /**
     * Stops this sound when it's played in this soundboard
     */
    @UiThread
    public void stopPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            stop(player);
        }
    }

    /**
     * Stops this player and removes it.
     */
    private void stop(SoundboardMediaPlayer player) {
        player.stop();
        removeMediaPlayer(player);
    }

    @UiThread
    private void removeMediaPlayer(@NonNull SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.release();
        mediaPlayers.values().remove(mediaPlayer);
    }

    public class Binder extends android.os.Binder {
        @UiThread
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Adds a media player and starts playing.
     */
    @UiThread
    public void play(@Nullable Soundboard soundboard, @NonNull Sound sound,
                     @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        checkNotNull(sound, "sound is null");

        MediaPlayerSearchId key = new MediaPlayerSearchId(soundboard, sound);
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(key);
        if (mediaPlayer == null) {
            mediaPlayer = new SoundboardMediaPlayer();
            mediaPlayer.setOnPlayingStopped(onPlayingStopped);
            initMediaPlayer(sound, mediaPlayer);
            mediaPlayers.put(key, mediaPlayer);
        } else {
            // update the callbacks
            mediaPlayer.setOnPlayingStopped(onPlayingStopped);
            mediaPlayer.reset();
            initMediaPlayer(sound, mediaPlayer);
        }

        mediaPlayer.prepareAsync();
    }

    /**
     * Initializes this mediaPlayer for this sound. Does not start playing yet.
     */
    @UiThread
    private void initMediaPlayer(@NonNull Sound sound, SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnErrorListener((ev, what, extra) -> onError(mediaPlayer, what, extra));
        try {
            mediaPlayer.setDataSource(sound.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
        setVolume(mediaPlayer, percentageToVolume(sound.getVolumePercentage()));
        mediaPlayer.setLooping(sound.isLoop());
        mediaPlayer.setOnPreparedListener(this::onPrepared);
        mediaPlayer.setOnCompletionListener((mbd) -> onCompletion((SoundboardMediaPlayer) mbd));
    }

    private static float percentageToVolume(int volumePercentage) {
        return (float) volumePercentage / 100f;
    }

    /**
     * Sets the volume for this <code>mediaPlayer</code>.
     */
    @UiThread
    private void setVolume(SoundboardMediaPlayer mediaPlayer, float volume) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setVolume(volume, volume);
    }

    @UiThread
    public boolean isPlaying(@NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        return mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(sound.getId()))
                .map(Map.Entry::getValue)
                .anyMatch(SoundboardMediaPlayer::isPlaying);
    }

    @UiThread
    public boolean isPlaying(@NonNull Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(soundboard, "soundboard is null");
        checkNotNull(sound, "sound is null");

        return isPlaying(soundboard, sound.getId());
    }

    @UiThread
    private boolean isPlaying(@NonNull Soundboard soundboard, @NonNull UUID soundId) {
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(
                new MediaPlayerSearchId(soundboard.getId(), soundId));
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * Called when MediaPlayer is ready
     */
    @UiThread
    private void onPrepared(MediaPlayer player) {
        player.start();
    }

    @UiThread
    private boolean onError(SoundboardMediaPlayer player, int what, int extra) {
        Log.e(TAG, "Error in media player: what: " + what + " extra: " + extra);

        removeMediaPlayer(player);
        return true;
    }

    @UiThread
    private void onCompletion(SoundboardMediaPlayer player) {
        removeMediaPlayer(player);
    }

    @Override
    @UiThread
    public void onDestroy() {
        for (Iterator<SoundboardMediaPlayer> playerIt =
             mediaPlayers.values().iterator(); playerIt.hasNext(); ) {
            SoundboardMediaPlayer player = playerIt.next();
            player.stop();
            player.release();
            playerIt.remove();
        }
        super.onDestroy();
    }
}
