package com.citrix.wrekt.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.citrix.wrekt.R;

public class CreateChannelDialogFragment extends DialogFragment {

    private CreateChannelActionListener listener;

    public static CreateChannelDialogFragment newInstance() {
        CreateChannelDialogFragment fragment = new CreateChannelDialogFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public CreateChannelDialogFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (CreateChannelActionListener) activity;
        } catch (ClassCastException exception) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + CreateChannelActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setMessage(R.string.create_channel_dialog_message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (listener != null) {
                                    listener.onCreateChannelConfirmed();
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


    public interface CreateChannelActionListener {

        void onCreateChannelConfirmed();
    }
}

