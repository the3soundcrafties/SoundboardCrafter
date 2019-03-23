package de.soundboardcrafter.activity.sound.edit;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Sound;

import static de.soundboardcrafter.common.ActivityConstants.BASE_PACKAGE;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_SOUND = BASE_PACKAGE + ".SOUND";

    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Sound sound) {
        Intent intent = new Intent(packageContext, SoundEditActivity.class);
        intent.putExtra(EXTRA_SOUND, sound);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Sound sound = (Sound) getIntent().getSerializableExtra(EXTRA_SOUND);

        return SoundEditFragment.newInstance(sound);
    }
}
