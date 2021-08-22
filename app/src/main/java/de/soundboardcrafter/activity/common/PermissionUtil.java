package de.soundboardcrafter.activity.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import de.soundboardcrafter.R;

public class PermissionUtil {
    private static final String SHARED_PREFERENCES =
            PermissionUtil.class.getName() + "_Prefs";

    @NonNull
    public static Intent buildAppSettingsIntent() {
        Intent intent = new Intent();
        intent.setAction(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", "de.soundboardcrafter", null);
        intent.setData(uri);
        return intent;
    }

    public static void showYourSoundsPermissionDialog(FragmentActivity activity, Runnable onOk,
                                                      Runnable onCancel) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.yourSoundsPermissionRationaleTitle)
                .setMessage(R.string.yourSoundsPermissionRationaleMsg)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> onOk.run())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> onCancel.run())
                .setOnDismissListener(dialog -> onCancel.run())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

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
