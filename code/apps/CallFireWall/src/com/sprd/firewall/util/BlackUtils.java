
package com.sprd.firewall.util;

import com.sprd.firewall.db.BlackColumns;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class BlackUtils {

    private static final String TAG = "BlackUtils";

    public static boolean CheckIsBlackNumber(Context context, String str) {
        ContentResolver cr = context.getContentResolver();
        String number;
        String[] columns = new String[] {
                BlackColumns.BlackMumber.MUMBER_VALUE
        };

        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    number = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MUMBER_VALUE));
                    if (PhoneNumberUtils.compareStrictly(str.trim(), number.trim())) {

                        return true;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            else {
                Log.i(TAG, "cursor == null");
            }
        }
        return false;
    }

    public static boolean putToBlockList(Context context, String phoneNumber, int Blocktype,
            String name) {
        ContentResolver cr = context.getContentResolver();
        String normalizeNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
        ContentValues values = new ContentValues();
        if (values != null) {
            try {
                values.put(BlackColumns.BlackMumber.MUMBER_VALUE, phoneNumber);
                values.put(BlackColumns.BlackMumber.BLOCK_TYPE, Blocktype);
                values.put(BlackColumns.BlackMumber.NAME, name);
                values.put(BlackColumns.BlackMumber.MIN_MATCH,
                        PhoneNumberUtils.toCallerIDMinMatch(normalizeNumber));

                Log.d(TAG, "putToBlockList:values=" + values);
            } catch (Exception e) {
                Log.e(TAG, "putToBlockList:exception");
            }
        }
        Uri result = null;
        result = cr.insert(BlackColumns.BlackMumber.CONTENT_URI, values);
        return result != null ? true : false;
    }

    public static boolean deleteFromBlockList(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        String[] columns = new String[] {
                BlackColumns.BlackMumber._ID, BlackColumns.BlackMumber.MUMBER_VALUE,
        };
        String number;
        int result = -1;
        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    number = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MUMBER_VALUE));

                    if (PhoneNumberUtils.compareStrictly(phoneNumber.trim(), number.trim())) {

                        result = cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                                BlackColumns.BlackMumber.MUMBER_VALUE + "='" + number + "'",
                                null);
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                Log.i(TAG, "cursor == null");
            }
        }
        if (result < 0) {
            return false;
        }
        return true;
    }

    public static int getBlockType(Context context, String str) {
        ContentResolver cr = context.getContentResolver();
        String number;
        int type;
        String[] columns = new String[] {
                BlackColumns.BlackMumber.MUMBER_VALUE,
                BlackColumns.BlackMumber.BLOCK_TYPE
        };
        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns,
                null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    number = cursor
                            .getString(cursor
                                    .getColumnIndex(BlackColumns.BlackMumber.MUMBER_VALUE));
                    if (PhoneNumberUtils.compareStrictly(str.trim(),
                            number.trim())) {
                        type = cursor
                                .getInt(cursor
                                        .getColumnIndex(BlackColumns.BlackMumber.BLOCK_TYPE));
                        return type;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                Log.e(TAG, "cursor == null");
            }
        }
        return 0;
    }
}
