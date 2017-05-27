
package com.android.providers.telephony.ext.mmsbackup.sms;

import android.database.Cursor;
import android.telephony.TelephonyManager;

import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.google.android.mms.pdu.CharacterSets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SmsBackupMessage implements TagStringDef {
    String address;
    int type;
    int read;
    int phone_id;
    int lock;
    long date;
    String body;
    private String TAG = "SmsBackupMessage";

    public SmsBackupMessage(Cursor cursor) {
        try {
            address = cursor.getString(mADDRESS);
            type = cursor.getInt(mTYPE);
            read = cursor.getInt(mREAD);
            phone_id = cursor.getInt(mPHONE_ID);
            lock = cursor.getInt(mLOCKED);
            date = cursor.getLong(mDATE);
            body = cursor.getString(mBODY);
        } catch (Exception e) {
            BackupLog.log(TAG, "the cursor is invalid.", e);
        }
    }

    public String getTel() {
        return address == null ? "" : address;
    }

    public String getType() {
        switch (type) {
            case INBOX:
                return _InBox;
            case SEND:
                return _Send;
                // case DRAFT:
                // return _Draft;
                // case OUTBOX:
                // return _OutBox;
            case FAILED:
                return _Failed;
            default:
                return _InBox;
        }
    }

    public String getRead() {
        switch (read) {
            case READ:
                return _Read;
            case UNREAD:
                return _UnRead;
            default:
                return _Read;
        }
    }

    public String getPhoneId() {
        String id = "0";
        if (isMSMS) {// dual phone
            id = phone_id + 1 + "";
        } else
            id = "0";
        return id;
    }

    public String getLocked() {
        switch (lock) {
            case LOCKED:
                return _Locked;
            case UNLOCKED:
                return _UnLocked;
            default:
                return _UnLocked;
        }
    }

    public String getDate() {
        Date d = new Date(date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String temp = sdf.format(d);
        return temp;

    }

    public String checkContent() {
        String encodeContent = "";
        if (body != null && !body.equals("")) {
            // set QP encoded
            try {
                encodeContent = QuotedPrintable.encode(body, CharacterSets.DEFAULT_CHARSET_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return encodeContent;
    }

    public static boolean isMSMS = TelephonyManager.getDefault().getPhoneCount() > 1;

}
