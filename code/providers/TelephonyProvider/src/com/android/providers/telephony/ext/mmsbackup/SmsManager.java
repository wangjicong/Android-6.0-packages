
package com.android.providers.telephony.ext.mmsbackup;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

//import com.android.providers.telephony.MmsSmsDatabaseHelper;
import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;
import com.android.providers.telephony.ext.mmsbackup.sms.SmsBackupThread;
import com.android.providers.telephony.ext.mmsbackup.sms.SmsRestoreThread;
import com.android.providers.telephony.ext.mmsbackup.sms.SmsRestoreThread1;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import java.util.List;

public class SmsManager extends NotifyImpl {
    private final static String TAG = "SmsManager";
    private PublicParameter parameter = null;
    private Context mContext = null;
    SmsBackupThread backupThread = null;
    WorkingThread restoreThread = null;

    public SmsManager() {
        BackupLog.log(TAG, "SmsManager");
    }

    private Context getContext() {
        if (mContext == null) {
            mContext = (Context) GetUserParamterByName(MmsBackupService.TAG);
        }
        return mContext;
    }

    private PublicParameter getParameter() {
        if (parameter == null) {
            //SQLiteDatabase db = MmsSmsDatabaseHelper.getInstance(getContext())
            //        .getWritableDatabase();
            SQLiteDatabase db = MmsSmsProviderAdapter.get(getContext()).getSQLiteDatabase();
            parameter = new PublicParameter(db, this, getContext());
        }
        return parameter;
    }

    private int backup(List<Object> phoneId) {
        BackupLog.log(TAG, "backup sms");
        int activeSubIdCount = 0;
	    List<SubscriptionInfo> activeSubIds = SubscriptionManager.from((Context)GetUserParamterByName(MmsBackupService.TAG)).getActiveSubscriptionInfoList();
	    if (activeSubIds!=null){
           activeSubIdCount = activeSubIds.size();
        }
        backupThread = new SmsBackupThread(getParameter(), phoneId, mContext, activeSubIdCount);
        backupThread.SetCallBack(this, TAG);
        new Thread(backupThread).start();
        return SUCC;
    }

    private int restore() {
        BackupLog.log(TAG, "Restore");
        boolean oldVersion = isOldVersionFile();
        if (oldVersion) {
            restoreThread = new SmsRestoreThread1(getParameter(), mContext);
        } else {
            restoreThread = new SmsRestoreThread(getParameter(), mContext);
        }
        restoreThread.SetCallBack(this, TAG);
        new Thread(restoreThread).start();

        return SUCC;
    }

    private int cancel() {
        BackupLog.log(TAG, "cancel SmsManager");
        if (restoreThread != null) {
            restoreThread.OnNotify(CMD_CANCEL, 0, 0, null, null);
            restoreThread = null;
        }
        if (backupThread != null) {
            backupThread.OnNotify(CMD_CANCEL, 0, 0, null, null);
            backupThread = null;
        }
        return SUCC;
    }

    @Override
    public int OnNotify(int nMsg, int nValue, long lValue, Object obj, List<Object> listObj) {
        switch (nMsg) {
            case CMD_BACKUP:
                return backup(listObj);
            case CMD_RESTORE:
                return restore();
            case CMD_CANCEL:
                return cancel();
                //
            case CMD_UPDATE_PROGRESS:
                return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
            case CMD_REPORT_RESUALT:
                return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
            default:
        }
        return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
    }

}
