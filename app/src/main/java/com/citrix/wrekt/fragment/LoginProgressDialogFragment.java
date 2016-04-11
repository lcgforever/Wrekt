package com.citrix.wrekt.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.citrix.wrekt.R;

public class LoginProgressDialogFragment extends DialogFragment {

    public static LoginProgressDialogFragment newInstance() {
        LoginProgressDialogFragment loginProgressDialogFragment = new LoginProgressDialogFragment();
        loginProgressDialogFragment.setRetainInstance(true);
        return loginProgressDialogFragment;
    }

    public LoginProgressDialogFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(getString(R.string.login_progress_dialog_message));
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
