package de.soundboardcrafter.activity.game.edit;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;

/**
 * Abstract super class for activities for editing a single Game
 */
public class GameCreateActivity extends SingleFragmentActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, GameCreateActivity.class);
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        return GameEditFragment.newInstance();
    }

}
