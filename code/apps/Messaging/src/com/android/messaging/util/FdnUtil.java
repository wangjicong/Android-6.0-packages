package com.android.messaging.util;

//sprd :562194 fixed  start
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.android.messaging.Factory;
import com.android.messaging.sms.MmsConfig;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.app.Activity;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FdnUtil {

    public static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] {
            "display_name", "data1", "fdn_contacts_subId" };

    public static FdnUtil mFdnUtil;

    public synchronized static FdnUtil getFdnUtilInstance() {
        if (mFdnUtil == null) {
            mFdnUtil = new FdnUtil();
        }
        return mFdnUtil;
    }

    public synchronized void initCacheMap() {
        if (!MmsConfig.getFdnContactFittingEnable()) {
            return;
        }
        initFdnHashTable();
    }

    private void clearCacheMap() {
        mFdnCacheMap.clear();
    }

    private String getFDNUri(String szSubid) {
        if (!isEmpty(szSubid)) {
            if (isMultiSimEnabled()) {
                return URI_MORE_SUBID + szSubid;
            }
            return URI_SINGAL_SUB;
        } else {
            return null;
        }
    }

    @SuppressLint("NewApi")
    private boolean isEmpty(String szString) {
        if (szString == null || szString.isEmpty()) {
            return true;
        } else {
            return false;
        }

    }

    protected ArrayList<Integer> getSubIds(
            List<SubscriptionInfo> mSubscriptionInfolist) {
        if (mSubscriptionInfolist == null) {
            return null;
        }
        Iterator<SubscriptionInfo> iterator = mSubscriptionInfolist.iterator();
        if (iterator == null) {
            return null;
        }

        ArrayList<Integer> arrayList = new ArrayList<Integer>(
                mSubscriptionInfolist.size());
        while (iterator.hasNext()) {
            SubscriptionInfo subInfo = iterator.next();
            int phoneId = subInfo.getSimSlotIndex();
            arrayList.add(subInfo.getSubscriptionId());
        }
        return arrayList;
    }

    protected static ArrayList<Integer> getSubIdsForOuter(
            List<SubscriptionInfo> mSubscriptionInfolist) {
        if (mSubscriptionInfolist == null) {
            return null;
        }
        Iterator<SubscriptionInfo> iterator = mSubscriptionInfolist.iterator();
        if (iterator == null) {
            return null;
        }

        ArrayList<Integer> arrayList = new ArrayList<Integer>(
                mSubscriptionInfolist.size());
        while (iterator.hasNext()) {
            SubscriptionInfo subInfo = iterator.next();
            int phoneId = subInfo.getSimSlotIndex();
            arrayList.add(subInfo.getSubscriptionId());
        }
        return arrayList;
    }

    public String getFormatPhoneNumber(String number) {
        if (number == null || number.equals("")) {
            return "";
        }
        if (number.length() > MmsConfig.getDefaultFdnKeyPhoneNumbeLength()) {
            return number.substring(number.length()
                    - MmsConfig.getDefaultFdnKeyPhoneNumbeLength());
        } else {
            return number;
        }
    }

    public synchronized String getCachFdnName(String defaultNameOrNumber,
            String name, long contactid) {
        if (!MmsConfig.getFdnContactFittingEnable()) {
            return name;
        }

        if (isEmpty(defaultNameOrNumber)) {
            return name;
        }

        if (getFdnCaheMap() == null) {
            initFdnHashTable();
        }

        String number = getFormatPhoneNumber(defaultNameOrNumber).replace(" ",
                "");
        Log.i("getCachFdnName", "getCachFdnName----name ---->" + number);
        if (getFdnCaheMap().containsKey(number)) {
            Log.i("getCachFdnName",
                    "getCachFdnName---getFdnCaheMap().containsKey(number)---->"
                            + number);
            return getFdnCaheMap().get(number);
        } else {
            Log.i("getCachFdnName",
                    "getCachFdnName---!getFdnCaheMap().containsKey(number)---->"
                            + number);

            return name;

        }
    }

    // sprd :562194 fixed end

    public ArrayList<Integer> getActivitySubIdList() {
        return getSubIds(SmsManager.getDefault().getActiveSubInfoList());
    }

    public static boolean getFdnEnable(int subID) {
        return SubscriptionManager.from(Factory.get().getApplicationContext())
                .getFdnEnable(subID);
    }

    private boolean isMultiSimEnabled() {
        return TelephonyManager.getDefault().isMultiSimEnabled();
    }


    public ArrayList<Integer> getActivitySubIds(final Context context) {
        return getActivitySubIdList();
    }

    public void initFdnHashTable() {

        Context context = Factory.get().getApplicationContext();
        initCachmapFromMmsSmsFdnContacts(context);
    }

    private void initCachmapFromMmsSmsFdnContacts(Context context){
             Cursor cursor = context.getContentResolver().query
                     (Uri.parse(FDN_URI), ADDRESS_BOOK_COLUMN_NAMES, null, null, null);
             getHashMapFromCusor(cursor);
    }

    private void getHashMapFromCusor(Cursor cursor) {
        if (null == cursor) {
            System.out
                    .println("enter getHashMapFromCusor(), the cursor is null");
            return;
        }
        try {
            while (cursor.moveToNext()) {
                String number = getFormatPhoneNumber(cursor.getString(1));
                String name = cursor.getString(0);
                getFdnCaheMap().put(number, name);
                Log.i(FDNUTI_TAG, "getTableFromCusor--fdn-->number-->" + number);
                Log.i(FDNUTI_TAG, "getTableFromCusor--fdn-->name-->" + name);
                Log.i(FDNUTI_TAG, "*********************************");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                cursor = null;
            }
        }
    }

    private class WorkerThread extends Thread {
        protected static final String TAG = "WorkerThread";
        private Handler mWorkerHandler;
        private Looper looper;

        public WorkerThread() {
            start();
        }

        @Override
        public void run() {
            super.run();
            looper = Looper.myLooper();
            looper.prepare();

            mWorkerHandler = new Handler(looper) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            };
            looper.loop();
        }

        public void exit() {
            if (looper != null) {
                looper.quit();
                looper = null;
            }
        }

        // This method returns immediately, it just push an Message into
        // Thread's MessageQueue.
        // You can also call this method continuously, the task will be executed
        // one by one in the
        // order of which they are pushed into MessageQueue(they are called).
        public void executeTask(String text) {
            if (looper == null || mWorkerHandler == null) {
                Message msg = Message.obtain();
                msg.obj = "Sorry man, it is out of service";
//                Fdnhandler.sendMessage(msg);
                return;
            }
            Message msg = Message.obtain();
            msg.obj = text;
            mWorkerHandler.sendMessage(msg);
        }

    }

    public static final int NOTIFICATION = 19999;
    public static final int NAME_INDEX = 0;
    public static final int NUMBER_INDEX = 1;
    public static final int EMAILS_INDEX = 2;
    private static final String URI_SINGAL_SUB = "content://icc/fdn";
    private static final String URI_MORE_SUBID = "content://icc/fdn/subId/";

    private static final String FDNUTI_TAG = "FdnUtil";

    public HashMap<String, String> getFdnCaheMap() {
        if (mFdnCacheMap == null) {
            mFdnCacheMap = new HashMap<String, String>();
            initFdnHashTable();
        }
        return mFdnCacheMap;
    }

    public static ArrayList<Integer> getActivitySubidListForOut() {
        if (mSubIdList == null) {
            return getSubIdsForOuter(SmsManager.getDefault()
                    .getActiveSubInfoList());
        }
        return mSubIdList;

    }

    private static HashMap<String, String> mFdnCacheMap = null;
    private static ArrayList<Integer> mSubIdList = null;

    //sprd: fix for telcel bug 557685 begin
    public static String FDN_URI = "content://mms-sms//contacts/fdn/";

    public static String LOCALCONTACTS_URI = "content://mms-sms//contacts/contacts/";

    public static String CONFUSE_CONTACTS_URI = "content://mms-sms//contacts/fdnContacts/";
    //sprd: fix for telcel bug 557685 end
    // sprd :562194 fixed end
}
