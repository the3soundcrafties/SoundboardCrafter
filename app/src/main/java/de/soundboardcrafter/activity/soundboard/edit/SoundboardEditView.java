package de.soundboardcrafter.activity.soundboard.edit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AnywhereInTheFileSystemAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.BasicAudioModel;

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
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new SelectableAudioFileListAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
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

    void setOnIconFolderUpClickListener(Runnable runnable) {
        iconFolderUp.setOnClickListener(view -> runnable.run());
    }

    void setOnListItemClickListener(AdapterView.OnItemClickListener listener) {
        listView.setOnItemClickListener(listener);
    }

    @ParametersAreNonnullByDefault
    public void setSelectionIcon(Context context, IAudioFileSelection selection) {
        setSelectionIcon(context, getIconId(selection));
    }

    public void notifyListDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    @DrawableRes
    private int getIconId(IAudioFileSelection selection) {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            return R.drawable.ic_long_list;
        } else if (selection instanceof FileSystemFolderAudioLocation) {
            return R.drawable.ic_folder;
        } else if (selection instanceof AssetFolderAudioLocation) {
            return R.drawable.ic_included;
        } else {
            throw new IllegalStateException(
                    "Unexpected type of selection: " + selection.getClass());
        }
    }

    @ParametersAreNonnullByDefault
    public void setSelectionIcon(Context context, @DrawableRes int iconId) {
        selectionImageButton.setImageDrawable(
                ContextCompat.getDrawable(context.getApplicationContext(), iconId));
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

    @UiThread
    void clearSelectableAudioFolderEntries() {
        setSelectableAudioFolderEntries(ImmutableList.of());
    }

    @UiThread
    void setSelectableAudioFolderEntries(
            List<SelectableModel<AbstractAudioFolderEntry>> selectableAudioFolderEntries) {
        adapter.setAudioFolderEntries(selectableAudioFolderEntries);
    }

    public Iterable<BasicAudioModel> getBasicAudioModelsSelected() {
        return adapter.getBasicAudioModelsSelected();
    }

    ImmutableList<AbstractAudioLocation> getAudioLocationsNotSelected() {
        return adapter.getNotSelectedAudioLocations();
    }

    public void setPositionPlaying(@Nullable Integer position) {
        adapter.setPositionPlaying(position);
    }

    public boolean isPlaying(int position) {
        return adapter.isPlaying(position);
    }

    public AbstractAudioFolderEntry getAudioFolderEntry(int position) {
        return adapter.getItem(position).getModel();
    }

    /**
     * Sets the name of the soundboard
     */
    @UiThread
    public void setName(String name) {
        nameTextView.setTextKeepState(name);
    }

    /**
     * Gets the name of the soundboard.
     */
    @UiThread
    public String getName() {
        return nameTextView.getText().toString();
    }

    void setButtonsInvisible() {
        save.setVisibility(View.INVISIBLE);
        cancel.setVisibility(View.INVISIBLE);
    }

    ImageButton getSelectionImageButton() {
        return selectionImageButton;
    }
}
