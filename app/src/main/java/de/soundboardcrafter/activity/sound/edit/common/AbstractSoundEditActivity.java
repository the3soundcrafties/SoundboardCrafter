package de.soundboardcrafter.activity.sound.edit.common;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import javax.annotation.Nullable;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Sound;

import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

/**
 * Abstract super class for activities for editing a single sound (name, volume etc.).
 */
abstract public class AbstractSoundEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_SOUND_ID = BASE_PACKAGE + ".soundId";
    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 1024;

    /**
     * Puts extras for sound and soundboard into the intent.
     */
    protected static void putExtras(Intent intent, Sound sound) {
        intent.putExtra(EXTRA_SOUND_ID, sound.getId().toString());
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID soundId = UUID.fromString(getIntent().getStringExtra(EXTRA_SOUND_ID));

        return SoundEditFragment.newInstance(soundId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        @Nullable final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isPermissionReadExternalStorageGrantedIfNoAskForIt();
    }

    private boolean isPermissionReadExternalStorageGrantedIfNoAskForIt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalPermission();
            return false;
        }
        return true;
    }

    private void showPermissionRationale() {
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


    @Override
    @UiThread
    public void onResume() {
        super.onResume();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    @UiThread
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showPermissionRationale();
                } else {
                    // User denied. Stop the app.
                    // TODO Handle gracefully
                    finishAndRemoveTask();
                    return;
                }
            }
        }
    }
}
