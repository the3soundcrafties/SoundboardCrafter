package de.soundboardcrafter.activity.soundboard.edit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;

/**
 * Activity for creating a new soundboard
 */
public class SoundboardCreateActivity extends SingleFragmentActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, SoundboardCreateActivity.class);
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
        return SoundboardEditFragment.newInstance();
    }

}
