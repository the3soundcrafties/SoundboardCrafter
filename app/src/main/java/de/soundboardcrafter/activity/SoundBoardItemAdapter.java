package de.soundboardcrafter.activity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
public class SoundboardItemAdapter extends BaseAdapter {
    private final Activity activity;
    private final Soundboard soundboard;

    public SoundboardItemAdapter(Activity activity, Soundboard soundboard) {
        this.activity = activity;
        this.soundboard = Preconditions.checkNotNull(soundboard, "soundboard is null");
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
    public Object getItem(int position) {
        return soundboard.getSounds().get(position);
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Sound sound = soundboard.getSounds().get(position);

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(activity);
            convertView = layoutInflater.inflate(R.layout.linearlayout_soundboarditem, null);
        }

        TextView nameTextView = (TextView) convertView.findViewById(R.id.name);
        nameTextView.setText(sound.getName());
        final Uri path = Uri.fromFile(new File(sound.getPath()));
        convertView.setOnClickListener(l -> OurSoundPlayer.playSound(activity, path));
        return convertView;
    }
}
