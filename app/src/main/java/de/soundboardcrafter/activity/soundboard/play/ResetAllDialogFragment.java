package de.soundboardcrafter.activity.soundboard.play;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import de.soundboardcrafter.R;

/**
 * Fragment showing a yes/no-question dialog whether the sounds shall be reset, that is, deleted and
 * recreated from the file system using default values.
 */
public class ResetAllDialogFragment extends DialogFragment {
    private OnOkCallback onOkCallback;

    @NonNull
    @Override
    @UiThread
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.reset_all_title)
                        .setMessage(R.string.reset_all_message)
                        .setPositiveButton(R.string.reset_all_title_ok, (dialog, which) -> sendResultOK(onOkCallback))
                        .setNegativeButton(R.string.reset_all_title_cancel, null)
                        .create();
    }

    void setOnOkCallback(OnOkCallback onOkCallback) {
        this.onOkCallback = onOkCallback;
    }

    public interface OnOkCallback {
        void ok(int requestCode, int resultCode, Intent data);
    }

    /**
     * Sends the result OK back to the calling Fragment.
     *
     * @param onOkCallback
     */
    @UiThread
    private void sendResultOK(@Nullable OnOkCallback onOkCallback) {
        if (onOkCallback == null) {
            return;
        }
        onOkCallback.ok(getTargetRequestCode(), Activity.RESULT_OK, null);
//        if (getTargetFragment() == null) {
//            return;
//        }
//
//        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
    }
}
