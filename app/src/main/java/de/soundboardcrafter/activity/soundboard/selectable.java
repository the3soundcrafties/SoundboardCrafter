package de.soundboardcrafter.activity.soundboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.SelectableSoundboard;

public class selectable {
    /**
     * Adapter for a {@link SelectableSoundboardListItemRow}.
     */
    public static class SelectableSoundboardListItemAdapter extends BaseAdapter {
        private final List<SelectableSoundboard> soundboards;


        public SelectableSoundboardListItemAdapter(List<SelectableSoundboard> soundboards) {
            this.soundboards = soundboards;
        }

        @Override
        public int getCount() {
            return soundboards.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public SelectableSoundboard getItem(int position) {
            return soundboards.get(position);
        }

        @Override
        @UiThread
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            if (!(convertView instanceof SelectableSoundboardListItemRow)) {
                convertView = new SelectableSoundboardListItemRow(parent.getContext());
            }
            SelectableSoundboardListItemRow itemRow = (SelectableSoundboardListItemRow) convertView;
            itemRow.setSoundboard(soundboards.get(position));
            itemRow.setEnabled(true);
            return convertView;
        }
    }

    /**
     * Row for a single soundboard that can be selected.
     */
    // TODO Use LinearLayoutCompat?
    static class SelectableSoundboardListItemRow extends LinearLayout {
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
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            checkboxSoundboard.setEnabled(enabled);
        }
    }
}
