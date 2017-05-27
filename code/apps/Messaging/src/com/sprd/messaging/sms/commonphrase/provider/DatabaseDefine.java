
package com.sprd.messaging.sms.commonphrase.provider;

import com.android.messaging.R;

public interface DatabaseDefine {

    String TABLE_NAME = "pharser";
    String COL_ID = "_id";
    String COL_PHARSER = "pharser";
    String COL_TPYE_MMS = "tp_mms";
    String COL_TPYE_TEL = "tp_tel";
    String COL_CAN_MODIFY = "can_modify";
    String COL_LAST_TIME = "last_time";

    String COL_RES_ID = "res_id";
    int DEFAULT_RES_ID = -1;

    String TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" + COL_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + COL_PHARSER + " TEXT DEFAULT NULL,"
            + COL_TPYE_MMS + " INT," + COL_TPYE_TEL + " INT," + COL_CAN_MODIFY + " INT,"
            + COL_LAST_TIME + " LONG," + COL_RES_ID + " INT DEFAULT " + DEFAULT_RES_ID + ");";

    int[] FIXED_PHRASE = {
            R.array.fixed_mms_phrase, R.array.fixed_tel_phrase, R.array.fixed_both_phrase
    };

}
