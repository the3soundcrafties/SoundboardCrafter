package de.soundboardcrafter.activity.sound.edit.common;

import android.content.Intent;
import android.media.AudioManager;

import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Sound;

import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

/**
 * Abstract super class for activities for editing a single sound (name, volume etc.).
 */
abstract public class AbstractSoundEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_SOUND_ID = BASE_PACKAGE + ".soundId";

    /**
     * Puts extras for sound and soundboard into the intent.
     */
    protected static void putExtras(Intent intent, Sound sound) {
        intent.putExtra(EXTRA_SOUND_ID, sound.getId().toString());
    }

    @Override
    @UiThread
    public void onResume() {
        super.onResume();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID soundId = UUID.fromString(getIntent().getStringExtra(EXTRA_SOUND_ID));

        return SoundEditFragment.newInstance(soundId);
    }

}
