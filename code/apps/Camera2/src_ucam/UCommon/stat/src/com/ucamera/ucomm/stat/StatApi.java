/*
 * Copyright (C) 2011,2013 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.stat;

import android.app.Activity;
import android.content.Context;
import java.util.Map;

public class StatApi {

    public static final String EVENT_CAMERA_MODE    = "CAMERA_MODE";
    public static final String EVENT_CAMERA_PREF    = "CAMERA_PREF";
    public static final String EVENT_MODULE_USAGE   = "MODULE_USAGE";
    public static final String EVENT_SNS_SHARE      = "SNS_SHARE";
    public static final String EVENT_SNAP_MODE      = "SNAP_MODE";
    public static final String EVENT_PIP_USAGE      = "PIP_USAGE";
    public static final String EVENT_MAGIC_USAGE    = "MAGIC_USAGE";
    public static final String EVENT_UGIF_USAGE     = "UGIF_USAGE";
    public static final String EVENT_UPHOTO_USAGE   = "UPHOTO_USAGE";
    public static final String EVENT_UPHOTO_BALLOON = "UPHOTO_BALLOON";
    public static final String EVENT_UPHOTO_LABEL   = "UPHOTO_LABEL";
    public static final String EVENT_UPHOTO_TITLE   = "UPHOTO_TITLE";
    public static final String EVENT_UPHOTO_COLOR   = "UPHOTO_COLOR";
    public static final String EVENT_UPHOTO_BRUSH   = "UPHOTO_BRUSH";

    public static final String EVENT_RESOURCE_FRAME         = "RES_USAGE_FRAME";
    public static final String EVENT_RESOURCE_DECOR         = "RES_USAGE_DECOR";
    public static final String EVENT_RESOURCE_FONT          = "RES_USAGE_FONT";
    public static final String EVENT_RESOURCE_PUZZLE        = "RES_USAGE_PUZZLE";
    public static final String EVENT_RESOURCE_TEXTURE       = "RES_USAGE_TEXTURE";
    public static final String EVENT_RESOURCE_PHOTOFRAME    = "RES_USAGE_PHOTOFRAME";
    public static final String EVENT_RESOURCE_MANGA    = "RES_MANGA_PHOTOFRAME";

    public static final String EVENT_CAMERA_PREF_NEW    = "CAMERA_PREF_NEW";
    public static final String EVENT_UGIF_USAGE_NEW     = "UGIF_USAGE_NEW";
    public static final String EVENT_UPHOTO_USAGE_NEW   = "UPHOTO_USAGE_NEW";

    public static final String EVENT_CAMERA_HOTAPP     = "CAMERA_HOTAPP";
    public static final String EVENT_SHAKE_LAUNCH      = "SHAKE_LAUNCH";

    protected static final String EVENT_DOWNLOAD_BASE  = "RES_DOWNLOAD_";

    private static final StatApiImpl sStatApi = null;

    private static StatApiImpl getStatApi() { return sStatApi; }

    public static void onPause(Activity context) {
        StatApiImpl api = getStatApi();
        if (api != null && context != null) {
            api.onPause(context);
            String target = context.getClass().getSimpleName();
            StatApi.onEventEnd(context, StatApi.EVENT_MODULE_USAGE, target);
        }
    }

    public static void onResume(Activity context) {
        StatApiImpl api = getStatApi();
        if (api != null && context != null) {
            api.onResume(context);
            String target = context.getClass().getSimpleName();
            StatApi.onEventBegin(context, StatApi.EVENT_MODULE_USAGE, target);
        }
    }

    public static void onEvent(Context context, String event, String param) {
        StatApiImpl api = getStatApi();
        if (api != null) {
            /*
             * BUG COMMENT: stat is not correct
             * Date: 2014-03-10
             */
            api.onEvent(context, event, param);
//            api.onEventBegin(context, event, param);
        }
    }

    public static void onEvent(Context context, String event, Map params) {
        StatApiImpl api = getStatApi();
        if (api != null) {
            api.onEvent(context, event, params);
        }
    }

    public static void onEventBegin(Context context, String event, String param) {
        StatApiImpl api = getStatApi();
        if (api != null) {
            api.onEventBegin(context, event, param);
        }
    }

    public static void onEventEnd(Context context, String event, String param) {
        StatApiImpl api = getStatApi();
        if (api != null) {
            api.onEventEnd(context, event, param);
        }
    }

    public static void updateOnlineConfig(Context context) {
        StatApiImpl api = getStatApi();
        if (api != null) {
            api.updateOnlineConfig(context);
        }
    }

    public static void downloadResource(Context context, String type, String resid){
        StatApiImpl api = getStatApi();
        if (api != null && type != null && resid != null) {
            StatApi.onEvent(context, StatApi.EVENT_DOWNLOAD_BASE + type.toUpperCase(), resid);
        }
    }


    public static String joinValue(String p, Object ...remains) {
        StringBuilder builder =new StringBuilder();
        builder.append(p);
        for(Object o: remains) {
            builder.append(":").append(o);
        }
        return builder.toString();
    }

    public static String getResourceName(String s) {
        int x = s.lastIndexOf("/");
        if (x != -1) {
            s = s.substring(x+1);
        }
        return s;
    }
}
