package de.soundboardcrafter.activity.sound.edit.audiofile.list;

import android.content.Context;
import android.content.Intent;

import de.soundboardcrafter.activity.sound.edit.common.AbstractSoundEditActivity;
import de.soundboardcrafter.model.Sound;

/**
 * Activity for editing a single sound (name, volume etc.), used from the audiofile list activity.
 */
public class AudiofileListSoundEditActivity extends AbstractSoundEditActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Sound sound) {
        Intent intent = new Intent(packageContext, AudiofileListSoundEditActivity.class);
        putExtras(intent, sound);
        return intent;
    }

    @Override
    protected boolean userCanChangeSoundboardSelection() {
        return true;
    }
}
