
package com.sprd.messaging.sms.commonphrase.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.Locale;

public class PhaserProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        sLocale = Locale.getDefault();
        mDBhelper = new DatabaseHelper(getContext());
        mPharserDB = getDatabaseHelper().getWritableDatabase();
        Log.d("PhaserProvider", mPharserDB.getPath());
        return (mPharserDB == null) ? false : true;
    }

    @Override
    public int delete(Uri uri, String where, String[] selectionArgs) {

        int count = 0;

        count = getSqliteDatabase().delete(DatabaseDefine.TABLE_NAME, where, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        String orderBy = null;
        Cursor cursor = null;
        int match = uriMatcher.match(uri);

        if (!TextUtils.isEmpty(sortOrder)) {
            orderBy = sortOrder;
        } else {
            orderBy = DEFAULT_SORT_ORDER;
        }

        switch (match) {
            case PHARSER_ALL: {
                cursor = mDBhelper.getReadableDatabase().query(DatabaseDefine.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, orderBy, null);
            }
                break;
            case PHARSER_ID: {
                String pharserID = uri.getPathSegments().get(1);
                cursor = mDBhelper.getReadableDatabase().query(
                        DatabaseDefine.TABLE_NAME,
                        projection,
                        DatabaseDefine.COL_ID + "=" + pharserID
                                + (!TextUtils.isEmpty(selection) ? "AND(" + selection + ')' : ""),
                        selectionArgs, null, null, orderBy, null);
            }
                break;
            case PHARSER_MMS: {
                cursor = mDBhelper.getReadableDatabase().query(
                        DatabaseDefine.TABLE_NAME,
                        projection,
                        DatabaseDefine.COL_TPYE_MMS + "!= 0"
                                + (!TextUtils.isEmpty(selection) ? "AND(" + selection + ')' : ""),
                        selectionArgs, null, null, orderBy, null);

            }
                break;
            case PHARSER_TEL: {
                cursor = mDBhelper.getReadableDatabase().query(
                        DatabaseDefine.TABLE_NAME,
                        projection,
                        DatabaseDefine.COL_TPYE_TEL + "!=0"
                                + (!TextUtils.isEmpty(selection) ? "AND(" + selection + ')' : ""),
                        selectionArgs, null, null, orderBy, null);

            }
                break;
            default:
                Log.e(TAG, "Fail to query, because of invalid query URI: " + uri);
                break;
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count = 0;

        count = getSqliteDatabase().update(DatabaseDefine.TABLE_NAME, values, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri arg0) {

        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        Uri iUri = null;

        long rowID = getSqliteDatabase().insert(DatabaseDefine.TABLE_NAME, null, values);
        if (rowID > 0) {
            iUri = Uri.parse("content://" + AUTHORITY + DatabaseDefine.TABLE_NAME + "/" + rowID);
        }
        if (iUri == null) {
            Log.e(TAG, "Fail to insert, ContentValues= {" + values.toString() + "}");
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return iUri;

    }

    public int bulkInsert(Uri iUri, ContentValues[] values) {
        int numValues = 0;
        getSqliteDatabase().beginTransaction();
        try {
            numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                insert(iUri, values[i]);
            }
            getSqliteDatabase().setTransactionSuccessful();
        } finally {
            getSqliteDatabase().endTransaction();
        }
        getContext().getContentResolver().notifyChange(iUri, null);
        return numValues;
    }

    private DatabaseHelper getDatabaseHelper() {
        return mDBhelper;
    }

    private SQLiteDatabase getSqliteDatabase() {
        return mPharserDB;
    }

    private DatabaseHelper mDBhelper;
    private SQLiteDatabase mPharserDB;

    private static final int PHARSER_ALL = 1;
    private static final int PHARSER_ID = 2;
    private static final int PHARSER_MMS = 3;
    private static final int PHARSER_TEL = 4;

    private static final String TAG = "PhaserProvider";
    private static final String AUTHORITY = "com.android.messaging.commonphrase";
    private static final String DEFAULT_SORT_ORDER = "_id DESC";
    public static final Uri PHARSER_URI = Uri.parse("content://" + AUTHORITY + '/'
            + DatabaseDefine.TABLE_NAME);

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, DatabaseDefine.TABLE_NAME, PHARSER_ALL);
        uriMatcher.addURI(AUTHORITY, DatabaseDefine.TABLE_NAME + "/#", PHARSER_ID);
        uriMatcher.addURI(AUTHORITY, DatabaseDefine.TABLE_NAME + "/" + DatabaseDefine.COL_TPYE_MMS,
                PHARSER_MMS);
        uriMatcher.addURI(AUTHORITY, DatabaseDefine.TABLE_NAME + "/" + DatabaseDefine.COL_TPYE_TEL,
                PHARSER_TEL);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!newConfig.locale.equals(sLocale)) {
            Log.d(TAG, "lxg - old locale=" + sLocale + ", new locale=" + newConfig.locale);
            sLocale = newConfig.locale;
            updateData();
        }
    }

    private void updateData() {
        if (mPharserDB != null && mPharserDB.isOpen()) {
            Cursor c = query(PHARSER_URI,
                    new String[]{DatabaseDefine.COL_ID, DatabaseDefine.COL_RES_ID},
                    DatabaseDefine.COL_RES_ID + "!=" + DatabaseDefine.DEFAULT_RES_ID,
                    null, DatabaseDefine.COL_RES_ID + " ASC");
            if (c != null && c.getCount() > 0) {
                ContentValues[] cvs = new ContentValues[c.getCount()];
                int i = 0;
                try {
                    Resources r = getContext().getResources();
                    while (c.moveToNext()) {
                        int resId = c.getInt(1);
                        if (resId != DatabaseDefine.DEFAULT_RES_ID) {
                            cvs[i] = new ContentValues();
                            cvs[i].put(DatabaseDefine.COL_ID, String.valueOf(c.getInt(0)));
                            int k = 0;
                            for (int j = 0; j < DatabaseDefine.FIXED_PHRASE.length; j++) {
                                String[] sRes = r.getStringArray(DatabaseDefine.FIXED_PHRASE[j]);
                                boolean found = false;
                                for (String res : sRes) {
                                    if (k == resId) {
                                        cvs[i++].put(DatabaseDefine.COL_PHARSER, res);
                                        found = true;
                                        break;
                                    } else {
                                        k++;
                                    }
                                }
                                if (found) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "lxg update data e:", e);
                } finally {
                    c.close();
                }
                bulkUpdate(PHARSER_URI, cvs);
            }
        }
    }

    public void bulkUpdate(Uri uri, ContentValues[] cvs) {
        if (uri != null && cvs.length > 0
                && mPharserDB != null && mPharserDB.isOpen() && !mPharserDB.isReadOnly()) {
            try {
                mPharserDB.beginTransaction();
                for (ContentValues cv : cvs) {
                    if (cv != null && cv.containsKey(DatabaseDefine.COL_ID)
                            && cv.containsKey(DatabaseDefine.COL_PHARSER)) {
                        String colId = String.valueOf(cv.get(DatabaseDefine.COL_ID));
                        cv.remove(DatabaseDefine.COL_ID);
                        update(uri, cv, DatabaseDefine.COL_ID + "=?", new String[]{colId});
                    }
                }
                mPharserDB.setTransactionSuccessful();
            } catch (Exception e) {
                Log.d(TAG, "lxg bulkUpdate e:", e);
            } finally {
                mPharserDB.endTransaction();
            }
        }
    }

    private static Locale sLocale;
}
