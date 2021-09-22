package de.soundboardcrafter.activity.soundboard.selectable;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.function.Function;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Soundboard;

/**
 * Row for a single soundboard that can be selected.
 */
class SelectableSoundboardListItemRow extends LinearLayout {
    @NonNull
    private final CheckBox checkboxSoundboard;
    private final Function<Soundboard, Boolean> isEnabled;

    @SuppressWarnings("unused")
    SelectableSoundboardListItemRow(Context context) {
        this(context, soundboard -> true);
    }

    SelectableSoundboardListItemRow(Context context,
                                    Function<Soundboard, Boolean> isEnabled) {
        super(context);
        this.isEnabled = isEnabled;
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.soundboard_multiselect_item, this, true);
        checkboxSoundboard = findViewById(R.id.checkbox_soundboard);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setSoundboard(SelectableModel<Soundboard> soundboard) {
        checkboxSoundboard.setText(soundboard.getModel().getDisplayName());

        checkboxSoundboard.setOnCheckedChangeListener(null);

        checkboxSoundboard.setChecked(soundboard.isSelected());

        // User cannot change provided soundboards
        setEnabled(isEnabled.apply(soundboard.getModel()));

        checkboxSoundboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkboxSoundboard.isEnabled()) {
                soundboard.setSelected(isChecked);
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        checkboxSoundboard.setEnabled(enabled);
    }
}
