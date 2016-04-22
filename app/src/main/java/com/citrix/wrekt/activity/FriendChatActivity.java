package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.adapter.ChatMessageAdapter;
import com.citrix.wrekt.data.ChatMessage;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.fragment.dialog.ProgressDialogFragment;
import com.citrix.wrekt.util.KeyboardUtils;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class FriendChatActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {

    private static final String EXTRA_FRIEND_UID = "EXTRA_FRIEND_UID";
    private static final String EXTRA_FRIEND_USERNAME = "EXTRA_FRIEND_USERNAME";
    private static final String TAG_RECORD_PROGRESS_DIALOG = "TAG_RECORD_PROGRESS_DIALOG";
    private static final String RECORD_FILE_NAME = "friend_chat_record";
    private static final String RECORD_FILE_EXTENTION = ".mp4";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private ActionMenuView actionMenuView;
    private RecyclerView friendChatRecyclerView;
    private LinearLayout chatEmptyContainer;
    private ViewSwitcher chatMethodViewSwitcher;
    private ImageButton chatMethodImageButton;
    private ImageButton sendChatImageButton;
    private TextInputEditText chatMessageEditText;
    private Button recordAudioButton;
    private RelativeLayout newMessageSnackbarContainer;

    private ActivityComponent activityComponent;
    private LinearLayoutManager linearLayoutManager;
    private FragmentManager fragmentManager;
    private ChatMessageAdapter chatMessageAdapter;
    private Firebase privateChatRef;
    private ChatValueEventListener chatValueEventListener;
    private Set<String> savedChatIdSet;
    private MediaRecorder recorder;
    private String friendUid;
    private String friendUsername;
    private String tempRecordFilePath;

    public static void start(Context context, String friendUid, String friendUsername) {
        Intent intent = new Intent(context, FriendChatActivity.class);
        intent.putExtra(EXTRA_FRIEND_UID, friendUid);
        intent.putExtra(EXTRA_FRIEND_USERNAME, friendUsername);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_chat);

        inject();

        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            friendUid = launchIntent.getStringExtra(EXTRA_FRIEND_UID);
            friendUsername = launchIntent.getStringExtra(EXTRA_FRIEND_USERNAME);
        } else {
            finish();
            return;
        }

        fragmentManager = getFragmentManager();
        try {
            File tempRecordFile = File.createTempFile(RECORD_FILE_NAME, RECORD_FILE_EXTENTION, getCacheDir());
            tempRecordFilePath = tempRecordFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setTitle(friendUsername);
        }

        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);
        friendChatRecyclerView = (RecyclerView) findViewById(R.id.channel_chat_recycler_view);
        chatEmptyContainer = (LinearLayout) findViewById(R.id.chat_empty_container);
        chatMethodViewSwitcher = (ViewSwitcher) findViewById(R.id.chat_method_view_switcher);
        chatMethodImageButton = (ImageButton) findViewById(R.id.chat_method_image_button);
        sendChatImageButton = (ImageButton) findViewById(R.id.send_chat_image_button);
        chatMessageEditText = (TextInputEditText) findViewById(R.id.chat_message_edit_text);
        recordAudioButton = (Button) findViewById(R.id.record_audio_button);
        newMessageSnackbarContainer = (RelativeLayout) findViewById(R.id.new_message_snackbar_container);
        TextView viewNewMessageTextView = (TextView) findViewById(R.id.view_new_message_text_view);

        linearLayoutManager = new LinearLayoutManager(this);
        friendChatRecyclerView.setLayoutManager(linearLayoutManager);
        chatMessageAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>(), uidPref.get());
        friendChatRecyclerView.setAdapter(chatMessageAdapter);

        chatMessageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    KeyboardUtils.hideKeyboard(FriendChatActivity.this);
                    return true;
                }
                return false;
            }
        });
        chatMessageEditText.addTextChangedListener(new ChatMessageTextWatcher());
        chatMethodImageButton.setOnClickListener(this);
        sendChatImageButton.setOnClickListener(this);
        recordAudioButton.setOnTouchListener(this);
        viewNewMessageTextView.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatMessageAdapter.clear();
        setupFirebaseAndListener(friendUid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (privateChatRef != null && chatValueEventListener != null) {
            privateChatRef.removeEventListener(chatValueEventListener);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideRecordProgressDialog();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_method_image_button:
                chatMethodViewSwitcher.showNext();
                if (chatMethodViewSwitcher.getDisplayedChild() == 1) {
                    chatMethodImageButton.setImageResource(R.drawable.ic_keyboard);
                } else {
                    chatMethodImageButton.setImageResource(R.drawable.ic_voice);
                }
                break;

            case R.id.send_chat_image_button:
                String message = chatMessageEditText.getText().toString();
                KeyboardUtils.hideKeyboard(this);
                chatMessageEditText.setText("");
                if (!TextUtils.isEmpty(message)) {
                    storeChatMessageInFirebase(message, false);
                }
                break;

            case R.id.view_new_message_text_view:
                scrollToBottom();
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                recordAudioButton.setBackgroundResource(R.drawable.round_corner_white);
                showRecordProgressDialog();
                startRecording();
                return true;

            case MotionEvent.ACTION_UP:
                recordAudioButton.setBackgroundResource(R.drawable.round_corner_button);
                hideRecordProgressDialog();
                stopRecordingAndUpload();
                return true;
        }
        return false;
    }

    private void setupFirebaseAndListener(final String friendUid) {
        savedChatIdSet = new HashSet<>();
        String myUid = uidPref.get();
        String privateChannelId;
        int compareResult = myUid.compareTo(friendUid);
        if (compareResult < 0) {
            privateChannelId = myUid + friendUid;
        } else {
            privateChannelId = friendUid + myUid;
        }
        privateChatRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getPrivateChatsUrl()).child(privateChannelId);
        chatValueEventListener = new ChatValueEventListener();
        privateChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ChatMessage> chatMessageList = new ArrayList<>();
                if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Map<String, Map<String, Object>> messagesMap = (Map<String, Map<String, Object>>) dataSnapshot.getValue();
                    for (Map<String, Object> messageMap : messagesMap.values()) {
                        String chatId = messageMap.get("id").toString();
                        boolean isRecording = Boolean.valueOf(messageMap.get("recording").toString());
                        String message = messageMap.get("message").toString();
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setId(chatId);
                        chatMessage.setSenderUid(messageMap.get("senderUid").toString());
                        chatMessage.setSenderName(messageMap.get("senderName").toString());
                        chatMessage.setChannelId(messageMap.get("channelId").toString());
                        chatMessage.setTime(Long.parseLong(messageMap.get("time").toString()));
                        chatMessage.setIsRecording(isRecording);
                        chatMessage.setMessage(isRecording ? "" : message);
                        chatMessage.setRecordingData(isRecording ? Base64.decode(message, 0) : null);
                        chatMessageList.add(chatMessage);
                        savedChatIdSet.add(chatId);
                    }
                }

                chatMessageAdapter.updateChatMessageList(chatMessageList);
                int messageCount = chatMessageAdapter.getItemCount();
                chatEmptyContainer.setVisibility(messageCount == 0 ? View.VISIBLE : View.GONE);
                friendChatRecyclerView.scrollToPosition(messageCount == 0 ? 0 : messageCount - 1);
                // Add child listener to listen for new messages
                privateChatRef.addChildEventListener(chatValueEventListener);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void scrollToBottom() {
        hideNewMessageSnackbar();
        int messageCount = chatMessageAdapter.getItemCount();
        friendChatRecyclerView.smoothScrollToPosition(messageCount == 0 ? 0 : messageCount - 1);
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(tempRecordFilePath);

        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingAndUpload() {
        if (recorder != null) {
            recorder.stop();
            recorder.reset();
            recorder.release();

            recorder = null;
        }

        File file = new File(tempRecordFilePath);
        byte[] audioBytes = readFileAsByteArray(file);
        if (audioBytes != null) {
            String encodedAudio = Base64.encodeToString(audioBytes, 0);
            storeChatMessageInFirebase(encodedAudio, true);
        }
    }

    private void showRecordProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_RECORD_PROGRESS_DIALOG);
        if (fragment == null) {
            fragment = ProgressDialogFragment.newInstance(R.string.recording_progress_dialog_message);
        }
        fragment.show(fragmentManager, TAG_RECORD_PROGRESS_DIALOG);
    }

    private void hideRecordProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_RECORD_PROGRESS_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showNewMessageSnackbar() {
        newMessageSnackbarContainer.setVisibility(View.VISIBLE);
    }

    private void hideNewMessageSnackbar() {
        newMessageSnackbarContainer.setVisibility(View.GONE);
    }

    private void storeChatMessageInFirebase(String message, boolean isRecording) {
        ChatMessage sendMessage = new ChatMessage();
        sendMessage.setSenderUid(uidPref.get());
        sendMessage.setSenderName(usernamePref.get());
        sendMessage.setChannelId("Private");
        sendMessage.setMessage(message);
        sendMessage.setTime(new Date().getTime());
        sendMessage.setIsRecording(isRecording);
        Firebase newChatRef = privateChatRef.push();
        String chatId = newChatRef.getKey();
        sendMessage.setId(chatId);
        newChatRef.setValue(sendMessage, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.e("findme: ", "Firebase send chat failed: " + firebaseError.getMessage());
                    Toast.makeText(FriendChatActivity.this,
                            R.string.send_chat_failed_message,
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private byte[] readFileAsByteArray(File file) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(file);

            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf))) {
                baos.write(buf, 0, n);
            }

            return baos.toByteArray();
        } catch (Exception e) {
            // TODO: handle exception
            Log.e("findme: ", "exception reading file: " + e.getMessage());
        }
        return null;
    }


    private class ChatValueEventListener implements ChildEventListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                Map<String, Object> messageMap = (Map<String, Object>) dataSnapshot.getValue();
                String chatId = messageMap.get("id").toString();
                if (!savedChatIdSet.contains(chatId)) {
                    boolean isRecording = Boolean.valueOf(messageMap.get("recording").toString());
                    String message = messageMap.get("message").toString();
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setId(chatId);
                    chatMessage.setSenderUid(messageMap.get("senderUid").toString());
                    chatMessage.setSenderName(messageMap.get("senderName").toString());
                    chatMessage.setChannelId(messageMap.get("channelId").toString());
                    chatMessage.setTime(Long.parseLong(messageMap.get("time").toString()));
                    chatMessage.setIsRecording(isRecording);
                    chatMessage.setMessage(isRecording ? "" : message);
                    chatMessage.setRecordingData(isRecording ? Base64.decode(message, 0) : null);
                    chatMessageAdapter.addNewMessage(chatMessage);
                    chatEmptyContainer.setVisibility(View.GONE);

                    // If user send a message or the list is already at the bottom, scroll to new bottom
                    if (uidPref.get().equals(chatMessage.getSenderUid()) ||
                            linearLayoutManager.findLastVisibleItemPosition() >= chatMessageAdapter.getItemCount() - 2) {
                        scrollToBottom();
                    } else {
                        showNewMessageSnackbar();
                    }
                }
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    private class ChatMessageTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            sendChatImageButton.setEnabled(!TextUtils.isEmpty(s));
        }
    }
}
