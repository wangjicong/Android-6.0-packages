
package com.android.providers.telephony.ext.mmsbackup.mms;

import android.content.ContentValues;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony.Mms;

import com.android.providers.telephony.ext.adapter.*;
import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter.TableInfo;
import com.android.providers.telephony.ext.mmsbackup.WorkingThread;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
//import com.google.android.mms.pdu.PduPersister.TableInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MmsRestoreThread extends WorkingThread {
    private String TAG = "MmsRestoreThread";
    ContentValues mXmlValues = null;
    static final String TABLE_PDU = "pdu";
    static final String TABLE_ADDR = "addr";
    static final String TABLE_PART = "part";
    static final String TABLE_THREADS = "threads";

    public MmsRestoreThread(PublicParameter db, String pduName, Context context,
            ContentValues values) {
        super(db, context);
        fileName = pduName;
        mXmlValues = values;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        BackupLog.log(TAG, "start to restore mms " + fileName);
        if (mCancel) {
            BackupLog.log(TAG, "cancel restore mms, break");
            return;
        }

        ParcelFileDescriptor fd = getFileDescriptor(READ, fileName);
        InputStream inputStream = null;
        try {
            if (fd != null) {
                inputStream = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                ContentValues map = StaticUtil.convertKeyToField2Restore(mXmlValues,
                        StaticUtil.TYPE.MMS);
                map.remove("_id");
                restore(map, inputStream);
            } else {
                BackupLog.logE(TAG, "Restore " + fileName + ", fd = null");
            }
        } catch (Exception e) {
            BackupLog.log(TAG, "Restore " + fileName + ", fd = null",e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.run();
    }

    @Override
    public int OnNotify(int nMsg, int nValue, long lValue, Object obj, List<Object> listObj) {
        switch (nMsg) {
        // case CMD_RESTORE:
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

    private boolean restore(ContentValues values, InputStream file) {
        int subId = -1;
        if (values.containsKey(Mms.SUBSCRIPTION_ID)) {
            Integer integer = values.getAsInteger(Mms.SUBSCRIPTION_ID);
            if (integer != null) {
                subId = integer.intValue();
            }
        }
        byte[] pdu = PduFile.read(file);
        PduPersister p = PduPersister.getPduPersister(mContext);
        PduParser parse = new PduParser(pdu, false);
        //GenericPdu restorePdu = (GenericPdu) (subId == -1 ? parse.parse() : parse.parse(subId));
        GenericPdu restorePdu = (GenericPdu)(parse.parse());
        if (restorePdu == null) {
            BackupLog.logE(TAG, "Restore " + fileName + ", restorePdu = null");
            return false;
        }
        List<TableInfo> tableInfos = null;
        try {
            tableInfos = LocalPduPersisterAdapter.get(mContext).
                               persistForRestoreMms(MmsSmsProviderAdapter.get(mContext).getSQLiteDatabase(),restorePdu, values);
            if (tableInfos == null) {
                BackupLog.logE(TAG, "Restore " + fileName + ", tableInfos == null");
                return false;
            }
            for (TableInfo info : tableInfos) {
                if (info.mTable.equals(TABLE_PDU)) {
                    ContentValues pduValues = info.mValues;
                    /** add value which not in pdu **/
                    pduValues.putAll(values);
                    break;
                }
            }
            parameter.insertMmsTables(tableInfos);
        } catch (Exception e) {
            BackupLog.log(TAG, "Restore " + fileName + ", catch exception ",e);
            return false;
        }
        BackupLog.log(TAG, "restore " + fileName + " return true.");
        return true;
    }

}
