package de.soundboardcrafter.activity.common.mediaplayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

public class SoundboardMediaPlayer {
    private static final String TAG = SoundboardMediaPlayer.class.getName();

    private AudioAttributes audioAttributes;
    private String dataSourcePath;
    private FileDescriptor dataSourceFileDescriptor;
    private long dataSourceOffset;
    private long dataSourceLength;
    private boolean looping;
    private boolean prepareAsyncCalled;

    @FunctionalInterface
    public interface OnSoundboardMediaPlayerCompletionListener {
        void onCompletion(SoundboardMediaPlayer soundboardMediaPlayer);
    }

    @FunctionalInterface
    public interface OnSoundboardMediaPlayerErrorListener {
        boolean onError(int what, int extra);
    }

    @FunctionalInterface
    public interface OnSoundboardMediaPlayerPlayingStopped extends Serializable {
        void stop();
    }

    private Context context;

    @Nonnull
    private MediaPlayer mediaPlayer;

    // See https://stackoverflow.com/questions/26274182/not-able-to-achieve-gapless-audio-looping
    // -so-far-on-android .
    @Nullable
    private MediaPlayer nextMediaPlayer;

    private float volume;

    /**
     * Name of the sound that's currently played - or the last sound played.
     */
    @Nullable
    private String soundName;

    private OnSoundboardMediaPlayerErrorListener onErrorListener;
    private OnSoundboardMediaPlayerCompletionListener onCompletionListener;

    @Nullable
    private OnSoundboardMediaPlayerPlayingStopped onPlayingStopped;

    SoundboardMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        setVolume(1f);
    }

    void init(Context context, OnSoundboardMediaPlayerErrorListener onErrorListener,
              OnSoundboardMediaPlayerCompletionListener onCompletionListener) {
        this.context = context.getApplicationContext();
        this.onErrorListener = onErrorListener;
        this.onCompletionListener = onCompletionListener;

        init();
    }

    private void init() {
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        setOnErrorListener();
        setOnCompletionListener();
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
        this.audioAttributes = audioAttributes;
        setAudioAttributes(mediaPlayer);

        // FIXME alternateMediaPlayer?
    }

    private void setAudioAttributes(MediaPlayer mp) {
        mp.setAudioAttributes(audioAttributes);
    }

    void setLooping(boolean looping) throws IOException {
        this.looping = looping;

        if (!prepareAsyncCalled) {
            return;
        }

        if (looping) {
            setLooping();
        } else {
            unsetLooping();
        }
    }

    private void setLooping() throws IOException {
        if (nextMediaPlayer != null) {
            return;
        }

        createNextPlayer();
    }

    private void createNextPlayer() throws IOException {
        nextMediaPlayer = new MediaPlayer();
        nextMediaPlayer.setVolume(volume, volume);
        setDataSource(nextMediaPlayer);
        setAudioAttributes(nextMediaPlayer);

        mediaPlayer.setNextMediaPlayer(nextMediaPlayer);

        setOnCompletionListener();
    }

    private void unsetLooping() {
        if (nextMediaPlayer == null) {
            return;
        }

        mediaPlayer.setNextMediaPlayer(null);
        nextMediaPlayer.stop();
        nextMediaPlayer.release();
        nextMediaPlayer = null;
    }

    float getVolume() {
        return volume;
    }

    void setVolume(float volume) {
        this.volume = volume;
        setVolume();
    }

    private void setVolume() {
        mediaPlayer.setVolume(volume, volume);
        if (nextMediaPlayer != null) {
            nextMediaPlayer.setVolume(volume, volume);
        }
    }

    void setDataSource(String dataSourcePath) throws IOException {
        dataSourceFileDescriptor = null;
        dataSourceOffset = 0;
        dataSourceLength = 0;
        this.dataSourcePath = dataSourcePath;

        setDataSource(mediaPlayer);

        if (nextMediaPlayer != null) {
            setDataSource(nextMediaPlayer);
        }
    }

    void setDataSource(FileDescriptor dataSourceFileDescriptor, long dataSourceOffset,
                       long dataSourceLength) throws IOException {
        this.dataSourceFileDescriptor = dataSourceFileDescriptor;
        this.dataSourceOffset = dataSourceOffset;
        this.dataSourceLength = dataSourceLength;
        dataSourcePath = null;

        setDataSource(mediaPlayer);

        if (nextMediaPlayer != null) {
            setDataSource(nextMediaPlayer);
        }
    }

    private void setDataSource(MediaPlayer mp) throws IOException {
        if (dataSourcePath != null) {
            mp.setDataSource(dataSourcePath);
        } else {
            mp.setDataSource(dataSourceFileDescriptor, dataSourceOffset, dataSourceLength);
        }
    }

    private void setOnCompletionListener() {
        mediaPlayer.setOnCompletionListener(event -> {
            if (nextMediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = nextMediaPlayer;

                try {
                    createNextPlayer();
                } catch (IOException e) {
                    Log.e(TAG, "Exception creating media player: " + e.getMessage(), e);

                    if (nextMediaPlayer != null) {
                        nextMediaPlayer.release();
                        nextMediaPlayer = null;
                    }
                }
            } else {
                try {
                    playingLogicallyStopped();
                } finally {
                    onCompletionListener.onCompletion(this);
                }
            }
        });
    }

    private void setOnErrorListener() {
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            if (nextMediaPlayer != null) {
                nextMediaPlayer.setNextMediaPlayer(null);
                nextMediaPlayer.stop();
                nextMediaPlayer.release();
                nextMediaPlayer = null;
            }

            try {
                playingLogicallyStopped();
            } catch (RuntimeException e) {
                // Seems, playingStartedOrStopped() did not work. Can't do anything about it.
            }

            return onErrorListener.onError(what, extra);
        });

        // FIXME How to configure alternateMediaPlayer?
    }

    void setOnPlayingStopped(@Nullable OnSoundboardMediaPlayerPlayingStopped onPlayingStopped) {
        this.onPlayingStopped = onPlayingStopped;
    }

    void prepareAsync(MediaPlayer.OnPreparedListener onPreparedListener) throws IOException {
        mediaPlayer.setOnPreparedListener(onPreparedListener);

        if (looping) {
            // FIXME Wrong place?   createNextPlayer();
        }

        mediaPlayer.prepareAsync();

        prepareAsyncCalled = true;

        // FIXME Nothing to do for the nextPlayer?
    }

    boolean isPlaying() {
        return mediaPlayer.isPlaying() || nextMediaPlayer != null;
    }

    public void stop() throws IllegalStateException {
        mediaPlayer.setNextMediaPlayer(null);

        mediaPlayer.stop();

        if (nextMediaPlayer != null) {
            nextMediaPlayer.stop();
            nextMediaPlayer.release();
            nextMediaPlayer = null;
        }

        playingLogicallyStopped();
    }

    void reset() {
        mediaPlayer.setNextMediaPlayer(null);

        mediaPlayer.reset();

        if (nextMediaPlayer != null) {
            nextMediaPlayer.stop();
            nextMediaPlayer.release();
            nextMediaPlayer = null;
        }

        prepareAsyncCalled = false;


        // FIXME alternateMediaPlayer?
    }

    void release() {
        mediaPlayer.release();

        // FIXME correct?! (Can lead to double releases...)
        if (nextMediaPlayer != null) {
            nextMediaPlayer.release();
        }
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

