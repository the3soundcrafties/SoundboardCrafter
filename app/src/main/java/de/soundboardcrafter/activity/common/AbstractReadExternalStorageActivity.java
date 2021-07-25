package de.soundboardcrafter.activity.common;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import de.soundboardcrafter.R;

/**
 * Abstract superclass for activities that need to read external storage.
 */
public abstract class AbstractReadExternalStorageActivity extends AppCompatActivity {
    protected static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 1024;

    protected boolean isPermissionReadExternalStorageGrantedIfNoAskForIt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalPermission();
            return false;
        }
        return true;
    }

    protected void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.yourSoundsPermissionRationaleTitle)
                .setMessage(R.string.yourSoundsPermissionRationaleMsg)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> requestReadExternalPermission())
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void requestReadExternalPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE);
    }

}
