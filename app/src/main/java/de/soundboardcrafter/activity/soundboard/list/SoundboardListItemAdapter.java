package de.soundboardcrafter.activity.soundboard.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class SoundboardListItemAdapter extends BaseAdapter {
    private static final String TAG = SoundboardListItemAdapter.class.getName();
    private final List<SoundboardWithSounds> soundboards;

    SoundboardListItemAdapter() {
        soundboards = new ArrayList<>();
    }

    public void setSoundboards(Collection<SoundboardWithSounds> soundboards) {
        this.soundboards.clear();
        this.soundboards.addAll(soundboards);

        notifyDataSetChanged();
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
    public SoundboardWithSounds getItem(int position) {
        return soundboards.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof SoundboardListItemRow)) {
            convertView = new SoundboardListItemRow(parent.getContext());
        }
        SoundboardListItemRow itemRow = (SoundboardListItemRow) convertView;
        itemRow.setSoundboard(soundboards.get(position));
        return convertView;
    }
}