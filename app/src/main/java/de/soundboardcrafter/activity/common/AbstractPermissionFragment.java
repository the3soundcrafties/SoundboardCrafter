package de.soundboardcrafter.activity.common;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public abstract class AbstractPermissionFragment extends Fragment {
    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 1024;

    @UiThread
    protected boolean isPermissionReadExternalStorageGrantedIfNotAskForIt() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalPermission();

            return false;
        }
        return true;
    }

    // This works, because the fragment is not nested. We *have* to do this here,
    // because our activity won't get the correct requestCode.
    // See https://stackoverflow.com/questions/36170324/receive-incorrect-resultcode-in-activitys
    // -onrequestpermissionsresult-when-reque/36186666 .
    @Override
    @UiThread
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (getActivity() == null) {
            return;
        }

        if (requestCode == REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                PermissionUtil.setShouldShowStatus(requireActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE);
                showPermissionRationale();
                return;
            }

            onPermissionReadExternalStorageGranted();
        }
    }

    protected abstract void onPermissionReadExternalStorageGranted();

    private void showPermissionRationale() {
        PermissionUtil.showYourSoundsPermissionDialog(requireActivity(), this::onOk,
                this::onPermissionReadExternalStorageNotGrantedUserGivesUp);
    }

    private void onOk() {
        if (PermissionUtil
                .androidDoesNotShowPermissionPopupsAnymore(requireActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
            startActivity(PermissionUtil.buildAppSettingsIntent());
        } else {
            requestReadExternalPermission();
        }
    }

    @UiThread
    private void requestReadExternalPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE);
    }

    protected abstract void onPermissionReadExternalStorageNotGrantedUserGivesUp();
}
