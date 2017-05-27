/*
 * Copyright (C) 2011,2012 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.stat;

import android.content.Context;
import com.umeng.analytics.MobclickAgent;

import java.util.Map;

class UmengStatApiImpl implements StatApiImpl {

    @Override
    public void updateOnlineConfig(Context context) {
        MobclickAgent.updateOnlineConfig(context);
    }

    @Override
    public void onEventBegin(Context context, String event, String param) {
        MobclickAgent.onEventBegin(context, event,param);
    }

    @Override
    public void onEventEnd(Context context, String event, String param) {
        MobclickAgent.onEventEnd(context, event,param);
    }

    @Override
    public void onEvent(Context context, String event, Map params) {
        MobclickAgent.onEvent(context, event,params);
    }

    @Override
    public void onEvent(Context context, String event, String param) {
        MobclickAgent.onEvent(context, event,param);
    }

    @Override
    public void onResume(Context context) {
        MobclickAgent.onResume(context);
    }

    @Override
    public void onPause(Context context) {
        MobclickAgent.onPause(context);
    }
}
