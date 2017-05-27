package com.android.providers.telephony.ext.simmessage;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.PackageParser.NewPermissionInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Filter;
import android.provider.Telephony.TextBasedSmsColumns;

import com.android.internal.telephony.PhoneFactory;
import com.android.providers.telephony.MmsSmsDatabaseHelper;
import com.android.providers.telephony.ext.Interface.IDatabaseOperate;

import android.telephony.SubscriptionManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;

public class ICCMessageManager implements IDatabaseOperate {
    //sprd #554003 start 2016/4/21
    public static final String SIM_MESSAGE_DELETE_ACTION = "com.android.providers.telephony.SIM_MESSAGE_DELETE";
    //sprd #554003 end 2016/4/21
    private Handler delayReadIccThread;
    private List<DeleteParameter>  mList = new ArrayList<DeleteParameter>();
    public  List<DeleteParameter>  getList(){ return mList;}

    //singleton
    public synchronized static ICCMessageManager getInstance() {
        if (mICCMessageManager == null) {
            mICCMessageManager = new ICCMessageManager();
        }
        return mICCMessageManager;
    }

    public static void Release() {
        mICCMessageManager = null;
    }

    public void setContext(Context Ctx) {
        mcr = Ctx;
        /****************************
         Ctx.enforceCallingPermission(
         "android.permission.RECEIVE_SMS",
         "Reading messages from SIM");
         **************************/
    }

    protected Context getContext() {
        return mcr;
    }

    public int InitEnv() {
        SQLiteDatabase db = GetSQLiteOpenHelper().getReadableDatabase();
        CreateStructure(db);
        readFromICC(db);
        return 0;
    }

    @Override
    public int CreateStructure(SQLiteDatabase db) {
        Log.v(TAG, "CreateStructure");
        db.execSQL(ICC_TABLE_DEF);
        Log.v(TAG, "CreateStructure finish");
        return 0;
    }

    public int RecordCount(SQLiteDatabase db) {
        int nCount = -1;
        String szSQL = "SELECT count(*) FROM  " + TB_ICC_MSG + ";";
        Cursor cursor = db.rawQuery(szSQL, null);
        if (cursor == null)
            return nCount;
        try {
            cursor.moveToFirst();
            nCount = cursor.getInt(0);
        } catch (Exception e) {
            Log.e(TAG, " Exec [" + szSQL + "] Error", e);
        } finally {
            cursor.close();
            cursor = null;
        }
        return nCount;
    }


    public void CheckTableExist() {
        SQLiteDatabase db = GetSQLiteOpenHelper().getReadableDatabase();
        CreateStructure(db);
    }


    public ContentValues InsertOperateFlag(ContentValues values, int Flag) {
        values.put(OPERATE_FLAG, "" + Flag);
        values.put(OPERATE_TIME, "" + System.currentTimeMillis());
        return values;
    }


    public String ConcatenateWhere(String where, String messageId, String subId) {
        String extraWhere = ID + "=" + messageId;
        where = DatabaseUtils.concatenateWhere(where, extraWhere);
        extraWhere = SUB_ID + "=" + subId;
        where = DatabaseUtils.concatenateWhere(where, extraWhere);
        Log.v(TAG, "ConcatenateWhere:[" + where + "]");
        return where;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection, String[] selectionArgs, String sort) {

        Log.e(TAG, "  Plus query  Sim Message!");

        Cursor ret = null;
        String table = TB_ICC_MSG;
        String szSystemSelect = OPERATE_FLAG + "!=" + OPERATE_DELETE;
        SQLiteDatabase db = GetSQLiteOpenHelper().getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(table);

        int match = sURLMatcher.match(url);
        String messageIndex = null;
        String phoneId = null;
        String subId = null;
        if (projectionIn == null) {
            projectionIn = ICC_COLUMNS;
        }

        Log.v(TAG, "query url:" + url.toString() + "  selection:" + selection + " sort:" + sort);
        switch (match) {
            case SMS_ALL_ICC:
                if (selection != null) {
                    szSystemSelect = "(" + szSystemSelect + ") AND " + selection;
                }
                break;
            case SMS_ICC:
                messageIndex = url.getPathSegments().get(1);
                phoneId = String.valueOf(DEFAULT_PHONE_ID);
                //  qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                if (selection != null && selection.length() > 2) {
                    szSystemSelect = "(" + szSystemSelect + " " + INDEX_ON_ICC + "=" + messageIndex + " AND " + PHONE_ID + "=" + phoneId + ") AND " + selection;
                }
                break;
            case SMS_ICC_WITH_PHONE_ID:
                messageIndex = url.getPathSegments().get(1);
                phoneId = url.getPathSegments().get(3);
                if (selection != null && selection.length() > 2) {
                    szSystemSelect = "(" + szSystemSelect + " " + INDEX_ON_ICC + "=" + messageIndex + " AND " + PHONE_ID + "=" + phoneId + ") AND " + selection;
                }
                break;
            case SMS_ICC_WITH_SUB_ID:

                messageIndex = url.getPathSegments().get(1);
                subId = url.getPathSegments().get(3);
                Log.d(TAG, "query SMS_ICC_WITH_SUB_ID: subId:[" + subId + "] messageIndex:[" + messageIndex + "] ");

                szSystemSelect = "(" + INDEX_ON_ICC + "=" + messageIndex + " AND " + SUB_ID + "=" + subId + ")";

                break;
            case LOAD_ICC_TO_TEL:
                if (Status.GetInstance().IsFirstInit()) {
                    Log.e(TAG, "SIM messages  Start Loader!");
                    delayReadIccThread.sendEmptyMessageDelayed(DoSomeworkThread.READ_MSG_FROM_ICC, 0);
                    return ret;
                }
                break;
            default:
                Log.e(TAG, "Invalid request uri: " + match);
                return ret;
        }
        Log.v(TAG, "query where:" + szSystemSelect);
        ret = qb.query(db, projectionIn, szSystemSelect, selectionArgs, null, null, sort);
        return ret;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        SQLiteDatabase db = GetSQLiteOpenHelper().getReadableDatabase();

        int match = sURLMatcher.match(url);
        Log.d(TAG, "insert:[" + url + "] match:[" + match + "]");
        String table = TB_ICC_MSG;
        String messageIndex = null;
        String phoneId = null;
        String subId = null;
        String body = null;
        String address = null;
        String date = null;
        Long date_Long;
        int subId_Int = 0;
        int PhoneId = 0;
        switch (match) {
            case SMS_ICC:
                messageIndex = url.getPathSegments().get(1);
                phoneId = String.valueOf(DEFAULT_PHONE_ID);
                initialValues.put(INDEX_ON_ICC, messageIndex);
                initialValues.put(PHONE_ID, phoneId);
                break;
            case SMS_ICC_WITH_PHONE_ID:
                messageIndex = url.getPathSegments().get(1);
                phoneId = url.getPathSegments().get(3);
                initialValues.put(INDEX_ON_ICC, messageIndex);
                initialValues.put(PHONE_ID, phoneId);
                break;
            case SMS_ICC_WITH_SUB_ID:
                messageIndex = url.getPathSegments().get(1);
                subId = url.getPathSegments().get(3);
                initialValues.put(INDEX_ON_ICC, messageIndex);
                initialValues.put(SUB_ID, subId);
                break;
            case SMS_ALL_ICC:
                Log.d(TAG, "SMS_ALL_ICC");
                /*
                phoneId = (String) initialValues.get(PHONE_ID);
                PhoneId = Integer.valueOf(phoneId);
                if(PhoneId == 0){
                    messageIndex = String.valueOf(++maxIndex0);
                }
                messageIndex = String.valueOf(++maxIndex1);
                initialValues.put(PHONE_ID, phoneId);
                initialValues.put(INDEX_ON_ICC, messageIndex);*/
                break;
            case SMSDB_TO_ICCDB:
                Log.d(TAG, "SMSDB_TO_ICCDB");
                subId = (String) initialValues.get(SUB_ID);
                //  subId_Int = Integer.valueOf(subId);
                body = (String) initialValues.get(BODY);
                address = (String) initialValues.get(ADDRESS);
                date = (String) initialValues.get(DATE);
                // date_Long = Long.parseLong(date);
                initialValues.put(INDEX_ON_ICC, -1);


                long rowId = db.insert(table, null, initialValues);
                Log.d(TAG, "SMSDB_TO_ICCDB rowId = " + rowId);

                DeleteParameter ins = new DeleteParameter();
                ins.setSubID(subId);
                ins.setbody(body);
                ins.setAddress(address);
                ins.setTime(date);
                if (rowId >= 0) {
                    ins.setMessageID(String.valueOf(rowId));
                    notifyChange();
                }


                // sync
                // insert to ICC database

                Message msg = delayReadIccThread.obtainMessage(DoSomeworkThread.INSERT_MSG_TO_ICC, ins);
                delayReadIccThread.sendMessage(msg);

                // copyMessageToIcc(body, address, date_Long, subId_Int);
                // Status.GetInstance().SetFirstInit(true);
                //Status.GetInstance().CheckCanLoader();
                return null;

            case ICCDB_TO_SMSDB:
                // ADDRESS Thead id
                // ID LIST BATCH insert SMS TABLE

                break;
            default:
                Log.e(TAG, "Invalid request uri: " + match);
                Log.e(TAG, "Invalid request uri: [" + url.toString() + "]");
                return null;
        }

        Log.d(TAG, "insert url:[" + url.toString() + "]");
        InsertOperateFlag(initialValues, OPERATE_INSERT);

        long rowID = db.insert(table, null, initialValues);
        Log.d(TAG, "insert rowID[" + rowID + "]");
        if (rowID > 0) {
            Uri uri = Uri.parse("content://icc/icc/" + rowID);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "insert " + uri + " succeeded");
            }
            getContext().getContentResolver().notifyChange(ICC_URI, null);
            if (PhoneId == 0) {
                maxIndex0++;
            }
            maxIndex1++;
            return uri;
        } else {
            Log.e(TAG, "insert: failed! " + initialValues.toString());
        }
        return null;
    }


    @Override
    public int update(Uri url, ContentValues values, String where,
                      String[] whereArgs) {
        /*if(!Status.GetInstance().CanDML()){
            Log.v(TAG,"SIM don't finish loading,can't update SIM messages!");
            return 0;
        }*/
        SQLiteDatabase db = GetSQLiteOpenHelper().getWritableDatabase();
        int count = 0;
        String table = TB_ICC_MSG;
        int match = sURLMatcher.match(url);
        String messageIndex = null;
        String phoneId = null;
        String subId = null;
        // update sim card  messages
        switch (match) {
            case SMS_ICC:
                messageIndex = url.getPathSegments().get(1);
                phoneId = String.valueOf(DEFAULT_PHONE_ID);
                break;
            case SMS_ICC_WITH_PHONE_ID:
                messageIndex = url.getPathSegments().get(1);
                phoneId = url.getPathSegments().get(3);
                break;
            case SMS_ICC_WITH_SUB_ID:
                messageIndex = url.getPathSegments().get(1);
                subId = url.getPathSegments().get(3);
                break;
            default:
                Log.e(TAG, "Invalid request uri: " + match);
                return count;
        }

        Log.v(TAG, "update url:[" + url.toString() + "]");

        try {
            Integer.parseInt(messageIndex);
            //Integer.parseInt(phoneId);
            Integer.parseInt(subId);
        } catch (Exception ex) {
            Log.e(TAG, "Bad messageIndex: " + messageIndex + " or Bad phoneId:" + phoneId);
            return PROC_FAILURE;
        }

        if (values == null) {
            values = new ContentValues();
        }

        InsertOperateFlag(values, OPERATE_DELETE);
        String szwhere = ConcatenateWhere(where, messageIndex, subId);
        Log.e(TAG, " condition [" + szwhere + "]");
        count = db.update(table, values, szwhere, whereArgs);

        if (count > 0) {
            Log.d(TAG, "update " + url + " succeeded:" + count);
            getContext().getContentResolver().notifyChange(ICC_URI, null);
        }

        return count;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        // TODO Auto-generated method stub
        //update will delete sim card message record
        int nRecordCount = update(url, null, where, whereArgs);
        if (nRecordCount == 0)
            return 0;

        int match = sURLMatcher.match(url);
        String messageIndex = null;
        String phoneId = null;
        String subId = null;
        switch (match) {
            case SMS_ICC:
                messageIndex = url.getPathSegments().get(1);
                phoneId = String.valueOf(DEFAULT_PHONE_ID);
                break;
            case SMS_ICC_WITH_PHONE_ID:
                messageIndex = url.getPathSegments().get(1);
                phoneId = url.getPathSegments().get(3);
                break;
            case SMS_ICC_WITH_SUB_ID:
                messageIndex = url.getPathSegments().get(1);
                subId = url.getPathSegments().get(3);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL");
        }

        Log.v(TAG, "====>>>>[delete]]]]===>>> url:[" + url.toString() + "]");
        //thread pool really delete sim card messages and table TB_ICC_MSG record

        // update flag
        String szWhere = ConcatenateWhere(where, messageIndex, subId);
        String szSQL  = " update "+ TB_ICC_MSG +"  set "+ OPERATE_FLAG + "=" + OPERATE_DELETE + " where " + szWhere;
        SQLiteDatabase db = GetSQLiteOpenHelper().getWritableDatabase();
        db.execSQL(szSQL);

        DeleteParameter ins = new DeleteParameter(messageIndex, subId, szWhere, whereArgs);

        Message msg =  delayReadIccThread.obtainMessage(DoSomeworkThread.Delete_MSG_FROM_ICC,  ins);
        delayReadIccThread.sendMessage(msg);

        getContext().getContentResolver().notifyChange(ICC_URI, null);
        return 0;
        //thread pool delete sim card messages finish
    }

    class myRunable implements Runnable{
        public myRunable( DeleteParameter ins){
            super();
            mins = ins;
        }
        @Override
        public void run() {
            Log.v(TAG, "==============>>>>>>  delete from myRunable");
            DeleteIcc(mins.getMessageID(),mins.getSubID(), mins.getWhere(), mins.getArgs());
        }
        DeleteParameter mins;
    }

    static public class DeleteParameter {
        private long rowID;

        public DeleteParameter() {

        }

        public DeleteParameter(String szMsgID, String szSubId, String szWhere, String[] args) {
            mszMesssageId = szMsgID;
            mszSubid = szSubId;
            mszWhere = szWhere;
            msargs = args;
        }


        public String getMessageID() {
            return mszMesssageId;
        }

        public String getSubID() {
            return mszSubid;
        }

        public String getWhere() {
            return mszWhere;
        }

        public String[] getArgs() {
            return msargs;
        }

        public void setSubID(String nSubID) {
            mszSubid = nSubID;
        }

        public String getBody() {
            return mszbody;
        }

        public String getDateTime() {
            return mszDate;
        }

        public String getAddress() {
            return mszAddress;
        }

        public String getRowID() {
            return mRowID;
        }

        public void setbody(String szbody) {
            mszbody = szbody;
        }

        public void setAddress(String szAddress) {
            mszAddress = szAddress;
        }

        public void setTime(String szTime) {
            mszDate = szTime;
        }

        public void setMessageID(String szMessageId) {
            mszMesssageId = szMessageId;
        }

        public void setRowID(String rowID) {
            this.mRowID = rowID;
        }

        String mszMesssageId = "";
        String mszSubid = "";
        String mszWhere = "";
        String mszbody = "";
        String mszDate = "";
        String mszAddress = "";
        String[] msargs = null;
        String mRowID = "";
    }
    public int DeleteIcc(String messageIndex, String subId, String where, String whereArgs[]) {
        int count = 0;
        int indexOnIcc = -1;
        SQLiteDatabase db = GetSQLiteOpenHelper().getWritableDatabase();
        String getIndexOnIccSql = "select " + INDEX_ON_ICC + " from " + TB_ICC_MSG +
                                  " where " + ID + "=" + messageIndex;
        Cursor cursor = db.rawQuery(getIndexOnIccSql, null);
        if (null == cursor) {
            return count;
        }
        try {
            if (cursor != null && cursor.getCount() > 0 &&
                    cursor.moveToFirst()) {
                Log.d(TAG, "DeleteIcc indexOnIcc = " + cursor.getInt(0) + "rowId = " + messageIndex);
                indexOnIcc = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                cursor = null;
            }
        }
        
        
        boolean deleteResult = deleteMessageFromIcc(getContext().getContentResolver(), String.valueOf(indexOnIcc), subId);
        if (deleteResult) {
            count = db.delete(TB_ICC_MSG, where, whereArgs);
            if (count > 0) {
                Log.d(TAG, "delete succeeded:" + count);
                //sprd #554003 start 2016/4/21
                Intent intent = new Intent(SIM_MESSAGE_DELETE_ACTION);
                intent.putExtra(SUB_ID, subId);
                getContext().sendBroadcast(intent);
                Log.d(TAG, "delete succeeded sendBroadcast !!! SIM_MESSAGE_DELETE_ACTION");
                //sprd #554003 end 2016/4/21
            }
            return count;
        } else {
            Log.d(TAG, "delete failed");
            return count;
        }
    }

    private void ResetIccTable(SQLiteDatabase db) {
        String szSQL = "delete from " + TB_ICC_MSG;

        db.execSQL(szSQL);
    }

    public void readFromICC(SQLiteDatabase db) {
        final long origId = Binder.clearCallingIdentity();
        ResetIccTable(db);
        SubscriptionManager sm = SubscriptionManager.from(getContext());
        if (sm != null) {
            List<SubscriptionInfo> subInfoList = sm.getActiveSubscriptionInfoList();//fix for bug 551407
            for (SubscriptionInfo subInfo : subInfoList) {
                int subId = subInfo.getSubscriptionId();
                ArrayList<SmsMessage> messagess = SmsManager.getSmsManagerForSubscriptionId(subId).getAllMessagesFromIcc();
                int nRecordCount = messagess.size();
                Log.d(TAG, "subId:[" + subId + "]   nRecordCount:[" + nRecordCount + "]");

                for (int i = 0; i < nRecordCount; i++) {
                    SmsMessage message = messagess.get(i);
                    if (message != null) {
                        Log.d(TAG, "subId:[" + subId + "]   index:[" + i + "] ");
                        convertIccToSms(db, message, i, subId);
                    }
                }
            }
            notifyChange();
        }
        Binder.restoreCallingIdentity(origId);

    }

    public void syncIndexFromICC(int subId, String rowId) {
        final long origId = Binder.clearCallingIdentity();
        SQLiteDatabase db = GetSQLiteOpenHelper().getReadableDatabase();
        SubscriptionManager sm = SubscriptionManager.from(getContext());
        if (sm != null) {
            ArrayList<SmsMessage> messagess = SmsManager.getSmsManagerForSubscriptionId(subId).getAllMessagesFromIcc();
            int nRecordCount = messagess.size();
            Log.d(TAG, "syncIndexFromICC:[" + subId + "]   nRecordCount:[" + nRecordCount + "]");
            SmsMessage message = messagess.get(nRecordCount - 1);
            if (message != null) {
                updateMsgIndex(db, message, nRecordCount - 1, subId, rowId);
                notifyChange();
            }
        }
        Binder.restoreCallingIdentity(origId);

    }
    private boolean updateMsgIndex(SQLiteDatabase db, SmsMessage message,
                                    int id, int subId, String rowId) {
        // N.B.: These calls must appear in the same order as the
        // columns appear in ICC_COLUMNS.
        // modify translation
        String szWhere = INDEX_ON_ICC + "= -1";
        String szSQL  = " update "+ TB_ICC_MSG +"  set "+ INDEX_ON_ICC + "=" + message.getIndexOnIcc() + " where " + szWhere;
        db.execSQL(szSQL);
        return true;
    }
    private void notifyChange() {
        getContext().getContentResolver().notifyChange(
                ICC_URI, null, true, UserHandle.USER_ALL);
    }
    private boolean convertIccToSms(SQLiteDatabase db, SmsMessage message,
                                    int id, int subId) {
        // N.B.: These calls must appear in the same order as the
        // columns appear in ICC_COLUMNS.
        // modify translation
        ContentValues values = new ContentValues();

        values.put(SERVICE_CENTER, message.getServiceCenterAddress());
        values.put(ADDRESS, message.getDisplayOriginatingAddress());
        values.put(BODY, message.getDisplayMessageBody());
        values.put(DATE, "" + message.getTimestampMillis());
        values.put(STATUS, "" + message.getStatusOnIcc());
        values.put(INDEX_ON_ICC, "" + message.getIndexOnIcc());
        values.put(TYPE, getSmsType(message));
        //values.put(ID, "" + id);
        values.put(SUB_ID, "" + subId);
        values.put(OPERATE_FLAG, "" + OPERATE_INSERT);
        Log.d(TAG, "convert STATUS = " + message.getStatusOnIcc() + "  TYPE" + getSmsType(message) + "  body = " + message.getDisplayMessageBody());
        long rowID = db.insert(TB_ICC_MSG, null, values);
        return (rowID > 0);
    }

    //get sent or receive
    private int getSmsType(SmsMessage message){
        if (message.getOriginatingAddress() != null) {
            return 2;
        }
        return 1;
    }

    public static String copyMessageToIcc(String body,
                                          String address, long date, int subId, String messageId) {
        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
        int status = 0;

        Log.d(TAG, "copyMessageToIcc address:[" + address + " body:[" + body);
        String copyMsgToIccEx = null;
        ArrayList<String> messages = null;
        messages = smsManager.divideMessage(body);
        if (messages.size() == 0) {
            copyMsgToIccEx = smsManager.copyMessageToIccEfWithResult(null, address, "", status,
                    date, subId);
        } else {
            for (int i = 0; i < messages.size(); i++) {
                String text = (String) messages.get(i);
                Log.d(TAG, "copyMessageToIcc smsManager.copyMessageToIccEfWithResult");
                copyMsgToIccEx = smsManager.copyMessageToIccEfWithResult(null, address, text,
                        status, date, subId);
                Log.d(TAG, "copyMessageToIcc copyMsgToIccEx:[" + copyMsgToIccEx + "]");
                if (!TextUtils.isEmpty(copyMsgToIccEx)) {
                    break;
                }
            }
        }
        return copyMsgToIccEx;
    }

    public boolean deleteMessageFromIcc(ContentResolver cr, String messageIndexString, String subId_String) {
        long token = Binder.clearCallingIdentity();
        int subId = Integer.parseInt(subId_String);
        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);

        try {
            return smsManager.deleteMessageFromIcc(Integer
                    .parseInt(messageIndexString));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Bad SMS ICC ID: "
                    + messageIndexString);
        } finally {

            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public int GetLastError() {
        // TODO Auto-generated method stub
        return 0;
    }

    private static ICCMessageManager mICCMessageManager = null;

    private SQLiteOpenHelper GetSQLiteOpenHelper() {
        return MmsSmsDatabaseHelper.getInstance(this.getContext());
    }


    /*********************
     * URI Operator
     ********************/
    public final static String AUTHOR = "sms";
    /************************************
     * ALL FIELD Define
     **********************************/
    public final static String VALIDE = "valide";
    public final static String TB_ICC_MSG = "icc_message";
    public final static String ADDRESS = "address";
    public final static String ID = "_id";
    public final static String BODY = "body";
    public final static String DATE = "date";
    public final static String TYPE = "type";
    public final static String LOCKED = "locked";
    public final static String PHONE_ID = "phone_id";
    public final static String SUB_ID = "sub_id";
    public final static String ERROR_CODE = "error_code";
    public final static String STATUS = "status";
    public final static String INDEX_ON_ICC = "index_on_icc";
    public final static String SERVICE_CENTER = "service_center";
    public final static String OPERATE_FLAG = "operate_flag";
    public final static String OPERATE_TIME = "operate_time";
    public final static String THREAD_ID = "thread_id";
    public final static String PERSON = "person";
    public final static String DATE_SENT = "date_sent";
    public final static String READ = "read";
    public final static String REPLY_PATH_PRESENT = "reply_path_present";
    public final static String SUBJECT = "subject";
    public final static String CREATOR = "creator";
    public final static String SEEN = "seen";
    private static final String PROTOCOL = "protocol";


    /**********************************************
     * Define Sim Infomation
     ********************************************/
    public final static String TB_SIM = "sim";
    private static final String ICC_CARD_MGR_DEF = "Create Table " + TB_SIM
            + " (" + ID + " INTEGER PRIMARY KEY," + ADDRESS + " TEXT,"
            + VALIDE + " INTEGER DEFAULT 0," + PHONE_ID + " INTEGER DEFAULT -1"
            + DATE + "  INTEGER DEFAULT 0" + ")";


    /******************
     * bug 271940 add
     *******************/
    private int maxIndex0 = 0;
    private int maxIndex1 = 0;
    /**********************************************
     * Define ICC Message Information
     ********************************************/


    public final static String[] ICC_COLUMNS = new String[]{


//sms columns begin
            ID,//    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            THREAD_ID,//            "thread_id INTEGER," +
            ADDRESS, // getDisplayOriginatingAddress//"address TEXT," +
            PERSON,//            "person INTEGER," +
            DATE, // getTimestampMillis// "date INTEGER," +
            DATE_SENT,//            "date_sent INTEGER DEFAULT 0," +
            PROTOCOL, // Always "sms".// "protocol INTEGER," +
            READ,//            "read INTEGER DEFAULT 0," +
            STATUS, // getStatusOnIcc//"status INTEGER DEFAULT -1," + // a TP-Status value or -1 if it status hasn't been received
            TYPE, // Always MESSAGE_TYPE_ALL.//"type INTEGER," +
            REPLY_PATH_PRESENT,//"reply_path_present INTEGER," +
            SUBJECT,//            "subject TEXT," +
            BODY, // getDisplayMessageBody//"body TEXT," +
            SERVICE_CENTER, // getServiceCenterAddress//"service_center TEXT," +
            LOCKED, // Always 0 (false).//"locked INTEGER DEFAULT 0," +
            SUB_ID,//"sub_id INTEGER DEFAULT " + SubscriptionManager.INVALID_SUBSCRIPTION_ID + ", " +
            ERROR_CODE, // Always 0//     "error_code INTEGER DEFAULT 0," +
            CREATOR,//  "creator TEXT," +
            SEEN,//"seen INTEGER DEFAULT 0" +
            INDEX_ON_ICC, // getIndexOnIcc
            OPERATE_FLAG,//OPERATE FLAG:default 0;insert 1;update 2,delete 4
            OPERATE_TIME//query anr time
    };

    public static final String ICC_TABLE_DEF = "CREATE TABLE IF NOT EXISTS " + TB_ICC_MSG + "  ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + THREAD_ID + " INTEGER,"
            + ADDRESS + " TEXT,"
            + PERSON + " INTEGER,"
            + DATE +" INTEGER,"
            + DATE_SENT + " INTEGER DEFAULT 0,"
            + PROTOCOL + " INTEGER,"
            + READ + " INTEGER DEFAULT 0,"
            + STATUS + " INTEGER DEFAULT 1,"
            + TYPE + " INTEGER,"
            + REPLY_PATH_PRESENT + " INTEGER,"
            + SUBJECT + " TEXT,"
            + BODY + " TEXT ,"
            + SERVICE_CENTER + " TEXT,"
            + LOCKED + " INTEGER DEFAULT 0,"
            + SUB_ID + " INTEGER DEFAULT " + SubscriptionManager.INVALID_SUBSCRIPTION_ID + ", "
            + ERROR_CODE + "  INTEGER DEFAULT 0,"
            + CREATOR + " TEXT,"
            + SEEN + " INTEGER DEFAULT 1,"
            + INDEX_ON_ICC + " INTEGER,"
            + OPERATE_FLAG + " INTEGER DEFAULT 0,"
            + OPERATE_TIME + " LONG DEFAULT " + System.currentTimeMillis() + ");";


    public static final int ICC_BASE = 0X01000000;
    private static final int SMS_ALL_ICC = (ICC_BASE | 0x00000001);
    private static final int SMS_ICC = (ICC_BASE | 0x00000002);
    private static final int SMS_DOUBLE_ICC = (ICC_BASE | 0x00000004);
    private static final int SMS_ICC_WITH_PHONE_ID = (ICC_BASE | 0x00000008);
    private static final int SIM_ID = (ICC_BASE | 0x00000010);
    private static final int SIM_COPY = (ICC_BASE | 0x00000020);
    private static final int SMS_ICC_WITH_SUB_ID = (ICC_BASE | 0x00000040);
    private static final int SMSDB_TO_ICCDB = (ICC_BASE | 0x00000080);
    private static final int LOAD_ICC_TO_TEL = (ICC_BASE | 0x00000100);
    private static final int ICCDB_TO_SMSDB = (ICC_BASE  | 0x00000200);
    //OPERATE_FLAG
    public static final int OPERATE_INSERT = 0x00000001;
    public static final int OPERATE_UPDATE = 0x00000002;
    public static final int OPERATE_DELETE = 0x00000004;


    private static final int DEFAULT_PHONE_ID = 0;
    Context mcr = null;
    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI(AUTHOR, "icc", SMS_ALL_ICC);
        sURLMatcher.addURI(AUTHOR, "doubleicc", SMS_DOUBLE_ICC);
        sURLMatcher.addURI(AUTHOR, "icc/#", SMS_ICC);
        sURLMatcher.addURI(AUTHOR, "icc/#/phone_id/#", SMS_ICC_WITH_PHONE_ID);
        sURLMatcher.addURI(AUTHOR, "icc/#/sub_id/#", SMS_ICC_WITH_SUB_ID);
        //add support muti sim card process
        sURLMatcher.addURI(AUTHOR, "sim", SIM_ID);
        sURLMatcher.addURI(AUTHOR, "#", SMSDB_TO_ICCDB);
        sURLMatcher.addURI(AUTHOR, "sim/#", ICCDB_TO_SMSDB);

        //copy sim to sim
        sURLMatcher.addURI(AUTHOR, "icc_copy", SIM_COPY);
        sURLMatcher.addURI(AUTHOR, "icc_load", LOAD_ICC_TO_TEL);
    }

    public static final Uri ICC_URI = Uri.parse("content://sms/icc");
    private static final Uri NOTIFICATION_URI = Uri.parse("content://sms");
    private final static String TAG = "ICCMessageManager";


    //@Override
    public boolean onCreate() {
        mICCMessageManager = this;
        // TODO Auto-generated method stub
        Log.v(TAG, " ICCMessageManager onCreate");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.setPriority(Integer.MAX_VALUE);
        getContext().registerReceiver(new ProvidersReceiver(), filter);
        return true;
    }

    public void initDelayHandler() {
        delayReadIccThread = DoSomeworkThread.initialDoSomeworkThread(mcr, null, this);
    }

    public void delayReadIcc() {
        if(delayReadIccThread.hasMessages(DoSomeworkThread.READ_MSG_FROM_ICC)) {
            return;
        }
        Log.d(TAG, "delayReadIcc");
        String fromSimLoaded = "true";
        Message msg = delayReadIccThread.obtainMessage(DoSomeworkThread.READ_MSG_FROM_ICC, fromSimLoaded);
        delayReadIccThread.sendMessageDelayed(msg, 60 * 1000);
    }

    /***************************************
     * runnable
     ****************************************/

    class DeleteRunnable implements Runnable {
        public DeleteRunnable(String szMessageIndex, String szPhoneID, String where, String whereArgs[]) {
            mszIndex = szMessageIndex;
            mszPhoneID = szPhoneID;
            mszwhere = where;
            mszwhereArgs = whereArgs;

        }

        @Override
        public void run() {
            Log.e(TAG, " delete from thread Start");
            DeleteIcc(mszIndex, mszPhoneID, mszwhere, mszwhereArgs);
            Log.e(TAG, " delete from thread end");
        }

        String mszIndex;
        String mszPhoneID;
        String mszwhere;
        String mszwhereArgs[];

    }
}
