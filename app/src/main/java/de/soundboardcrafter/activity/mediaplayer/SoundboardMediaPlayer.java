package de.soundboardcrafter.activity.mediaplayer;

import android.media.MediaPlayer;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class SoundboardMediaPlayer extends MediaPlayer {

    @FunctionalInterface
    public interface InitializeCallback extends Serializable {
        void initialize();
    }

    @FunctionalInterface
    public interface StartPlayCallback extends Serializable {
        void onStartPlaying();
    }

    @FunctionalInterface
    public interface StopPlayCallback extends Serializable {
        void stop();
    }

    @Nullable
    private StartPlayCallback startPlayCallback;

    @Nullable
    private StopPlayCallback stopPlayCallback;

    SoundboardMediaPlayer(@Nullable InitializeCallback initializeCallback, @Nullable StartPlayCallback startPlayCallback, @Nullable StopPlayCallback stopPlayCallback) {
        super();
        if (initializeCallback != null) {
            initializeCallback.initialize();
        }
        this.startPlayCallback = startPlayCallback;
        this.stopPlayCallback = stopPlayCallback;

    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        super.setOnCompletionListener(event -> {
            stopPlayCallback.stop();
            listener.onCompletion(this);
        });
    }

    void setStartPlayCallback(@Nullable StartPlayCallback startPlayCallback) {
        this.startPlayCallback = startPlayCallback;
    }

    void setStopPlayCallback(@Nullable StopPlayCallback stopPlayCallback) {
        this.stopPlayCallback = stopPlayCallback;
    }

    @Override
    public void stop() throws IllegalStateException {
        if (stopPlayCallback != null) {
            stopPlayCallback.stop();
        }
        super.stop();
    }

    @Override
    public void start() throws IllegalStateException {
        if (startPlayCallback != null) {
            startPlayCallback.onStartPlaying();
        }
        super.start();
    }
}

