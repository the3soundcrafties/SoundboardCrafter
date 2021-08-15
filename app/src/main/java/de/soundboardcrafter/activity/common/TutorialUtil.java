package de.soundboardcrafter.activity.common;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Static Utility methods for the Tutorial hints.
 */
@ParametersAreNonnullByDefault
public class TutorialUtil {
    private TutorialUtil() {
    }

    @NonNull
    public static Rect getTapTargetBounds(final View view, final int xOffsetDp,
                                          final int yOffsetDp,
                                          final int tapTargetRadiusDp) {
        final int[] location = TutorialUtil.getLocation(view, xOffsetDp, yOffsetDp);

        final int tapTargetRadius = dp(view.getContext(), tapTargetRadiusDp);

        return new Rect(location[0] - tapTargetRadius, location[1] - tapTargetRadius,
                location[0] + tapTargetRadius, location[1] + tapTargetRadius);
    }

    /**
     * Calculates a location (x / y in Pixels) within this view with these offsets (in dp),
     * starting from the left side.
     */
    @NonNull
    public static int[] getLocation(final View view, int xOffsetDp, int yOffsetDp) {
        return getLocation(view, xOffsetDp, yOffsetDp, false);
    }

    /**
     * Calculates a location (x / y in Pixels) within this view with these offsets (in dp).
     *
     * @param fromTheRight whether the offset is substracted from the right side
     *                     (instead of adding it to the left side)
     */
    @NonNull
    public static int[] getLocation(final View view, int xOffsetDp, int yOffsetDp,
                                    boolean fromTheRight) {
        final int[] location = new int[2];
        view.getLocationOnScreen(location);

        location[0] = fromTheRight ?
                location[0] + view.getWidth() - xOffsetDp :
                location[0] + xOffsetDp;
        location[1] += yOffsetDp;

        return location;
    }

    /**
     * Calculates the pixels from this dp.
     */
    public static int dp(Context context, final int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}
