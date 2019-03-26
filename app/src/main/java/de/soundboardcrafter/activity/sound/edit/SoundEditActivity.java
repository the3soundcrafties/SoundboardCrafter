package de.soundboardcrafter.activity.sound.edit;

import android.content.Context;
import android.content.Intent;

import java.util.UUID;

import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Sound;

import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_SOUND_ID = BASE_PACKAGE + ".soundId";

    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Sound sound) {
        Intent intent = new Intent(packageContext, SoundEditActivity.class);
        intent.putExtra(EXTRA_SOUND_ID, sound.getId().toString());
        return intent;
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID soundId = UUID.fromString(getIntent().getStringExtra(EXTRA_SOUND_ID));

        return SoundEditFragment.newInstance(soundId);
    }
}
