
package com.android.providers.telephony.ext.mmsbackup;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.os.Binder;

import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;
import com.android.providers.telephony.R;

import com.google.android.mms.pdu.PduHeaders;
import com.sprd.appbackup.service.AbstractAppBackupAgent;
import com.sprd.appbackup.service.Account;
import com.sprd.appbackup.service.Category;
import com.sprd.appbackup.service.IAppBackupAgent;
import com.sprd.appbackup.service.IAppBackupRepository;
import com.sprd.appbackup.service.IAppBackupRestoreObserver;
import com.sprd.plat.Impl.BaseUserMessage;
import com.sprd.plat.Impl.NotifyStatus;
import com.sprd.plat.Interface.INotify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.File;

public class MmsBackupService extends Service implements Defines {
    public final static String TAG = "MmsBackupService";

    public final static int FLAG_FAIL = -1;
    public final static int FLAG_SUCCESS = 0;
    public final static int FLAG_NO_DATA = -2;
    public final static int FLAG_SDCARD_STORAGE_LACK = 1;
    public final static int FLAG_INTERNAL_STORAGE_LACK = 2;
    public final static int FLAG_FILE_INVLALD = 3;
    public final static int FLAG_DUPLICATION_SUCCESS = 5;
    IAppBackupRestoreObserver mObserver = null;
    BaseManager mBaseManager = null;
    IAppBackupRepository mRepo = null;

    IBinder mIBinder = new AbstractAppBackupAgent() {
        public int onBackup(IAppBackupRepository repo, IAppBackupRestoreObserver observer,
                int categoryCode, List<Account> phoneId) {
            return backup(repo, observer, categoryCode, phoneId);
        }

        public int onRestore(IAppBackupRepository repo, IAppBackupRestoreObserver observer,
                int categoryCode) {
            return restore(repo, observer, categoryCode);
        }

        public boolean isEnabled(int categoryCode) {
            return MmsBackupService.this.isEnabled(categoryCode);
        }

        public Category[] getCategory() {
            return MmsBackupService.this.getCategory();
        }

        public int onCancel(int categoryCode) {
            return cancel(categoryCode);
        }

        public String getBackupInfo(IAppBackupRepository repo) {
            return MmsBackupService.this.getBackupInfo(repo);
        }

        public List<Account> getAccounts() {
            return MmsBackupService.this.getAccounts();
        }

        public int onDeduplicate(IAppBackupRestoreObserver observer, final int categoryCode) {
            return deduplicate(observer, categoryCode);
        }
    };

    // /////////////mBaseManager
    public int backup(IAppBackupRepository repo, IAppBackupRestoreObserver observer,
            int categoryCode, List<Account> phoneId) {
        final long origId = Binder.clearCallingIdentity();
        mObserver = observer;
        mRepo = repo;
        int ret = 0;
        List<Object> phoneIds = new ArrayList<Object>();
        for (Account a : phoneId) {
            phoneIds.add(a);
        }
        ret = getBaseManager().OnNotify(CMD_BACKUP, categoryCode, 0, (Object) repo, phoneIds);
        if (NotifyStatus.IsSucc(ret)) {
            ret = FLAG_SUCCESS;
        } else {
            ret = FLAG_FAIL;
        }
        //SQLiteDatabase db = MmsSmsProviderAdapter.get(MmsBackupService.this).getSQLiteDatabase();
        //updateAllThreads(db, null, null);
        Binder.restoreCallingIdentity(origId);
        return ret;
    }

    public int restore(IAppBackupRepository repo, IAppBackupRestoreObserver observer,
            int categoryCode) {
        final long origId = Binder.clearCallingIdentity();
        mObserver = observer;
        mRepo = repo;
        int ret = 0;
        ret = getBaseManager().OnNotify(CMD_RESTORE, categoryCode, 0, (Object) repo, null);
        if (NotifyStatus.IsSucc(ret)) {
            ret = FLAG_SUCCESS;
        } else {
            ret = FLAG_FAIL;
        }
        //SQLiteDatabase db = MmsSmsProviderAdapter.get(MmsBackupService.this).getSQLiteDatabase();
        //updateAllThreads(db, null, null);
        Binder.restoreCallingIdentity(origId);
        return ret;
    }

    public int cancel(int categoryCode) {
        final long origId = Binder.clearCallingIdentity();
        int ret = 0;
        ret = getBaseManager().OnNotify(CMD_CANCEL, categoryCode, 0, null, null);
        if (NotifyStatus.IsSucc(ret)) {
            ret = FLAG_SUCCESS;
        } else {
            ret = FLAG_FAIL;
        }
        Binder.restoreCallingIdentity(origId);
        return ret;
    }

    public boolean isEnabled(int categoryCode) {
        final long origId = Binder.clearCallingIdentity();
        InOutParameter retParameter = new InOutParameter();
        int ret = getBaseManager().OnNotify(CMD_GET_STATE, categoryCode, 0, retParameter, null);
        if (NotifyStatus.IsFailure(ret)) {
            return false;
        }
        BackupLog.log(TAG, "isEnabled");
        Binder.restoreCallingIdentity(origId);
        return retParameter.getBoolean();
    }

    private int deduplicate(IAppBackupRestoreObserver observer, final int categoryCode) {
        BackupLog.log(TAG, "onDeduplicate, categoryCode = " + categoryCode);
        final long origId = Binder.clearCallingIdentity();
        final IAppBackupRestoreObserver dObserver = observer;
        new Thread(new Runnable() {
            public void run() {
                try {
                   // SQLiteDatabase db = MmsSmsDatabaseHelper.getInstance(MmsBackupService.this)
                   //         .getWritableDatabase();
                    SQLiteDatabase db = MmsSmsProviderAdapter.get(MmsBackupService.this).getSQLiteDatabase();
                    if (categoryCode == SHORT_MESSAGE_CODE) {
                        uniqueSms(db);
                        dObserver.onResult(FLAG_DUPLICATION_SUCCESS);
                    } else if (categoryCode == MEDIA_MESSAGE_CODE) {
                        uniqueMms(db);
                        dObserver.onResult(FLAG_DUPLICATION_SUCCESS);
                    }
                } catch (RemoteException e) {
                    BackupLog.log(TAG, "Exception in onDeduplicate " + e);
                    try {
                        dObserver.onResult(FLAG_FAIL);
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }).start();
        Binder.restoreCallingIdentity(origId);
        return 0;
    }

    private void uniqueSms(SQLiteDatabase db) {
        long t = System.currentTimeMillis();
        String sql = "delete from sms where _id in("
                + "select _id from sms where _id not in ("
                + "select _id from sms group by substr(date,0,length(date)-2), address, body, type))";
        db.execSQL(sql);
        updateAllThreads(db, null, null);
        getContentResolver().notifyChange(MmsSms.CONTENT_URI, null);
        getContentResolver().notifyChange(Mms.CONTENT_URI, null);
        BackupLog.log(TAG, "uniqueSms cost " + (System.currentTimeMillis() - t) + " ms");
    }

    private void updateAllThreads(final SQLiteDatabase db, String where, String[] whereArgs){
        new Thread(){
            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                MmsSmsProviderAdapter.get(MmsBackupService.this).updateAllThreads(db, null, null);
                super.run();
            }
        }.start();
    }

    private void uniqueMms(SQLiteDatabase db) {
        long t = System.currentTimeMillis();
        List<String> filesToDelete = new ArrayList<String>();
        Cursor cursor = db
                .rawQuery("select _data from part where mid in (" +
                        "select _id from pdu where _id not in (" +
                        "select _id from pdu group by date, tr_id, thread_id))",
                        null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        String path = cursor.getString(cursor.getColumnIndex("_data"));
                        if ((path != null) && (!path.equals(""))) {
                            filesToDelete.add(path);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(filesToDelete.size() % 100 == 0){
                        deleteFiles(filesToDelete);
                    }
                } while (cursor.moveToNext());
                deleteFiles(filesToDelete);
                String sql = "delete from pdu where _id in " +
                        "(select _id from pdu where _id not in (" +
                        "select _id from pdu group by date, tr_id, thread_id))";
                db.execSQL(sql);
                getContentResolver().notifyChange(MmsSms.CONTENT_URI, null);
                getContentResolver().notifyChange(Mms.CONTENT_URI, null);
                updateAllThreads(db, null, null);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        BackupLog.log(TAG, "uniqueMms cost " + (System.currentTimeMillis() - t) + " ms");
    }

    /*Modify by SPRD for Bug:548912  2016.04.15 Start */
    /*Add by SPRD for Bug:536627  2016.03.15 Start */
    private int getSmsCountBySubId(SQLiteDatabase db,int subId){
        Cursor cursor_sms = db.rawQuery("select count(*) from sms where sub_id ="+subId,null);
//        String body = "";
        int smsCount = 0;
        try {
            if(cursor_sms != null && cursor_sms.moveToFirst()){
                try {
                    smsCount = cursor_sms.getInt(0);
                    BackupLog.log(TAG, "smsCount:"+smsCount);
//                    body = cursor.getString(cursor.getColumnIndex("body"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally{
            if (cursor_sms != null) {
                cursor_sms.close();
            }
        }
        return smsCount;
    }
    /*Add by SPRD for Bug:536627  2016.03.15 End */
    /*Modify by SPRD for Bug:548912  2016.04.18 End */

    /*Add by SPRD for Bug:548912  2016.04.18 Start */
    private int getMmsCountBySubId(SQLiteDatabase db,int subId){
        Cursor cursor_mms = db.rawQuery("select count(*) from pdu where sub_id ="+subId,null);
        int mmsCount = 0;
        try {
            if(cursor_mms != null && cursor_mms.moveToFirst()){
                try {
                    mmsCount = cursor_mms.getInt(0);
                    BackupLog.log(TAG, "mmsCount:"+mmsCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally{
            if (cursor_mms != null) {
                cursor_mms.close();
            }
        }
        return mmsCount;
    }
    /*Add by SPRD for Bug:548912  2016.04.18 End */

    private void deleteFiles(List<String> filesToDelete){
        for(String path : filesToDelete){
            try {
                if ((path != null) && (!path.equals(""))) {
                    File file = new File(path);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        filesToDelete.clear();
    }

    public Category[] getCategory() {
        BackupLog.log(TAG, "getCategory");
        final long origId = Binder.clearCallingIdentity();
        Category[] category = new Category[2];
        category[0] = new Category(SHORT_MESSAGE_CODE,
                MmsBackupService.this.getString(R.string.category_sms));
        category[1] = new Category(MEDIA_MESSAGE_CODE,
                MmsBackupService.this.getString(R.string.category_mms));
        Binder.restoreCallingIdentity(origId);
        return category;
    }

    private List<Account> getAccounts() {
        BackupLog.log(TAG, "getAccounts()");
        final long origId = Binder.clearCallingIdentity();
        List<Account> accountList = new ArrayList<Account>();
        List<SubscriptionInfo> activeSubIds = SubscriptionManager.from(getBaseContext()).getActiveSubscriptionInfoList();
        if (!isEmptyCollection(activeSubIds)) {
            /*Add by SPRD for Bug:536627  2016.03.15 {@*/
            SQLiteDatabase db = MmsSmsProviderAdapter.get(MmsBackupService.this).getSQLiteDatabase();
            /*@}*/
            for (int i = 0; i < activeSubIds.size(); i++) {
                String name = MmsBackupService.this.getString(R.string.sim_x, i + 1);
                /*Add by SPRD for Bug:536627  2016.03.15 {@*/
                int subId = activeSubIds.get(i).getSubscriptionId();
                int smsCount = getSmsCountBySubId(db, subId);//Modify for Bug:548912
                int mmsCount = getMmsCountBySubId(db, subId);//Add for Bug:548912
                /*@}*/
                Account account = new Account(String.valueOf(i), name, null, true, true);
                account.setAccountName(name);
                account.setAccountType("type");
                /*Modify by SPRD for Bug:536627  2016.03.15 Start*/
                account.setAccountId(subId + "");
                if (smsCount > 0 || mmsCount > 0) {//Modify for Bug:548912
                    accountList.add(account);
                } else {
                    BackupLog.log(TAG, "The sim:" + subId + " has no message, So do not add this account.");
                }
                /*Modify by SPRD for Bug:536627  2016.03.15 End*/
            }
        } else {
            String name = MmsBackupService.this.getString(R.string.sim_x, "all");
            Account account = new Account("-1", name, null, true, true);
            account.setAccountName(name);
            account.setAccountId("-1");
            account.setAccountType("type");
            accountList.add(account);
        }
        Binder.restoreCallingIdentity(origId);
        return accountList;
    }

    private boolean isEmptyCollection(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public String getBackupInfo(IAppBackupRepository repo) {
        BackupLog.log(TAG, "getBackupInfo");
        return "Mms Backup and Restore";
    }

    @Override
    public IBinder onBind(Intent intent) {
        BackupLog.log(TAG, "Mms: onBind");
        return mIBinder;
    }

    private BaseManager getBaseManager() {
        if (mBaseManager == null) {
            mBaseManager = new BaseManager(this);
            mBaseManager.SetCallBack(getCallBack(), MmsBackupService.TAG);
        }
        return mBaseManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BackupLog.log(TAG, "MmsBackupService: onCreate");
    }

    private INotify getCallBack() {
        return service;
    }

    private INotify service = new ServiceNotify();

    class ServiceNotify extends BaseUserMessage {
        @Override
        public int OnNotify(int nMsg, int nValue, long lValue, Object obj, List<Object> lstObj) {
            try {
                switch (nMsg) {
                    case CMD_REPORT_RESUALT:
                        mObserver.onResult(nValue);
                        if (obj != null) {
                            if (obj instanceof Throwable) {
                                BackupLog.log("REPORT_RESUALT", "", (Throwable) obj);
                            } else if (obj instanceof String) {
                                BackupLog.logE("REPORT_RESUALT", (String) obj);
                            }
                        }
                        return SUCC;
                    case CMD_UPDATE_PROGRESS:
                        mObserver.onUpdate(nValue, (int) lValue);
                        return SUCC;
                    case CMD_GET_PARAMETER:
                        InOutParameter iop;
                        switch (nValue) {
                            case PARAMETER_FD:
                                iop = (InOutParameter) obj;
                                String file = iop.getString();
                                boolean read = iop.getBoolean();
                                ParcelFileDescriptor fd;
                                if (read) {
                                    fd = mRepo.read(file);
                                } else {
                                    fd = mRepo.write(file);
                                }
                                iop.setObject(fd);
                                break;
                            case PARAMETER_VERSION:
                                iop = (InOutParameter) obj;
                                iop.setBoolean(mRepo.isOldVersionFile());
                                break;
                            default:
                                return FAILURE;
                        }
                        return SUCC;
                    default:
                        return FAILURE;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                BackupLog.log(TAG, "OnNotify error ", e);
            }
            return FAILURE;
        }

    }
}
