
package com.android.providers.telephony.ext.mmsbackup.mms;

import android.content.ContentValues;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.TelephonyManager;
import com.android.providers.telephony.ext.mmsbackup.BackupLog;

public class StaticUtil {
    private static final String TAG = "StaticUtil";

    private final static int READ = 1;
    private final static String SIM_ID_0 = "0";
    private final static String SIM_ID_1 = "1";
    private final static String SIM_ID_2 = "2";
    private final static int PHONE_ID_0 = 0;
    private final static int PHONE_ID_1 = 1;
    private final static int ISREAD = 1;// MMS_FIELDS[1],read
    private final static int PHONE_ID = 5;// default MMS_FIELDS[5],phone_id

    public final static int FLAG_FAIL = -1;
    public final static int FLAG_SUCCESS = 0;
    public final static int FLAG_SDCARD_STORAGE_LACK = 1;
    public final static int FLAG_INTERNAL_STORAGE_LACK = 2;
    public final static int FLAG_FILE_INVLALD = 3;

    public static enum TYPE {
        SMS, MMS
    }

    public static final String[] MMS_KEYS = new String[] {
            "_id", "isread", "msg_box",
            "date", "m_size", "sim_id",
            "islocked"
    };

    public static final String[] MMS_FIELDS = new String[] {
            Mms._ID, Mms.READ, Mms.MESSAGE_BOX,
            Mms.DATE, Mms.MESSAGE_SIZE, Mms.SUBSCRIPTION_ID,
            Mms.LOCKED
    };

    public static final String[] SMS_KEYS = new String[] {
            "category",
            "_id", "isread", "local_date", "msg_box", "st", "date", "m_size",
    };

    public static final String[] SMS_FIELDS = new String[] {
            "xxx1"/* default 0 */, Sms._ID, Sms.READ, "xxx2"/* default 0 */,
            Sms.TYPE, Sms.STATUS, Sms.DATE, "xxx3"/* default 0 */
    };

    public static ContentValues convertKeyToField2Restore(ContentValues values, TYPE type) {
        ContentValues v = new ContentValues();
        String[] fields;
        String[] keys;
        if (TYPE.MMS == type) {
            keys = MMS_KEYS;
            fields = MMS_FIELDS;
        } else if (TYPE.SMS == type) {
            keys = SMS_KEYS;
            fields = SMS_FIELDS;
        } else {
            return v;
        }

        boolean findSubId = false;
        for (int index = 0; index < keys.length; index++) {
            String key = keys[index];
            Object o = values.get(key);
            //if (index == PHONE_ID) {// use true value
            //    v.put(fields[index], getPhoneidValue(String.valueOf(o)));
            //} else {
            if (o != null) {
                 if (key.equals("sim_id")){
                     findSubId = true;
                 }
                 String value = String.valueOf(o);
                 String field = fields[index];
                 if (field.startsWith("xxx")) {
                        // BackupLog.log("Key = " + key + ",Field = " + field +
                        // ", not push");
                     continue;
                 }
		   //BackupLog.log(TAG, "type = " + type + ",Field = " + field +", value="+value);
                 v.put(field, value);
            }
            //}
        }
        if (!findSubId){
             v.put(Mms.SUBSCRIPTION_ID, String.valueOf(1));
        }
        return v;
    }

    public static ContentValues convertFieldToKey2Backup(ContentValues values, TYPE type) {
        ContentValues v = new ContentValues();
        String[] fields;
        String[] keys;
        if (TYPE.MMS == type) {
            keys = MMS_KEYS;
            fields = MMS_FIELDS;
        } else {
            return v;
        }
        for (int index = 0; index < fields.length; index++) {
            String field = fields[index];
            Object o = values.get(field);
            if (index == PHONE_ID) {// default MMS_FIELDS[5],phone_id
                v.put(keys[index], getPhoneId(Integer.parseInt(String.valueOf(o))));
            } else {
                if (o != null) {
                    String value = String.valueOf(o);
                    String key = keys[index];
                    // BackupLog.log("field = " + field + ",key = " +
                    // keys[index] + ", push");
                    v.put(key, value);
                } else {
                    // set default value
                    if (index == ISREAD) {// default MMS_FIELDS[1],read
                        v.put(keys[index], READ);
                    }
                    // BackupLog.log("field = " + field + ",key = " +
                    // keys[index] + ", set default 0");
                    v.put(keys[index], 0);
                }
            }
        }
        return v;
    }

    public static int getPhoneidValue(String phoneid) {
        if (SIM_ID_0.equals(phoneid) || SIM_ID_1.equals(phoneid)) {
            return PHONE_ID_0;
        } else if (SIM_ID_2.equals(phoneid)) {
            return PHONE_ID_1;
        } else {
            BackupLog.log(TAG, "the values of phone_id is error !!!");
            return PHONE_ID_0;
        }
    }

    public static String getPhoneId(int phoneId) {
        String id = "0";
        if (isMSMS) {// dual phone
            id = phoneId + 1 + "";
        } else
            id = "0";
        return id;
    }

    private static boolean isMSMS = TelephonyManager.getDefault().getPhoneCount() > 1;
}
