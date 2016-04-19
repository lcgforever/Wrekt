package com.citrix.wrekt.fragment.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

public class ProgressDialogFragment extends DialogFragment {

    private static final String EXTRA_MESSAGE_RESOURCE_ID = "EXTRA_MESSAGE_RESOURCE_ID";

    public static ProgressDialogFragment newInstance(int messageResourceId) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setRetainInstance(true);
        Bundle arguments = new Bundle();
        arguments.putInt(EXTRA_MESSAGE_RESOURCE_ID, messageResourceId);
        fragment.setArguments(arguments);
        return fragment;
    }

    public ProgressDialogFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(getString(arguments.getInt(EXTRA_MESSAGE_RESOURCE_ID)));
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
