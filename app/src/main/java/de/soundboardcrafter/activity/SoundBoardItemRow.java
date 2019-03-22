package de.soundboardcrafter.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

public class SoundBoardItemRow extends RelativeLayout {

    private TextView nameTextView;


    public SoundBoardItemRow(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_item, this, true);

        nameTextView = (TextView) findViewById(R.id.name);
    }

    /**
     * Set the data for the view, and populate the
     * children views with the model text.
     */
    void setSound(Soundboard soundboard, Sound sound, MediaPlayerManagerService service) {
        nameTextView.setText(sound.getName());
        setPlayStopIcon(soundboard, sound, service);
        setOnClickListener(l -> {
            if (!service.shouldBePlaying(soundboard, sound)) {
                service.playSound(soundboard, sound,
                        () -> setImage(R.drawable.ic_stop),
                        () -> setImage(R.drawable.ic_play));
            } else {
                service.stopSound(soundboard, sound, () -> setImage(R.drawable.ic_play));
            }
        });
    }

    private void setPlayStopIcon(Soundboard soundboard, Sound sound, MediaPlayerManagerService service) {
        if (service.shouldBePlaying(soundboard, sound)) {
            setImage(R.drawable.ic_stop);
        } else {
            setImage(R.drawable.ic_play);
        }
    }

    private void setImage(int p) {
        nameTextView.setCompoundDrawablesWithIntrinsicBounds(p, 0, 0, 0);
    }
}
