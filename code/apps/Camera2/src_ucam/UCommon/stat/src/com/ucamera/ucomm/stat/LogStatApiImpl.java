/*
 * Copyright (C) 2011,2013 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.stat;

import android.content.Context;

import java.util.Map;

class LogStatApiImpl implements StatApiImpl {

    private static final void log(String fmt, Object... params) {
        System.out.println(String.format("STAT:" + fmt, params));
    }

    @Override
    public void updateOnlineConfig(Context context) {
        log("[updateOnlineConfig]context=%s", context.getClass().getName());
    }

    @Override
    public void onEventBegin(Context context, String event, String param) {
        log("[onEventBegin]context=%s,event=%s,param=%s", context.getClass().getName(), event,
                param);
    }

    @Override
    public void onEventEnd(Context context, String event, String param) {
        log("[onEventEnd]context=%s,event=%s,param=%s", context.getClass().getName(), event,
                param);
    }

    @Override
    public void onEvent(Context context, String event, Map params) {
        log("[onEvent]context=%s,event=%s,params=%s", context.getClass().getName(), event,
                params.toString());
    }

    @Override
    public void onEvent(Context context, String event, String param) {
        log("[onEvent]context=%s,event=%s,param=%s", context.getClass().getName(), event, param);
    }

    @Override
    public void onResume(Context context) {
        log("[onResume]context=%s", context.getClass().getName());
    }

    @Override
    public void onPause(Context context) {
        log("[onPause]context=%s", context.getClass().getName());
    }
}
