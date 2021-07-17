package de.soundboardcrafter.activity.common;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import java.util.function.Supplier;

abstract public class AbstractTutorialListAdapter extends BaseAdapter {
    private static final int RADIUS = 100;

    private boolean rightPlaceToShowTutorialHints;

    public void markAsRightPlaceToShowTutorialHints() {
        rightPlaceToShowTutorialHints = true;
    }

    protected void showTutorialHintIfNecessary(int position, View itemRow,
                                               View itemNameView,
                                               Supplier<Boolean> condition,
                                               int descriptionId) {
        if (rightPlaceToShowTutorialHints
                && position == 0
                && condition.get()) {
            itemRow.post(() -> {
                @Nullable final Context innerContext = itemRow.getContext();
                if (innerContext instanceof Activity) {
                    // Cache bounds
                    final int[] location = new int[2];
                    itemNameView.getLocationOnScreen(location);
                    location[0] += RADIUS;
                    location[1] += itemRow.getHeight() / 2;

                    Rect bounds = new Rect(location[0] - RADIUS, location[1] - RADIUS,
                            location[0] + RADIUS, location[1] + RADIUS);

                    TapTargetView.showFor((Activity) innerContext,
                            TapTarget.forBounds(bounds,
                                    innerContext.getResources().getString(descriptionId))
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
    }
}
