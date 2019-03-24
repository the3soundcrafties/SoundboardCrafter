package de.soundboardcrafter.activity.mediaplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

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

    public void stopPlaying(@Nonnull Soundboard soundboard, @Nonnull Sound sound) {
        Preconditions.checkNotNull(soundboard, "soundboard is null");
        Preconditions.checkNotNull(sound, "sound is null");
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.stop();
            removeMediaPlayer(player);
        }
    }

    private void removeMediaPlayer(@Nonnull SoundboardMediaPlayer mediaPlayer) {
        Preconditions.checkNotNull(mediaPlayer, "mediaPlayer is null");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            MediaPlayerSearchId key = findKey(mediaPlayer);
            if (key != null) {
                mediaPlayers.remove(key);
            }
            mediaPlayer = null;
        }
    }

    @Nullable
    private MediaPlayerSearchId findKey(@Nonnull SoundboardMediaPlayer toBeFound) {
        Preconditions.checkNotNull(toBeFound, "toBeFound is null");
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
     * adds a MediaPlayer but it is not starting it
     *
     * @param soundboard
     * @param sound
     * @param initializeCallback
     * @param startPlayCallback
     * @param stopPlayCallback
     */
    public void initMediaPlayer(@Nonnull Soundboard soundboard, @Nonnull Sound sound,
                                @Nullable SoundboardMediaPlayer.InitializeCallback initializeCallback,
                                @Nullable SoundboardMediaPlayer.StartPlayCallback startPlayCallback,
                                @Nullable SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
        MediaPlayerSearchId key = new MediaPlayerSearchId(soundboard, sound);
        SoundboardMediaPlayer existingMediaPlayer = mediaPlayers.get(key);
        if (existingMediaPlayer == null) {
            SoundboardMediaPlayer mediaPlayer = new SoundboardMediaPlayer(initializeCallback, startPlayCallback, stopPlayCallback);
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setOnErrorListener((ev, what, extra) -> onError(mediaPlayer, what, extra));
            try {
                mediaPlayer.setDataSource(sound.getPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
            mediaPlayer.setVolume((float) sound.getVolumePercentage() / 100f, (float) sound.getVolumePercentage() / 100f);
            mediaPlayer.setLooping(sound.isLoop());
            mediaPlayer.setOnPreparedListener(this::onPrepared);
            mediaPlayer.setOnCompletionListener((mbd) -> onCompletion((SoundboardMediaPlayer) mbd));
            mediaPlayers.put(key, mediaPlayer);
        } else {
            //it should be set here one mre time
            existingMediaPlayer.setStartPlayCallback(startPlayCallback);
            existingMediaPlayer.setStopPlayCallback(stopPlayCallback);
            mediaPlayers.put(key, existingMediaPlayer);
        }

    }

    /**
     * starts to play the song in the Mediaplayer. The Mediaplayer must be initalized
     *
     * @param soundboard
     * @param sound
     */
    public void startPlaying(@Nonnull Soundboard soundboard, @Nonnull Sound sound) {
        Preconditions.checkNotNull(soundboard, "soundboard is null");
        Preconditions.checkNotNull(sound, "sound is null");
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        Preconditions.checkNotNull(mediaPlayer, "there is no mediaplayer for Soundboard %s and Sound %s", soundboard, sound);
        mediaPlayer.prepareAsync();
    }

    public boolean shouldBePlaying(Soundboard soundboard, Sound sound) {
        return mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound)) != null;
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
