package de.soundboardcrafter.activity.soundboard.edit;

import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Soundboard;

/**
 * Activity for editing or copying a soundboard
 */
public class SoundboardEditOrCopyActivity extends SingleFragmentActivity {
    private static final String EXTRA_SOUNDBOARD_ID = BASE_PACKAGE + ".soundboardId";
    private static final String EXTRA_COPY = BASE_PACKAGE + ".copy";

    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     *
     * @param copy Whether to copy the soundboard (instead of editing it)
     */
    public static Intent newIntent(Context packageContext, Soundboard soundboard, boolean copy) {
        Intent intent = new Intent(packageContext, SoundboardEditOrCopyActivity.class);
        intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());
        intent.putExtra(EXTRA_COPY, copy);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        @Nullable final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID soundboardId = UUID.fromString(getIntent().getStringExtra(EXTRA_SOUNDBOARD_ID));
        boolean copy = getIntent().getBooleanExtra(EXTRA_COPY, false);
        return SoundboardEditFragment.newInstance(soundboardId, copy);
    }

}
