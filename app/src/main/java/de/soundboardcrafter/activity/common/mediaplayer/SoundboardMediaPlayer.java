package de.soundboardcrafter.activity.common.mediaplayer;

import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class SoundboardMediaPlayer extends MediaPlayer {
    @FunctionalInterface
    public interface OnSoundboardMediaPlayerCompletionListener {
        void onCompletion(SoundboardMediaPlayer soundboardMediaPlayer);
    }

    @FunctionalInterface
    public interface OnSoundboardMediaPlayerErrorListener {
        boolean onError(SoundboardMediaPlayer soundboardMediaPlayer, int what, int extra);
    }

    @FunctionalInterface
    public interface OnPlayingStopped extends Serializable {
        void stop();
    }

    private float volume;

    /**
     * Name of the sound that's currently played - or the last sound played.
     */
    private @Nullable
    String soundName;

    @Nullable
    private OnPlayingStopped onPlayingStopped;

    SoundboardMediaPlayer() {
        setVolume(1f);
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

    float getVolume() {
        return volume;
    }

    void setVolume(float volume) {
        this.volume = volume;

        setVolume(volume, volume);
    }

    void setOnSoundboardMediaPlayerCompletionListener(
            OnSoundboardMediaPlayerCompletionListener listener) {
        super.setOnCompletionListener(event -> {
            try {
                playingLogicallyStopped();
            } finally {
                listener.onCompletion(this);
            }
        });
    }

    void setOnSoundboardMediaPlayerErrorListener(
            OnSoundboardMediaPlayerErrorListener listener) {
        super.setOnErrorListener((mp, what, extra) -> {
            try {
                playingLogicallyStopped();
            } catch (RuntimeException e) {
                // Seems, playingStartedOrStopped() did not work. Can't do anything about it.
            }

            return listener.onError(this, what, extra);
        });
    }

    void setOnPlayingStopped(@Nullable OnPlayingStopped onPlayingStopped) {
        this.onPlayingStopped = onPlayingStopped;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        playingLogicallyStopped();
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

