package de.soundboardcrafter.activity.common;

import android.content.Context;
import android.util.TypedValue;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Static GUI utility methods.
 */
@ParametersAreNonnullByDefault
public class ViewUtil {
    private ViewUtil() {
    }

    /**
     * Calculates the pixels from this dp.
     */
    public static int dpToPx(Context context, final int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}
