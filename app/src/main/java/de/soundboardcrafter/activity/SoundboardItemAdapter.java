package de.soundboardcrafter.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.R;
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
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.linearlayout_soundboarditem, null);
        }

        TextView nameTextView = (TextView) convertView.findViewById(R.id.name);
        ImageView playStopImageView = (ImageView) convertView.findViewById(R.id.play_stop_button);
        setPlayStopIcon(soundboard, sound, playStopImageView);
        nameTextView.setText(sound.getName());
        convertView.setOnClickListener(l -> {
            if (!mediaPlayerManagerService.shouldBePlaying(soundboard, sound)) {
                mediaPlayerManagerService.playSound(soundboard, sound,
                        () -> playStopImageView.setImageResource(R.drawable.ic_stop),
                        () -> playStopImageView.setImageResource(R.drawable.ic_play));
            } else {
                mediaPlayerManagerService.stopSound(soundboard, sound, () -> playStopImageView.setImageResource(R.drawable.ic_play));
            }
        });

        return convertView;
    }

    private void setPlayStopIcon(Soundboard soundboard, Sound sound, ImageView imageView) {
        if (mediaPlayerManagerService.shouldBePlaying(soundboard, sound)) {
            imageView.setImageResource(R.drawable.ic_stop);
        } else {
            imageView.setImageResource(R.drawable.ic_play);
        }
    }
}
