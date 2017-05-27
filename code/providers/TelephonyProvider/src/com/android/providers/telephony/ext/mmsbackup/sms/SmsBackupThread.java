
package com.android.providers.telephony.ext.mmsbackup.sms;

import android.content.Context;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;

import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.android.providers.telephony.ext.mmsbackup.Defines;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter;
import com.android.providers.telephony.ext.mmsbackup.StorageUtil;
import com.android.providers.telephony.ext.mmsbackup.WorkingThread;
import com.android.providers.telephony.ext.adapter.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import com.sprd.appbackup.service.Account;

public class SmsBackupThread extends WorkingThread implements TagStringDef {
    private String TAG = "SmsBackupThread";
    public final static String SHORT_MESSAGE_FILE = "sms.vmsg";
    List<Object> mPhoneId = null;
    private int mActiveSubIdCount;
    public SmsBackupThread(PublicParameter db, List<Object> phoneId, Context context, int activeSubIdCount) {
        super(db, context);
        mPhoneId = phoneId;
        mActiveSubIdCount = activeSubIdCount;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Writer writer = null;
        try {
            parameter.beginTimer();
            String[] writeArrray = backup();
            if (writeArrray == null || writeArrray.length == 0) {
                reportFail(ERROR_OTHER, "backup sms writeArrray = " + writeArrray);
                return;
            }
            parameter.setMnTotal(writeArrray.length);
            ParcelFileDescriptor fd = getFileDescriptor(WRITE, SHORT_MESSAGE_FILE);
            if (fd == null) {
                reportFail(ERROR_FD_NULL, "backup sms fd == null");
                return;
            }
            OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
                    fd);
            writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            for (int i = 0; i < writeArrray.length; i++) {
                if (mCancel) {
                    break;
                }
                if (StorageUtil.getExternalStorage().getFreeSpace() < (long) writeArrray[i]
                        .length()) {
                    reportFail(ERROR_STORAGE_LACK, "backup sms ERROR_STORAGE_LACK");
                    return;
                }
                writer.write(writeArrray[i]);
                if ((i + 1) % 50 == 0) {
                    GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, i + 1, writeArrray.length,
                            null, null);
                }
            }
            //fix for Bug 319529 begin
            GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, writeArrray.length, writeArrray.length,
                    null, null);
            //fix for Bug 319529 end
            writer.flush();
            writer.close();
            parameter.endTimer();
            GetCallBack().OnNotify(Defines.CMD_REPORT_RESUALT, 0, 0, null, null);
        } catch (Exception e) {
            reportFail(ERROR_EXCEPTION, e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        super.run();
    }

    private String[] backup() {
        Cursor cursor = null;
        BackupLog.log(TAG, "SmsSource::backup()->current phone_id: " + mPhoneId);
        String phoneId = "";
        StringBuffer sBuffer = new StringBuffer();
        if (mPhoneId == null || mPhoneId.size() == 0 || mActiveSubIdCount ==0 || mActiveSubIdCount == mPhoneId.size()) {
             BackupLog.log(TAG, "backup all sms");
        } else {
            sBuffer.append("and ( sub_id = ");
            for (int i = 0; i < mPhoneId.size(); i++) {
                sBuffer.append(((Account) (mPhoneId.get(i))).getAccountId());
                if (i < mPhoneId.size() - 1) {
                    sBuffer.append(" or sub_id = ");
                }
            }
            sBuffer.append(" )");
        }
        phoneId = sBuffer.toString();
	    BackupLog.log(TAG, "SmsSource::backup()->raw query : " + SqlRawQueryStringAdapter.getSmsBackupRawQuery(phoneId));
        try {
            cursor = parameter.rawQuery(SqlRawQueryStringAdapter.getSmsBackupRawQuery(phoneId), null);
            if (cursor != null) {
                int count = cursor.getCount();
                String[] mStreams = new String[count];
                if (count == 0) {
                    reportFail(ERROR_SIZE_ZERO, "sms to backup count == 0");
                    return null;
                }
                if (count > 0) {
                    for (int j = 0; j < count; j++) {
                        if (mCancel) {
                            BackupLog.log(TAG, "cancel backup sms, break");
                            break;
                        }
                        boolean isEnd = cursor.moveToPosition(j);
                        if (!isEnd) {
                            break;
                        }
                        SmsBackupMessage sms = new SmsBackupMessage(cursor);
                        mStreams[j] = getStream(sms).toString();
                    }
                    return mStreams;
                } else {
                    reportFail(ERROR_SIZE_ZERO, "cursor.getCount() == 0");
                    return null;
                }
            } else {
                reportFail(ERROR_OTHER, "backup sms cursor == null");
                return null;
            }
        } catch (Exception e) {
            reportFail(ERROR_EXCEPTION, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private StringBuffer getStream(SmsBackupMessage smsMessage) {
        StringBuffer bf = new StringBuffer();
        String enterStr = "\n";
        bf.append(_begin_vmsg).append(enterStr);
        bf.append(_version).append(_version1_1).append(enterStr);
        bf.append(_begin_vcard).append(enterStr);
        bf.append(_tel).append(smsMessage.getTel()).append(enterStr);
        bf.append(_end_vcard).append(enterStr);
        bf.append(_begin_vbody).append(enterStr);
        bf.append(_box).append(smsMessage.getType()).append(enterStr);
        bf.append(_status).append(smsMessage.getRead()).append(enterStr);
        // default simid is 0,because cmcc phone is single mode
        bf.append(_simid).append(smsMessage.getPhoneId()).append(enterStr);
        bf.append(_locked).append(smsMessage.getLocked()).append(enterStr);
        bf.append(xtypeString).append(enterStr);
        bf.append(_date).append(smsMessage.getDate()).append(enterStr);
        bf.append(contentShareString).append(smsMessage.checkContent()).append(enterStr);
        bf.append(_end_vbody).append(enterStr);
        bf.append(_end_vmsg).append(enterStr).append(enterStr);
        return bf;
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

}
