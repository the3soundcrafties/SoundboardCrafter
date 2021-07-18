package de.soundboardcrafter.activity.common;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@UiThread
abstract public class AbstractTutorialListAdapter extends BaseAdapter {
    private static final int TAP_TARGET_RADIUS_DP = 44;

    private boolean rightPlaceToShowTutorialHints;

    public void markAsRightPlaceToShowTutorialHints() {
        rightPlaceToShowTutorialHints = true;
    }

    protected void showTutorialHintIfNecessary(
            int position, View itemRow,
            Supplier<Boolean> condition, final Consumer<Activity> showTutorialHint) {
        if (rightPlaceToShowTutorialHints && position == 0 && condition.get()) {
            itemRow.post(() -> {
                @Nullable final Context context = itemRow.getContext();
                if (context instanceof Activity) {
                    showTutorialHint.accept((Activity) context);
                }
            }); // We don't care if return value where false - there is always next time.

            rightPlaceToShowTutorialHints = false;
        }
    }

    protected void showTutorialHintForLongClick(Activity activity, View itemRow,
                                                View itemNameView, int descriptionId) {
        TapTargetView.showFor(activity,
                TapTarget.forBounds(
                        getTapTargetBoundsForItemName(activity, itemRow, itemNameView),
                        activity.getResources().getString(descriptionId))
                        .transparentTarget(true)
                        .targetRadius(TAP_TARGET_RADIUS_DP),
                new TapTargetView.Listener() {
                    @Override
                    public void onTargetClick(TapTargetView view) {
                        // Don't dismiss view
                    }

                    @Override
                    public void onTargetLongClick(TapTargetView view) {
                        super.onTargetClick(view); // dismiss view

                        // Simulate a long somewhat middle-left in the list item
                        itemRow.performLongClick(100, itemRow.getHeight() / 2f);
                    }
                });
    }


    protected void showTutorialHintForClick(Activity activity, View view, int descriptionId) {
        TapTargetView.showFor(activity,
                TapTarget.forView(view,
                        activity.getResources().getString(descriptionId)),
                new TapTargetView.Listener() {
                    @Override
                    public void onTargetClick(TapTargetView tapTargetView) {
                        super.onTargetClick(tapTargetView); // dismiss tapTargetView

                        view.performClick();
                    }

                    @Override
                    public void onTargetLongClick(TapTargetView view) {
                        // Don't dismiss view and don't handle like a single click
                    }
                });
    }

    @NonNull
    private Rect getTapTargetBoundsForItemName(Context innerContext, View itemRow,
                                               View itemNameView) {
        final int[] location = getTapTargetLocation(innerContext, itemRow, itemNameView);

        final int tapTargetRadius = dp(innerContext, TAP_TARGET_RADIUS_DP);

        return new Rect(location[0] - tapTargetRadius, location[1] - tapTargetRadius,
                location[0] + tapTargetRadius, location[1] + tapTargetRadius);
    }

    @NonNull
    private int[] getTapTargetLocation(Context context, View itemRow, View itemNameView) {
        final int[] location = new int[2];
        itemNameView.getLocationOnScreen(location);

        location[0] += dp(context, 20);
        location[1] += itemRow.getHeight() / 2.5;
        return location;
    }

    private static int dp(Context context, int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val, context.getResources().getDisplayMetrics());
    }
}
