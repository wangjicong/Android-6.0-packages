package com.sprd.messaging.ui.smsmergeforward;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.text.format.Time;

class SmsMessageItem {
    public static final short PROJECTION_ADDRESS= 0;
    public static final short PROJECTION_ID= 1;
    public static final short PROJECTION_DATE = 2;
    public static final short PROJECTION_TYPE = 3;
    public static final short PROJECTION_BODY = 4;
    
    public int mId;
    public String mAddress;
    public String mtimeStamp;
    public String mBody;
    public int mType;
   
    public SmsMessageItem(){
        
    }

    public SmsMessageItem(Context context, Cursor cursor) {
        mId = cursor.getInt(PROJECTION_ID);
        mAddress = cursor.getString(PROJECTION_ADDRESS);
        Long datel = cursor.getLong(PROJECTION_DATE);
        mtimeStamp = formatTimeStampString(context, datel);
        mBody = cursor.getString(PROJECTION_BODY);
        mType = cursor.getInt(PROJECTION_TYPE);   
    }
    
    public String formatTimeStampString(Context context, long when) {
        return formatTimeStampString(context, when, false);
    }

    private String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return DateUtils.formatDateTime(context, when, format_flags);
    }

}
