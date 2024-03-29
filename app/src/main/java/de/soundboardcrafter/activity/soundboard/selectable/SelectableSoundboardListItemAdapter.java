package de.soundboardcrafter.activity.soundboard.selectable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Function;

import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Soundboard;

/**
 * Adapter for a {@link SelectableSoundboardListItemRow}.
 */
public class SelectableSoundboardListItemAdapter extends BaseAdapter {
    private final List<SelectableModel<Soundboard>> soundboards;
    private final Function<Soundboard, Boolean> isEnabled;

    public SelectableSoundboardListItemAdapter(List<SelectableModel<Soundboard>> soundboards) {
        this(soundboards, soundboard -> true);
    }

    public SelectableSoundboardListItemAdapter(List<SelectableModel<Soundboard>> soundboards,
                                               Function<Soundboard, Boolean> isEnabled) {
        this.soundboards = soundboards;
        this.isEnabled = isEnabled;
    }

    public ImmutableList<SelectableModel<Soundboard>> copySelectableSoundboards() {
        return ImmutableList.copyOf(soundboards);
    }

    @Override
    public int getCount() {
        return soundboards.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public SelectableModel<Soundboard> getItem(int position) {
        return soundboards.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof SelectableSoundboardListItemRow)) {
            convertView = new SelectableSoundboardListItemRow(parent.getContext(), isEnabled);
        }
        SelectableSoundboardListItemRow itemRow = (SelectableSoundboardListItemRow) convertView;
        itemRow.setSoundboard(soundboards.get(position));
        return convertView;
    }
}
