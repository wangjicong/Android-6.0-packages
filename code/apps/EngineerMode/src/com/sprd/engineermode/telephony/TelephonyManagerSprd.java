package com.sprd.engineermode.telephony;

import android.os.SystemProperties;
import android.util.Log;
import java.util.EnumSet;

public class TelephonyManagerSprd {

    static final String TAG = "TelephonyManagerSprd";
    static final String MODEM_TYPE = "ril.radio.modemtype";

    /** @hide */
    public static final int MODEM_TYPE_GSM = 0;
    /** @hide */
    public static final int MODEM_TYPE_TDSCDMA = 1;
    /** @hide */
    public static final int MODEM_TYPE_WCDMA = 2;
    /** @hide */
    public static final int MODEM_TYPE_LTE = 3;

    /**
     * @hide
     */
    public static int getModemType() {
        String baseBand = SystemProperties.get(MODEM_TYPE, "");
        String modemValue = null;
        if (baseBand != null && !baseBand.equals("")) {
            modemValue = baseBand.trim();
            if (modemValue.equals("1")) {
                return MODEM_TYPE_TDSCDMA;
            } else if (modemValue.equals("2")) {
                return MODEM_TYPE_WCDMA;
            } else if (modemValue.equals("3")) {
                return MODEM_TYPE_LTE;
            }
        }
        Log.d(TAG, "can not get the baseband version");
        return MODEM_TYPE_GSM;
    }
}
