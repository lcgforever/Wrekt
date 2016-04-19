package com.citrix.wrekt.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.citrix.wrekt.R;

public class EmailUtils {

    public static void sendEmailToRecipients(Context context, String[] recipientEmails) {
        Intent messageIntent;
        messageIntent = new Intent(Intent.ACTION_SENDTO);
        StringBuilder stringBuilder = new StringBuilder("mailto:");
        for (int i = 0; i < recipientEmails.length; i++) {
            if (i != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(recipientEmails[i]);
        }
        messageIntent.setType("message/rfc822");
        messageIntent.setData(Uri.parse(stringBuilder.toString()));

        context.startActivity(Intent.createChooser(messageIntent, context.getString(R.string.send_email_text)));
    }
}
