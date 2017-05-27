
package com.sprd.messaging.sms.commonphrase.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import android.app.Activity;
import com.sprd.messaging.sms.commonphrase.provider.DatabaseDefine;
import com.sprd.messaging.sms.commonphrase.provider.DatabaseHelper;
import com.sprd.messaging.sms.commonphrase.provider.PhaserProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class PharserManager extends HashMap<Integer, ItemData> {

    private PharserManager() {
    }

    public static PharserManager getInstance() {
        return mPharserManager;
    }

    public static void releasetInstance() {
        mPharserManager.clear();
        mPharserManager = null;
    }

    public synchronized boolean LoadFromDatabase(Context mContext) {
        Cursor cursor = null;
        if(isInit){
            return true;
        }
        ContentResolver contentResolver = mContext.getContentResolver();
        cursor = contentResolver.query(PhaserProvider.PHARSER_URI, null, null, null, null);
        return addRecordToMap(cursor);
    }

    private boolean addRecordToMap(Cursor cursor) {

        if (cursor == null) {
            return false;
        }
        try {
            this.clear();// clear PharserManager,reload data from database

            /* Modify by SPRD for Bug:511964 2015.12.15 Start */
//            cursor.moveToFirst();
            if (!cursor.moveToFirst()) {
                return false;
            }
            /* Modify by SPRD for Bug:511964 2015.12.15 End */

            do {
                ItemData ins = new ItemData(cursor);
                put(ins.getRowID(), ins);

            } while (cursor.moveToNext());
            isInit = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        return true;
    }

    public static Cursor LoadFromDatabase(int intentType, Context context) {

        Cursor cursor = null;
        Uri uri = null;
        if (intentType == MMS) {
            uri = Uri.withAppendedPath(PhaserProvider.PHARSER_URI, DatabaseDefine.COL_TPYE_MMS);
        } else if (intentType == TEL) {
            uri = Uri.withAppendedPath(PhaserProvider.PHARSER_URI, DatabaseDefine.COL_TPYE_TEL);
        }
        if (uri != null) {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) {
                return null;
            }
        }
        return cursor;
    }

    public static ArrayList<String> intentArray(Cursor cursor) {
        ArrayList<String> sArray = new ArrayList<String>();
        try {
            sArray.clear();
            cursor.moveToFirst();
            do {
                ItemData sItem = new ItemData(cursor);
                sArray.add(sItem.getPharser());
            } while (cursor.moveToNext());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return sArray;
    }

    public synchronized boolean WriteToDisk(Context mContext) {
        ArrayList<ItemData> insertArray = new ArrayList<ItemData>();
        ArrayList<ItemData> deleteArray = new ArrayList<ItemData>();
        ArrayList<ItemData> updateArray = new ArrayList<ItemData>();

        Set<Integer> hashset = keySet();
        if (hashset == null && !isInit) {
            return false;
        }
        Log.d(TAG, "=======zhongjihao=====WriteToDisk=====hashset: " + hashset.size());
        for (int nID : hashset) {
            Log.d(TAG, "=======zhongjihao=====WriteToDisk=====flag: " + get(nID).getFlag()+"   nID: "+nID);
            if (get(nID).getFlag() == IModify.OP_DELETE) {
                deleteArray.add(get(nID));
            }
            if (get(nID).getFlag() == IModify.OP_UPDATE && (nID <= 10000)) {
                updateArray.add(get(nID));
                get(nID).setFlag(IModify.OP_NORMAL);
            }
            if (get(nID).getFlag() == IModify.OP_INSERT
                    || ((get(nID).getFlag() == IModify.OP_UPDATE) && (nID > 10000))) {
                insertArray.add(get(nID));
                get(nID).setFlag(IModify.OP_NORMAL);
            }
        }

        Log.d(TAG, "======zhongjihao====WriteToDisk=====insertArray.size()=" + insertArray.size());

        // bluck insert;
        if (insertArray.size() != 0) {
            mContext.getContentResolver().bulkInsert(PhaserProvider.PHARSER_URI,
                    insertData(insertArray));
        }

        // bulk update
        if (updateArray.size() != 0) {
            bulkUpdate(mContext, PhaserProvider.PHARSER_URI, updateData(updateArray));
        }

        // bulk delete
        if (deleteArray.size() != 0) {
            mContext.getContentResolver().delete(PhaserProvider.PHARSER_URI,
                    changeWhere(deleteArray), null);
        }
        isInit = false;
        return true;
    }

    private int bulkUpdate(Context context, Uri pharserUri, ContentValues[] updateData) {
        // TODO Auto-generated method stub
        int count = 0;
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.getWritableDatabase().beginTransaction();
        try {
            count = updateData.length;
            for (int i = 0; i < count; i++) {
                dbHelper.getWritableDatabase().update(DatabaseDefine.TABLE_NAME, updateData[i],
                        "_id = ?", new String[] {
                            updateData[i].get(DatabaseDefine.COL_ID).toString()
                        });
            }
            dbHelper.getWritableDatabase().setTransactionSuccessful();
        } finally {
            dbHelper.getWritableDatabase().endTransaction();
        }
        context.getContentResolver().notifyChange(pharserUri, null);
        return count;
    }

    private ContentValues[] insertData(ArrayList<ItemData> insertArray) {
        int count = insertArray.size();
        ContentValues[] values = new ContentValues[count];
        Log.d(TAG, "=====zhongjihao===insertData========count: " + count);
        for(int i=0;i<count;i++){
            for(int j=i+1;j<count;j++){
                if(insertArray.get(j).getRowID()<insertArray.get(i).getRowID()){
                    insertArray.get(j).swapObject(insertArray.get(i));
                }
            }
        }
        for (int i = 0; i <count ; i++) {
            Log.d(TAG, "=====zhongjihao===insertData========RowId: " + insertArray.get(i).getRowID()+"   txt: "+insertArray.get(i).getPharser());
            ContentValues temp = new ContentValues();
            values[i] = changeData(temp, insertArray.get(i));
        }
        return values;
    }

    private ContentValues[] updateData(ArrayList<ItemData> updateArray) {
        ContentValues[] values = new ContentValues[updateArray.size()];
        for (int i = 0; i < updateArray.size(); i++) {
            ContentValues temp = new ContentValues();
            temp.put(DatabaseDefine.COL_ID, updateArray.get(i).getRowID());
            values[i] = changeData(temp, updateArray.get(i));
        }
        return values;
    }

    private ContentValues changeData(ContentValues contentValues, ItemData data) {
        contentValues.put(DatabaseDefine.COL_PHARSER, data.getPharser());
        contentValues.put(DatabaseDefine.COL_TPYE_MMS, data.getMmsType());
        contentValues.put(DatabaseDefine.COL_TPYE_TEL, data.getTelType());
        contentValues.put(DatabaseDefine.COL_CAN_MODIFY, data.canModify());
        contentValues.put(DatabaseDefine.COL_RES_ID, data.getResId());
        return contentValues;
    }

    private String changeWhere(ArrayList<ItemData> changeArray) {
        StringBuilder where = new StringBuilder();
        where.append("_id IN(");
        for (int i = 0; i < changeArray.size(); i++) {
            if (i != 0) {
                where.append(',');
            }
            where.append(Integer.toString(changeArray.get(i).getRowID()));
        }
        where.append(")");
        return where.toString();
    }

    public int addNewData(String szValue, int nType) {
        Log.d(TAG, "=====>>> addNewData");
        if (szValue != null && !szValue.isEmpty()) {
            ItemData ins = new ItemData();
            ins.setFlag(IModify.OP_INSERT);
            ins.setPharser(szValue);
            ins.setType(nType);
            ins.setRowID(GroupID());
            put(ins.getRowID(), ins);
            return 0;
        } else {
            Log.d(TAG, "=====>>> addNewData error ");
            return -1;
        }
    }

    public int addNewData(String szValue, int nMms, int nTel) {
        if (szValue != null && !szValue.isEmpty()) {
            ItemData ins = new ItemData();
            ins.setFlag(IModify.OP_INSERT);
            ins.setPharser(szValue);
            ins.setType(nMms, nTel);
            ins.setRowID(GroupID());
            ins.setModify(1);
            put(ins.getRowID(), ins);
            Log.d(TAG, "====zhongjihao======addNewData======mNewID: "+ins.getRowID() +"    szValue: " + szValue+"  nMms: "+nMms+"   nTel: "+nTel);
            return 0;
        } else {
            Log.d(TAG, "=====>>> addNewData error ");
            return -1;
        }
    }

    public int DelByID(int nKey) {
        if (!containsKey(nKey)) {
            return -1;
        }

        get(nKey).setFlag(IModify.OP_DELETE);
        return 0;
    }

    public ArrayList<ItemData> RecoveryByID(ArrayList<ItemData> list) {
        if (list == null) {
            return null;
        }
        list.clear();
        Object[] keys = keySet().toArray();

        Log.d(TAG, "==============RecoveryByID()=====> hashset.size()="
                + keys.length);
        if (keys == null) {
            return null;
        }
        Arrays.sort(keys);

        for (int i = 0; i < keys.length; i++) {
            Log.d(TAG, "===========zhongjihao=====RecoveryByID()=====flag: "
                    + get(keys[i]).getFlag());
            get(keys[i]).setFlag(IModify.OP_UNKNOW);
            get(keys[i]).setIndexOfArray(list.size());
            list.add(get(keys[i]));
        }
        return list;
    }

    public int updateByID(int nKey, String szValue, Integer nType) {
        if ((szValue == null && nType == null) || !containsKey(nKey)) {
            return -1;
        }
        Log.d("PharserManager", "updateByID=====>>>>  szValues=" + szValue);
        ItemData ins = get(nKey);
        if (szValue != null) {
            ins.setPharser(szValue);
        }
        if (nType != null) {
            ins.setType(nType);
        }

        ins.setFlag(IModify.OP_UPDATE);
        return 0;

    }

    // / ArrayList :: HashMap -->list Array;
    public ArrayList<ItemData> MapToArrayList(ArrayList<ItemData> list) {
        if (list == null) {
            Log.d(TAG, "MapToArrayList(mDataList)=====> mDataList is null");
            return null;
        } else {
            Log.d(TAG, "MapToArrayList(mDataList)=====> mDataList is not null,clear it!");
            list.clear();
        }

        Object[] keys = keySet().toArray();

        Log.d(TAG, "MapToArrayList(mDataList)=====> hashset.size()=" + keys.length);
        if (keys == null) {
            return list;
        }
        Arrays.sort(keys);

        for (int i = 0; i < keys.length; i++) {
            if (get(keys[i]).getFlag() == IModify.OP_DELETE) {
                Log.i("jerry", "MapToArrayList(mDataList)=====> nID= delete Before" + keys[i]);
                get(keys[i]).Debug();
                Log.i("jerry", "MapToArrayList(mDataList)=====> nID= delete After " + keys[i]);
                continue;
            } else {
                Log.d(TAG, "MapToArrayList(mDataList)=====> nID=" + keys[i]);
                Log.i("jerry", "list size --------->"+list.size());
                get(keys[i]).Debug();
                get(keys[i]).setIndexOfArray(list.size());
                list.add(get(keys[i]));
            }
        }
        return list;
    }
/*
    public ArrayList<ItemData> MapToArrayList() {
        ArrayList<ItemData> ins = new ArrayList<ItemData>();
        Object[] keys = keySet().toArray();
        Arrays.sort(keys);

        if (keys == null) {
            return ins;
        }
        for (int i = 0; i < keys.length; i++) {
            ins.add(get(keys[i]));
        }
        return ins;
    }
*/
    private int GroupID() {
        return (++mNewID);
    }

    /* And by SPRD for Bug:509485 2015.12.11 Start */
    private void ensureQuery() {
        isInit = false;
    }
    public void reloadFromDB(Context c) {
        if(c == null) {
            return;
        }
        if(c instanceof Activity) {
            Activity a = (Activity) c;
            if(a.isFinishing() || a.isDestroyed()) {
                return;
            }
        }
        ensureQuery();
        LoadFromDatabase(c);
    }
    /* And by SPRD for Bug:509485 2015.12.11 End */

    private static int mNewID = 10000;
    private static PharserManager mPharserManager = new PharserManager();
    public static int MMS = 1;
    public static int TEL = 2;
    private static String TAG = "PharserManager";
    private boolean isInit = false;
}
