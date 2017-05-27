
package com.android.providers.telephony.ext.mmsbackup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms.Part;
import android.provider.Telephony.MmsSms;
import com.sprd.plat.Interface.INotify;
//import com.android.providers.telephony.ThreadsIdContainer;
import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;

//import java.util.ArrayList;
import java.util.*;

public class PublicParameter {
    private Integer mnCount = 0;
    private int mnTotal;
    private SQLiteDatabase db;
    private INotify callback;
    private int mSQLCount = 0;
    private int mMmsSQLCount = 0;
    List<List<TableInfo>> mmsList;
    private Context mContext = null;

    static final String TABLE_PDU = "pdu";
    static final String TABLE_ADDR = "addr";
    static final String TABLE_PART = "part";
    static final String TABLE_THREADS = "threads";
    public static final String TAG = "PublicParameter";

    public static class TableInfo {
        public String mTable;
        public ContentValues mValues;
        public HashSet<String> extraParameter;

        public TableInfo(String table, ContentValues values) {
            mTable = table;
            mValues = values;
        }

        public TableInfo(String table, HashSet<String> obj) {
            mTable = table;
            extraParameter = obj;
        }
    }


    public PublicParameter(SQLiteDatabase ddataBase, INotify cbf, Context context) {
        mContext = context;
        callback = cbf;
        db = ddataBase;
        mmsList = new ArrayList<List<TableInfo>>();
    }

//    public ThreadsIdContainer  getThreadsIdContainer() {
//        return ThreadsIdContainer.getInstance(mContext);
//    }

    public void beginTransaction() {
        if (!db.inTransaction()) {
            BackupLog.log(TAG, "beginTransaction");
            db.beginTransaction();
        }
    }

    public void endTransaction() {
        if (db.inTransaction()) {
            BackupLog.log(TAG, "endTransaction");
            db.endTransaction();
            mSQLCount = 0;
        }
    }

    public void setTransactionSuccessful() {
        if (db.inTransaction() && mSQLCount > 0) {
            BackupLog.log(TAG, "setTransactionSuccessful");
            db.setTransactionSuccessful();
        }
    }

    public int getMnCount() {
        return mnCount;
    }

    public void setMnCount(int mnCount) {
        this.mnCount = mnCount;
    }

    public int getMnTotal() {
        return mnTotal;
    }

    int step = 1;

    public void setMnTotal(int mnTotal) {
        this.mnTotal = mnTotal;
        step = mnTotal / 100;
        if (step <= 0) {
            step = 1;
        }
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        mSQLCount++;
        Cursor cursor = db.rawQuery(sql, selectionArgs);
        return cursor;
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        mSQLCount++;
        return db.insert(table, nullColumnHack, values);
    }

    private void flushMms() {
        beginTransaction();
        try {
            for (List<TableInfo> tableInfos : mmsList) {
                insertSingleMmsDB(tableInfos);
                increaseCount();
            }
            mMmsSQLCount = 0;
            mmsList.clear();
            setTransactionSuccessful();
        } catch (Exception e) {
            BackupLog.log(TAG, "flushMms catch Exception", e);
        }
        endTransaction();
    }

    public void notifySmsChange() {
        mContext.getContentResolver().notifyChange(MmsSms.CONTENT_URI, null);
        mContext.getContentResolver().notifyChange(Sms.CONTENT_URI, null);
    }

    public void notifyMmsChange() {
        mContext.getContentResolver().notifyChange(MmsSms.CONTENT_URI, null);
        mContext.getContentResolver().notifyChange(Mms.CONTENT_URI, null);
    }

    private void forceFlushMms() {
        if (mmsList.size() > 0) {
            flushMms();
        }
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void increaseCount() {
        synchronized (mnCount) {
            mnCount++;
            if (mnCount >= mnTotal) {
                callback.OnNotify(Defines.CMD_UPDATE_PROGRESS, mnCount, mnTotal, null, null);
                callback.OnNotify(Defines.CMD_REPORT_RESUALT, 0, 0, null, null);
                endTimer();
                mnCount = mnTotal;
            } else {
                if (mnCount % step == 0) {
                    callback.OnNotify(Defines.CMD_UPDATE_PROGRESS, mnCount, mnTotal, null, null);
                    notifyMmsChange();
                    BackupLog.log(TAG, " finished mms Count= " + mnCount);
                }
            }
        }
    }

    // called after all back/restore threads are complete, no mater whether
    // success or not.
    public void finalClear() {
        //fix for bug 319529 begin
        forceFlushMms();
        //fix for bug 319529 end
        if (mnCount == mnTotal) { // all success
            return;
        } else if (mnTotal > mnCount) { // some failed
            BackupLog.logE(TAG, " finished mms Count= " + mnCount);
            BackupLog.logE(TAG, " failed mms Count= " + (mnTotal - mnCount));
            //fix for bug 319529 begin
            callback.OnNotify(Defines.CMD_REPORT_RESUALT, -1, 0, null, null);
            //fix for bug 319529 end
        }
    }

    synchronized public void insertMmsTables(List<TableInfo> tableInfos) {
        mmsList.add(tableInfos);
        mMmsSQLCount += tableInfos.size();
        if (mmsList.size() + mnCount >= mnTotal) {
            forceFlushMms();
        } else {
            checkFlushMms();
        }
    }

    private void insertSingleMmsDB(List<TableInfo> tableInfos) {
        long msgId = -1;
        long tId = -1;
        // update threadId
        for (TableInfo info : tableInfos) {
            if (info.mTable.equals(TABLE_THREADS)) {
                List<String> recipientList = new ArrayList<String>(info.extraParameter);
                tId = MmsSmsProviderAdapter.get(mContext).getOrCreateThreadId(recipientList,
                        new ArrayList<String>());
            }
        }
        // insert pdu
        for (TableInfo info : tableInfos) {
            if (info.mTable.equals(TABLE_PDU)) {
                ContentValues pduValues = info.mValues;
                pduValues.put(Mms.THREAD_ID, tId);
                pduValues.put(Mms.SEEN, 1);
                msgId = insert(TABLE_PDU, null, pduValues);
                tableInfos.remove(info);
                BackupLog.log(TAG , "pdu msgId = " + msgId);
                break;
            }
        }
        // insert parts
        for (TableInfo info : tableInfos) {
            if (info.mTable.equals(TABLE_PART)) {
                ContentValues partValues = info.mValues;
                partValues.put(Part.MSG_ID, msgId);
                long rowId = insert(TABLE_PART, null, partValues);
                String contentType = partValues.getAsString("ct");
                if ("text/plain".equals(contentType)) {
                    ContentValues cv = new ContentValues();
                    cv.put(Telephony.MmsSms.WordsTable.ID, (2 << 32) + rowId);
                    cv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, partValues.getAsString("text"));
                    cv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowId);
                    cv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 2);
                    db.insert("words", Telephony.MmsSms.WordsTable.INDEXED_TEXT, cv);
                }
                BackupLog.log(TAG , "parts msgId = " + msgId);
            }
        }
        // insert addr
        for (TableInfo info : tableInfos) {
            if (info.mTable.equals(TABLE_ADDR)) {
                ContentValues addrValues = info.mValues;
                addrValues.put("msg_id", msgId);
                insert(TABLE_ADDR, null, addrValues);
                BackupLog.log(TAG , "addr msgId = " + msgId);
            }
        }
    }

    private void checkFlushMms() {
        if ((mmsList.size() > 15) || (mMmsSQLCount > 100)) {
            flushMms();
        }
    }

    // test
    private long time = 0;

    public void beginTimer() {
        time = System.currentTimeMillis();
    }

    public void endTimer() {
        System.out.println("restore/backup " + mnTotal + " mms/sms cost: "
                + ((System.currentTimeMillis() - time) / 1000.0) + " S!");
    }

}
