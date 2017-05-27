package com.sprd.providers.stubs.backup;

import java.util.List;

import android.content.Context;
import android.database.sqlite.*;

import com.sprd.providers.stubs.backup.ThreadsIdContainer;
import com.android.providers.telephony.MmsSmsDatabaseHelper;

import android.database.sqlite.SQLiteOpenHelper;

import java.util.*;
import java.lang.*;
import java.lang.reflect.*;

import android.util.Log;

public class MmsBackupStub {

    private static final String TAG = "MmsBackupStub";
    private static MmsBackupStub sMmsBackupStub;
    private Context mContext;


    private MmsBackupStub(Context context) {
        mContext = context;
    }

    public synchronized static MmsBackupStub getInstance(Context context) {
        if (sMmsBackupStub == null) {
            sMmsBackupStub = new MmsBackupStub(context);
        }
        return sMmsBackupStub;
    }

    public boolean isEnable() {
        return true;
    }

    public SQLiteDatabase getWritableSQLiteDatabase() {
        return MmsSmsDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    public long getOrCreateThreadId(List<String> recipients, List<String> recipientNames) {
        long threadId = -1;
        threadId = ThreadsIdContainer.getInstance(mContext).getOrCreateThreadId(recipients, recipientNames);
        return threadId;
    }

    public void updateAllThreads(SQLiteDatabase db, String where, String[] whereArgs) {
        MmsSmsDatabaseHelper.getInstance(mContext).updateAllThreads(db, where, whereArgs);
    }
}
