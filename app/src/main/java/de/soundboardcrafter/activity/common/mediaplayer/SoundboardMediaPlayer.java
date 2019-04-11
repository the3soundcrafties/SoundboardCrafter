package de.soundboardcrafter.activity.common.mediaplayer;

import android.media.MediaPlayer;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class SoundboardMediaPlayer extends MediaPlayer {
    @FunctionalInterface
    public interface OnPlayingStopped extends Serializable {
        void stop();
    }

    @Nullable
    private OnPlayingStopped onPlayingStopped;

    SoundboardMediaPlayer() {
        super();
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        super.setOnCompletionListener(event -> {
            if (onPlayingStopped != null) {
                onPlayingStopped.stop();
            }
            listener.onCompletion(this);
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

