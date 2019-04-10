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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

public class MediaPlayerService extends Service {
    private final IBinder binder = new Binder();
    static final String EXTRA_SOUND = "Sound";
    static final String EXTRA_SOUNDBOARD = "Soundboard";
    private HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> mediaPlayers = new HashMap<>();

    public MediaPlayerService() {
        Log.d(getClass().getName(), "MediaPlayerService is created");
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


    public void setMediaPlayerCallbacks(Soundboard soundboard, Sound sound, SoundboardMediaPlayer.StartPlayCallback startPlayCallback, SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.setStartPlayCallback(startPlayCallback);
            player.setStopPlayCallback(stopPlayCallback);
        }
    }

    /**
     * Sets the volume for this sound.
     */
    public void setVolumePercentage(UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        setVolume(soundId, percentageToVolume(volumePercentage));
    }

    /**
     * Sets the volume for this sound.
     */
    private void setVolume(UUID soundId, float volume) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .forEach(e -> setVolume(e.getValue(), volume));
    }

    public void stopPlaying(@Nonnull Soundboard soundboard, @Nonnull Sound sound) {
        checkNotNull(soundboard, "soundboard is null");
        checkNotNull(sound, "sound is null");
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.stop();
            removeMediaPlayer(player);
        }
    }

    private void removeMediaPlayer(@Nonnull SoundboardMediaPlayer mediaPlayer) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            MediaPlayerSearchId key = findKey(mediaPlayer);
            if (key != null) {
                mediaPlayers.remove(key);
            }
        }
    }

    @Nullable
    private MediaPlayerSearchId findKey(@Nonnull SoundboardMediaPlayer toBeFound) {
        checkNotNull(toBeFound, "toBeFound is null");
        Optional<Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer>> foundPlayer =
                mediaPlayers.entrySet().stream().filter(mediaPlayer -> mediaPlayer.getValue().equals(toBeFound)).findFirst();
        if (foundPlayer.isPresent()) {
            return foundPlayer.get().getKey();
        }
        return null;
    }

    public class Binder extends android.os.Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Adds a media player without yet starting it
     */
    public void initMediaPlayer(@Nullable Soundboard soundboard, @Nonnull Sound sound,
                                @Nullable SoundboardMediaPlayer.InitializeCallback initializeCallback,
                                @Nullable SoundboardMediaPlayer.StartPlayCallback startPlayCallback,
                                @Nullable SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
        MediaPlayerSearchId key = new MediaPlayerSearchId(soundboard, sound);
        SoundboardMediaPlayer existingMediaPlayer = mediaPlayers.get(key);
        if (existingMediaPlayer == null) {
            SoundboardMediaPlayer mediaPlayer = new SoundboardMediaPlayer(initializeCallback, startPlayCallback, stopPlayCallback);
            initMediaPlayer(sound, mediaPlayer);
            mediaPlayers.put(key, mediaPlayer);
        } else {
            // update the callbacks
            existingMediaPlayer.setStartPlayCallback(startPlayCallback);
            existingMediaPlayer.setStopPlayCallback(stopPlayCallback);
            existingMediaPlayer.reset();
            initMediaPlayer(sound, existingMediaPlayer);
        }
    }

    /**
     * Initializes this mediaPlayer for this sound. Does not start playing yet.
     */
    private void initMediaPlayer(@Nonnull Sound sound, SoundboardMediaPlayer mediaPlayer) {
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

    private static final float percentageToVolume(int volumePercentage) {
        return (float) volumePercentage / 100f;
    }

    /**
     * Sets the volume for this <code>mediaPlayer</code>.
     */
    private void setVolume(SoundboardMediaPlayer mediaPlayer, float volume) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setVolume(volume, volume);
    }

    /**
     * Starts to play the song in the Mediaplayer. The Mediaplayer must have been initalized.
     */
    public void startPlaying(@Nullable Soundboard soundboard, @Nonnull Sound sound) {
        checkNotNull(sound, "sound is null");
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        checkNotNull(mediaPlayer, "there is no mediaplayer for Soundboard %s and Sound %s", soundboard, sound);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.prepareAsync();
        }

    }

    public boolean shouldBePlaying(@Nonnull Sound sound) {
        checkNotNull(sound, "sound is null");

        for (Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer> entry : mediaPlayers.entrySet()) {
            // TODO Refactor to stream API?

            if (entry.getKey().getSoundId().equals(sound.getId())) {
                @Nullable SoundboardMediaPlayer mediaPlayer = entry.getValue();
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean shouldBePlaying(@Nonnull Soundboard soundboard, @Nonnull Sound sound) {
        checkNotNull(soundboard, "soundboard is null");
        checkNotNull(sound, "sound is null");

        return shouldBePlaying(soundboard, sound.getId());
    }

    private boolean shouldBePlaying(@Nonnull Soundboard soundboard, @Nonnull UUID soundId) {
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
    private void onPrepared(MediaPlayer player) {
        player.start();
    }

    private boolean onError(SoundboardMediaPlayer player, int what, int extra) {
        removeMediaPlayer(player);
        return true;
    }

    private void onCompletion(SoundboardMediaPlayer player) {
        removeMediaPlayer(player);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (SoundboardMediaPlayer mediaPlayer : mediaPlayers.values()) {
            mediaPlayer.stop();
            removeMediaPlayer(mediaPlayer);
        }
    }
}
