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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service used for playing media (sounds, actually).
 */
public class MediaPlayerService extends Service {
    private static final String TAG = MediaPlayerService.class.getName();

    private final IBinder binder = new Binder();
    private HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> mediaPlayers = new HashMap<>();

    @MainThread
    public MediaPlayerService() {
        Log.d(TAG, "MediaPlayerService is created");
    }

    @Override
    @UiThread // TODO Or any thread?!
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    @UiThread
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @UiThread // TODO Or any thread?!
    public void setMediaPlayerCallbacks(Soundboard soundboard, Sound sound, SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.setStopPlayCallback(stopPlayCallback);
        }
    }

    /**
     * Sets the volume for this sound.
     */
    @UiThread // TODO Or any thread?!
    public void setVolumePercentage(UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        setVolume(soundId, percentageToVolume(volumePercentage));
    }

    /**
     * Sets the volume for this sound.
     */
    @UiThread // TODO Or any thread?!
    private void setVolume(UUID soundId, float volume) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .forEach(e -> setVolume(e.getValue(), volume));
    }

    @UiThread // TODO Or any thread?!
    public void stopPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.stop();
            removeMediaPlayer(player);
        }
    }

    @UiThread // TODO Or any thread?!
    private void removeMediaPlayer(@NonNull SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.release();
        MediaPlayerSearchId key = findKey(mediaPlayer);
        if (key != null) {
            mediaPlayers.remove(key);
        }
    }

    @Nullable
    private MediaPlayerSearchId findKey(@NonNull SoundboardMediaPlayer toBeFound) {
        checkNotNull(toBeFound, "toBeFound is null");
        Optional<Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer>> foundPlayer =
                mediaPlayers.entrySet().stream().filter(mediaPlayer -> mediaPlayer.getValue().equals(toBeFound)).findFirst();

        return foundPlayer.map(Map.Entry::getKey).orElse(null);

        //if (foundPlayer.isPresent()) {
        //    return foundPlayer.get().getKey();
        //}
        //return null;
    }

    public class Binder extends android.os.Binder {
        @UiThread
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Adds a media player without yet starting it
     */
    @UiThread
    public void initMediaPlayer(@Nullable Soundboard soundboard, @NonNull Sound sound,
                                @Nullable SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
        MediaPlayerSearchId key = new MediaPlayerSearchId(soundboard, sound);
        SoundboardMediaPlayer existingMediaPlayer = mediaPlayers.get(key);
        if (existingMediaPlayer == null) {
            SoundboardMediaPlayer mediaPlayer = new SoundboardMediaPlayer(stopPlayCallback);
            initMediaPlayer(sound, mediaPlayer);
            mediaPlayers.put(key, mediaPlayer);
        } else {
            // update the callbacks
            existingMediaPlayer.setStopPlayCallback(stopPlayCallback);
            existingMediaPlayer.reset();
            initMediaPlayer(sound, existingMediaPlayer);
        }
    }

    /**
     * Initializes this mediaPlayer for this sound. Does not start playing yet.
     */
    @UiThread // TODO Or any thread?!
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
    @UiThread // TODO Or any thread?!
    private void setVolume(SoundboardMediaPlayer mediaPlayer, float volume) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setVolume(volume, volume);
    }

    /**
     * Starts to play the song in the Mediaplayer. The Mediaplayer must have been initalized.
     */
    @UiThread // TODO Or any thread?!
    public void startPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(sound, "sound is null");
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        checkNotNull(mediaPlayer, "there is no mediaplayer for Soundboard %s and Sound %s", soundboard, sound);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.prepareAsync();
        }

    }

    @UiThread // TODO Or any thread?!
    public boolean shouldBePlaying(@NonNull Sound sound) {
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

    @UiThread // TODO Or any thread?!
    public boolean shouldBePlaying(@NonNull Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(soundboard, "soundboard is null");
        checkNotNull(sound, "sound is null");

        return shouldBePlaying(soundboard, sound.getId());
    }

    @UiThread // TODO Or any thread?!
    private boolean shouldBePlaying(@NonNull Soundboard soundboard, @NonNull UUID soundId) {
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
    @UiThread // TODO Or any thread?!
    private void onPrepared(MediaPlayer player) {
        player.start();
    }

    @UiThread // TODO Or any thread?!
    private boolean onError(SoundboardMediaPlayer player, int what, int extra) {
        Log.e(TAG, "Error in media player: what: " + what + " extra: " + extra);

        removeMediaPlayer(player);
        return true;
    }

    @UiThread // TODO Or any thread?!
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
