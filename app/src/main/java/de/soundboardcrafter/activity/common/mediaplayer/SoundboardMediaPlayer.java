package de.soundboardcrafter.activity.common.mediaplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

public class SoundboardMediaPlayer {
    // FIXME Check for changes...

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
    private LoopMediaPlayer2 mediaPlayer;

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
        }
    }

    private void setLooping() throws IOException {
        createNextPlayer();
    }

    private void createNextPlayer() throws IOException {
        setOnCompletionListener();
    }

    float getVolume() {
        return volume;
    }

    void setVolume(float volume) {
        this.volume = volume;
        setVolume();
    }

    private void setVolume() {
    }

    void setDataSource(String dataSourcePath) throws IOException {
    }

    void setDataSource(Context context, AssetFileDescriptor assetFileDescriptor)
            throws IOException {
        mediaPlayer = new LoopMediaPlayer2(context.getApplicationContext(),
                assetFileDescriptor);
        mediaPlayer.start();
    }

    private void setOnCompletionListener() {
    }

    private void setOnErrorListener() {
    }

    void setOnPlayingStopped(@Nullable OnSoundboardMediaPlayerPlayingStopped onPlayingStopped) {
        this.onPlayingStopped = onPlayingStopped;
    }

    void prepareAsync(MediaPlayer.OnPreparedListener onPreparedListener) throws IOException {
    }

    boolean isPlaying() {
        return false;
    }

    public void stop() throws IllegalStateException {
    }

    void reset() {
        prepareAsyncCalled = false;


        // FIXME alternateMediaPlayer?
    }

    void release() {
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

