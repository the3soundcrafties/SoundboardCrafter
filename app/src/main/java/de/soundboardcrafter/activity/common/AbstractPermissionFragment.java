package de.soundboardcrafter.activity.common;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public abstract class AbstractPermissionFragment extends Fragment {
    private static final int REQUEST_PERMISSION_TO_READ_AUDIO_FILE = 1024;

    @UiThread
    protected final boolean isPermissionToReadAudioFilesGrantedIfNotAskForIt() {
        if (ContextCompat.checkSelfPermission(requireActivity(),
                PermissionUtil.calcPermissionToReadAudioFiles())
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionToReadAudioFiles();

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

        if (requestCode == REQUEST_PERMISSION_TO_READ_AUDIO_FILE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                PermissionUtil.setShouldShowStatus(requireActivity(),
                        PermissionUtil.calcPermissionToReadAudioFiles());
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
                        PermissionUtil.calcPermissionToReadAudioFiles())) {
            startActivity(PermissionUtil.buildAppSettingsIntent());
        } else {
            requestPermissionToReadAudioFiles();
        }
    }

    @UiThread
    private void requestPermissionToReadAudioFiles() {
        requestPermissions(new String []{PermissionUtil.calcPermissionToReadAudioFiles()},
                REQUEST_PERMISSION_TO_READ_AUDIO_FILE);
    }
    protected abstract void onPermissionReadExternalStorageNotGrantedUserGivesUp();

}
