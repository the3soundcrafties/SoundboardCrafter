package de.soundboardcrafter.activity.soundboard.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.AnywhereInTheFileSystemAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;

/**
 * Custom view for editing a soundboard.
 */
public class SoundboardEditView extends ConstraintLayout {
    private TextView nameTextView;
    private Button cancel;
    private Button save;
    private ImageButton selectionImageButton;
    private ListView listView;
    private ConstraintLayout folderLayout;
    private ImageView iconFolderUp;
    private TextView folderPath;
    private SelectableAudioFileListAdapter adapter;

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
        selectionImageButton = findViewById(R.id.selectionImageButton);
        folderLayout = findViewById(R.id.folderLayout);
        iconFolderUp = findViewById(R.id.icon_folder_up);
        folderPath = findViewById(R.id.folder_path);
        listView = findViewById(R.id.list_view_audiofile);

        initAudioFileListItemAdapter();

        // TODO on icon folder up click

        // TODO on item click
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new SelectableAudioFileListAdapter();
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    void setAudioFolderEntries(
            List<SelectableModel<AbstractAudioFolderEntry>> selectableAudioFolderEntries) {
        // TODO stopPlaying();
        adapter.setAudioFolderEntries(selectableAudioFolderEntries);
    }

    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    void setOnClickListenerSave(Runnable runnable) {
        save.setOnClickListener(view -> runnable.run());
    }

    void setOnClickListenerCancel(Runnable runnable) {
        cancel.setOnClickListener(view -> runnable.run());
    }

    void setOnClickListenerSelection(Runnable runnable) {
        selectionImageButton.setOnClickListener(view -> runnable.run());
    }

    void setSelection(IAudioFileSelection selection) {
        folderPath.setText(selection.getDisplayPath());
        setVisibilityFolder(selection instanceof AnywhereInTheFileSystemAudioLocation ? View.GONE :
                View.VISIBLE);
        if (selection.isRoot()) {
            iconFolderUp.setVisibility(View.INVISIBLE);
        }
    }

    private void setVisibilityFolder(int visibility) {
        folderLayout.setVisibility(visibility);
        iconFolderUp.setVisibility(visibility);
        folderPath.setVisibility(visibility);
    }

    public void setSelectionIcon(Context context, int iconId) {
        selectionImageButton.setImageDrawable(
                ContextCompat.getDrawable(context.getApplicationContext(), iconId));
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
