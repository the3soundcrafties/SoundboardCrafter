package de.soundboardcrafter.activity.sound.edit.soundboard.play;

import android.content.Context;
import android.content.Intent;

import de.soundboardcrafter.activity.sound.edit.common.AbstractSoundEditActivity;
import de.soundboardcrafter.model.Sound;

/**
 * Activity for editing a single sound (name, volume etc.), used from the currently-playing
 * activity.
 */
public class PlayingSoundEditActivity extends AbstractSoundEditActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Sound sound) {
        Intent intent = new Intent(packageContext, PlayingSoundEditActivity.class);
        putExtras(intent, sound);
        return intent;
    }
}
