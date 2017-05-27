
package com.sprd.firewall.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.sprd.firewall.model.BlackCallEntity;
import com.sprd.firewall.model.BlackNumberEntity;

public class BlackCallsDb {
    protected static final String TAG = "BlackCallsDb";

    private final Context context;

    public static Vector<BlackNumberEntity> BlackNumberVector = new Vector<BlackNumberEntity>();

    public static Vector<BlackCallEntity> BlackCallVector = new Vector<BlackCallEntity>();

    public static final String AUTHORITY = "com.android.providers.contacts.block";

    public static final class BlockMumber implements BaseColumns {
        public static final Uri CONTENT_URI = Uri
                .parse("content://com.android.providers.contacts.block/block_mumbers");

        public static final String MUMBER_VALUE = "mumber_value";

        public static final String BLOCK_TYPE = "block_type";

        public static final String NOTES = "notes";

        public static final String NAME = "name";

        public static final String MIN_MATCH = "min_match";
    }

    public static final class BlockRecorder implements BaseColumns {
        public static final Uri CONTENT_URI = Uri
                .parse("content://com.android.providers.contacts.block/block_recorded");

        public static final String MUMBER_VALUE = "mumber_value";

        public static final String CALL_TYPE = "call_type";

        public static final String BLOCK_DATE = "block_date";

        public static final String NAME = "name";
    }

    public BlackCallsDb(Context context) {
        this.context = context;
    }

    public List<BlackCallEntity> QueryBlackRecoder() {
        ContentResolver cr = context.getContentResolver();
        String[] columns = new String[] {
                BlockRecorder.MUMBER_VALUE, BlockRecorder.CALL_TYPE,
                BlockRecorder.BLOCK_DATE, BlockRecorder.NAME
        };

        Cursor cursor = cr.query(BlockRecorder.CONTENT_URI, columns, null, null, null);
        BlackCallVector = new Vector<BlackCallEntity>();
        List<BlackCallEntity> result = new ArrayList<BlackCallEntity>();

        try {
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    BlackCallEntity BlackCalls = new BlackCallEntity();
                    BlackCalls.setId(cursor.getInt(cursor.getColumnIndex(BlockRecorder._ID)));
                    BlackCalls.setNumber(cursor.getString(cursor
                            .getColumnIndex(BlockRecorder.MUMBER_VALUE)));
                    BlackCalls.setType(cursor.getInt(cursor
                            .getColumnIndex(BlockRecorder.CALL_TYPE)));
                    BlackCalls.setTime(cursor.getLong(cursor
                            .getColumnIndex(BlockRecorder.BLOCK_DATE)));
                    BlackCalls.setName(cursor.getString(cursor
                            .getColumnIndex(BlockRecorder.NAME)));
                    result.add(BlackCalls);
                    cursor.moveToNext();
                    BlackCallVector.add(BlackCalls);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    public List<BlackNumberEntity> QueryBlackCalls() {
        ContentResolver cr = context.getContentResolver();
        String[] columns = new String[] {
                BlockMumber.MUMBER_VALUE, BlockMumber.BLOCK_TYPE, BlockMumber.NAME
        };
        Cursor cursor = cr.query(BlockMumber.CONTENT_URI, columns, null, null, null);
        BlackNumberVector = new Vector<BlackNumberEntity>();
        List<BlackNumberEntity> result = new ArrayList<BlackNumberEntity>();

        try {
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    BlackNumberEntity BlackCalls = new BlackNumberEntity();
                    BlackCalls.setId(cursor.getInt(cursor.getColumnIndex(BlockMumber._ID)));
                    BlackCalls.setNumber(cursor.getString(cursor
                            .getColumnIndex(BlockMumber.MUMBER_VALUE)));
                    BlackCalls
                            .setType(cursor.getInt(cursor.getColumnIndex(BlockMumber.BLOCK_TYPE)));
                    BlackCalls.setNotes(cursor.getString(cursor.getColumnIndex(BlockMumber.NOTES)));
                    BlackCalls.setName(cursor.getString(cursor.getColumnIndex(BlockMumber.NAME)));
                    BlackCalls.setMinmatch(cursor.getString(cursor
                            .getColumnIndex(BlockMumber.MIN_MATCH)));
                    result.add(BlackCalls);
                    cursor.moveToNext();
                    BlackNumberVector.add(BlackCalls);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public boolean AddBlackRecoders(String BlackCallsNumber, int calltype, String date, String name) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(BlockRecorder.MUMBER_VALUE, BlackCallsNumber.trim());
        values.put(BlockRecorder.CALL_TYPE, calltype);
        values.put(BlockRecorder.BLOCK_DATE, date);
        values.put(BlockRecorder.NAME, name);

        return cr.insert(BlockRecorder.CONTENT_URI, values) != null;
    }

    public boolean AddBlackCalls(String BlackCallsNumber, int Type, String name) {
        ContentResolver cr = context.getContentResolver();
        // SPRD: modify for bug 499921
        cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                BlackColumns.BlackMumber.MUMBER_VALUE + "='"
                + BlackCallsNumber + "'", null);
        ContentValues values = new ContentValues();
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(BlackCallsNumber);
        values.put(BlackColumns.BlackMumber.MUMBER_VALUE, BlackCallsNumber);
        values.put(BlackColumns.BlackMumber.BLOCK_TYPE, Type);
        values.put(BlackColumns.BlackMumber.NAME, name);
        values.put(BlackColumns.BlackMumber.MIN_MATCH,
                PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber));
        return cr.insert(BlackColumns.BlackMumber.CONTENT_URI, values) != null;
    }

    public boolean DelBlackCalls(String BlackCallsNumber) {
        ContentResolver cr = context.getContentResolver();

        return cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                BlackColumns.BlackMumber.MUMBER_VALUE + "='" + BlackCallsNumber + "'", null) > 0;
    }

    public boolean UpdateBlackType(String BlackCallsNumber, int Type) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(BlackColumns.BlackMumber.BLOCK_TYPE, Type);
        return cr.update(BlackColumns.BlackMumber.CONTENT_URI, values,
                BlackColumns.BlackMumber.MUMBER_VALUE + "='" + BlackCallsNumber + "'", null) > 0;
    }

    public boolean UpdateBlackNumber(String BlackCallsNumber, String NewBlackNumber, int Type,
            String BlackCallsName) {
        ContentResolver cr = context.getContentResolver();
        int result = -1;
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(NewBlackNumber);
        ContentValues values = new ContentValues();
        values.put(BlackColumns.BlackMumber.MUMBER_VALUE, NewBlackNumber);
        values.put(BlackColumns.BlackMumber.BLOCK_TYPE, Type);
        values.put(BlackColumns.BlackMumber.NAME, BlackCallsName);
        values.put(BlackColumns.BlackMumber.MIN_MATCH,
                PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber));
        Log.d(TAG, "values" + values);
        String mumber_value = null;
        String[] columns = new String[] {
                BlackColumns.BlackMumber._ID, BlackColumns.BlackMumber.MUMBER_VALUE,
                BlackColumns.BlackMumber.BLOCK_TYPE, BlackColumns.BlackMumber.NOTES,
                BlackColumns.BlackMumber.NAME, BlackColumns.BlackMumber.MIN_MATCH
        };

        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    mumber_value = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MUMBER_VALUE));

                    if (PhoneNumberUtils.compareStrictly(BlackCallsNumber.trim(),
                            mumber_value.trim())) {
                        result = cr.update(BlackColumns.BlackMumber.CONTENT_URI, values,
                                BlackColumns.BlackMumber.MUMBER_VALUE + "='" + mumber_value + "'",
                                null);
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                Log.v(TAG, "cursor == null");
            }
        }

        if (result < 0) {
            return false;
        }
        return true;
    }

    public boolean DelBlackLogs(String BlackLogsNumber) {

        ContentResolver cr = context.getContentResolver();
        return cr.delete(BlockRecorder.CONTENT_URI, BlockRecorder.MUMBER_VALUE + "='"
                + BlackLogsNumber.trim() + "'", null) > 0;
    }

    public boolean DelNewBlackLogs(String BlackLogsNumber) {

        ContentResolver cr = context.getContentResolver();
        return cr.delete(BlackColumns.BlockRecorder.CONTENT_URI,
                BlackColumns.BlockRecorder.MUMBER_VALUE + "='" + BlackLogsNumber + "'", null) > 0;
    }

    public boolean DelNewBlackLogsFromId(Integer LogsId) {

        ContentResolver cr = context.getContentResolver();
        return cr.delete(BlackColumns.BlockRecorder.CONTENT_URI, BlackColumns.BlockRecorder._ID
                + "='" + LogsId + "'", null) > 0;
    }

    public boolean DelSmsLogs(String SmsLogsNumber) {

        ContentResolver cr = context.getContentResolver();
        return cr.delete(BlackColumns.SmsBlockRecorder.CONTENT_URI,
                BlackColumns.SmsBlockRecorder.MUMBER_VALUE + "='" + SmsLogsNumber + "'", null) > 0;
    }

    public boolean DelSmsLogs_from_id(Integer SmsId) {

        ContentResolver cr = context.getContentResolver();
        return cr.delete(BlackColumns.SmsBlockRecorder.CONTENT_URI,
                BlackColumns.SmsBlockRecorder._ID + "='" + SmsId + "'", null) > 0;
    }

    public boolean FindAddNumber(String AddNumber) {
        boolean flg;

        ContentResolver cr = context.getContentResolver();
        Cursor cursor;
        String[] columns = new String[] {
                BlackColumns.BlackMumber._ID, BlackColumns.BlackMumber.MUMBER_VALUE,
                BlackColumns.BlackMumber.BLOCK_TYPE, BlackColumns.BlackMumber.NAME
        };
        String selection = BlackColumns.BlackMumber.MUMBER_VALUE + "='" + AddNumber + "'";
        cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, selection, null, null);
        Integer CurCount = 0;
        if (cursor != null) {
            CurCount = cursor.getCount();
            cursor.close();
        }
        if (CurCount != 0) {
            flg = true;
            Log.v(TAG, "Number already exists, please re-fill");
            return flg;
        } else {
            flg = false;
            return flg;
        }
    }

    public String FindPhoneNamebyNumber(String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor;
        String phoneName = null;
        String min_match = null;
        String[] columns = new String[] {
            BlackColumns.BlackMumber.NAME, BlackColumns.BlackMumber.MIN_MATCH
        };
        cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    min_match = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MIN_MATCH));
                    String incoming = PhoneNumberUtils.toCallerIDMinMatch(phoneNumber);
                    if (PhoneNumberUtils.compareStrictly(incoming,min_match)) {
                        phoneName = cursor.getString(cursor.getColumnIndex(BlockMumber.NAME));
                        Log.v(TAG, "phoneName=" + phoneName);
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                Log.v(TAG, "cursor == null");
            }
        }
        return phoneName;
    }

    public List<BlackNumberEntity> selectBlacklistByNumber(String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        List<BlackNumberEntity> result = new ArrayList<BlackNumberEntity>();
        String mumber_value = null;
        String[] columns = new String[] {
                BlackColumns.BlackMumber._ID, BlackColumns.BlackMumber.MUMBER_VALUE,
                BlackColumns.BlackMumber.BLOCK_TYPE, BlackColumns.BlackMumber.NOTES,
                BlackColumns.BlackMumber.NAME
        };

        Cursor cursor = cr.query(BlackColumns.BlackMumber.CONTENT_URI, columns, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    mumber_value = cursor.getString(cursor.getColumnIndex(
                            BlackColumns.BlackMumber.MUMBER_VALUE));

                    if (PhoneNumberUtils.compareStrictly(phoneNumber.trim(), mumber_value.trim())) {
                        BlackNumberEntity BlackCalls = new BlackNumberEntity();
                        BlackCalls.setId(cursor.getInt(cursor.getColumnIndex(BlockMumber._ID)));
                        BlackCalls.setNumber(cursor.getString(cursor
                                .getColumnIndex(BlockMumber.MUMBER_VALUE)));
                        BlackCalls.setType(cursor.getInt(cursor
                                .getColumnIndex(BlockMumber.BLOCK_TYPE)));
                        BlackCalls.setNotes(cursor.getString(cursor
                                .getColumnIndex(BlockMumber.NOTES)));
                        BlackCalls
                                .setName(cursor.getString(cursor.getColumnIndex(BlockMumber.NAME)));
                        result.add(BlackCalls);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                Log.v(TAG, "cursor == null");
            }
        }
        return result;
    }

}
