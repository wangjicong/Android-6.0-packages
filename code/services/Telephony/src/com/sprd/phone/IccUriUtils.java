/*
 * SPRD: create
 */

package com.sprd.phone;

import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.ITelephony;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.telephony.TelephonyManager;

public class IccUriUtils {
    public static final String AUTHORITY = "icc";

    public static final int LND = 0;
    public static final int SDN = 1;
    public static final String LIST_TPYE = "list_type";

    public static final String LND_STRING = "lnd";
    public static final String SDN_STRING = "sdn";

    public static final int MIN_PIN_LENGTH = 4;
    public static final int MAX_PIN_LENGTH = 8;
    public static final int MAX_INPUT_TIMES =3;

    public static Uri getIccURI(String path, int subId) {
        String uri = "content://" + AUTHORITY + "/" + getPathName(path, subId);
        return Uri.parse(uri);
    }

    private static String getPathName(String path, int subId) {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            return path + "/" + "subId" + "/" + subId ;
        } else {
            return path;
        }
    }

    /**
     * SPRD: get remain times func
     * @param type
     * @return
     */
    public static int getRemainTimes(Context context, int type, int subId) {
        // TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        try {
            return telephony.getRemainTimesForSubscriber(type, subId);
        } catch (RemoteException ex) {
            Log.e("IccUri", "Error calling ITelephony#getRemainTimesForSubscriber", ex);
            return -1;
        } catch (NullPointerException ex) {
            Log.e("IccUri", "Error calling ITelephony#getRemainTimesForSubscriber", ex);
            return -1;
        }
    }

    /**
     * SPRD: Validate the pin entry.
     *
     * @param pin This is the pin to validate
     * @param isPuk Boolean indicating whether we are to treat the pin input as
     *            a puk.
     */
    public static boolean validatePin(String pin, boolean isPUK) {
        // for pin, we have 4-8 numbers, or puk, we use only 8.
        int pinMinimum = isPUK ? MAX_PIN_LENGTH : MIN_PIN_LENGTH;

        // check validity
        if (pin == null || pin.length() < pinMinimum || pin.length() > MAX_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }
}
