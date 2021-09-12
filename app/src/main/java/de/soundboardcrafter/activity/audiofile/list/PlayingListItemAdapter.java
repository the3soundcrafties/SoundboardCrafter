package de.soundboardcrafter.activity.audiofile.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AudioItem;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Adapter for the list of sounds currently playing.
 */
class PlayingListItemAdapter extends BaseAdapter {
    // FIXME Rename.
    private final List<AudioModelAndSound> audioFolderEntries = new ArrayList<>();

    private final AudioItem.Callback callback;

    /**
     * Creates an adapter that's initially empty
     */
    PlayingListItemAdapter(AudioItem.Callback callback) {
        this.callback = callback;
    }

    // FIXME Rename.
    public void setAudioFolderEntries(Collection<AudioModelAndSound> audioModelsAndSounds) {
        audioFolderEntries.clear();
        audioFolderEntries.addAll(audioModelsAndSounds);

        notifyDataSetChanged();
    }

    // FIXME Rename.
    public ImmutableList<AudioModelAndSound> copyAudioFolderEntries() {
        return ImmutableList.copyOf(audioFolderEntries);
    }

    @Override
    public int getCount() {
        return audioFolderEntries.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AudioModelAndSound getItem(int position) {
        return audioFolderEntries.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        AudioModelAndSound audioModelAndSound = audioFolderEntries.get(position);

        if (!(convertView instanceof AudioItem)) {
            convertView = new AudioItem(parent.getContext());
        }
        AudioItem itemRow = (AudioItem) convertView;

        configureItemRow(itemRow, audioModelAndSound);

        return itemRow;
    }

    private void configureItemRow(@NonNull AudioItem itemRow,
                                  AudioModelAndSound audioModelAndSound) {
        itemRow.setAudioFile(audioModelAndSound, callback);
        itemRow.setImage(R.drawable.ic_play);
    }
}