package de.soundboardcrafter.activity.common.mediaplayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

// See https://stackoverflow.com/questions/26274182/not-able-to-achieve-gapless-audio-looping-so
// -far-on-android
public class LoopMediaPlayer {
    // FIXME Check for differences with original...

    // FIXME Merge with MediaPlayer...

    public static final String TAG = LoopMediaPlayer.class.getSimpleName();

    long mDataSourceOffset;
    long mDataSourceLength;

    private final MediaPlayer currentPlayer;
    private final MediaPlayer nextPlayer;


    LoopMediaPlayer(Context context, FileDescriptor fileDescriptor, long dataSourceOffset,
                    long dataSourceLength) throws IOException {
        currentPlayer = new MediaPlayer();

        currentPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build());
        currentPlayer.setDataSource(fileDescriptor, mDataSourceOffset, mDataSourceLength);
        currentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                currentPlayer.start();
            }
        });
        currentPlayer.prepareAsync();

        nextPlayer = new MediaPlayer();
        nextPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build());
        nextPlayer.setDataSource(fileDescriptor, mDataSourceOffset, mDataSourceLength);
        nextPlayer.prepare();

        currentPlayer.setNextMediaPlayer(nextPlayer);


        currentPlayer.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            currentPlayer.reset();
                            currentPlayer.setDataSource(fileDescriptor, mDataSourceOffset,
                                    mDataSourceLength);
                            currentPlayer.prepare();
                            nextPlayer.setNextMediaPlayer(currentPlayer);
                        } catch (Exception e) {
                            Log.e(TAG, "onCompletion: unexpected exception", e);
                        }
                    }
                });
        nextPlayer.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            nextPlayer.reset();
                            nextPlayer.setDataSource(fileDescriptor, mDataSourceOffset,
                                    mDataSourceLength);
                            nextPlayer.prepare();
                            currentPlayer.setNextMediaPlayer(nextPlayer);
                        } catch (Exception e) {
                            Log.e(TAG, "onCompletion: unexpected exception", e);
                        }
                    }
                });

        currentPlayer.start();
    }
}