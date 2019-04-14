package de.soundboardcrafter.activity.common.mediaplayer;

import android.media.MediaPlayer;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class SoundboardMediaPlayer extends MediaPlayer {
    /**
     * Name of the sound that's currently played - or the last sound played.
     */
    private @Nullable
    String soundName;

    @FunctionalInterface
    public interface OnPlayingStopped extends Serializable {
        void stop();
    }

    @Nullable
    private OnPlayingStopped onPlayingStopped;

    SoundboardMediaPlayer() {
        super();
    }

    /**
     * Sets the name of the sound that's currently played - or the last sound played.
     */
    void setSoundName(String soundName) {
        this.soundName = soundName;
    }

    /**
     * Returns the name of the sound that's currently played - or the last sound played.
     */
    @Nullable
    String getSoundName() {
        return soundName;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        super.setOnCompletionListener(event -> {
            try {
                if (onPlayingStopped != null) {
                    onPlayingStopped.stop();
                }
            } finally {
                listener.onCompletion(this);
            }
        });
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        super.setOnErrorListener((mp, what, extra) -> {
            try {
                if (onPlayingStopped != null) {
                    onPlayingStopped.stop();
                }
            } finally {
                return listener.onError(mp, what, extra);
            }
        });
    }

    void setOnPlayingStopped(@Nullable OnPlayingStopped onPlayingStopped) {
        this.onPlayingStopped = onPlayingStopped;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        if (onPlayingStopped != null) {
            onPlayingStopped.stop();
        }
    }
}

