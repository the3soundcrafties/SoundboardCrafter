package de.soundboardcrafter.activity.game.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.sound.edit.common.SelectableSoundboardListItemAdapter;
import de.soundboardcrafter.model.SelectableSoundboard;

/**
 * Custom view for editing a game (name, soundboards etc.).
 */
public class GameEditView extends ConstraintLayout {
    private TextView nameTextView;
    private ListView soundboardsListView;
    private SelectableSoundboardListItemAdapter adapter;
    private Button cancel;
    private Button save;


    public GameEditView(Context context) {
        super(context);
        init();
    }

    public GameEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_game_edit, this);
        nameTextView = findViewById(R.id.nameText);
        soundboardsListView = findViewById(R.id.soundboardsList);
        cancel = findViewById(R.id.cancel);
        save = findViewById(R.id.save);
        setEnabled(true);
    }

    void setOnClickListenerSave(Runnable runnable) {
        save.setOnClickListener(l -> {
            runnable.run();
        });
    }

    void setOnClickListenerCancel(Runnable runnable) {
        cancel.setOnClickListener(l -> {
            runnable.run();
        });
    }

    /**
     * Sets the soundboards and the info whether they can be changed.
     */
    void setSoundboards(List<SelectableSoundboard> soundboards) {
        // TODO Better re-use an existing adapter?!
        adapter = new SelectableSoundboardListItemAdapter(soundboards, true);
        soundboardsListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    List<SelectableSoundboard> getSelectableSoundboards() {
        ImmutableList.Builder<SelectableSoundboard> res = new ImmutableList.Builder<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            res.add(adapter.getItem(i));
        }
        return res.build();
    }

    /**
     * Sets the view's <code>enabled</code>e state.
     */
    @Override
    @UiThread
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        nameTextView.setEnabled(enabled);
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


}
