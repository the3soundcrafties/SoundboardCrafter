package de.soundboardcrafter.activity.favorites.edit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;

/**
 * Abstract super class for activities for editing favorites
 */
public class FavoritesCreateActivity extends SingleFragmentActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, FavoritesCreateActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        return FavoritesEditFragment.newInstance();
    }

}
