package de.soundboardcrafter.activity.common.mediaplayer;

import android.media.MediaPlayer;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class SoundboardMediaPlayer extends MediaPlayer {
    @FunctionalInterface
    public interface StopPlayCallback extends Serializable {
        void stop();
    }

    @Nullable
    private StopPlayCallback stopPlayCallback;

    SoundboardMediaPlayer(@Nullable StopPlayCallback stopPlayCallback) {
        super();
        this.stopPlayCallback = stopPlayCallback;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        super.setOnCompletionListener(event -> {
            if (stopPlayCallback != null) {
                stopPlayCallback.stop();
            }
            listener.onCompletion(this);
        });

    }

    void setStopPlayCallback(@Nullable StopPlayCallback stopPlayCallback) {
        this.stopPlayCallback = stopPlayCallback;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        if (stopPlayCallback != null) {
            stopPlayCallback.stop();
        }
    }
}

