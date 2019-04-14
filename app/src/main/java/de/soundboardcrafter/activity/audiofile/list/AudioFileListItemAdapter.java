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
    private List<AudioModel> audioFiles = new ArrayList<>();

    AudioFileListItemAdapter(List<AudioModel> audioFiles) {
        this.audioFiles = audioFiles;
    }

    @Override
    public int getCount() {
        return audioFiles.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AudioModel getItem(int position) {
        return audioFiles.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof SoundboardListItemRow)) {
            convertView = new AudioFileItemRow(parent.getContext());
        }
        AudioFileItemRow itemRow = (AudioFileItemRow) convertView;
        itemRow.setAudioFile(audioFiles.get(position));
        return convertView;
    }
}