
package com.android.providers.telephony.ext.mmsbackup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony.Mms;

//import com.android.providers.telephony.MmsSmsDatabaseHelper;
import com.android.providers.telephony.ext.adapter.*;
import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;
import com.android.providers.telephony.ext.mmsbackup.mms.MmsBackupThread;
import com.android.providers.telephony.ext.mmsbackup.mms.MmsRestoreThread;
import com.android.providers.telephony.ext.mmsbackup.mms.XmlUtil;
import com.google.android.mms.pdu.PduHeaders;
import com.sprd.appbackup.service.Account;
import com.sprd.plat.Interface.INotify;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MmsManager extends NotifyImpl {
    private final static String TAG = "MmsManager";
    private INotify baseManager = null;
    private PublicParameter parameter = null;
    private Context mContext = null;
    private List<WorkingThread> restoreThreads = null;
    private List<WorkingThread> backThreads = null;
    private boolean mCancel = false;
    private ExecutorService mBackupPool = null;
    private ExecutorService mRestorePool = null;

    public MmsManager() {
    }

    private Context getContext() {
        if (mContext == null) {
            mContext = (Context) GetUserParamterByName(MmsBackupService.TAG);
        }
        return mContext;
    }

    public INotify getBaseManager() {
        if (baseManager == null) {
            baseManager = GetCallBack();
        }
        return baseManager;
    }

    public PublicParameter getParameter() {
        if (parameter == null) {
            //SQLiteDatabase db = MmsSmsDatabaseHelper.getInstance(getContext())
            //        .getWritableDatabase();
            SQLiteDatabase db = MmsSmsProviderAdapter.get(getContext()).getSQLiteDatabase();
            parameter = new PublicParameter(db, this, getContext());
        }
        return parameter;
    }

    /**
     * @param objParam
     */
    private int backup(List<Object> phoneId) {
        BackupLog.log(TAG, "Backup");
        new BackupTask(phoneId).start();
        return SUCC;
    }

    /**
     * @param objParamter
     */
    private int restore() {
        BackupLog.log(TAG, "Restore");
        new RestoreTask().start();
        return SUCC;
    }

    private int cancel() {
        BackupLog.log(TAG, "cancel MmsManager");
        mCancel = true;
        if (mBackupPool != null) {
            mBackupPool.shutdownNow();
        }
        if (mRestorePool != null) {
            mRestorePool.shutdownNow();
        }
        if (restoreThreads != null) {
            for (WorkingThread thread : restoreThreads) {
                thread.OnNotify(CMD_CANCEL, 0, 0, null, null);
            }
            restoreThreads.clear();
        }
        if (backThreads != null) {
            for (WorkingThread thread : backThreads) {
                thread.OnNotify(CMD_CANCEL, 0, 0, null, null);
            }
            backThreads.clear();
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

            case CMD_UPDATE_PROGRESS:
                return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
            case CMD_REPORT_RESUALT:
                return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
            default:
        }
        return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
    }

    class BackupTask extends Thread {
        List<Object> phoneId = null;

        public BackupTask(List<Object> phoneId) {
            this.phoneId = phoneId;
        }

        @Override
        public void run() {
            backThreads = new ArrayList<WorkingThread>();
            StringBuffer phoneIdSelection = new StringBuffer();
            parameter = null; // create a new one
            getParameter().beginTimer();

            List<SubscriptionInfo> activeSubIds = SubscriptionManager.from((Context)GetUserParamterByName(MmsBackupService.TAG)).getActiveSubscriptionInfoList();
            if (phoneId == null || phoneId.size() == 0 || activeSubIds==null || activeSubIds.size() ==phoneId.size() ) {
                     BackupLog.log(TAG, "backup all mms");
            } else {
                phoneIdSelection.append(" and (sub_id = ");
                for (int i = 0; i < phoneId.size(); i++) {
                    phoneIdSelection.append(((Account) (phoneId.get(i))).getAccountId());
                    if (i < phoneId.size() - 1) {
                        phoneIdSelection.append(" or sub_id = ");
                    }
                }
                phoneIdSelection.append(")");
            }
            Cursor cursor = null;
            try {
              //fix for bug 319529 begin
                cursor = getParameter().rawQuery(SqlRawQueryStringAdapter.getMmsBackupRawQuery(phoneIdSelection.toString()), null);
                //fix for bug 319529 end

                int mmsCount = cursor.getCount();
                if (mmsCount < 1) {
                    reportFail(ERROR_OTHER, "mmsCount=" + mmsCount);
                    GetCallBack().OnNotify(Defines.CMD_REPORT_RESUALT, 0, 0, null, null);
                    return;
                }
                getParameter().setMnTotal(mmsCount);
                mBackupPool = Executors.newFixedThreadPool(Runtime.getRuntime()
                        .availableProcessors());
                XmlUtil xml = new XmlUtil("Mms");
                int i = 1;
                int columnIndex = cursor.getColumnIndex("_id");
                while (cursor.moveToNext()) {
                    if (mCancel) {
                        BackupLog.log(TAG, "====>>>cancel backup mms, break");
                        break;
                    }
                    long threadId = cursor.getLong(columnIndex);
                    String pduFileName = i++ + ".pdu";
                    MmsBackupThread thread = new MmsBackupThread(getParameter(), pduFileName,
                            getContext(), threadId, xml);
                    thread.SetCallBack(MmsManager.this, TAG);
                    mBackupPool.submit(thread);
                    backThreads.add(thread);
                }
                mBackupPool.shutdown();
                mBackupPool.awaitTermination(30, TimeUnit.MINUTES);
                BackupLog.log(TAG, "wirte xml file for backup");
                mfd = getFileDescriptor(WRITE, "mms_backup.xml");
                moutputStreamxml = new ParcelFileDescriptor.AutoCloseOutputStream(
                        mfd);
                xml.save(moutputStreamxml);
                getParameter().finalClear();
            } catch (Exception e) {
                e.printStackTrace();

                cancel();
                reportFail(ERROR_OTHER, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (mfd != null) {
                    try {
                        mfd.close();
                        moutputStreamxml.close();

                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

                if (moutputStreamxml != null) {
                    try {
                        moutputStreamxml.close();
                        moutputStreamxml = null;

                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }


            }
            // super.run();
        }

        ParcelFileDescriptor mfd = null;
        FileOutputStream moutputStreamxml = null;

    }

    class RestoreTask extends Thread {
        public RestoreTask() {
        }

        @Override
        public void run() {
            try {
                restoreThreads = new ArrayList<WorkingThread>();
                
                boolean oldVersion = isOldVersionFile();
                if (oldVersion) {
                    mfd = getFileDescriptor(READ, "msg_box.xml");
                } else {
                    mfd = getFileDescriptor(READ, "mms_backup.xml");
                }

                if (mfd == null) {
                    GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, -1, -1, null, null);
                    reportFail(ERROR_FD_NULL, "RestoreTask fd=null");
                    return;
                }

                minputStream = new ParcelFileDescriptor.AutoCloseInputStream(mfd);
                XmlUtil xmlparse = new XmlUtil();
                xmlparse.loadFile(minputStream, XmlUtil.MMS_KEYS);
                ArrayList<ContentValues> conteneValueList = xmlparse
                        .getContentValues();
                if (conteneValueList.size() == 0) {
                    GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, -1, -1, null, null);
                    reportFail(ERROR_OTHER, "conteneValueList.size()=" + conteneValueList.size());
                    return;
                }
                mRestorePool = Executors
                        .newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                parameter = null; // create a new one
                getParameter().setMnTotal(conteneValueList.size());
                getParameter().beginTimer();
                for (ContentValues values : conteneValueList) {
                    if (mCancel) {
                        BackupLog.log(TAG, "====>>> cancel backup mms, break");
                        break;
                    }
                    String pduName = values.getAsString("_id");
                    MmsRestoreThread restoreThread = new MmsRestoreThread(getParameter(), pduName,
                            getContext(), values);
                    restoreThread.SetCallBack(MmsManager.this, null);
                    restoreThreads.add(restoreThread);
                    mRestorePool.submit(restoreThread);
                }
                mRestorePool.shutdown();
                mRestorePool.awaitTermination(30, TimeUnit.MINUTES);
                getParameter().finalClear();
            } catch (Exception e) {
                e.printStackTrace();
                cancel();
                reportFail(ERROR_OTHER, e);
            } finally {
                try {
                    if (mfd != null) {
                        mfd.close();
                        mfd = null;
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }


                try {
                    if (minputStream != null) {
                        minputStream.close();
                        minputStream = null;
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            //super.run();
        }


        ParcelFileDescriptor mfd = null;
        InputStream minputStream = null;
    }
}
