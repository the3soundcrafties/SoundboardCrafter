package de.soundboardcrafter.activity.favorites.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.List;

import de.soundboardcrafter.model.FavoritesWithSoundboards;

/**
 * Adapter for favorites.
 */
class FavoritesListItemAdapter extends BaseAdapter {
    private final List<FavoritesWithSoundboards> favoritesWithSoundboards;

    FavoritesListItemAdapter(List<FavoritesWithSoundboards> favoritesWithSoundboards) {
        this.favoritesWithSoundboards = favoritesWithSoundboards;
    }

    @Override
    public int getCount() {
        return favoritesWithSoundboards.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public FavoritesWithSoundboards getItem(int position) {
        return favoritesWithSoundboards.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof FavoritesListItemRow)) {
            convertView = new FavoritesListItemRow(parent.getContext());
        }
        FavoritesListItemRow itemRow = (FavoritesListItemRow) convertView;

        itemRow.setFavoritesWithSoundboards(favoritesWithSoundboards.get(position));

        return convertView;
    }

    public void remove(FavoritesWithSoundboards favoritesWithSoundboards) {
        this.favoritesWithSoundboards.stream()
                .filter(g -> g.getFavorites().getId()
                        .equals(favoritesWithSoundboards.getFavorites().getId()))
                .findFirst()
                .ifPresent(obj -> {
                    this.favoritesWithSoundboards.remove(obj);
                    notifyDataSetChanged();
                });
    }
}