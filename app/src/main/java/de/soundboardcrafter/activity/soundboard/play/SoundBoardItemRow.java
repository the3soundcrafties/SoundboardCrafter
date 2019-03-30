package de.soundboardcrafter.activity.soundboard.play;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
class SoundBoardItemRow extends RelativeLayout {
    private final String TAG = SoundBoardItemRow.class.getName();
    @NonNull
    private final TextView soundItem;
    private Sound sound;
    private SoundboardMediaPlayer.InitializeCallback initializeCallback;
    private SoundboardMediaPlayer.StartPlayCallback startPlayCallback;
    private SoundboardMediaPlayer.StopPlayCallback stopPlayCallback;

    SoundBoardItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_item, this, true);
        soundItem = findViewById(R.id.sound_item);
        initializeCallback = () -> setImage(R.drawable.ic_init_mediaplayer);
        startPlayCallback = () -> setImage(R.drawable.ic_stop);
        stopPlayCallback = () -> {
            setImage(R.drawable.ic_play);
        };
    }

    SoundBoardItemRow(Context context, SoundboardMediaPlayer.InitializeCallback initializeCallback, SoundboardMediaPlayer.StartPlayCallback startPlayCallback, SoundboardMediaPlayer.StopPlayCallback stopPlayCallback) {
        this(context);
        this.initializeCallback = initializeCallback;
        this.startPlayCallback = startPlayCallback;
        this.stopPlayCallback = stopPlayCallback;
    }

    SoundboardMediaPlayer.InitializeCallback getInitializeCallback() {
        return initializeCallback;
    }

    SoundboardMediaPlayer.StartPlayCallback getStartPlayCallback() {
        return startPlayCallback;
    }

    SoundboardMediaPlayer.StopPlayCallback getStopPlayCallback() {
        return stopPlayCallback;
    }


    public interface MediaPlayerServiceCallback {
        boolean isConnected();

        boolean shouldBePlaying(Soundboard soundboard, Sound sound);

        void initMediaPlayer(Soundboard soundboard, Sound sound, SoundboardMediaPlayer.InitializeCallback initializeCallback,
                             SoundboardMediaPlayer.StartPlayCallback playCallback, SoundboardMediaPlayer.StopPlayCallback stopPlayCallback);

        void startPlaying(Soundboard soundboard, Sound sound);

        void setMediaPlayerCallbacks(Soundboard soundboard, Sound sound,
                                     SoundboardMediaPlayer.StartPlayCallback startPlayCallback, SoundboardMediaPlayer.StopPlayCallback stopPlayCallback);

        void stopPlaying(Soundboard soundboard, Sound sound);
    }

    /**
     * Set the data for the view, and populate the
     * children views with the model text.
     */
    void setSound(Soundboard soundboard, Sound sound, MediaPlayerServiceCallback mediaPlayerServiceCallback) {
        if (!mediaPlayerServiceCallback.isConnected()) {
            return;
        }
        this.sound = sound;
        soundItem.setText(this.sound.getName());

        boolean isPlaying = mediaPlayerServiceCallback.shouldBePlaying(soundboard, sound);
        if (isPlaying) {
            setImage(R.drawable.ic_stop);
            mediaPlayerServiceCallback.setMediaPlayerCallbacks(soundboard, sound, startPlayCallback, stopPlayCallback);
        } else {
            setImage(R.drawable.ic_play);
        }

        setOnClickListener(l -> {
            if (!mediaPlayerServiceCallback.shouldBePlaying(soundboard, sound)) {
                mediaPlayerServiceCallback.initMediaPlayer(soundboard, sound, initializeCallback,
                        startPlayCallback, stopPlayCallback);
                mediaPlayerServiceCallback.startPlaying(soundboard, this.sound);
            } else {
                mediaPlayerServiceCallback.setMediaPlayerCallbacks(soundboard, sound, startPlayCallback, stopPlayCallback);
                mediaPlayerServiceCallback.stopPlaying(soundboard, this.sound);
            }
        });

        setOnLongClickListener(l -> {
            // Do NOT consume long clicks.
            // Without this, this context menu on the grid view won't work
            return false;
        });
    }


    private void setImage(int p) {
        soundItem.setCompoundDrawablesWithIntrinsicBounds(p, 0, 0, 0);
    }

    String getSoundName() {
        return getSound().getName();
    }

    Sound getSound() {
        return sound;
    }
}
