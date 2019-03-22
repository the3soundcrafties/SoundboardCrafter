package de.soundboardcrafter.activity;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
public class SoundboardItemAdapter extends BaseAdapter {
    private final Soundboard soundboard;
    private static String TAG = MediaPlayerManagerService.class.getName();
    private final MediaPlayerManagerService mediaPlayerManagerService;
    private final Context context;

    SoundboardItemAdapter(@Nonnull Context context, @Nonnull MediaPlayerManagerService mediaPlayerManagerService, @Nonnull Soundboard soundboard) {
        this.soundboard = Preconditions.checkNotNull(soundboard, "soundboard is null");
        this.mediaPlayerManagerService = Preconditions.checkNotNull(mediaPlayerManagerService, "mediaPlayerManagerService!=null");
        this.context = Preconditions.checkNotNull(context, "context is null");
    }

    @Override
    public int getCount() {
        return soundboard.getSounds().size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Sound getItem(int position) {
        return soundboard.getSounds().get(position);
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Sound sound = soundboard.getSounds().get(position);
        if (convertView == null) {
            convertView = new SoundBoardItemRow(parent.getContext());
        }

        // We can now safely cast and set the data
        ((SoundBoardItemRow) convertView).setSound(soundboard, sound, mediaPlayerManagerService);


        return convertView;
    }


}
