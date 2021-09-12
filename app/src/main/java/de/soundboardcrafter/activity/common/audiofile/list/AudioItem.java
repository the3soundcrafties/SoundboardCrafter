package de.soundboardcrafter.activity.common.audiofile.list;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Item in a list of audio files / sounds representing one audio file.
 * Allows the file to be played and stopped again and to be opened in edit mode.
 */
public class AudioItem extends AbstractAudioFileRow {
    public interface Callback {
        void onEdit(AudioModelAndSound audioModelAndSound);
    }

    @NonNull
    private final View iconEdit;

    public AudioItem(Context context) {
        super(context, R.layout.audio_item);
        iconEdit = findViewById(R.id.icon_sound_edit);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    public void setAudioFile(AudioModelAndSound audioFileAndSound, AudioItem.Callback callback) {
        super.setAudioFile(audioFileAndSound);
        iconEdit.setOnClickListener(l -> callback.onEdit(getAudioModelAndSound()));
    }
}
