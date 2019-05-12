package de.soundboardcrafter.activity.game.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.List;

import de.soundboardcrafter.model.GameWithSoundboards;

/**
 * Adapter for a GameItem.
 */
class GameListItemAdapter extends BaseAdapter {
    private List<GameWithSoundboards> games;

    GameListItemAdapter(List<GameWithSoundboards> games) {
        this.games = games;
    }

    @Override
    public int getCount() {
        return games.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public GameWithSoundboards getItem(int position) {
        return games.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof GameListItemRow)) {
            convertView = new GameListItemRow(parent.getContext());
        }
        GameListItemRow itemRow = (GameListItemRow) convertView;
        itemRow.setGameWithSoundboards(games.get(position));
        return convertView;
    }
}