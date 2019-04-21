package de.soundboardcrafter.activity.audiofile.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import de.soundboardcrafter.activity.soundboard.list.SoundboardListItemRow;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class AudioFileListItemAdapter extends BaseAdapter {
    private static final String TAG = AudioFileListItemAdapter.class.getName();

    private final AudioFileItemRow.Callback callback;
    private List<AudioModelAndSound> audioModelAndSounds = new ArrayList<>();

    AudioFileListItemAdapter(List<AudioModelAndSound> audioFiles,
                             AudioFileItemRow.Callback callback) {
        audioModelAndSounds = audioFiles;
        this.callback = callback;
    }

    @Override
    public int getCount() {
        return audioModelAndSounds.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AudioModelAndSound getItem(int position) {
        return audioModelAndSounds.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof SoundboardListItemRow)) {
            convertView = new AudioFileItemRow(parent.getContext());
        }
        AudioFileItemRow itemRow = (AudioFileItemRow) convertView;
        itemRow.setAudioFile(audioModelAndSounds.get(position), callback);
        return convertView;
    }
}