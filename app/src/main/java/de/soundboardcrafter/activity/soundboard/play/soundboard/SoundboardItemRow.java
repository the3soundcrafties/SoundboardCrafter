package de.soundboardcrafter.activity.soundboard.play.soundboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.google.common.base.Strings;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
class SoundboardItemRow extends RelativeLayout {
    @NonNull
    private final TextView soundItem;

    SoundboardItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_item, this, true);
        soundItem = findViewById(R.id.sound_item);

        // This is work-around: Without setting max lines set
        // (and to which value?), texts that are too long
        // will only be truncated, not ellipsized.
        // See https://stackoverflow.com/questions/14173776/ellipsize-not-working-properly-for-a
        // -multiline-textview-with-an-arbitrary-maximu .
        ViewTreeObserver observer = soundItem.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int maxLines = (int) soundItem.getHeight() / soundItem.getLineHeight();
                final String text = soundItem.getText().toString();
                if (!Strings.isNullOrEmpty(text)) {
                    // Seems to be necessary in case of orientation changes.
                    soundItem.setText(text);
                }

                soundItem.setMaxLines(maxLines);

                soundItem.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public interface MediaPlayerServiceCallback {
        boolean isConnected();

        /**
         * Returns whether this sound is <i>actively playing</i> in this soundboard, that is,
         * it is playing <i>and not fading out</i>.
         */
        boolean isActivelyPlaying(Soundboard soundboard, Sound sound);

        void setOnPlayingStopped(Soundboard soundboard, Sound sound,
                                 SoundboardMediaPlayer.OnSoundboardMediaPlayerPlayingStopped onPlayingStopped);

        /**
         * Stops the sound when it is played in this soundboard.
         *
         * @param fadeOut Whether the sound shall be faded out.
         */
        void stopPlaying(Soundboard soundboard, Sound sound, boolean fadeOut);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setSound(Soundboard soundboard, Sound sound,
                  MediaPlayerServiceCallback mediaPlayerServiceCallback) {
        if (!mediaPlayerServiceCallback.isConnected()) {
            return;
        }

        soundItem.setText(sound.getName());

        setImage(mediaPlayerServiceCallback.isActivelyPlaying(soundboard, sound) ?
                R.drawable.ic_stop : R.drawable.ic_play);
    }

    @UiThread
    void setImage(int p) {
        soundItem.setCompoundDrawablesWithIntrinsicBounds(p, 0, 0, 0);
    }
}
