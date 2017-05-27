
package com.sprd.messaging.sms.commonphrase.model;

import android.database.Cursor;

public class ItemData extends ModifyImpl implements IColumnInfo {

    public ItemData() {
    }

    protected ItemData(Cursor cursor) {
        init(cursor);
    }

    protected boolean init(Cursor cursor) {
        if (cursor == null) {
            return false;
        }
        try {

            setRowID(cursor.getInt(ID));
            setPharser(cursor.getString(PHARSER));
            setModify(cursor.getInt(CAN_MODIFY));
            int nSms = cursor.getInt(TYPE_MMS);
            int nTel = cursor.getInt(TYPE_TEL);
            setType(nSms, nTel);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public int getRowID() {
        return mnID;
    }

    public int getType() {
        return mnType;
    }

    public String getPharser() {
        return mStrPharser;
    }

    public boolean canModify() {
        return (mbModify == 1);
    }

    public long getLastModifyTime() {
        if (mlLasttime > 0) {
            return mlLasttime;
        } else {
            return System.currentTimeMillis();
        }
    }

    public void setLastModifyTime(long lastModifyTime) {
        mlLasttime = lastModifyTime;
    }

    public void setRowID(int nID) {
        mnID = nID;
    }

    public void setType(int nMMS, int nTel) {
        if (nMMS != 0) {
            mnType = 1 << MMS_POS;
        }

        if (nTel != 0) {
            mnType |= 1 << TEL_POS;
        }
    }

    public int getMmsType() {
        return (mnType >> MMS_POS & TYPE_MASK);
    }

    public int getTelType() {
        return (mnType >> TEL_POS & TYPE_MASK);
    }

    public void setType(int nType) {
        mnType = nType;
    }

    public void setPharser(String strPharser) {
        mStrPharser = strPharser;
    }

    public void setModify(int nModify) {
        mbModify = nModify;
    }

    public int getModify() {
        return mbModify;
    }

    public void Debug() {
        System.out.println("\r\n<<<=====================start=============================>>>");
        System.out.println("ID=[" + getRowID() + "]");
        System.out.println("Context=[" + getPharser() + "]");
        System.out.println("FLAG=[0X" + Integer.toHexString(getFlag()) + "]");
        System.out.println("MMS=[" + getMmsType() + "]  TEL=[" + getTelType() + "]");
        System.out.println("\r\n<<<====================end==============================>>>");
    }

    public void setIndexOfArray( int nIndex)
    {
         mIndexOfArray = nIndex;
    }

    public int getIndexOfArray()
    {
        return mIndexOfArray;
    }

    private int mnID;
    private int mnType;
    private int mbModify;
    private long mlLasttime;
    private String mStrPharser;
    private int  mIndexOfArray;

    public void swapObject(ItemData data){
        int temp_mnId = mnID;
        int temp_mnType = mnType;
        int temp_mbModify = mbModify;
        long temp_mlLasttime = mlLasttime;
        String temp_mStrPharser = mStrPharser;
        int temp_mIndexOfArray = mIndexOfArray;
        int temp_mnFlag = this.getFlag();

        this.mnID = data.getRowID();
        this.mnType = data.getType();
        this.mbModify = data.getModify();
        this.mlLasttime = data.getLastModifyTime();
        this.mStrPharser = data.getPharser();
        this.mIndexOfArray = data.getIndexOfArray();
        this.setFlag(data.getFlag());

        data.setRowID(temp_mnId);
        data.setType(temp_mnType);
        data.setModify(temp_mbModify);
        data.setLastModifyTime(temp_mlLasttime);
        data.setPharser(temp_mStrPharser);
        data.setIndexOfArray(temp_mIndexOfArray);
        data.setFlag(temp_mnFlag);
    }

    public int getResId() {
        // FIXME: Fixed value.
        return -1;
    }

}
