package de.soundboardcrafter.activity.common.mediaplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

public class LoopMediaPlayer2 {
    // FIXME Merge with LoopMediaPlayer and remove

    public static final String TAG = LoopMediaPlayer2.class.getSimpleName();

    private final MediaPlayer currentPlayer;
    private final MediaPlayer nextPlayer;
    private final AssetFileDescriptor soundFileDescriptor;
    private float volume;

    LoopMediaPlayer2(Context context, AssetFileDescriptor assetFileDescriptor) throws IOException {
        soundFileDescriptor = // context.getResources().openRawResourceFd(resId);
                assetFileDescriptor;

        final FileDescriptor fileDescriptor = soundFileDescriptor.getFileDescriptor();
        final long startOffset = soundFileDescriptor.getStartOffset();
        final long length = soundFileDescriptor.getLength();

        currentPlayer = new MediaPlayer();
        currentPlayer.setDataSource(fileDescriptor, startOffset, length);
        currentPlayer.prepare();

        nextPlayer = new MediaPlayer();
        nextPlayer.setDataSource(fileDescriptor, startOffset, length);
        nextPlayer.prepare();

        currentPlayer.setNextMediaPlayer(nextPlayer);

        currentPlayer.setOnCompletionListener(
                mediaPlayer -> {
                    try {
                        currentPlayer.reset();
                        currentPlayer.setDataSource(fileDescriptor, startOffset, length);
                        currentPlayer.prepare();
                        nextPlayer.setNextMediaPlayer(currentPlayer);
                    } catch (Exception e) {
                        Log.w(TAG, "onCompletion: unexpected exception", e);
                    }
                });
        nextPlayer.setOnCompletionListener(
                mediaPlayer -> {
                    try {
                        nextPlayer.reset();
                        nextPlayer.setDataSource(fileDescriptor, startOffset, length);
                        nextPlayer.prepare();
                        currentPlayer.setNextMediaPlayer(nextPlayer);
                    } catch (Exception e) {
                        Log.w(TAG, "onCompletion: unexpected exception", e);
                    }
                });
    }

    public void start() {
        try {
            currentPlayer.start();
        } catch (IllegalStateException e) {
            Log.w(TAG, "start() failed: " + e.toString());
        }
    }

    public boolean isPlaying() {
        return currentPlayer.isPlaying() || nextPlayer.isPlaying();
    }

    public void pause() {
        try {
            currentPlayer.pause();
            nextPlayer.pause();
        } catch (Exception e) {
            Log.w(TAG, "pause() failed: " + e.toString());
        }
    }

    public MediaPlayer getMediaPlayer() {
        return currentPlayer;
    }

    public void setNextMediaPlayer(MediaPlayer mediaPlayer) {
        try {
            currentPlayer.setNextMediaPlayer(mediaPlayer);
        } catch (Exception e) {
            Log.w(TAG, "setNextMediaPlayer() failed: ", e);
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;
        try {
            currentPlayer.setVolume(volume, volume);
            nextPlayer.setVolume(volume, volume);
        } catch (Exception e) {
            Log.w(TAG, "setVolume() failed: ", e);
        }
    }

    public void mute() {
        try {
            currentPlayer.setVolume(0, 0);
            nextPlayer.setVolume(0, 0);
        } catch (Exception e) {
            Log.w(TAG, "mute() failed: ", e);
        }
    }

    public void unmute() {
        try {
            currentPlayer.setVolume(volume, volume);
            nextPlayer.setVolume(volume, volume);
        } catch (Exception e) {
            Log.w(TAG, "unmute() failed: ", e);
        }
    }

    public void release() {
        try {
            currentPlayer.release();
            nextPlayer.release();
            soundFileDescriptor.close();
        } catch (Exception e) {
            Log.w(TAG, "release() failed: ", e);
        }
    }
}
