package com.citrix.wrekt.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.citrix.wrekt.R;

public class VideoChatDialogFragment extends DialogFragment {

    private static final String EXTRA_JOIN_VIDEO_CHAT = "EXTRA_JOIN_VIDEO_CHAT";

    private VideoChatActionListener listener;

    public static VideoChatDialogFragment newInstance(boolean joinVideoChat) {
        VideoChatDialogFragment fragment = new VideoChatDialogFragment();
        fragment.setRetainInstance(true);
        Bundle arguments = new Bundle();
        arguments.putBoolean(EXTRA_JOIN_VIDEO_CHAT, joinVideoChat);
        fragment.setArguments(arguments);
        return fragment;
    }

    public VideoChatDialogFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (VideoChatActionListener) activity;
        } catch (ClassCastException exception) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + VideoChatActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        final boolean joinVideoChat = arguments.getBoolean(EXTRA_JOIN_VIDEO_CHAT);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setMessage(joinVideoChat
                        ? R.string.join_video_chat_dialog_message
                        : R.string.leave_video_chat_dialog_message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (listener != null) {
                                    if (joinVideoChat) {
                                        listener.onJoinVideoChatConfirmed();
                                    } else {
                                        listener.onLeaveVideoChatConfirmed();
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


    public interface VideoChatActionListener {

        void onJoinVideoChatConfirmed();

        void onLeaveVideoChatConfirmed();
    }
}
