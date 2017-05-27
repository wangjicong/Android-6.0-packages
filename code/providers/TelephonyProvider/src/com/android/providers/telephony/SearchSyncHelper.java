package com.android.providers.telephony;

import android.net.Uri;
import android.R.integer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import android.content.Context;
import android.database.Cursor;
import android.os.HandlerThread;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SearchSyncHelper {

    private Context mContext = null;
    private Handler mHandler = null;
    public static final int STATUS_ERROR = 0;
    public static final int STATUS_SUCCESS = 1;
    private SQLiteOpenHelper mOpenHelper = null;
    private static SearchSyncHelper mSearchHelper = null;
    public static final String TAG = "SearchSyncHelper";
    private HandlerThread mSearchHandlerThread = null;
    public static final String URI_TEMP_CONVERSATION = "content://mms-sms/temp_conversation";
    public static final int SYNC_ALL_DATA = -1;
    public static final int SYNC_TEMP_CONVERSATION_TABLE_ALL_RECORD = 1;
    public static final int SYNC_TEMP_CONVERSATION_TABLE_ADD_RECORD = 2;
    public static final int SYNC_TEMP_CONVERSATION_TABLE_DELETE_RECORD = 3;
    //add for bug 543691 begin
    public static final int SYNC_TEMP_CONVERSATION_TABLE_CREATE = 4;
    //add for bug 543691 end

    public final String URI_CONVERSATION = "content://com.android.messaging.datamodel.MessagingContentProvider/conversation";

    private SearchSyncHelper(Context context, SQLiteOpenHelper openHelper) {
        mContext = context;
        mOpenHelper = openHelper;
        initData();
    }

    public static synchronized SearchSyncHelper getInstance(Context context,
            SQLiteOpenHelper openHelper) {
        System.out.println(TAG + "enter getInstance()");
        if (null == mSearchHelper) {
            mSearchHelper = new SearchSyncHelper(context, openHelper);
        }
        return mSearchHelper;
    }

    public static synchronized void releaseIns() {
        if (null != mSearchHelper) {
            mSearchHelper = null;
        }
    }

    private Context getContext() {
        return mContext;
    }

    private SQLiteOpenHelper getOpenHelper() {
        return mOpenHelper;
    }

    private Handler getHandler() {
        return mHandler;
    }

    private void initData() {
        System.out.println(TAG + "enter initData()");
        mSearchHandlerThread = new HandlerThread("SearchHandlerThread");
        mSearchHandlerThread.start();
        mHandler = new Handler(mSearchHandlerThread.getLooper()) {
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                //add for bug 543691 begin
                case SYNC_TEMP_CONVERSATION_TABLE_CREATE:
                    CheckTable();
                //add for bug 543691 end
                    break;

                case SYNC_TEMP_CONVERSATION_TABLE_ALL_RECORD:
                    // getHandler().removeMessages(
                    // SYNC_TEMP_CONVERSATION_TABLE_ALL_RECORD);
                    syncTempTableRecord(SYNC_ALL_DATA);
                    break;
                case SYNC_TEMP_CONVERSATION_TABLE_ADD_RECORD:
                    syncTempTableRecord(((Integer) msg.obj).intValue());
                    break;
                case SYNC_TEMP_CONVERSATION_TABLE_DELETE_RECORD:
                    deleteThread(((Integer) msg.obj).intValue());
                    break;
                default:
                    break;
                }
            }
        };
    }

    public void insertRecord(int threadId) {
        System.out.println("the id of thread = "
                + mSearchHandlerThread.getThreadId());
        System.out.println("enter insertRecord(),threadId = " + threadId);
        // add for bug 543676 begin
        System.out.println("insert data after 4 seconds");
        Message msg = getHandler().obtainMessage(
                SYNC_TEMP_CONVERSATION_TABLE_ADD_RECORD, threadId);
        getHandler().sendMessageDelayed(msg, 4000);
        // add for bug 543676 end
    }

    public void deleteRecord(int threadId) {
        System.out.println("enter deleteRecord(),threadId = " + threadId);
        getHandler().obtainMessage(SYNC_TEMP_CONVERSATION_TABLE_DELETE_RECORD,
                threadId).sendToTarget();
    }

    public void syncRecord(int time) {
        System.out.println(TAG + "enter syncRecord(), time = [" + time + "]");
        Message msg = getHandler().obtainMessage(
                SYNC_TEMP_CONVERSATION_TABLE_ALL_RECORD);
        getHandler().sendMessageDelayed(msg, time);
    }

    private int deleteThread(int threadId) {
        System.out.println(TAG + "enter deleteThread, threadId = " + threadId);
        //SPCSS00353104 for Mms
        /*String deleteSql = String.format(
                "delete from temp_conversation where sms_thread_id = '%d' ;",
                threadId);*/
        String deleteSql = "delete from temp_conversation where sms_thread_id = '" + threadId +"' ;";
        /*String whereClause = String.format("sms_thread_id = '%d' ;", threadId);*/
        String whereClause = "sms_thread_id = '"+threadId+"' ;";
        //SPCSS00353104 for Mms
        System.out.println("deleteSql = " + deleteSql + "/n whereClause = "
                + whereClause);
        try {
            // getOpenHelper().getWritableDatabase().rawQuery(deleteSql, null);
            if (getOpenHelper().getWritableDatabase().delete(
                    "temp_conversation", whereClause, null) > 0) {
                System.out.println("delete thread = " + threadId + " success");
            } else {
                System.out.println("delete thread = " + threadId + " fail");
            }

        } catch (Exception e) {
            System.out.println("Exception occurs in deleteThread[" + threadId
                    + "], e = " + e.toString());
            return -1;
        }
        return 1;
    }
    //add for bug 543691 begin
    public void CheckTable() {
        System.out.println("====>>>>Enter Create Table ");
        SQLiteDatabase messagingDb = getOpenHelper().getWritableDatabase();

        System.out.println("isSyncAllData, table is not exist");
        // add for bug 543691 begin
        String sqlCreate = "create table if not exists temp_conversation(conversation_id INTEGER,sms_thread_id INT DEFAULT(0),recipient_name TEXT,snippet_text TEXT,recipient_address TEXT);";
        // add for bug 543691 end
        messagingDb.execSQL(sqlCreate); // create an temporary table
        System.out.println("====>>>>Exit Create Table ");
    }

    private void ClearData(SQLiteDatabase messagingDb) {
        System.out.println("====>>>>Enter ClearData ");
        String sqlCreate = "delete from temp_conversation;";
        messagingDb.execSQL(sqlCreate); // create an temporary table
        System.out.println("====>>>>Exit ClearData");
    }

    private boolean isNeedSyncAll(SQLiteDatabase messagingDb) {
        if (messagingDb == null) {
            return false;
        }
        Cursor cursor = null;
        String szSQl = " Select * from temp_conversation;";
        try {
            cursor = messagingDb.rawQuery(szSQl, null);
            return (cursor == null || cursor.getCount() <= 0);
        } catch (Exception e) {
            throw e;
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }
    //add for bug 543691 end

    public void syncTempTableRecord(int threadId) {
        System.out.println("enter syncTempTableRecord(), threadId = "
                + threadId);
        boolean isSyncAllData = false;
        Cursor cursor = null;
        Cursor cursor1 = null;
        Cursor cursor2 = null;
        Cursor cursor3 = null;
        SQLiteDatabase messagingDb = getOpenHelper().getWritableDatabase();
        isSyncAllData = isNeedSyncAll(messagingDb);

        if (threadId == SYNC_ALL_DATA) {
            isSyncAllData = true;
            ClearData(messagingDb);
            System.out.println("threadId = -1, will syncAllRecord");
        }

        try {
            Uri convUri = Uri.parse(URI_CONVERSATION);
            ContentResolver cr = getContext().getContentResolver();
            // add for bug 543691 begin
            if (!isSyncAllData) {
                //SPCSS00353104 for Mms
                /*String selection = String
                        .format("sms_thread_id = %d", threadId);*/
                String selection = "sms_thread_id = "+ threadId;
                //SPCSS00353104 for Mms
                cursor = cr.query(convUri, new String[] { "_id",
                        "sms_thread_id", "name", "snippet_text" }, selection,
                        null, null);
            } else {
                cursor = cr.query(convUri, new String[] { "_id",
                        "sms_thread_id", "name", "snippet_text" }, null, null,
                        null);
            }
            // add for bug 543691 end
            // add for bug 543676 begin
            if (cursor == null || cursor.getCount() <= 0) {
                System.out
                        .println("the cursor result from conversation is null or count is <= 0");
                return;
            }
            // add for bug 543676 end
            System.out.println("query sms_thread_id,name success,the count = ["
                    + cursor.getCount() + "]");
            ArrayList<String> list = new ArrayList<String>();
            ContentValues cv = new ContentValues();

            System.out.println("messagingDb.beginTransaction()");
            messagingDb.beginTransaction();
            while (cursor.moveToNext()) {
                System.out.println("insert data to temp_DB");
                // add for bug 543691 begin
                cv.put("conversation_id",
                        cursor.getString(cursor.getColumnIndex("_id")));
                // add for bug 543691 end
                cv.put("recipient_name",
                        cursor.getString(cursor.getColumnIndex("name")));
                cv.put("sms_thread_id",
                        cursor.getInt(cursor.getColumnIndex("sms_thread_id")));
                cv.put("snippet_text",
                        cursor.getString(cursor.getColumnIndex("snippet_text")));
                list.add(cursor.getString(cursor.getColumnIndex("name")));
                messagingDb.insert("temp_conversation", null, cv);
                System.out.println("temp_conversation cv = " + cv.toString());
            }
            // add the data of recipient_address
            String sql1 = "";
            if (isSyncAllData) {
                sql1 = "select sms_thread_id from temp_conversation;";
            } else {
                //SPCSS00353104 for Mms
                /*sql= String
                        .format("select sms_thread_id from temp_conversation where sms_thread_id = %d;",
                                threadId);*/
                sql1  = "select sms_thread_id from temp_conversation where sms_thread_id = "+ threadId + ";" ;
                //SPCSS00353104 for Mms
                System.out.println("just add one record threadId = ["
                        + threadId + "]");
            }
            //SPCSS00353104 for Mms
            System.out.println("[SearchSyncHelper]=====sql1:"+sql1);
            //SPCSS00353104 for Mms
            // String sql2 =
            // "select recipient_ids from threads where _id = ? ;";
            // String sql3 =
            // "select address from canonical_addresses where _id = ?;";
            // String sql4 =
            // "insert into temp_conversation (recipient_address) values('?');";
            cursor1 = messagingDb.rawQuery(sql1, null);
            if (cursor1 == null) {
                System.out.println("cursor1 is null, will rerturn");
                return;
            }
            if (cursor1 != null) {
                System.out.println("cursor1 is not null");
                while (cursor1.moveToNext()) {
                    int thread_Id = cursor1.getInt(cursor1
                            .getColumnIndexOrThrow("sms_thread_id"));
                    System.out.println("thread_Id = " + thread_Id);
                    //SPCSS00353104 for Mms
                    /*String sql2 = String
                            .format("select recipient_ids from threads where _id = %d ;",
                                    thread_Id);*/
                    String sql2  = "select recipient_ids from threads where _id = "+ thread_Id + " ;" ;
                    //SPCSS00353104 for Mms
                    System.out.println("sql2 = " + sql2);
                    cursor2 = messagingDb.rawQuery(sql2, null);
                    if (cursor2 == null || cursor2.getCount() <= 0) {
                        System.out.println("cursor2 =  RS == NULL");
                        break;
                    } /*else {
                        String szRet[] = cursor2.getColumnNames();

                        int nIndexOfColumns = 0;
                        for (String szItem : szRet) {
                            System.out.println(" cursor2 Column["
                                    + nIndexOfColumns + "]  ====>>> [" + szItem
                                    + "]");
                            ++nIndexOfColumns;
                        }
                        System.out
                                .println("cursor2  INDEX OF recipient_ids ="
                                        + cursor2
                                                .getColumnIndexOrThrow("recipient_ids"));
                        System.out
                                .println("END cursor2  INDEX OF recipient_ids ");
                        cursor2.moveToFirst();
                    }*/
                    cursor2.moveToFirst();
                    String recipient_ids = cursor2.getString(0);
                    if (cursor2 != null && !cursor2.isClosed()) {
                        cursor2.close();
                        cursor2 = null;
                    }
                    System.out.println("recipient_ids = [" + recipient_ids
                            + "]");
                    String[] recipientIds = recipient_ids.split(" ");
                    String recipient_address = "";
                    if (recipientIds != null && recipient_ids.length() != 0) {
                        for (String item : recipientIds) {
                            //SPCSS00353104 for Mms
                            /*String sql3 = String
                                    .format("select address from canonical_addresses where _id = %s;",
                                            item);*/
                            String sql3 = "select address from canonical_addresses where _id = "+ item +";";
                            //SPCSS00353104 for Mms
                            System.out.println("sql3 = " + sql3);
                            cursor3 = messagingDb.rawQuery(sql3, null);
                            if (cursor3 == null || cursor3.getCount() <= 0) {
                                System.out
                                        .println("cursor3 == null or size = 0");
                                break;
                            } else {
                                System.out.println("cursor3 != null");
                            }
                            while (cursor3.moveToNext()) {
                                String address = cursor3.getString(cursor3
                                        .getColumnIndex("address"));
                                if (recipient_address == "") {
                                    recipient_address = address;
                                } else {
                                    recipient_address += "," + address;
                                }
                            }
                            if (cursor3 != null && !cursor3.isClosed()) {
                                cursor3.close();
                                cursor3 = null;
                            }
                            System.out.println("the item = " + item
                                    + ", recipient_address = "
                                    + recipient_address);
                        }
                        // insert the recipient_address to temp_conversation
                        ContentValues values = new ContentValues();
                        values.put("recipient_address", recipient_address);
                        String whereClause = "sms_thread_id = ?";
                        String[] whereArgs = new String[] { String
                                .valueOf(thread_Id) };
                        if (messagingDb.update("temp_conversation", values,
                                whereClause, whereArgs) > 0) {
                            System.out.println("update the threadId = ["
                                    + thread_Id + "] success");
                        }
                    }
                }
                if (cursor1 != null && !cursor1.isClosed()) {
                    cursor1.close();
                    cursor1 = null;
                }
            }
            messagingDb.setTransactionSuccessful();
            messagingDb.endTransaction();
            System.out.println("temp_conversation nameList = "
                    + list.toString());
        } catch (Exception e) {
            System.out
                    .println("syncTempTableAllRecord error : " + e.toString());
            // add for bug 543676 begin
            e.printStackTrace();
            // add for bug 543676 end
            return;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                cursor = null;
            }
            if (cursor1 != null && !cursor1.isClosed()) {
                cursor1.close();
                cursor1 = null;
            }
            if (cursor2 != null && !cursor2.isClosed()) {
                cursor2.close();
                cursor2 = null;
            }
            if (cursor3 != null && !cursor3.isClosed()) {
                cursor3.close();
                cursor3 = null;
            }
            if (messagingDb != null) {
                messagingDb = null;
            }
        }
    }

    private boolean isTableExist(String tableName) {
        boolean result = false;
        if (tableName == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getOpenHelper().getWritableDatabase();
            String sql = "select * from " + tableName + " ;";
            cursor = db.rawQuery(sql, null);
            if (null == cursor) {
                return false;
            }
            while (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    result = true;
                }
            }
        } catch (Exception e) {
            System.out.println("isTableExist() occurs Exception : "
                    + e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return result;
    }

}
