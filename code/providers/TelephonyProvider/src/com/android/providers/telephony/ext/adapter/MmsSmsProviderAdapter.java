package com.android.providers.telephony.ext.adapter;

import android.content.*;
import android.database.sqlite.*;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;

import com.sprd.providers.stubs.backup.MmsBackupStub;

public class MmsSmsProviderAdapter {
    
    private static final String TAG="MmsSmsProviderAdapter";
    
    private Context mContext;
    
    private SQLiteDatabase mSQLiteDatabase;
    private Method mUpdateAllThreads;
    
    private static MmsSmsProviderAdapter sMmsSmsProviderAdapter;
    
    private MmsSmsProviderAdapter(Context context){
        mContext = context;
    }
    
    public static synchronized MmsSmsProviderAdapter get(Context context){
        if (sMmsSmsProviderAdapter==null){
            sMmsSmsProviderAdapter = new MmsSmsProviderAdapter(context);
        }
        return sMmsSmsProviderAdapter;
    }
    
    public SQLiteDatabase getSQLiteDatabase(){
        if (mSQLiteDatabase == null){
            mSQLiteDatabase = MmsBackupStub.getInstance(mContext).getWritableSQLiteDatabase();
        }
        return mSQLiteDatabase;
    }
    
    public long getOrCreateThreadId(List<String> recipients, List<String> recipientNames) {
        long threadId = -1;
        threadId = MmsBackupStub.getInstance(mContext).getOrCreateThreadId(recipients, recipientNames);
        return threadId;
    }
    
    public void updateAllThreads(SQLiteDatabase db, String where, String[] whereArgs){
        MmsBackupStub.getInstance(mContext).updateAllThreads(db, where, whereArgs);
    }
}