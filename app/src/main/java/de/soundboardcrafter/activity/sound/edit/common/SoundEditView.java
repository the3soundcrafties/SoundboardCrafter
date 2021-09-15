package de.soundboardcrafter.activity.sound.edit.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.soundboard.selectable.SelectableSoundboardListItemAdapter;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Soundboard;

/**
 * Custom view for editing a sound (name, volume etc.).
 */
public class SoundEditView extends ConstraintLayout {
    private TextView nameTextView;
    private Switch loopSwitch;
    private SeekBar volumePercentageSeekBar;
    private ListView soundboardsListView;
    private int maxVolumePercentage;
    private SeekBarChangeListener seekBarChangeListener;
    private OnCheckedChangeListener onCheckedChangeListener;

    /**
     * A callback that notifies clients when the volume percentage has been
     * changed. This includes changes that were initiated by the user
     * as well as changes that were initiated programmatically.
     */
    public interface OnVolumePercentageChangeListener {
        /**
         * Notification that the volume percentage has changed. Clients can use the fromUser
         * parameter to distinguish user-initiated changes from those that occurred
         * programmatically.
         *
         * @param volumePercentage The current volumePercentage. This will be in the
         *                         range 0…max where max was set
         *                         by {@link SoundEditView#setMaxVolumePercentage(int)}.
         *                         (The default value for the max value is 100.)
         * @param fromUser         True if the change was initiated by the user.
         */
        void onVolumePercentageChanged(int volumePercentage, boolean fromUser);
    }

    /**
     * A callback that notifies clients when it has changed whether the sound shall be
     * played in a loop. This includes changes that were initiated by the user
     * as well as changes that were initiated programmatically.
     */
    public interface OnLoopChangeListener {
        /**
         * Notification that it has changed whether the sound shall be
         * played in a loop.
         *
         * @param loop The current value whether the sound shall be played in a loop.
         */
        void onLoopChanged(boolean loop);
    }

    public SoundEditView(Context context) {
        super(context);
        init();
    }

    public SoundEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SoundEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_sound_edit, this);

        nameTextView = findViewById(R.id.nameText);

        volumePercentageSeekBar = findViewById(R.id.volumePercentageSeekBar);
        setMaxVolumePercentage(100);
        seekBarChangeListener = new SeekBarChangeListener();
        volumePercentageSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        loopSwitch = findViewById(R.id.loopSwitch);
        onCheckedChangeListener = new OnCheckedChangeListener();
        loopSwitch.setOnCheckedChangeListener(onCheckedChangeListener);

        soundboardsListView = findViewById(R.id.soundboardsList);

        setEnabled(false);
    }

    @UiThread
    void setMaxVolumePercentage(int maxVolumePercentage) {
        this.maxVolumePercentage = maxVolumePercentage;
        int maxSeekBar = volumePercentageToSeekBar(maxVolumePercentage);
        volumePercentageSeekBar.setMax(maxSeekBar);
    }

    void setVolumePercentage(int volumePercentage) {
        int seekBar = volumePercentageToSeekBar(volumePercentage);
        volumePercentageSeekBar.setProgress(seekBar);
    }

    int getVolumePercentage() {
        return seekBarToVolumePercentage(volumePercentageSeekBar.getProgress());
    }

    /**
     * Sets the soundboards, and the info whether they can be changed.
     */
    void setSoundboards(List<SelectableModel<Soundboard>> soundboards) {
        SelectableSoundboardListItemAdapter adapter =
                new SelectableSoundboardListItemAdapter(soundboards,
                        soundboard -> !soundboard.isProvided());
        soundboardsListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    /**
     * Sets the view's <code>enabled</code>e state.
     */
    @Override
    @UiThread
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        nameTextView.setEnabled(enabled);
        loopSwitch.setEnabled(enabled);
        volumePercentageSeekBar.setEnabled(enabled);
    }

    /**
     * Sets the name of the sound
     */
    @UiThread
    public void setName(String name) {
        nameTextView.setTextKeepState(name);
    }

    /**
     * Gets the name of the sound
     */
    @UiThread
    public String getName() {
        return nameTextView.getText().toString();
    }

    /**
     * Sets whether the sound is played in a loop.
     */
    @UiThread
    void setLoop(boolean loop) {
        loopSwitch.setChecked(loop);
    }

    /**
     * Gets whether the sound is played in a loop.
     */
    @UiThread
    boolean isLoop() {
        return loopSwitch.isChecked();
    }

    private int volumePercentageToSeekBar(int volumePercentage) {
        if (volumePercentage <= 0) {
            return 0;
        }

        // seekBar = log10(volume)
        //int res = (int) (Math.log10((volumePercentage + 100) / 100.0) * 1000.0);

        // seekBar 0 => volume 0
        // seekBar 100 => volumePercentage 100
        // exponentially in between
        int res = Math.toIntExact(Math.round(
                Math.log(volumePercentage * (Math.E - 1.0) / maxVolumePercentage + 1.0) * 100.0));

        return Math.min(res, 100);

    }

    private int seekBarToVolumePercentage(int seekBar) {
        if (seekBar <= 0) {
            return 0;
        }

        // seekBar 0 => volume 0
        // seekBar 100 => volumePercentage 100
        // exponentially in between
        int res = Math.toIntExact(Math.round(
                (Math.pow(Math.E, seekBar / 100.0) - 1.0) / (Math.E - 1.0) * maxVolumePercentage));

        if (res < 0) {
            return 0;
        }

        return Math.min(res, maxVolumePercentage);
    }

    /**
     * Sets a listener to receive notifications of changes to the volume percentage.
     */
    void setOnVolumePercentageChangeListener(OnVolumePercentageChangeListener l) {
        seekBarChangeListener.setOnVolumePercentageChangeListener(l);
    }

    /**
     * Sets a listener to receive notifications of changes to whether the sound shall
     * be looped.
     */
    void setOnLoopChangeListener(OnLoopChangeListener l) {
        onCheckedChangeListener.setOnLoopChangeListener(l);
    }

    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private OnVolumePercentageChangeListener onVolumePercentageChangeListener;

        void setOnVolumePercentageChangeListener(OnVolumePercentageChangeListener l) {
            onVolumePercentageChangeListener = l;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (onVolumePercentageChangeListener == null) {
                return;
            }

            int volumePercentage = seekBarToVolumePercentage(progress);
            onVolumePercentageChangeListener.onVolumePercentageChanged(volumePercentage, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private static class OnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        private OnLoopChangeListener onLoopChangeListener;

        void setOnLoopChangeListener(OnLoopChangeListener l) {
            onLoopChangeListener = l;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (onLoopChangeListener == null) {
                return;
            }

            onLoopChangeListener.onLoopChanged(isChecked);
        }
    }
}
