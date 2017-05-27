
package com.sprd.firewall.util;

import com.sprd.firewall.db.BlackColumns;
import com.sprd.firewall.model.BlackCallEntity;
import com.sprd.firewall.model.BlackNumberEntity;
import com.sprd.firewall.model.BlackSmsEntity;

import android.database.Cursor;

public class BlackEntityUtil {

    public static boolean transform(BlackNumberEntity entity, Cursor cursor) {
        if (entity == null || cursor == null) {
            return false;
        }
        entity.setId(cursor.getInt(cursor.getColumnIndex(BlackColumns.BlackMumber._ID)));
        entity.setNumber(cursor.getString(cursor
                .getColumnIndex(BlackColumns.BlackMumber.MUMBER_VALUE)));
        entity.setType(cursor.getInt(cursor.getColumnIndex(BlackColumns.BlackMumber.BLOCK_TYPE)));
        entity.setNotes(cursor.getString(cursor.getColumnIndex(BlackColumns.BlackMumber.NOTES)));
        entity.setName(cursor.getString(cursor
                .getColumnIndex(BlackColumns.BlackMumber.NAME)));
        return true;
    }

    public static boolean transform(BlackCallEntity entity, Cursor cursor) {
        if (entity == null || cursor == null || cursor.getCount() <= 0) {
            return false;
        }
        entity.setId(cursor.getInt(cursor.getColumnIndex(BlackColumns.BlockRecorder._ID)));
        entity.setNumber(cursor.getString(cursor
                .getColumnIndex(BlackColumns.BlockRecorder.MUMBER_VALUE)));
        entity.setType(cursor.getInt(cursor.getColumnIndex(BlackColumns.BlockRecorder.CALL_TYPE)));
        entity.setTime(cursor.getLong(cursor.getColumnIndex(BlackColumns.BlockRecorder.BLOCK_DATE)));
        entity.setName(cursor.getString(cursor
                .getColumnIndex(BlackColumns.BlockRecorder.NAME)));
        return true;
    }

    public static boolean transform(BlackSmsEntity entity, Cursor cursor) {
        if (entity == null || cursor == null) {
            return false;
        }
        entity.setId(cursor.getInt(cursor.getColumnIndex(BlackColumns.SmsBlockRecorder._ID)));
        entity.setNumber(cursor.getString(cursor
                .getColumnIndex(BlackColumns.SmsBlockRecorder.MUMBER_VALUE)));
        entity.setContent(cursor.getString(cursor
                .getColumnIndex(BlackColumns.SmsBlockRecorder.BLOCK_SMS_CONTENT)));
        entity.setTime(cursor.getLong(cursor
                .getColumnIndex(BlackColumns.SmsBlockRecorder.BLOCK_DATE)));
        entity.setName(cursor.getString(cursor
                .getColumnIndex(BlackColumns.SmsBlockRecorder.NAME)));
        return true;
    }
}
