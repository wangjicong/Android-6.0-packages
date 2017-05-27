/*
 * Copyright (C) 2011,2012 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.stat;

import android.content.Context;

import java.util.Map;

interface StatApiImpl {

    public void updateOnlineConfig(Context context);

    public void onEventBegin(Context context, String event, String param);
    public void onEventEnd(Context context, String event, String param);

    public void onEvent(Context context, String event, Map params);
    public void onEvent(Context context, String event, String param);

    public void onResume(Context context);
    public void onPause(Context context);
}
