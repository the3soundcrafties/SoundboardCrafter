package de.soundboardcrafter.activity.common.mediaplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

public class LoopMediaPlayer2 {
    public static final String TAG = LoopMediaPlayer2.class.getSimpleName();

    private final MediaPlayer currentPlayer;
    private final MediaPlayer nextPlayer;
    private final AssetFileDescriptor soundFileDescriptor;
    private float volume;

    LoopMediaPlayer2(Context context, int resId) {
        soundFileDescriptor = context.getResources().openRawResourceFd(resId);

        currentPlayer = MediaPlayer.create(context, resId);
        nextPlayer = MediaPlayer.create(context, resId);
        currentPlayer.setNextMediaPlayer(nextPlayer);

        currentPlayer.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            currentPlayer.reset();
                            currentPlayer.setDataSource(
                                    soundFileDescriptor.getFileDescriptor(),
                                    soundFileDescriptor.getStartOffset(),
                                    soundFileDescriptor.getLength());
                            currentPlayer.prepare();
                            nextPlayer.setNextMediaPlayer(currentPlayer);
                        } catch (Exception e) {
                            Log.w(TAG, "onCompletion: unexpected exception", e);
                        }
                    }
                });
        nextPlayer.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            nextPlayer.reset();
                            nextPlayer.setDataSource(
                                    soundFileDescriptor.getFileDescriptor(),
                                    soundFileDescriptor.getStartOffset(),
                                    soundFileDescriptor.getLength());
                            nextPlayer.prepare();
                            currentPlayer.setNextMediaPlayer(nextPlayer);
                        } catch (Exception e) {
                            Log.w(TAG, "onCompletion: unexpected exception", e);
                        }
                    }
                });
    }

    public static LoopMediaPlayer2 create(Context context, int resId) {
        return new LoopMediaPlayer2(context, resId);
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
