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

    public static final String TAG = LoopMediaPlayer.class.getSimpleName();

    private Context mContext = null;
    private final FileDescriptor mFileDescriptor;
    long mDataSourceOffset;
    long mDataSourceLength;
    private int mCounter = 1;

    private MediaPlayer mCurrentPlayer = null;
    private MediaPlayer mNextPlayer = null;


    LoopMediaPlayer(Context context, FileDescriptor fileDescriptor, long dataSourceOffset,
                    long dataSourceLength) throws IOException {
        mContext = context;
        mFileDescriptor = fileDescriptor;
        mDataSourceOffset = dataSourceOffset;
        mDataSourceLength = dataSourceLength;

        mCurrentPlayer = new MediaPlayer();
        mCurrentPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build());
        mCurrentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mCurrentPlayer.start();
            }
        });
        mCurrentPlayer.setDataSource(fileDescriptor, mDataSourceOffset, mDataSourceLength);
        mCurrentPlayer.prepareAsync();

        createNextMediaPlayer();
    }

    private void createNextMediaPlayer() throws IOException {
        mNextPlayer = new MediaPlayer();
        mNextPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build());
        mNextPlayer.setDataSource(mFileDescriptor, mDataSourceOffset, mDataSourceLength);
        mNextPlayer.prepare();

        mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
        mCurrentPlayer.setOnCompletionListener(onCompletionListener);
    }

    private final MediaPlayer.OnCompletionListener onCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.release();
                    mCurrentPlayer = mNextPlayer;

                    try {
                        createNextMediaPlayer();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }

                    Log.d(TAG, String.format("Loop #%d", ++mCounter));
                }
            };
}