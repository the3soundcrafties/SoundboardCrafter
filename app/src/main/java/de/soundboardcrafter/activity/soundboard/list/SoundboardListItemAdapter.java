package de.soundboardcrafter.activity.soundboard.list;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class SoundboardListItemAdapter extends BaseAdapter {
    private final List<SoundboardWithSounds> soundboards;

    private boolean rightPlaceToShowTutorialHints;

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

    void markAsRightPlaceToShowTutorialHints() {
        rightPlaceToShowTutorialHints = true;
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

        if (rightPlaceToShowTutorialHints
                && position == 0
                && tutorialDao.isChecked(TutorialDao.Key.SOUNDBOARD_PLAY_START_SOUND)
                && !tutorialDao.isChecked(TutorialDao.Key.SOUNDBOARD_LIST_CONTEXT_MENU)) {
            // Don't dismiss view
            // dismiss view
            // Simulate a long in the middle of the item
            itemRow.post(() -> {
                @Nullable final Context innerContext = itemRow.getContext();
                if (innerContext instanceof Activity) {
                    // Cache bounds
                    final int[] location = new int[2];
                    itemRow.getTvSoundboardName().getLocationOnScreen(location);
                    location[0] += 30;
                    location[1] += itemRow.getHeight() / 2;

                    Rect bounds = new Rect(location[0] - 30, location[1] - 30,
                            location[0] + 30, location[1] + 30);

                    TapTargetView.showFor((Activity) innerContext,
                            TapTarget.forBounds(bounds,
                                    innerContext.getResources().getString(
                                            R.string.tutorial_soundboard_list_context_menu_description))
                                    .transparentTarget(true),
                            new TapTargetView.Listener() {
                                @Override
                                public void onTargetClick(TapTargetView view) {
                                    // Don't dismiss view
                                }

                                @Override
                                public void onTargetLongClick(TapTargetView view) {
                                    super.onTargetClick(view); // dismiss view

                                    // Simulate a long somewhat middle-left in the list item
                                    itemRow.performLongClick(
                                            100,
                                            itemRow.getHeight() / 2f);
                                }
                            });
                }
            }); // We don't care if return value where false - there is always next time.

            rightPlaceToShowTutorialHints = false;
        }

        return convertView;
    }
}