package de.soundboardcrafter.activity.soundboard.edit;

import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Soundboard;

/**
 * Abstract super class for activities for editing favorites
 */
public class SoundboardEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_SOUNDBOARD_ID = BASE_PACKAGE + ".soundboardId";

    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Soundboard soundboard) {
        Intent intent = new Intent(packageContext, SoundboardEditActivity.class);
        intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID soundboardId = UUID.fromString(getIntent().getStringExtra(EXTRA_SOUNDBOARD_ID));
        return SoundboardEditFragment.newInstance(soundboardId);
    }

}
