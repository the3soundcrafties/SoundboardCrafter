package de.soundboardcrafter.activity.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;

import de.soundboardcrafter.R;

public class PermissionUtil {
    /**
     * Stores the last shown sounds permission Dialog.
     */
    @Nullable
    private static WeakReference<AlertDialog> yourSoundsPermissionDialogRef;


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
        if (yourSoundsPermissionDialogRef != null) {
            @Nullable final AlertDialog oldDialog = yourSoundsPermissionDialogRef.get();
            if (oldDialog != null && oldDialog.isShowing()) {
                oldDialog.setOnDismissListener(null);
                try {
                    oldDialog.dismiss();
                } catch (IllegalArgumentException e) {
                    // java.lang.IllegalArgumentException:
                    // View=DecorView@6c799f2[SoundboardCreateActivity] not attached to window
                    // manager
                    // is expected in some circumstances...
                }
            }
        }

        yourSoundsPermissionDialogRef = new WeakReference<>(
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.yourSoundsPermissionRationaleTitle)
                        .setMessage(R.string.yourSoundsPermissionRationaleMsg)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> onOk.run())
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> onCancel.run())
                        .setOnDismissListener(dialog -> onCancel.run())
                        .show());
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

    private static boolean getRationaleDisplayStatus(final Context context,
                                                     final String permission) {
        SharedPreferences genPrefs =
                context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return genPrefs.getBoolean(permission, false);
    }

    @NonNull
    public static String calcPermissionToReadAudioFiles() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ?
                Manifest.permission.READ_EXTERNAL_STORAGE :
                Manifest.permission.READ_MEDIA_AUDIO;
    }
}
