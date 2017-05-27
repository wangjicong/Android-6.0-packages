package com.android.providers.telephony.ext.adapter;

import android.content.*;
import android.database.sqlite.*;
import android.database.*;
import com.google.android.mms.pdu.*;
import com.google.android.mms.*;
import com.google.android.mms.util.*;
import android.provider.Telephony.Mms;

import com.google.android.mms.MmsException;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter.TableInfo;
import com.android.providers.telephony.ext.mmsbackup.BackupLog;

import java.util.*;
import java.io.*;
import java.util.Map.*;

public class LocalPduPersisterAdapter{
    
    private static final String TAG="LocalPduPersisterAdapter";
    
    private Context mContext;
    private static LocalPduPersisterAdapter sLocalPduPersisterAdapter;
    private LocalPduPersister mLocalPduPersister;
    
    private LocalPduPersisterAdapter(Context context){
        mContext = context;
        mLocalPduPersister = LocalPduPersister.getLocalPduPersister(mContext);
    }
    
    public static synchronized LocalPduPersisterAdapter get(Context context){
        if (sLocalPduPersisterAdapter == null){
            sLocalPduPersisterAdapter = new LocalPduPersisterAdapter(context);
        }
        return sLocalPduPersisterAdapter;
    }
    
    public GenericPdu loadForBackupMms(SQLiteDatabase db, long msgId){
        GenericPdu res = null;
        try{
            mLocalPduPersister.setSQLiteDatabase(db);
            res = mLocalPduPersister.load(msgId);
        }catch(MmsException ex){
           BackupLog.log(TAG , "loadForBackupMms: ex = "+ex);
        }
        return res;
    }

    public List<TableInfo> persistForRestoreMms(SQLiteDatabase db, GenericPdu pduData, ContentValues restoreValues ){
        
        List<TableInfo>      res = new ArrayList<TableInfo>();
        HashSet<String>      recipients = new HashSet<String>();
        ContentValues        pdu = new ContentValues();
        List<ContentValues>  parts = new ArrayList<ContentValues>();
        List<ContentValues>  addrs = new ArrayList<ContentValues>();
        
 	    try{
	        mLocalPduPersister.setSQLiteDatabase(db);
	        mLocalPduPersister.persist(pduData, restoreValues, false, false, null, recipients, pdu, parts, addrs);

            BackupLog.log(TAG , "persistForRestoreMms: recipients = " + recipients.size()+"  pdus="+pdu.size()+" parts="+parts.size()+" addrs="+addrs.size());
	        res.add(new TableInfo("threads", recipients));
	        res.add(new TableInfo("pdu", pdu));
	        for(int i=0; i<parts.size(); ++i){
	            res.add(new TableInfo("part", parts.get(i)));
	        }
            for(int i=0; i<addrs.size(); ++i){
                res.add(new TableInfo("addr", addrs.get(i)));
            }
	    }catch(MmsException ex){
	        BackupLog.log(TAG , "persistForRestoreMms: ex = " + ex);
	    }
        return res;
    }
}