package de.soundboardcrafter.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
class SoundBoardItemRow extends RelativeLayout {
    @NonNull
    private final TextView soundItem;
    private Sound sound;

    SoundBoardItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_item, this, true);

        soundItem = findViewById(R.id.sound_item);
    }

    public interface MediaPlayerServiceCallback {
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
        this.sound = sound;
        soundItem.setText(this.sound.getName());
        SoundboardMediaPlayer.InitializeCallback initializeCallback = () -> setImage(R.drawable.ic_init_mediaplayer);
        SoundboardMediaPlayer.StartPlayCallback startPlayCallback = () -> setImage(R.drawable.ic_stop);
        SoundboardMediaPlayer.StopPlayCallback stopPlayCallback = () -> setImage(R.drawable.ic_play);
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
