package de.soundboardcrafter.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
public class SoundBoardItemRow extends RelativeLayout {
    @NonNull
    private final TextView soundItem;

    private Sound sound;

    public SoundBoardItemRow(Context context) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_item, this, true);

        soundItem = findViewById(R.id.sound_item);
    }

    /**
     * Set the data for the view, and populate the
     * children views with the model text.
     */
    void setSound(Soundboard soundboard, Sound sound, MediaPlayerManagerService service) {
        this.sound = sound;
        soundItem.setText(this.sound.getName());
        setPlayStopIcon(soundboard, this.sound, service);

        setOnClickListener(l -> {
            if (!service.shouldBePlaying(soundboard, this.sound)) {
                service.playSound(soundboard, this.sound,
                        () -> setImage(R.drawable.ic_stop),
                        () -> setImage(R.drawable.ic_play));
            } else {
                service.stopSound(soundboard, this.sound, () -> setImage(R.drawable.ic_play));
            }
        });

        setOnLongClickListener(l -> {
            // Do NOT consume long clicks.
            // Without this, this context menu on the grid view won't work
            return false;
        });
    }

    private void setPlayStopIcon(Soundboard soundboard, Sound sound, MediaPlayerManagerService service) {
        if (service.shouldBePlaying(soundboard, sound)) {
            setImage(R.drawable.ic_stop);
        } else {
            setImage(R.drawable.ic_play);
        }
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
