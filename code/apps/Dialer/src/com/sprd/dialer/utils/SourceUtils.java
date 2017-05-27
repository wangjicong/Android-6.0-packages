package com.sprd.dialer.utils;

import com.android.dialer.R;

import android.provider.CallLog.Calls;

public class SourceUtils {

    public static final String CALL_LOG_TYPE_EXTRA = "call_log_type";

    public static int getDrawableFromCallType(int callType) {
        switch (callType) {
            case Calls.INCOMING_TYPE:
                return R.drawable.ic_call_incoming_holo_dark_ex;
            case Calls.OUTGOING_TYPE:
                return R.drawable.ic_call_outgoing_holo_dark_ex;
            case Calls.MISSED_TYPE:
                return R.drawable.ic_call_missed_holo_dark_ex;
            case Calls.VOICEMAIL_TYPE:
                return R.drawable.ic_call_voicemail_holo_dark_ex;
            default:
                throw new IllegalArgumentException("Unknow call type = " + callType);
        }
    }

}