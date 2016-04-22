package com.citrix.wrekt.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;

import com.citrix.wrekt.R;
import com.citrix.wrekt.activity.ChannelChatActivity;
import com.citrix.wrekt.activity.FriendActivity;
import com.citrix.wrekt.service.FriendRequestService;

public class RequestNotifier implements IRequestNotifier {

    private Context context;

    public RequestNotifier(Context context) {
        this.context = context;
    }

    @Override
    public void showRequestNotification(String[] params) {
        Notification notification = createRequestNotification(params);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_REQUEST_ID, notification);
    }

    @Override
    public void cancelRequestNotification() {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_REQUEST_ID);
    }

    @Override
    public void showOngoingVideoCallNotifcation(String channelName) {
        Notification notification = createOngoingVideoCallNotification(channelName);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ONGOING_VIDEO_CALL_ID, notification);
    }

    @Override
    public void cancelOngoingVideoCallNotification() {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ONGOING_VIDEO_CALL_ID);
    }

    private Notification createRequestNotification(String[] params) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(createLargeIcon())
                .setContentTitle(context.getString(R.string.friend_request_notification_title))
                .setContentText(String.format(context.getString(R.string.friend_request_notification_message),
                        params[FriendRequestService.PARAM_FRIEND_USERNAME]))
                .setContentIntent(createRequestIntent())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOngoing(false);

        notificationBuilder.addAction(createAction(R.drawable.ic_reject, context.getString(R.string.reject_request_text), NOTIFICATION_ACTION_REJECT_REQUEST, params));
        notificationBuilder.addAction(createAction(R.drawable.ic_postpone, context.getString(R.string.postpone_request_text), NOTIFICATION_ACTION_POSTPONE_REQUEST, params));
        notificationBuilder.addAction(createAction(R.drawable.ic_accept, context.getString(R.string.accept_request_text), NOTIFICATION_ACTION_ACCEPT_REQUEST, params));
        return notificationBuilder.build();
    }

    private Notification createOngoingVideoCallNotification(String channelName) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(createLargeIcon())
                .setContentTitle(context.getString(R.string.ongoing_video_call_notification_title))
                .setContentText(String.format(context.getString(R.string.ongoing_video_call_notification_message), channelName))
                .setContentIntent(createOngoingVideoCallIntent())
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true);

        return notificationBuilder.build();
    }

    private NotificationCompat.Action createAction(int icon, String title, String intentAction, String[] params) {
        return new NotificationCompat.Action.Builder(icon, title, createRequestActionIntent(intentAction, params)).build();
    }

    private PendingIntent createRequestIntent() {
        Intent intent = new Intent(context, FriendActivity.class);
        intent.setAction(NOTIFICATION_ACTION_REQUEST_INTENT);
        return PendingIntent.getActivity(context, REQ_CODE_REQUEST_INTENT, intent, 0);
    }

    private PendingIntent createRequestActionIntent(String intentAction, String[] params) {
        Intent intent = new Intent(context, FriendRequestService.class);
        intent.setAction(intentAction);
        intent.putExtra(EXTRA_PARAMS, params);
        return PendingIntent.getService(context, REQ_CODE_REQUEST_ACTION, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createOngoingVideoCallIntent() {
        Intent intent = new Intent(context, ChannelChatActivity.class);
        intent.setAction(NOTIFICATION_ACTION_ONGOING_VIDEO_CALL_INTENT);
        return PendingIntent.getActivity(context, REQ_CODE_ONGOING_VIDEO_CALL_INTENT, intent, 0);
    }

    private Bitmap createLargeIcon() {
        return BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
    }
}
