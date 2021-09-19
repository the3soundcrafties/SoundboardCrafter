package de.soundboardcrafter.activity.sound.edit.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.slider.Slider;

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
    private Slider volumePercentageSlider;
    private ListView soundboardsListView;
    private SliderChangeListener sliderChangeListener;
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
         *                         range 0…100.
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

        volumePercentageSlider = findViewById(R.id.volumePercentageSlider);
        sliderChangeListener = new SliderChangeListener();
        volumePercentageSlider.addOnChangeListener(sliderChangeListener);

        loopSwitch = findViewById(R.id.loopSwitch);
        onCheckedChangeListener = new OnCheckedChangeListener();
        loopSwitch.setOnCheckedChangeListener(onCheckedChangeListener);

        soundboardsListView = findViewById(R.id.soundboardsList);

        setEnabled(false);
    }

    void setVolumePercentage(int volumePercentage) {
        int sliderValue = volumePercentageToSliderValue(volumePercentage);
        volumePercentageSlider.setValue(sliderValue);
    }

    int getVolumePercentage() {
        return sliderValueToVolumePercentage(volumePercentageSlider.getValue());
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
        volumePercentageSlider.setEnabled(enabled);
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

    private int volumePercentageToSliderValue(int volumePercentage) {
        if (volumePercentage <= 0) {
            return 0;
        }

        // seekBar = log10(volume)
        //int res = (int) (Math.log10((volumePercentage + 100) / 100.0) * 1000.0);

        // seekBar 0 => volume 0
        // seekBar 100 => volumePercentage 100
        // exponentially in between
        int res = Math.toIntExact(Math.round(
                Math.log(volumePercentage * (Math.E - 1.0) / 100.0 + 1.0) * 100.0));

        return Math.min(res, 100);

    }

    private int sliderValueToVolumePercentage(float sliderValue) {
        if (sliderValue <= 0) {
            return 0;
        }

        // sliderValue 0 => volume 0
        // sliderValue 100 => volumePercentage 100
        // exponentially in between
        int res = Math.toIntExact(Math.round(
                (Math.pow(Math.E, sliderValue / 100.0) - 1.0) / (Math.E - 1.0) * 100.0));

        if (res < 0) {
            return 0;
        }

        return Math.min(res, 100);
    }

    /**
     * Sets a listener to receive notifications of changes to the volume percentage.
     */
    void setOnVolumePercentageChangeListener(OnVolumePercentageChangeListener l) {
        sliderChangeListener.setOnVolumePercentageChangeListener(l);
    }

    /**
     * Sets a listener to receive notifications of changes to whether the sound shall
     * be looped.
     */
    void setOnLoopChangeListener(OnLoopChangeListener l) {
        onCheckedChangeListener.setOnLoopChangeListener(l);
    }

    private class SliderChangeListener implements Slider.OnChangeListener {
        private OnVolumePercentageChangeListener onVolumePercentageChangeListener;

        void setOnVolumePercentageChangeListener(OnVolumePercentageChangeListener l) {
            onVolumePercentageChangeListener = l;
        }

        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            if (onVolumePercentageChangeListener == null) {
                return;
            }

            int volumePercentage = sliderValueToVolumePercentage(value);
            onVolumePercentageChangeListener.onVolumePercentageChanged(volumePercentage, fromUser);
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
