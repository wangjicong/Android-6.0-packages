package com.sprd.soundrecorder;

import com.android.soundrecorder.R;
import com.android.soundrecorder.R.string;

import android.content.Context;

public class Utils {

    public static String makeTimeString4MillSec(Context context, int millSec) {
        String str = "";
        int hour = 0;
        int minute = 0;
        int second = 0;
        second =  Math.round((float)millSec/1000);
        if (second > 59) {
            minute = second / 60;
            second = second % 60;
        }
        if (minute > 59) {
            hour = minute / 60;
            minute = minute % 60;
        }
        str = (hour < 10 ? "0" + hour : hour) + ":"
                + (minute < 10 ? "0" + minute : minute) + ":"
                + (second < 10 ? "0" + second : second);
        if(hour == 0 && minute ==0 && second == 0){
            str = "< "+ context.getString(R.string.less_than_one_second);
        }
        return str;
    }
}
