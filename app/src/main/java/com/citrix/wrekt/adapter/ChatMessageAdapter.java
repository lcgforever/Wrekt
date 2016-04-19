package com.citrix.wrekt.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.citrix.wrekt.R;
import com.citrix.wrekt.data.ChatMessage;
import com.citrix.wrekt.util.TimeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder> {

    private static final String TEMP_FILE_NAME = "temp";
    private static final String TEMP_FILE_EXT = "mp4";
    private static final int TYPE_RECEIVED_CHAT = 0;
    private static final int TYPE_SENT_CHAT = 1;

    private Context context;
    private LayoutInflater layoutInflater;
    private List<ChatMessage> chatMessageList;
    private String myUid;
    private ChatViewHolder playingAudioHolder;
    private MediaPlayer mediaPlayer;
    private boolean isAudioPlaying;
    private String playingAudioId;

    public ChatMessageAdapter(Context context, List<ChatMessage> chatMessageList, String myUid) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        Collections.sort(chatMessageList);
        this.chatMessageList = chatMessageList;
        this.myUid = myUid;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case TYPE_SENT_CHAT:
                view = layoutInflater.inflate(R.layout.layout_sent_chat_message_row, parent, false);
                return new SentChatViewHolde(view);

            case TYPE_RECEIVED_CHAT:
            default:
                view = layoutInflater.inflate(R.layout.layout_received_chat_message_row, parent, false);
                return new ReceivedChatViewHolde(view);
        }
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessageList.get(position);
        if (chatMessage.isRecording()) {
            holder.messageContentTextView.setVisibility(View.GONE);
            holder.clickToListenButton.setVisibility(View.VISIBLE);
        } else {
            holder.clickToListenButton.setVisibility(View.GONE);
            holder.messageContentTextView.setVisibility(View.VISIBLE);
            holder.messageContentTextView.setText(chatMessage.getMessage());
        }
        holder.messageTimeTextView.setText(TimeUtils.getDateAndTime(chatMessage.getTime()));

        if (holder instanceof SentChatViewHolde) {
            holder.messageSenderTextView.setText(R.string.sender_me_text);
        } else {
            String senderName = chatMessage.getSenderName();
            holder.messageSenderTextView.setText(senderName);
            ((ReceivedChatViewHolde) holder).senderInitialTextView.setVisibility(View.VISIBLE);
            ((ReceivedChatViewHolde) holder).senderInitialTextView.setText(senderName.substring(0, 1).toUpperCase());
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage chatMessage = chatMessageList.get(position);
        if (myUid.equals(chatMessage.getSenderUid())) {
            return TYPE_SENT_CHAT;
        } else {
            return TYPE_RECEIVED_CHAT;
        }
    }

    @Override
    public int getItemCount() {
        return chatMessageList == null ? 0 : chatMessageList.size();
    }

    public void updateChatMessageList(List<ChatMessage> chatMessageList) {
        if (chatMessageList != null) {
            this.chatMessageList.clear();
            Collections.sort(chatMessageList);
            this.chatMessageList.addAll(chatMessageList);
            notifyDataSetChanged();
        }
    }

    public void addNewMessage(ChatMessage chatMessage) {
        chatMessageList.add(chatMessage);
        Collections.sort(chatMessageList);
        notifyItemInserted(chatMessageList.size() - 1);
    }

    public void clear() {
        chatMessageList.clear();
        notifyDataSetChanged();
    }

    private void startAudioPlaying(byte[] audioData, String playingAudioId, ChatViewHolder playingAudioHolder) {
        try {
            // Stop media which is playing
            if (isAudioPlaying) {
                stopAudioPlaying();
            }

            // create temp file that will hold byte array
            File tempAudioRecording = File.createTempFile(TEMP_FILE_NAME, TEMP_FILE_EXT, context.getCacheDir());
            tempAudioRecording.deleteOnExit();
            FileOutputStream fileOutputStream = new FileOutputStream(tempAudioRecording);
            fileOutputStream.write(audioData);
            fileOutputStream.close();

            mediaPlayer = new MediaPlayer();
            FileInputStream fileInputStream = new FileInputStream(tempAudioRecording);
            mediaPlayer.setDataSource(fileInputStream.getFD());
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopAudioPlaying();
                }
            });

            mediaPlayer.prepare();
            mediaPlayer.start();
            isAudioPlaying = true;
            this.playingAudioId = playingAudioId;
            this.playingAudioHolder = playingAudioHolder;
        } catch (IOException e) {
            Log.e("findme: ", "Play audio recording error: " + e.getMessage());
        }
    }

    private void stopAudioPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
            isAudioPlaying = false;
            playingAudioId = "";
            playingAudioHolder.clickToListenButton.setText(R.string.click_to_listen_message);
            playingAudioHolder = null;
        }
    }


    protected class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView messageSenderTextView;
        private TextView messageTimeTextView;
        private TextView messageContentTextView;
        private Button clickToListenButton;

        public ChatViewHolder(View itemView) {
            super(itemView);
            messageSenderTextView = (TextView) itemView.findViewById(R.id.sender_name);
            messageTimeTextView = (TextView) itemView.findViewById(R.id.message_time);
            messageContentTextView = (TextView) itemView.findViewById(R.id.message_content);
            clickToListenButton = (Button) itemView.findViewById(R.id.click_to_listen_button);

            clickToListenButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.click_to_listen_button:
                    int position = getAdapterPosition();
                    ChatMessage chatMessage = chatMessageList.get(position);
                    if (chatMessage.isRecording()) {
                        if (isAudioPlaying && playingAudioId.equals(chatMessage.getId())) {
                            stopAudioPlaying();
                            clickToListenButton.setText(R.string.click_to_listen_message);
                        } else {
                            clickToListenButton.setText(R.string.playing_audio_message);
                            startAudioPlaying(chatMessage.getRecordingData(), chatMessage.getId(), this);
                        }
                    }
                    break;
            }
        }
    }

    private class ReceivedChatViewHolde extends ChatViewHolder {

        private TextView senderInitialTextView;

        public ReceivedChatViewHolde(View itemView) {
            super(itemView);
            senderInitialTextView = (TextView) itemView.findViewById(R.id.initials_text);
        }
    }

    private class SentChatViewHolde extends ChatViewHolder {

        public SentChatViewHolde(View itemView) {
            super(itemView);
        }
    }
}