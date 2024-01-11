package de.soundboardcrafter.activity.soundboard.play.playing;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

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
    private static final String TAG = PlayingListItemAdapter.class.getName();

    private final List<AudioModelAndSound> audiosPlaying = new ArrayList<>();

    private final AudioItem.Callback callback;

    /**
     * Creates an adapter that's initially empty
     */
    PlayingListItemAdapter(AudioItem.Callback callback) {
        this.callback = callback;
    }

    void setAudiosPlaying(Collection<AudioModelAndSound> audiosPlaying) {
        this.audiosPlaying.clear();
        this.audiosPlaying.addAll(audiosPlaying);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        Log.v(TAG, "PlayingListItemAdapter#getCount(): " + audiosPlaying.size());

        return audiosPlaying.size();
    }

    @Override
    public long getItemId(int position) {
        Log.v(TAG, "PlayingListItemAdapter#getItemId(" + position + ")");

        return position;
    }

    @Override
    public AudioModelAndSound getItem(int position) {
        Log.v(TAG, "PlayingListItemAdapter#getItem(" + position + ")");

        return audiosPlaying.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Log.v(TAG, "PlayingListItemAdapter#getView(" + position + ", ...)");

        AudioModelAndSound audioModelAndSound = audiosPlaying.get(position);

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
        itemRow.setImage(R.drawable.ic_stop);
    }
}