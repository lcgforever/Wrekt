package com.citrix.wrekt.data;

import android.support.annotation.NonNull;

public class ChatMessage implements Comparable<ChatMessage> {

    private String id;
    private String senderUid;
    private String senderName;
    private String channelId;
    private String message;
    private Long time;
    private boolean isRecording;
    private byte[] recordingData;

    public ChatMessage() {
    }

    public ChatMessage(String id, String senderUid, String senderName, String channelId, String message, Long time, boolean isRecording, byte[] recordingData) {
        this.id = id;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.channelId = channelId;
        this.message = message;
        this.time = time;
        this.isRecording = isRecording;
        this.recordingData = recordingData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setIsRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public byte[] getRecordingData() {
        return recordingData;
    }

    public void setRecordingData(byte[] recordingData) {
        this.recordingData = recordingData;
    }

    @Override
    public int compareTo(@NonNull ChatMessage another) {
        return time.compareTo(another.getTime());
    }
}
