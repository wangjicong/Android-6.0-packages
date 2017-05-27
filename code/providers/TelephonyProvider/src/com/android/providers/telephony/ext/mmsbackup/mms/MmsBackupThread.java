
package com.android.providers.telephony.ext.mmsbackup.mms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony.Mms;
import com.android.internal.telephony.IccUtils;

import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter;
import com.android.providers.telephony.ext.mmsbackup.StorageUtil;
import com.android.providers.telephony.ext.mmsbackup.WorkingThread;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.android.providers.telephony.ext.adapter.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MmsBackupThread extends WorkingThread {
    private long threadId = -1;
    private XmlUtil mXml = null;
    private String TAG = "MmsBackupThread";

    public MmsBackupThread(PublicParameter db, String name, Context context, long threadId,
            XmlUtil xmlUtil) {
        super(db, context);
        mXml = xmlUtil;
        fileName = name;
        this.threadId = threadId;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        BackupLog.log("backup mms, fileName = " + fileName);
        if (mCancel) {
            BackupLog.log(TAG, "cancel backup mms, break");
            return;
        }
        ContentValues m = getAttribute(threadId);
        ContentValues map = StaticUtil.convertFieldToKey2Backup(m, StaticUtil.TYPE.MMS);
        map.put("_id", fileName);
        synchronized (mXml) {
            mXml.setAttribute(map);
        }

        ParcelFileDescriptor fd = getFileDescriptor(WRITE, fileName);
        if (fd == null) {
            BackupLog.logE(TAG, "Backup " + fileName + ", fd = null");
            return;
        }

        FileOutputStream output = null;
        try {
            byte[] pdu = getPdu(threadId);
            //fix for bug 319529 begin
            if(pdu == null){
                BackupLog.log(TAG, "backup " + fileName + ", pdu==null");
                return;
            }
            //fix for bug 319529 end
            if (StorageUtil.getExternalStorage().getFreeSpace() < (long) pdu.length) {
                reportFail(ERROR_STORAGE_LACK, "backup mms ERROR_STORAGE_LACK");
                return;
            }
            output = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
            output.write(pdu);
        } catch (Exception e) {
            BackupLog.log(TAG, "Backup " + fileName + ", fd = null",e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        parameter.increaseCount();
        super.run();
    }

    @Override
    public int OnNotify(int nMsg, int nValue, long lValue, Object obj, List<Object> listObj) {
        switch (nMsg) {
        // case CMD_BACKUP:
        // new Thread(this).start();
        // return SUCC;
            case CMD_CANCEL:
                stopExec();
                return SUCC;
                //
            default:
        }
        return super.OnNotify(nMsg, nValue, lValue, obj, listObj);
    }

    public ContentValues getAttribute(long id) {

        Cursor cursor = null;
        ContentValues values = new ContentValues();
        try {
            cursor = parameter.rawQuery(SqlRawQueryStringAdapter.getOneMmsBackupRawQuery(id), null);
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    for (int i = 0; i < SqlRawQueryStringAdapter.MmsProjectsForBackup.length; i++) {
                        if (Mms.SUBJECT.equals(SqlRawQueryStringAdapter.MmsProjectsForBackup[i])) {                          
                            Cursor curText = parameter.rawQuery(SqlRawQueryStringAdapter.getMmsBackupAttributeRawQuery(id), null);
                            try {
                                if (curText != null && curText.moveToFirst()) {
                                    if (curText.getCount() > 0) {
                                        values.put(Mms.Part.TEXT, curText.getString(0));
                                        values.put(Mms.Part.CONTENT_TYPE, curText.getString(1));
                                    } else {
                                        values.put(Mms.Part.TEXT, "");
                                        values.put(Mms.Part.CONTENT_TYPE, "");
                                    }
                                } else {
                                    values.put(Mms.Part.TEXT, "");
                                    values.put(Mms.Part.CONTENT_TYPE, "");
                                }
                            } finally {
                                if (curText != null) {
                                    curText.close();
                                }
                            }
                        } else {
                            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values,
                                    SqlRawQueryStringAdapter.MmsProjectsForBackup[i]);
                        }
                    }
                }
            }
        } finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return values;
    }

    private byte[] getPdu(long id) {
        byte[] pdu = null;
        try {// make Pdu
            //PduPersister persister = PduPersister.getPduPersister(mContext);
            SQLiteDatabase db = parameter.getDb();
            GenericPdu backPdu = (GenericPdu) LocalPduPersisterAdapter.get(mContext).loadForBackupMms(db, id);
            pdu = new PduComposer(mContext, backPdu).make();
        } catch (Exception e) {
            BackupLog.log(TAG, "getPdu MmsException", e);
            e.printStackTrace();
            return null;
        }
        return pdu;
    }
}
