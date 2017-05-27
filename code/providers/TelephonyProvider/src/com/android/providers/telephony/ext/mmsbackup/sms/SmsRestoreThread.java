
package com.android.providers.telephony.ext.mmsbackup.sms;

import android.content.ContentValues;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony;

import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.android.providers.telephony.ext.mmsbackup.Defines;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter;
import com.android.providers.telephony.ext.mmsbackup.WorkingThread;
//import com.android.providers.telephony.ThreadsIdContainer;
import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class SmsRestoreThread extends WorkingThread implements TagStringDef {
    private static final String TAG = "SmsRestoreThread";

    public SmsRestoreThread(PublicParameter db, Context context) {
        super(db, context);
    }

    @Override
    public void run() {
        if (mCancel) {
            BackupLog.log(TAG, "===>>cancel restore sms");
            return;
        }
        BackupLog.log(TAG, "start to restore sms");
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        parameter.beginTimer();
        //ThreadsIdContainer threads = ThreadsIdContainer.getInstance(mContext);
        InputStream inputStream = null;
        try {
            ParcelFileDescriptor fd = getFileDescriptor(Defines.READ, SHORT_MESSAGE_FILE);
            if (fd != null) {
                inputStream = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                SmsRestoreParser parser = new SmsRestoreParser();
                ArrayList<ContentValues> contentsValues = parser.getContentValues(inputStream);
                parser.close();
                if (contentsValues == null || contentsValues.size() == 0) {
                    reportFail(ERROR_SIZE_ZERO, TAG + " contentsValues = " + contentsValues);
                    return;
                }

                parameter.beginTransaction();
                int total = contentsValues.size();
                parameter.setMnTotal(total);
                for (int i = 0; i < total; i++) {
                    if (mCancel) {
                        BackupLog.log(TAG, "===>>cancel restore sms");
                        break;
                    }
                    ContentValues cv = contentsValues.get(i);
                    String tel = (String) cv.get(cADDRESS);
                    List<String> recipients = new ArrayList<String>();
                    recipients.add(tel);
                    long threadId = MmsSmsProviderAdapter.get(mContext).getOrCreateThreadId(recipients,new ArrayList<String>());
                    cv.put(cTHREAD_ID, threadId);
                    long rowID = parameter.insert("sms", "body", cv);
                    ContentValues wordsCv = new ContentValues();
                    wordsCv.put(Telephony.MmsSms.WordsTable.ID, rowID);
                    wordsCv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, cv.getAsString("body"));
                    wordsCv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowID);
                    wordsCv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 1);
                    parameter.insert("words", Telephony.MmsSms.WordsTable.INDEXED_TEXT, wordsCv);
                    if ((i + 1) % 50 == 0) {
                        GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, i + 1, total, null, null);
                        parameter.setTransactionSuccessful();
                        parameter.endTransaction();
                        parameter.notifySmsChange();
                        parameter.beginTransaction();
                        BackupLog.log(TAG, "restore sms finished " + (i+1));
                    }
                }
                //fix for Bug 319529 begin
                GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, total, total, null, null);
                //fix for Bug 319529 end
                parameter.setTransactionSuccessful();
                parameter.endTransaction();
                parameter.endTimer(); // print how long did it take
                GetCallBack().OnNotify(Defines.CMD_REPORT_RESUALT, 0, 0, null, null);
            }else{
                reportFail(Defines.ERROR_FD_NULL, "Restore " + SHORT_MESSAGE_FILE + ", fd = null");
                GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, -1, -1, null, null);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
            reportFail(ERROR_OTHER, e);
            return;
        } finally {

        }
        BackupLog.log(TAG, "finish restore sms");
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

}
