/*
 * SPRD: create
 */

package com.sprd.server.telecom;

import android.net.Uri;
import android.telephony.TelephonyManager;

public class IccUriUtils {
    public static final int LND = 0;
    public static final int SDN = 1;
    public static final String LIST_TPYE = "list_type";

    public static final String LND_STRING = "lnd";
    public static final String SDN_STRING = "sdn";

    public static final String NAME = "tag";
    public static final String NUMBER = "number";

    public static Uri getIccURI(String path, String subId) {
        String uri = "content://" + "icc" + "/" + getPathName(path, subId);
        return Uri.parse(uri);
    }

    private static String getPathName(String path, String subId) {
        if (TelephonyManager.getDefault().isMultiSimEnabled() && !"E".equals(subId)) {
            return path + "/" + "subId" + "/" + subId ;
        } else {
            return path;
        }
    }
}
