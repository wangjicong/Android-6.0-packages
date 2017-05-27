package com.android.messaging.ui.contact;

import com.sprd.plat.mms.plugin.Interface.INotify;
import com.android.messaging.ui.contact.NumberFilter;

class NumberMatchUtils {
    private INotify notify;
    public  final int FAILURE = 0XFF000000;
    public  final int SUCC = 0XEE000000;
    private static NumberMatchUtils instance = new NumberMatchUtils();

    private NumberMatchUtils() {
        notify = new NumberFilter();
    }

    public static NumberMatchUtils getNumberMatchUtils() {
        return instance;
    }

    public INotify getNumberMatchNotify() {
        return notify;
    }
}