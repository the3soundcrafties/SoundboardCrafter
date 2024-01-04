package de.soundboardcrafter.activity.soundboard.play;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.soundboardcrafter.R;

/**
 * Fragment showing a yes/no-question dialog whether all data shall be deleted.
 */
public class ResetAllDialogFragment extends DialogFragment {
    /**
     * Callback interface, an activity that wants to use this fragment has to
     * implement this interface
     */
    public interface OnOkCallback {
        void doResetAll();
    }

    private OnOkCallback onOkCallback;

    @NonNull
    @Override
    @UiThread
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.reset_all_title)
                        .setMessage(R.string.reset_all_message)
                        .setPositiveButton(R.string.reset_all_title_ok, (dialog, which) -> sendResultOK())
                        .setNegativeButton(R.string.reset_all_title_cancel, null)
                        .create();
    }

    // This method is called whenever the fragment is added to
    // an(other) Activity
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener, so we can send events to the host
            onOkCallback = (OnOkCallback) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnOkCallback");
        }
    }

    /**
     * Sends the result OK back to the calling Fragment.
     */
    @UiThread
    private void sendResultOK() {
        if (onOkCallback == null) {
            return;
        }
        onOkCallback.doResetAll();
    }
}
