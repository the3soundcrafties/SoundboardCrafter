package de.soundboardcrafter.activity.soundboard.edit;

import android.content.Context;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AbstractAudioFileRow;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Row in the list of audio files representing one audio file - that can be
 * selected or not. Allows the file to be played and stopped again.
 */
class SoundboardEditSelectableAudioRow extends AbstractAudioFileRow {
    @NonNull
    private final CheckBox chooseFileCheckBox;

    SoundboardEditSelectableAudioRow(Context context) {
        super(context, R.layout.soundboard_edit_audiofile_choose_file);
        chooseFileCheckBox = findViewById(R.id.edit_audiofile_choose_file_check_box);
    }

    void setAudioFile(
            SelectableModel<AbstractAudioFolderEntry> selectableAudioModelAndSound) {
        super.setAudioFile((AudioModelAndSound) selectableAudioModelAndSound.getModel());
        chooseFileCheckBox.setChecked(selectableAudioModelAndSound.isSelected());

        chooseFileCheckBox.setOnClickListener(v -> {
            if (chooseFileCheckBox.isEnabled()) {
                selectableAudioModelAndSound.setSelected(chooseFileCheckBox.isChecked());
            }
        });
    }
}
