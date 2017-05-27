
package com.sprd.messaging.ui.appsettings;

import java.util.ArrayList;

import com.android.messaging.Factory;
import com.android.messaging.mmslib.SqliteWrapper;
import com.android.messaging.sms.MmsSmsUtils;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.R;
import android.content.Context;
import java.util.ArrayList;

import android.telephony.TelephonyManager;
import android.net.Uri;
import android.database.Cursor;
import android.text.TextUtils;
import com.android.messaging.mmslib.SqliteWrapper;
import android.content.ContentValues;
import android.widget.Toast;
import android.util.Log;

public class SmscManager extends ArrayList<String> {

    TelephonyManager mTelephonyManager;
    Uri iccUri;
    private static final String COL_NAME = "name";
    private static final String COL_NUMBER = "number";
    private String mSmscTagStr;// corresponding COL_NAME
    private static SmscManager mSmscList = new SmscManager();

    private static Context getContext() {
        return Factory.get().getApplicationContext();
    }

    private SmscManager() {

    }

    public static SmscManager getInstance() {
        return mSmscList;
    }

    public static void releaseInstance() {
        mSmscList.clear();
        mSmscList = null;
    }

    public static final String[] COLUMNS = new String[] {
        COL_NUMBER
    };

    public boolean isMultiSmsc(int subId) {
        // read the IccProvider
        Cursor cursor = query(subId);
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                cursor.close();
                return false;
            } else {
                cursor.close();
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean LoadFromIccDatabase(int subId) {
        Cursor cursor = query(subId);
        if (cursor == null) {
            return false;
        }
        try {
            this.clear();
            cursor.moveToFirst();
            do {
                add(cursor.getString(cursor.getColumnIndex(COL_NUMBER)));
            } while (cursor.moveToNext());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return true;
    }

    public static Cursor query(int subId) {
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(getContext(), getContext().getContentResolver(),
                    getIccUri(subId), COLUMNS, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    private String getTagStr(int position, int subId) {
        Cursor cursor = query(subId);
        String name = null;
        try {
            cursor.moveToPosition(position);
            name = cursor.getString(cursor.getColumnIndex(COL_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    public int update(String oldValue, String newValue, int index, int subId) {
        ContentValues values = new ContentValues();
        String tagName = getTagStr(index, subId);
        values.put("tag", tagName);
        values.put("number", oldValue);
        values.put("newTag", tagName);
        values.put("newNumber", newValue);
        values.put("index", index + 1);
        int count = 0;
        try {
            count = SqliteWrapper.update(getContext(), getContext().getContentResolver(),
                    getIccUri(subId), values, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private static Uri getIccUri(int subId) {

        String uri;
        // if(getTelephonyManager().isMultiSimEnabled()){
        uri = "content://icc/" + subId + "/" + "smsp";
        // }else{
        // uri="content://icc/smsp";
        // }
        return Uri.parse(uri);
    }

    public static String getSmscString(Context context, int subId) {
        String smscStr = "";
        smscStr = getTelephonyManager().getSmscForSubscriber(subId);
        if ((smscStr == null || smscStr.equals("") || smscStr.equals("refresh error"))) {
            smscStr = BuglePrefs.getSubscriptionPrefs(subId).getString(
                    context.getString(R.string.smsc_pref_key), ""); // smscStr
        }
        Log.d("SmscManager","=====add smsc=======getSmscString=====smscStr: "+smscStr);
        return smscStr;
    }

    public static boolean setSmscString(Context context, String smscStr, int subId) {
        Log.d("SmscManager","=====add smsc=======setSmscString====subId:" + subId+"    smscStr: "+smscStr);
        boolean setResult = false;
        setResult = getTelephonyManager().setSmscForSubscriber(smscStr, subId);
        Log.d("SmscManager","=====add smsc=======setSmscString=====setResult: "+setResult);
        if (setResult) {
            Toast.makeText(context, R.string.smsc_set_success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, R.string.smsc_set_fails, Toast.LENGTH_LONG).show();
        }

        if (!TextUtils.isEmpty(smscStr) && MmsSmsUtils.isPhoneNumber(smscStr)) {
            System.out.println("[SmscManager]====save new Smsc to sharedPref");
            BuglePrefs.getSubscriptionPrefs(subId).putString(
                    context.getString(R.string.smsc_pref_key), smscStr);
        }
        return setResult;

    }

    public static TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
    }

}
