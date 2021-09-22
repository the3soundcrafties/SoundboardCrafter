package de.soundboardcrafter.activity.soundboard.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
class SoundboardListItemRow extends RelativeLayout {
    @NonNull
    private final TextView soundboardName;
    @Nonnull
    private final TextView soundCount;

    private SoundboardWithSounds soundboard;

    SoundboardListItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_list_item, this, true);
        soundboardName = findViewById(R.id.soundboard_name);
        soundCount = findViewById(R.id.sound_count);
    }

    @Nullable
    Soundboard getSoundboard() {
        return Optional.ofNullable(getSoundboardWithSounds())
                .map(SoundboardWithSounds::getSoundboard).orElse(null);
    }

    @Nullable
    SoundboardWithSounds getSoundboardWithSounds() {
        return soundboard;
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setSoundboard(SoundboardWithSounds soundboard) {
        this.soundboard = soundboard;
        soundboardName.setText(this.soundboard.getSoundboard().getDisplayName());
        soundCount.setText(getSoundCountText());
    }

    private String getSoundCountText() {
        int count = soundboard.getSounds().size();
        return getResources().getQuantityString(
                R.plurals.sound_count_text,
                count, count);
    }
}
