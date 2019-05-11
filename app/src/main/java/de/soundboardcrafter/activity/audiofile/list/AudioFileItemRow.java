package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
class AudioFileItemRow extends RelativeLayout {
    private static final String TAG = AudioFileItemRow.class.getName();

    public interface Callback {
        public void onEditAudioFile(AudioModelAndSound audioModelAndSound);
    }

    @NonNull
    private final TextView audioName;
    @Nonnull
    private final TextView audioArtist;

    private AudioModelAndSound audioModelAndSound;

    AudioFileItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.audiofile_list_item, this, true);
        audioName = findViewById(R.id.audio_name);
        audioArtist = findViewById(R.id.audio_artist);
    }


    /**
     * Set the data for the view.
     */
    @UiThread
    void setAudioFile(AudioModelAndSound audioFileAndSound, Callback callback) {
        audioModelAndSound = audioFileAndSound;
        audioName.setText(abbreviateName(audioModelAndSound.getName()));
        audioArtist.setText(audioModelAndSound.getAudioModel().getArtist());

        // TODO Choose appropriate image: + or - ?!
        /*
        setImage(is...() ? R.drawable.... : R.drawable....);
        */

        ImageView iconAdd = findViewById(R.id.icon_add);
        iconAdd.setOnClickListener(l -> callback.onEditAudioFile(audioModelAndSound));
    }

    private static String abbreviateName(String name) {
        if (name.length() > 35) {
            return name.substring(0, 35) + "...";
        }

        return name;
    }
}
