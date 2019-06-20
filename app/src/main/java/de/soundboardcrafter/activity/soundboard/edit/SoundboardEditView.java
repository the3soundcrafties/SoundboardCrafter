package de.soundboardcrafter.activity.soundboard.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import de.soundboardcrafter.R;

/**
 * Custom view for editing a game (name, soundboards etc.).
 */
public class SoundboardEditView extends ConstraintLayout {
    private TextView nameTextView;
    private Button cancel;
    private Button save;


    public SoundboardEditView(Context context) {
        super(context);
        init();
    }

    public SoundboardEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SoundboardEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_soundboard_edit, this);
        nameTextView = findViewById(R.id.nameText);
        cancel = findViewById(R.id.cancel);
        save = findViewById(R.id.save);
    }

    void setOnClickListenerSave(Runnable runnable) {
        save.setOnClickListener(l -> runnable.run());
    }

    void setOnClickListenerCancel(Runnable runnable) {
        cancel.setOnClickListener(l -> runnable.run());
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

    void setButtonsInvisible() {
        save.setVisibility(View.INVISIBLE);
        cancel.setVisibility(View.INVISIBLE);
    }
}
