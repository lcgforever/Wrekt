package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.http.SslError;
import android.os.Build;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.citrix.wrekt.fragment.dialog.LeaveChannelDialogFragment;
import com.citrix.wrekt.fragment.dialog.ProgressDialogFragment;
import com.citrix.wrekt.fragment.dialog.VideoChatDialogFragment;
import com.citrix.wrekt.notification.IRequestNotifier;
import com.citrix.wrekt.util.KeyboardUtils;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
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

public class ChannelChatActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener,
        ActionMenuView.OnMenuItemClickListener, LeaveChannelDialogFragment.LeaveChannelActionListener,
        VideoChatDialogFragment.VideoChatActionListener {

    public static final String EXTRA_CHANNEL_ID = "EXTRA_CHANNEL_ID";
    public static final String EXTRA_CHANNEL_NAME = "EXTRA_CHANNEL_NAME";
    public static final String EXTRA_CHANNEL_CONF_URL = "EXTRA_CHANNEL_CONF_URL";

    private static final String EXTRA_IN_VIDEO_CHAT = "EXTRA_IN_VIDEO_CHAT";
    private static final String TAG_JOIN_OR_LEAVE_VIDEO_CHAT_DIALOG = "TAG_JOIN_OR_LEAVE_VIDEO_CHAT_DIALOG";
    private static final String TAG_RECORD_PROGRESS_DIALOG = "TAG_RECORD_PROGRESS_DIALOG";
    private static final String TAG_LEAVE_CHANNEL_DIALOG = "TAG_LEAVE_CHANNEL_DIALOG";
    private static final String TAG_LEAVE_CHANNEL_PROGRESS_DIALOG = "TAG_LEAVE_CHANNEL_PROGRESS_DIALOG";
    private static final String RECORD_FILE_NAME = "chat_record";
    private static final String RECORD_FILE_EXTENSION = ".mp4";
    private static final boolean SHOW_JOIN_VIDEO_CHAT_DIALOG = true;
    private static final boolean SHOW_LEAVE_VIDEO_CHAT_DIALOG = false;

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

    @Inject
    IRequestNotifier requestNotifier;

    private ActionMenuView actionMenuView;
    private WebView videoWebView;
    private RecyclerView chatRecyclerView;
    private LinearLayout chatEmptyContainer;
    private ViewSwitcher chatMethodViewSwitcher;
    private ImageButton chatMethodImageButton;
    private ImageButton sendChatImageButton;
    private TextInputEditText chatMessageEditText;
    private Button recordAudioButton;
    private RelativeLayout newMessageSnackbarContainer;

    private ActivityComponent activityComponent;
    private MenuItem webcamMenuItem;
    private LinearLayoutManager linearLayoutManager;
    private FragmentManager fragmentManager;
    private ChatMessageAdapter chatMessageAdapter;
    private Firebase chatRef;
    private ChatValueEventListener chatValueEventListener;
    private Set<String> savedChatIdSet;
    private MediaRecorder recorder;
    private String channelId;
    private String channelName;
    private String channelConferenceUrl;
    private String tempRecordFilePath;
    private boolean isInVideoChat;

    public static void start(Context context, String channelId, String channelName, String channelConfUrl) {
        Intent intent = new Intent(context, ChannelChatActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_CHANNEL_CONF_URL, channelConfUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_chat);

        inject();

        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            channelId = launchIntent.getStringExtra(EXTRA_CHANNEL_ID);
            channelName = launchIntent.getStringExtra(EXTRA_CHANNEL_NAME);
            String confUrlPostfix = launchIntent.getStringExtra(EXTRA_CHANNEL_CONF_URL);
            channelConferenceUrl = String.format(getString(R.string.channel_conference_url_format), confUrlPostfix);
        } else {
            finish();
            return;
        }

        fragmentManager = getFragmentManager();
        try {
            File tempRecordFile = File.createTempFile(RECORD_FILE_NAME, RECORD_FILE_EXTENSION, getCacheDir());
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
            supportActionBar.setTitle(channelName);
        }

        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);
        videoWebView = (WebView) findViewById(R.id.video_web_view);
        chatRecyclerView = (RecyclerView) findViewById(R.id.channel_chat_recycler_view);
        chatEmptyContainer = (LinearLayout) findViewById(R.id.chat_empty_container);
        chatMethodViewSwitcher = (ViewSwitcher) findViewById(R.id.chat_method_view_switcher);
        chatMethodImageButton = (ImageButton) findViewById(R.id.chat_method_image_button);
        sendChatImageButton = (ImageButton) findViewById(R.id.send_chat_image_button);
        chatMessageEditText = (TextInputEditText) findViewById(R.id.chat_message_edit_text);
        recordAudioButton = (Button) findViewById(R.id.record_audio_button);
        newMessageSnackbarContainer = (RelativeLayout) findViewById(R.id.new_message_snackbar_container);
        TextView viewNewMessageTextView = (TextView) findViewById(R.id.view_new_message_text_view);

        actionMenuView.setOnMenuItemClickListener(this);
        linearLayoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(linearLayoutManager);
        chatMessageAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>(), uidPref.get());
        chatRecyclerView.setAdapter(chatMessageAdapter);

        videoWebView.setWebViewClient(new VideoWebViewClient());
        videoWebView.setWebChromeClient(new VideoWebChromeClient());
        // Enable JavaScript and video auto play in web view
        videoWebView.getSettings().setJavaScriptEnabled(true);
        videoWebView.getSettings().setSupportZoom(true);
        videoWebView.getSettings().setBuiltInZoomControls(true);
        videoWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        chatMessageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    KeyboardUtils.hideKeyboard(ChannelChatActivity.this);
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
        setupFirebaseAndListener(channelId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatRef != null && chatValueEventListener != null) {
            chatRef.removeEventListener(chatValueEventListener);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideJoinOrLeaveVideoChatDialog();
        hideRecordProgressDialog();
        hideLeaveChannelDialog();
        hideLeaveProgressDialog();
        outState.putBoolean(EXTRA_IN_VIDEO_CHAT, isInVideoChat);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isInVideoChat = savedInstanceState.getBoolean(EXTRA_IN_VIDEO_CHAT, false);
        if (isInVideoChat) {
            videoWebView.loadUrl(channelConferenceUrl);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoWebView.destroy();
        activityComponent = null;
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Menu actionMenu = actionMenuView.getMenu();
        actionMenu.clear();
        getMenuInflater().inflate(R.menu.menu_channel_chat, actionMenu);
        webcamMenuItem = actionMenu.findItem(R.id.action_webcam);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_webcam:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (isInVideoChat) {
                        showJoinOrLeaveVideoChatDialog(SHOW_LEAVE_VIDEO_CHAT_DIALOG);
                    } else {
                        showJoinOrLeaveVideoChatDialog(SHOW_JOIN_VIDEO_CHAT_DIALOG);
                    }
                } else {
                    Toast.makeText(this,
                            R.string.web_rtc_not_supported_message,
                            Toast.LENGTH_SHORT)
                            .show();
                }
                return true;

            case R.id.action_leave_channel:
                showLeaveChannelDialog();
                return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isInVideoChat) {
            Toast.makeText(this,
                    R.string.leave_video_chat_reminder_message,
                    Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else {
            finish();
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if (isInVideoChat) {
            Toast.makeText(this,
                    R.string.leave_video_chat_reminder_message,
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            super.onBackPressed();
        }
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

    @Override
    public void onJoinVideoChatConfirmed() {
        recordAudioButton.setEnabled(false);
        webcamMenuItem.setTitle(R.string.action_webcam_off);
        webcamMenuItem.setIcon(R.drawable.ic_webcam_off);
        videoWebView.loadUrl(channelConferenceUrl);
        videoWebView.setVisibility(View.VISIBLE);
        requestNotifier.showOngoingVideoCallNotifcation(channelName);
        isInVideoChat = true;
        Toast.makeText(this,
                R.string.video_chat_in_progress_message,
                Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void onLeaveVideoChatConfirmed() {
        recordAudioButton.setEnabled(true);
        webcamMenuItem.setTitle(R.string.action_webcam);
        webcamMenuItem.setIcon(R.drawable.ic_webcam);
        videoWebView.loadUrl("javascript:window.leave()");
        videoWebView.setVisibility(View.GONE);
        requestNotifier.cancelOngoingVideoCallNotification();
        isInVideoChat = false;
    }

    @Override
    public void onLeaveChannelConfirmed() {
        showLeaveProgressDialog();
        updateChannelMemberCount();
    }

    private void setupFirebaseAndListener(final String channelId) {
        savedChatIdSet = new HashSet<>();
        chatRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChatsUrl()).child(channelId);
        chatValueEventListener = new ChatValueEventListener();
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                chatRecyclerView.scrollToPosition(messageCount == 0 ? 0 : messageCount - 1);
                // Add child listener to listen for new messages
                chatRef.addChildEventListener(chatValueEventListener);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void showJoinOrLeaveVideoChatDialog(boolean joinVideoChat) {
        VideoChatDialogFragment fragment
                = (VideoChatDialogFragment) fragmentManager.findFragmentByTag(TAG_JOIN_OR_LEAVE_VIDEO_CHAT_DIALOG);
        if (fragment == null) {
            fragment = VideoChatDialogFragment.newInstance(joinVideoChat);
        }
        fragment.show(fragmentManager, TAG_JOIN_OR_LEAVE_VIDEO_CHAT_DIALOG);
    }

    private void hideJoinOrLeaveVideoChatDialog() {
        VideoChatDialogFragment fragment
                = (VideoChatDialogFragment) fragmentManager.findFragmentByTag(TAG_JOIN_OR_LEAVE_VIDEO_CHAT_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void updateChannelMemberCount() {
        Firebase channelRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl()).child(channelId);
        channelRef.child("memberCount").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(0);
                } else {
                    long newValue = (Long) currentData.getValue() - 1;
                    if (newValue < 0) {
                        newValue = 0;
                    }
                    currentData.setValue(newValue);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                if (firebaseError == null) {
                    updateChannelMembersData();
                } else {
                    showLeaveChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void updateChannelMembersData() {
        Firebase channelMemberRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelMembersUrl()).child(channelId);
        channelMemberRef.child(uidPref.get()).removeValue(new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    removeSubscriptionToChannel();
                } else {
                    showLeaveChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void removeSubscriptionToChannel() {
        Firebase subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        subscriptionRef.child(channelId).removeValue(new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    hideLeaveProgressDialog();
                    finish();
                } else {
                    showLeaveChannelFailedMessage(firebaseError.getMessage());
                }
            }
        });
    }

    private void showLeaveChannelFailedMessage(String failureMessage) {
        Log.e("findme: ", "Firebase add subscription failed: " + failureMessage);
        hideLeaveProgressDialog();
        Toast.makeText(this,
                R.string.join_channel_failed_message,
                Toast.LENGTH_SHORT)
                .show();
    }

    private void scrollToBottom() {
        hideNewMessageSnackbar();
        int messageCount = chatMessageAdapter.getItemCount();
        chatRecyclerView.smoothScrollToPosition(messageCount == 0 ? 0 : messageCount - 1);
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

    private void showLeaveChannelDialog() {
        LeaveChannelDialogFragment fragment
                = (LeaveChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_DIALOG);
        if (fragment == null) {
            fragment = LeaveChannelDialogFragment.newInstance();
        }
        fragment.show(fragmentManager, TAG_LEAVE_CHANNEL_DIALOG);
    }

    private void hideLeaveChannelDialog() {
        LeaveChannelDialogFragment fragment
                = (LeaveChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showLeaveProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_PROGRESS_DIALOG);
        if (fragment == null) {
            fragment = ProgressDialogFragment.newInstance(R.string.leave_channel_progress_dialog_message);
        }
        fragment.show(fragmentManager, TAG_LEAVE_CHANNEL_PROGRESS_DIALOG);
    }

    private void hideLeaveProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_LEAVE_CHANNEL_PROGRESS_DIALOG);
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
        sendMessage.setChannelId(channelId);
        sendMessage.setMessage(message);
        sendMessage.setTime(new Date().getTime());
        sendMessage.setIsRecording(isRecording);
        Firebase newChatRef = chatRef.push();
        String chatId = newChatRef.getKey();
        sendMessage.setId(chatId);
        newChatRef.setValue(sendMessage, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.e("findme: ", "Firebase send chat failed: " + firebaseError.getMessage());
                    Toast.makeText(ChannelChatActivity.this,
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
                String chatId = "";
                if (messageMap.containsKey("id") && messageMap.get("id") != null) {
                    chatId = messageMap.get("id").toString();
                }
                if (TextUtils.isEmpty(chatId) || !savedChatIdSet.contains(chatId)) {
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

    private class VideoWebViewClient extends WebViewClient {

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e("findme: ", "Receive error: " + error.getErrorCode() + "   " + error.getDescription());
            }
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e("findme: ", "Receive error: " + errorResponse.getStatusCode() + "   " + errorResponse.getReasonPhrase());
            }
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.e("findme: ", "Receive SSL error: " + error);
            handler.proceed();
        }
    }

    private class VideoWebChromeClient extends WebChromeClient {

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request.grant(request.getResources());
            } else {
                super.onPermissionRequest(request);
            }
        }
    }
}
