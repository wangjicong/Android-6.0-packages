package com.sprd.dialer.calllog;

import android.provider.CallLog.Calls;

public interface CallLogClearColumn {

    /** The projection to use when querying the call log table */
    public static final String[] CALL_LOG_PROJECTION = new String[] {
            Calls._ID,
            Calls.NUMBER,
            Calls.DATE,
            Calls.DURATION,
            Calls.TYPE,
            Calls.CACHED_NAME,
            Calls.CACHED_NUMBER_TYPE,
            Calls.CACHED_NUMBER_LABEL, /* TODO,
            Calls.SUB_ID*/
            /* SPRD: add for search call log in google search box feature @{ */
            Calls.PHONE_ACCOUNT_ID,
            Calls.PHONE_ACCOUNT_COMPONENT_NAME,
            Calls.PHONE_ACCOUNT_ID
            /* @} */
    };

    public static final int ID_COLUMN_INDEX = 0;
    public static final int NUMBER_COLUMN_INDEX = 1;
    public static final int DATE_COLUMN_INDEX = 2;
    public static final int DURATION_COLUMN_INDEX = 3;
    public static final int CALL_TYPE_COLUMN_INDEX = 4;
    public static final int CALLER_NAME_COLUMN_INDEX = 5;
    public static final int CALLER_NUMBERTYPE_COLUMN_INDEX = 6;
    public static final int CALLER_NUMBERLABEL_COLUMN_INDEX = 7;
    public static final int SIM_COLUMN_INDEX = 8;
    /* SPRD: add for search call log in google search box feature @{ */
    public static final int ACCOUNT_COMPONENT_NAME = 9;
    public static final int ACCOUNT_ID = 10;
    /* @} */
}
