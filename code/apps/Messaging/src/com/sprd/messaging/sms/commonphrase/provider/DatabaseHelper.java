
package com.sprd.messaging.sms.commonphrase.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper implements DatabaseDefine {

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    public DatabaseHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    public DatabaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // create table--common_phrases
            db.execSQL(TABLE_SQL);
            Log.d(TAG, "db=" + db.getPath());
            initFromResource(db);

        } catch (SQLiteException e) {
            Log.e(TAG, "Sqlite occur exception: " + e);
        }

    }

    // XML CONTEXT , tel/mms/ tel+mms
    private boolean initFromResource(SQLiteDatabase db) {
        List<ContentValues> valuesArray = new ArrayList<ContentValues>();
        int type = -1;
        try {
            String[] fixed_common_phrase;

            int index = 0;
            for (int i = 0; i < FIXED_PHRASE.length; i++) {
                if (getContext() != null) {
                    fixed_common_phrase = getContext().getResources()
                            .getStringArray(FIXED_PHRASE[i]);
                    if (i == 0) {
                        type = TYPE_MMS;
                    } else if (i == 1) {
                        type = TYPE_TEL;
                    } else if (i == 2) {
                        type = TYPE_BOTH;
                    } else {
                        Log.e(TAG, "error type!");
                        continue;
                    }
                    for (int j = 0; j < fixed_common_phrase.length; j++) {
                        ContentValues temp = new ContentValues();
                        valuesArray.add(addOneRecord(temp, fixed_common_phrase[j], type, index++));
                    }
                }
            }

            db.beginTransaction();
            for (int i = 0; i < valuesArray.size(); i++) {
                Log.d(TAG, "initFromResource=====>valuesArray.get(i)=" + valuesArray.get(i));
                db.insert(TABLE_NAME, null, valuesArray.get(i));
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "lxg initFromResource e:", e);
        } finally {
            db.endTransaction();
        }
        return true;
    }

    /**
     * @param resIndex system's phrases > -1, user's phrases(added or modified) == -1.
     */
    private ContentValues addOneRecord(ContentValues tempValue, String str, int type, int resIndex) {
        tempValue.put(COL_PHARSER, str);
        tempValue.put(COL_TPYE_MMS, type & TYPE_MASK);
        tempValue.put(COL_TPYE_TEL, type >> 1);
        tempValue.put(COL_CAN_MODIFY, 0);
        tempValue.put(COL_RES_ID, resIndex);
        return tempValue;
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    private Context getContext() {
        return mContext;
    }

    private Context mContext;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "pharser.db";
    private static final String TAG = "DatabaseHelper";

    private static final int TYPE_MMS = 0x00000001;
    private static final int TYPE_TEL = 0x00000002;
    private static final int TYPE_BOTH = 0x00000003;
    private static final int TYPE_MASK = 0x00000001;

}
