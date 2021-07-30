package de.soundboardcrafter.activity.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class PermissionUtil {
    private static final String SHARED_PREFERENCES =
            PermissionUtil.class.getName() + "_Prefs";

    public static boolean androidDoesNotShowPermissionPopupsAnymore(final Activity activity,
                                                                    final String permission) {
        final boolean prevShouldShowStatus = getRationaleDisplayStatus(activity, permission);
        final boolean currShouldShowStatus =
                activity.shouldShowRequestPermissionRationale(permission);
        return prevShouldShowStatus != currShouldShowStatus;
    }

    public static void setShouldShowStatus(final Context context, final String permission) {
        SharedPreferences genPrefs =
                context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = genPrefs.edit();
        editor.putBoolean(permission, true);
        editor.apply();
    }

    public static boolean getRationaleDisplayStatus(final Context context,
                                                    final String permission) {
        SharedPreferences genPrefs =
                context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return genPrefs.getBoolean(permission, false);
    }
}
