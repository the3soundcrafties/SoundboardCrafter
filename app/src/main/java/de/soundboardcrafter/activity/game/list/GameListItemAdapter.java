package de.soundboardcrafter.activity.game.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import de.soundboardcrafter.model.Game;

/**
 * Adapter for a GameItem.
 */
class GameListItemAdapter extends BaseAdapter {
    private static final String TAG = GameListItemAdapter.class.getName();
    private List<Game> games = new ArrayList<>();

    GameListItemAdapter(List<Game> games) {
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
    public Game getItem(int position) {
        return games.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof GameListItemRow)) {
            convertView = new GameListItemRow(parent.getContext());
        }
        GameListItemRow itemRow = (GameListItemRow) convertView;
        itemRow.setGame(games.get(position));
        return convertView;
    }
}