package de.soundboardcrafter.activity.favorites.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.soundboard.selectable.SelectableSoundboardListItemAdapter;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Soundboard;

/**
 * Custom view for editing favorites (name, soundboards etc.).
 */
public class FavoritesEditView extends ConstraintLayout {
    private TextView nameTextView;
    private ListView soundboardsListView;
    private SelectableSoundboardListItemAdapter adapter;
    private Button cancel;
    private Button save;

    public FavoritesEditView(Context context) {
        super(context);
        init();
    }

    public FavoritesEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FavoritesEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_favorites_edit, this);
        nameTextView = findViewById(R.id.nameText);
        soundboardsListView = findViewById(R.id.soundboardsList);
        cancel = findViewById(R.id.cancel);
        save = findViewById(R.id.save);
        setEnabled(true);
    }

    void setOnClickListenerSave(Runnable runnable) {
        save.setOnClickListener(l -> runnable.run());
    }

    void setOnClickListenerCancel(Runnable runnable) {
        cancel.setOnClickListener(l -> runnable.run());
    }

    void setButtonsInvisible() {
        save.setVisibility(View.INVISIBLE);
        cancel.setVisibility(View.INVISIBLE);
    }

    void setSoundboards(List<SelectableModel<Soundboard>> soundboards) {
        adapter = new SelectableSoundboardListItemAdapter(soundboards);
        soundboardsListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    List<SelectableModel<Soundboard>> getSelectableSoundboards() {
        ImmutableList.Builder<SelectableModel<Soundboard>> res = new ImmutableList.Builder<>();
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
