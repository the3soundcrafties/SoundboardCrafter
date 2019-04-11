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

