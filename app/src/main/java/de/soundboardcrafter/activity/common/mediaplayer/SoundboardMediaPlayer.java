package de.soundboardcrafter.activity.common.mediaplayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.Serializable;

public class SoundboardMediaPlayer {
    @FunctionalInterface
    public interface OnSoundboardMediaPlayerCompletionListener {
        void onCompletion(SoundboardMediaPlayer soundboardMediaPlayer);
    }

    @FunctionalInterface
    public interface OnSoundboardMediaPlayerErrorListener {
        boolean onError(SoundboardMediaPlayer soundboardMediaPlayer, int what, int extra);
    }

    @FunctionalInterface
    public interface OnSoundboardMediaPlayerPlayingStopped extends Serializable {
        void stop();
    }

    private final MediaPlayer mediaPlayer;

    private float volume;

    /**
     * Name of the sound that's currently played - or the last sound played.
     */
    private @Nullable
    String soundName;

    @Nullable
    private OnSoundboardMediaPlayerPlayingStopped onPlayingStopped;

    SoundboardMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        setVolume(1f);
    }

    void init(Context context, OnSoundboardMediaPlayerErrorListener onErrorListener,
              MediaPlayer.OnPreparedListener onPreparedListener,
              OnSoundboardMediaPlayerCompletionListener onCompletionListener) {
        mediaPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        setOnSoundboardMediaPlayerErrorListener(onErrorListener);
        mediaPlayer.setOnPreparedListener(onPreparedListener);
        setOnSoundboardMediaPlayerCompletionListener(onCompletionListener);
    }

    /**
     * Sets the name of the sound that's currently played - or the last sound played.
     */
    void setSoundName(@NonNull String soundName) {
        this.soundName = soundName;
    }

    /**
     * Returns the name of the sound that's currently played - or the last sound played.
     */
    @Nullable
    String getSoundName() {
        return soundName;
    }

    void setAudioAttributes(AudioAttributes audioAttributes) {
        mediaPlayer.setAudioAttributes(audioAttributes);
    }

    void setLooping(boolean looping) {
        // FIXME User own implementation. This leads to gaps or white noise.
        mediaPlayer.setLooping(looping);
    }

    float getVolume() {
        return volume;
    }

    void setVolume(float volume) {
        this.volume = volume;

        mediaPlayer.setVolume(volume, volume);
    }

    void setDataSource(String path) throws IOException {
        mediaPlayer.setDataSource(path);
    }

    void setDataSource(FileDescriptor fd, long offset, long length) throws IOException {
        mediaPlayer.setDataSource(fd, offset, length);
    }

    private void setOnSoundboardMediaPlayerCompletionListener(
            OnSoundboardMediaPlayerCompletionListener listener) {
        mediaPlayer.setOnCompletionListener(event -> {
            try {
                playingLogicallyStopped();
            } finally {
                listener.onCompletion(this);
            }
        });
    }

    void setOnPreparedListener(MediaPlayer.OnPreparedListener onPreparedListener) {
        mediaPlayer.setOnPreparedListener(onPreparedListener);
    }

    private void setOnSoundboardMediaPlayerErrorListener(
            OnSoundboardMediaPlayerErrorListener listener) {
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            try {
                playingLogicallyStopped();
            } catch (RuntimeException e) {
                // Seems, playingStartedOrStopped() did not work. Can't do anything about it.
            }

            return listener.onError(this, what, extra);
        });
    }

    void setOnPlayingStopped(@Nullable OnSoundboardMediaPlayerPlayingStopped onPlayingStopped) {
        this.onPlayingStopped = onPlayingStopped;
    }

    void prepareAsync() {
        mediaPlayer.prepareAsync();
    }

    boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void stop() throws IllegalStateException {
        mediaPlayer.stop();
        playingLogicallyStopped();
    }

    void reset() {
        mediaPlayer.reset();
    }

    void release() {
        mediaPlayer.release();
    }

    /**
     * This is called / has to be called when playing has logically
     * stopped.
     */
    void playingLogicallyStopped() {
        if (onPlayingStopped != null) {
            onPlayingStopped.stop();
            onPlayingStopped = null;
        }
    }
}

