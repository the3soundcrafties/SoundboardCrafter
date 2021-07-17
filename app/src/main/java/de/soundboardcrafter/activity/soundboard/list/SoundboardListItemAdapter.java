package de.soundboardcrafter.activity.soundboard.list;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.AbstractTutorialListAdapter;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class SoundboardListItemAdapter extends AbstractTutorialListAdapter {
    private final List<SoundboardWithSounds> soundboards;

    SoundboardListItemAdapter() {
        soundboards = new ArrayList<>();
    }

    public void setSoundboards(Collection<SoundboardWithSounds> soundboards) {
        this.soundboards.clear();
        this.soundboards.addAll(soundboards);

        notifyDataSetChanged();
    }

    void remove(SoundboardWithSounds soundboard) {
        soundboards.stream()
                .filter(s -> s.getSoundboard().getId().equals(soundboard.getId()))
                .findFirst()
                .ifPresent(obj -> {
                    soundboards.remove(obj);
                    notifyDataSetChanged();
                });
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
    public SoundboardWithSounds getItem(int position) {
        return soundboards.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof SoundboardListItemRow)) {
            convertView = new SoundboardListItemRow(parent.getContext());
        }
        SoundboardListItemRow itemRow = (SoundboardListItemRow) convertView;

        TutorialDao tutorialDao = TutorialDao.getInstance(itemRow.getContext());

        itemRow.setSoundboard(soundboards.get(position));

        showTutorialHintIfNecessary(position, itemRow, itemRow.getTvSoundboardName(),
                () -> tutorialDao.isChecked(TutorialDao.Key.SOUNDBOARD_PLAY_START_SOUND)
                        && !tutorialDao.isChecked(TutorialDao.Key.SOUNDBOARD_LIST_CONTEXT_MENU),
                R.string.tutorial_soundboard_list_context_menu_description);

        return convertView;
    }

}