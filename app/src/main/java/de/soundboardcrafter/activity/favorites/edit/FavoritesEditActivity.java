package de.soundboardcrafter.activity.favorites.edit;

import static java.util.Objects.requireNonNull;
import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Favorites;

/**
 * Abstract super class for activities for editing favorites.
 */
public class FavoritesEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_FAVORITES_ID = BASE_PACKAGE + ".favoritesId";

    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Favorites favorites) {
        Intent intent = new Intent(packageContext, FavoritesEditActivity.class);
        intent.putExtra(EXTRA_FAVORITES_ID, favorites.getId().toString());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireNonNull(getSupportActionBar()).hide();
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID favoritesId = UUID.fromString(getIntent().getStringExtra(EXTRA_FAVORITES_ID));
        return FavoritesEditFragment.newInstance(favoritesId);
    }

}
