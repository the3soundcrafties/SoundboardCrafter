package de.soundboardcrafter.activity.soundboard.selectable;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.SelectableSoundboard;

/**
 * Row for a single soundboard that can be selected.
 */
// TODO Use LinearLayoutCompat?
class SelectableSoundboardListItemRow extends LinearLayout {
    @NonNull
    private final CheckBox checkboxSoundboard;

    SelectableSoundboardListItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_multiselect_item, this, true);
        checkboxSoundboard = findViewById(R.id.checkbox_soundboard);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setSoundboard(SelectableSoundboard soundboard) {
        checkboxSoundboard.setText(soundboard.getSoundboard().getName());
        checkboxSoundboard.setChecked(soundboard.isSelected());

        checkboxSoundboard.setOnClickListener(v -> {
            if (checkboxSoundboard.isEnabled()) {
                soundboard.setSelected(checkboxSoundboard.isChecked());
            }
        });

        // User cannot change provided soundboards
        setEnabled(!soundboard.getSoundboard().isProvided());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        checkboxSoundboard.setEnabled(enabled);
    }
}
