package de.soundboardcrafter.activity.favorites.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.FavoritesWithSoundboards;

/**
 * Tile for a favorites instance
 */
class FavoritesListItemRow extends RelativeLayout {
    private static final String EXTRA_FAVORITES_ID = "FavoritesId";
    @NonNull
    private final TextView favoritesName;
    @Nonnull
    private final TextView soundboardCount;

    private FavoritesWithSoundboards favoritesWithSoundboards;

    FavoritesListItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.favorites_list_item, this, true);
        favoritesName = findViewById(R.id.favorites_name);
        soundboardCount = findViewById(R.id.soundboard_count);
    }


    /**
     * Set the data for the view.
     */
    @UiThread
    void setFavoritesWithSoundboards(FavoritesWithSoundboards favoritesWithSoundboards) {
        this.favoritesWithSoundboards = favoritesWithSoundboards;
        favoritesName.setText(favoritesWithSoundboards.getFavorites().getName());
        soundboardCount.setText(getSoundboardCountText());
    }

    private String getSoundboardCountText() {
        int count = favoritesWithSoundboards.getSoundboards().size();
        return getResources().getQuantityString(
                R.plurals.soundboard_count_text,
                count, count);

    }

    FavoritesWithSoundboards getFavoritesWithSoundboards() {
        return favoritesWithSoundboards;
    }
}
