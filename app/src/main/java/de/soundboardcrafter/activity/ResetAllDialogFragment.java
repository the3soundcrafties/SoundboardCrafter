package de.soundboardcrafter.activity;

import android.app.Activity;
import android.app.Dialog;
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
    @NonNull
    @Override
    @UiThread
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.reset_all_title)
                        .setMessage(R.string.reset_all_message)
                        .setPositiveButton(R.string.reset_all_title_ok, (dialog, which) -> sendResultOK())
                        .setNegativeButton(R.string.reset_all_title_cancel, null)
                        .create();
    }

    /**
     * Sends the result OK back to the calling Fragment.
     */
    @UiThread
    private void sendResultOK() {
        if (getTargetFragment() == null) {
            return;
        }

        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
    }
}
