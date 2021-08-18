package de.soundboardcrafter.activity.common;

import static de.soundboardcrafter.activity.common.ViewUtil.dpToPx;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Static Utility methods for the Tutorial hints.
 */
@ParametersAreNonnullByDefault
public class TutorialUtil {
    private TutorialUtil() {
    }

    @NonNull
    public static TapTargetView.Listener createClickTutorialListener(
            Runnable onClick) {
        return new TapTargetView.Listener() {
            @Override
            public void onTargetClick(TapTargetView tapTargetView) {
                super.onTargetClick(tapTargetView); // dismiss view

                onClick.run();
            }

            @Override
            public void onTargetLongClick(TapTargetView tapTargetView) {
                // Don't dismiss view and don't handle like a single click
            }
        };
    }

    @NonNull
    public static TapTargetView.Listener createLongClickTutorialListener(
            Runnable onLongClick) {
        return new TapTargetView.Listener() {
            @Override
            public void onTargetClick(TapTargetView tapTargetView) {
                // Don't dismiss view
            }

            @Override
            public void onTargetLongClick(TapTargetView tapTargetView) {
                super.onTargetClick(tapTargetView); // dismiss view

                onLongClick.run();
            }
        };
    }

    @UiThread
    public static void showTutorialHint(
            final Activity activity,
            final View view, final int xOffsetDp,
            final int yOffsetDp,
            final int tapTargetRadiusDp,
            boolean fromTheRight,
            int descriptionId,
            TapTargetView.Listener tapTargetViewListener) {
        if (view.getWidth() == 0 && view.getHeight() == 0) {
            return;
        }

        @Nullable final TapTarget tapTargetForBounds =
                tapTargetForBounds(view, xOffsetDp, yOffsetDp, tapTargetRadiusDp,
                        fromTheRight, descriptionId);
        if (tapTargetForBounds == null) {
            return;
        }

        TapTargetView.showFor(activity, tapTargetForBounds, tapTargetViewListener);
    }

    /**
     * Builds a tap target for a tutorial hint for this view with these offsets (in
     * dp).
     *
     * @param fromTheRight Whether the offset shall be subtracted from the right side
     *                     (instead of adding it to the left side)
     */
    @UiThread
    @Nullable
    private static TapTarget tapTargetForBounds(
            final View view, final int xOffsetDp,
            final int yOffsetDp,
            final int tapTargetRadiusDp,
            boolean fromTheRight,
            int descriptionId) {
        @Nullable final Rect tapTargetBounds =
                getTapTargetBounds(view, xOffsetDp, yOffsetDp, tapTargetRadiusDp, fromTheRight);

        if (tapTargetBounds == null) {
            return null;
        }

        return TapTarget.forBounds(
                tapTargetBounds,
                view.getResources().getString(descriptionId))
                .transparentTarget(true)
                .targetRadius(tapTargetRadiusDp);
    }

    /**
     * Gets the bounds for a tutorial hint tap target for this view with these offsets (in
     * dp).
     *
     * @param fromTheRight Whether the offset shall be subtracted from the right side
     *                     (instead of adding it to the left side)
     */
    @Nullable
    @UiThread
    private static Rect getTapTargetBounds(final View view, final int xOffsetDp,
                                           final int yOffsetDp,
                                           final int tapTargetRadiusDp,
                                           boolean fromTheRight) {
        @Nullable final int[] location = getLocation(view, xOffsetDp, yOffsetDp, fromTheRight);
        if (location == null) {
            return null;
        }

        final int tapTargetRadius = dpToPx(view.getContext(), tapTargetRadiusDp);

        return new Rect(location[0] - tapTargetRadius, location[1] - tapTargetRadius,
                location[0] + tapTargetRadius, location[1] + tapTargetRadius);
    }

    /**
     * Calculates a location (x / y in Pixels) within this view with these offsets (in dp).
     *
     * @param fromTheRight whether the offset shall be subtracted from the right side
     *                     (instead of adding it to the left side)
     */
    @Nullable
    @UiThread
    private static int[] getLocation(final View view, int xOffsetDp, int yOffsetDp,
                                     boolean fromTheRight) {
        final int[] location = new int[2];
        view.getLocationOnScreen(location);

        if (location[0] <= 0 && location[1] <= 0) {
            return null;
        }

        final int xOffset = dpToPx(view.getContext(), xOffsetDp);
        location[0] = fromTheRight ?
                location[0] + view.getWidth() - xOffset :
                location[0] + xOffset;
        location[1] += dpToPx(view.getContext(), yOffsetDp);

        return location;
    }
}
