package de.soundboardcrafter.activity.soundboard.list;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
public class SoundboardListItemRow extends RelativeLayout {
    public static final String EXTRA_SOUNDBOARD_ID = "SoundboardId";
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

    SoundboardWithSounds getSoundboardWithSounds() {
        return soundboard;
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setSoundboard(SoundboardWithSounds soundboard) {
        this.soundboard = soundboard;
        soundboardName.setText(this.soundboard.getName());
        soundCount.setText(soundboard.getSounds().size() + " Sounds");

        setOnClickListener(l -> {
            Intent intent = new Intent(getContext(), SoundboardPlayActivity.class);
            intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
            getContext().startActivity(intent);
        });

        setOnLongClickListener(l -> {
            // Do NOT consume long clicks.
            // Without this, this context menu on the list view won't work
            return false;
        });
    }


}
