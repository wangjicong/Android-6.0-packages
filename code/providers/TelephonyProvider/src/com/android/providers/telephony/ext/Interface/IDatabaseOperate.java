package com.android.providers.telephony.ext.Interface;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public interface IDatabaseOperate {
    int InitEnv();

    int CreateStructure(SQLiteDatabase db);

    void CheckTableExist();

    Cursor query(Uri url, String[] projectionIn, String selection, String[] selectionArgs, String sort);

    Uri insert(Uri url, ContentValues initialValues);

    int update(Uri url, ContentValues values, String where, String[] whereArgs);

    int delete(Uri url, String where, String[] whereArgs);

    int GetLastError();

    static final int PROC_OK = 0x0000001; // PLUS PROCESS COMPLETE
    static final int PROC_ERROR_PARAMETER = 0x0000002;  // PARENT NEED CONTINUE PROCESS
    static final int PROC_FAILURE = 0x0000004;// PARENT NEED CONTINUE PROCESS
    static final int PROC_CONTINUE = 0x0000008;// PARENT NEED CONTINUE PROCESS
}
