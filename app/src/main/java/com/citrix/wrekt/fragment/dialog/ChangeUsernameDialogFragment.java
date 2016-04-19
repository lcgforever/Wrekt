package com.citrix.wrekt.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;

import com.citrix.wrekt.R;

public class ChangeUsernameDialogFragment extends DialogFragment {

    private static final String EXTRA_USERNAME = "EXTRA_USERNAME";

    private TextInputLayout changeUsernameTextInputLayout;
    private TextInputEditText changeUsernameEditText;

    private ChangeUsernameActionListener listener;
    private boolean usernameValid;

    public static ChangeUsernameDialogFragment newInstance(String username) {
        ChangeUsernameDialogFragment fragment = new ChangeUsernameDialogFragment();
        fragment.setRetainInstance(true);
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_USERNAME, username);
        fragment.setArguments(arguments);
        return fragment;
    }

    public ChangeUsernameDialogFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ChangeUsernameActionListener) activity;
        } catch (ClassCastException exception) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + ChangeUsernameActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setupViews();

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_username_dialog_title)
                .setCancelable(false)
                .setView(changeUsernameTextInputLayout)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (listener != null) {
                                    if (usernameValid) {
                                        listener.onUsernameChanged(changeUsernameEditText.getText().toString());
                                    } else {
                                        Toast.makeText(getActivity(),
                                                R.string.change_username_failed_message,
                                                Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                    dialog.dismiss();
                                }
                            }
                        })
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void setupViews() {
        changeUsernameTextInputLayout = (TextInputLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.layout_change_username_dialog, null);
        changeUsernameEditText
                = (TextInputEditText) changeUsernameTextInputLayout.findViewById(R.id.change_username_edit_text);
        Bundle arguments = getArguments();
        String oldUsername = arguments.getString(EXTRA_USERNAME, "");
        usernameValid = !TextUtils.isEmpty(oldUsername) && oldUsername.length() >= 3;
        changeUsernameEditText.setText(arguments.getString(EXTRA_USERNAME, ""));
        changeUsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                usernameValid = !TextUtils.isEmpty(s) && s.length() >= 3;
                changeUsernameTextInputLayout.setError(usernameValid
                        ? null
                        : getString(R.string.username_invalid_message));
            }
        });
    }


    public interface ChangeUsernameActionListener {

        void onUsernameChanged(String newUsername);
    }
}

