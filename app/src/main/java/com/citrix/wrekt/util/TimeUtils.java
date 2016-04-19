package com.citrix.wrekt.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TimeUtils {

    private static final String TEMPLATE_FULL_DATE = "MMM d, yyyy";
    private static final String TEMPLATE_DATE_AND_TIME = "MMM d, H:mm";
    private static final String TEMPLATE_TIME = "H:mm";

    private static SimpleDateFormat fullDateFormat = new SimpleDateFormat(TEMPLATE_FULL_DATE, Locale.getDefault());
    private static SimpleDateFormat dateFormat = new SimpleDateFormat(TEMPLATE_DATE_AND_TIME, Locale.getDefault());
    private static SimpleDateFormat timeFormat = new SimpleDateFormat(TEMPLATE_TIME, Locale.getDefault());

    public static String getDateAndTime(long time) {
        Calendar currentTimeCal = Calendar.getInstance();
        Calendar givenTimeCal = Calendar.getInstance();
        givenTimeCal.setTimeInMillis(time);
        if (currentTimeCal.get(Calendar.YEAR) == givenTimeCal.get(Calendar.YEAR)
                && currentTimeCal.get(Calendar.DAY_OF_YEAR) == givenTimeCal.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + timeFormat.format(givenTimeCal.getTime());
        } else {
            return dateFormat.format(givenTimeCal.getTime());
        }
    }

    public static String getFullDate(long time) {
        Calendar givenTimeCal = Calendar.getInstance();
        givenTimeCal.setTimeInMillis(time);
        return fullDateFormat.format(time);
    }
}
