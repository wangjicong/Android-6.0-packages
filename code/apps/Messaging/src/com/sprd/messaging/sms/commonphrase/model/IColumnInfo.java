
package com.sprd.messaging.sms.commonphrase.model;

public interface IColumnInfo {
    public static final int ID = 0;
    public static final int PHARSER = 1;
    public static final int TYPE_MMS = 2;
    public static final int TYPE_TEL = 3;
    public static final int CAN_MODIFY = 4;

    public static final int MMS_POS = 8;
    public static final int TEL_POS = 16;
    public static final int TYPE_MASK = 0X0000000F;

    // Query Project;
    public static final String PROJECT[] = {
            "_id", "parser", "tp_mms", "tp_tel", "can_modify"
    };
}
