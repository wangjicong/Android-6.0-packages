
package com.android.providers.telephony.ext.mmsbackup.sms;

import android.content.ContentValues;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsHeader.ConcatRef;
import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.android.providers.telephony.ext.mmsbackup.Defines;
import com.android.providers.telephony.ext.mmsbackup.PublicParameter;
import com.android.providers.telephony.ext.mmsbackup.WorkingThread;
import com.android.providers.telephony.ext.mmsbackup.mms.PduFile;
import com.android.providers.telephony.ext.mmsbackup.mms.StaticUtil;
import com.android.providers.telephony.ext.mmsbackup.mms.XmlUtil;
//import com.android.providers.telephony.ThreadsIdContainer;
import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SmsRestoreThread1 extends WorkingThread implements TagStringDef {
    private static final String TAG = "SmsRestoreThread1";

    public SmsRestoreThread1(PublicParameter db, Context context) {
        super(db, context);
    }

    @Override
    public void run() {
        if (mCancel) {
            BackupLog.log(TAG, "cancel restore sms");
            return;
        }
        BackupLog.log(TAG, "start to restore sms");
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        parameter.beginTimer();
        //ThreadsIdContainer threads = ThreadsIdContainer.getInstance(mContext);
        InputStream inputStream = null;
        InputStream pduInputStream = null;
        try {
            ParcelFileDescriptor fd = getFileDescriptor(Defines.READ, SHORT_MESSAGE_FILE_1);
            if(fd == null){
                reportFail(Defines.ERROR_FD_NULL, "Restore " + SHORT_MESSAGE_FILE_1 + ", fd = null");
                GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, -1, -1, null, null);
                return;
            }
            inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    fd);
            XmlUtil xml = new XmlUtil("sms");
            xml.loadFile(inputStream, StaticUtil.SMS_KEYS);
            ArrayList<ContentValues> list = xml.getContentValues();
            parameter.beginTransaction();
            mLongSmsMap = new HashMap<Integer, Set<SmsData>>();
            int total = list.size();
            parameter.setMnTotal(total);
            for (int i = 0; i < list.size(); i++) {
                if (mCancel) {
                    BackupLog.log(TAG, "cancel restore sms from old version");
                    return;
                }
                ContentValues c = list.get(i);
                // get pdu file name from attribute
                String pduName = c.getAsString("_id");
                if (!TextUtils.isEmpty(pduName)) {
                    // to parse pdu from pdu file
                    ParcelFileDescriptor fileDescriptor = getFileDescriptor(Defines.READ, pduName);
                    pduInputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                            fileDescriptor);
                    byte[] pdu = PduFile.read(pduInputStream);
                    pduInputStream.close();
                    pduInputStream = null;
                    ContentValues map = StaticUtil
                            .convertKeyToField2Restore(c, StaticUtil.TYPE.SMS);
                    map.remove("_id");

                    ContentValues insertValues = restore(map, pdu);
                    if (insertValues == null){
                        BackupLog.log(TAG, "restore sms insertValues is null!");
                        continue;
                    }
                    String tel = (String) insertValues.get(cADDRESS);
                    List<String> recipients = new ArrayList<String>();
                    recipients.add(tel);
                    BackupLog.log(TAG, "restore sms recipients "+tel);
                    long threadId = MmsSmsProviderAdapter.get(mContext).getOrCreateThreadId(recipients,new ArrayList<String>());
                    insertValues.put(cTHREAD_ID, threadId);
                    long rowID = parameter.insert("sms", "body", insertValues);
                    ContentValues wordsCv = new ContentValues();
                    wordsCv.put(Telephony.MmsSms.WordsTable.ID, rowID);
                    wordsCv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, insertValues.getAsString("body"));
                    wordsCv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowID);
                    wordsCv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 1);
                    parameter.insert("words", Telephony.MmsSms.WordsTable.INDEXED_TEXT, wordsCv);
                    if ((i + 1) % 50 == 0) {
                        parameter.setTransactionSuccessful();
                        parameter.endTransaction();
                        parameter.notifySmsChange();
                        parameter.beginTransaction();
                        GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, i + 1, total, null, null);
                        BackupLog.log(TAG, "restore sms finished " + (i+1));
                    }
                }
            }
            //fix for Bug 319529 begin
            GetCallBack().OnNotify(Defines.CMD_UPDATE_PROGRESS, total, total, null, null);
            //fix for Bug 319529 end
            mLongSmsMap.clear();
            parameter.setTransactionSuccessful();
            parameter.endTransaction();
            parameter.endTimer(); // print how long did it take
            GetCallBack().OnNotify(Defines.CMD_REPORT_RESUALT, 0, 0, null, null);
        } catch (Exception e) {
            reportFail(ERROR_OTHER, e);
            return;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(pduInputStream != null){
                    pduInputStream.close();
                    pduInputStream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BackupLog.log(TAG, "finish restore sms");
    }

    @Override
    public int OnNotify(int nMsg, int nValue, long lValue, Object obj, List<Object> listObj) {
        switch (nMsg) {
        // case CMD_RESTORE:
        // new Thread(this).start();
        // return SUCC;
            case CMD_CANCEL:
                stopExec();
                return SUCC;
                //
            default:
        }
        return super.OnNotify(nMsg, nValue, lValue, obj, listObj);
    }

    private static Map<Integer, Set<SmsData>> mLongSmsMap;

    public ContentValues restore(ContentValues values, byte[] pdu) {
        String body = "";
        SmsMessage msg = null;
        String strPdu = IccUtils.bytesToHexString(pdu);
        strPdu = strPdu.substring(2);
        //BackupLog.log(TAG, "restore sms: pdu is {"+strPdu+"}");
        msg = SmsMessage.createFromPdu(IccUtils.hexStringToBytes(strPdu));
        String address = msg.getOriginatingAddress();
        //BackupLog.log(TAG, "restore sms: user data is {"+IccUtils.bytesToHexString(msg.getUserData())+"}");
	    //ByteArrayInputStream inStream = new ByteArrayInputStream(msg.getUserData());
	    //while (inStream.available() > 0) {
	    //	int length = inStream.read();
	    //      BackupLog.log(TAG, "restore sms: user data length is {"+length+"}");
	    //}
	    SmsHeader smsHeader = null;
        try{
            smsHeader = SmsHeader.fromByteArray(msg.getUserData());
        }catch(Exception ex){
            BackupLog.log(TAG, "restore sms: ex {"+ex+"}");
        }
        if ((smsHeader != null) && (smsHeader.concatRef != null)) {
            // BackupLog.log(TAG, "  refnum:" + smsHeader.concatRef.refNumber +
            // " "
            // + smsHeader.concatRef.seqNumber + "/"
            // + smsHeader.concatRef.msgCount + " body:" +
            // msg.getDisplayMessageBody());
            Set<SmsData> dataset = mLongSmsMap.get(smsHeader.concatRef.refNumber);
            if (dataset == null) {
                dataset = new TreeSet<SmsData>(new SmsDataComparator());
                mLongSmsMap.put(smsHeader.concatRef.refNumber, dataset);
            }

            if (dataset.size() < (smsHeader.concatRef.msgCount - 1)) {
                SmsData data = new SmsData();
                data.ref = smsHeader.concatRef;
                data.body = msg.getDisplayMessageBody();
                dataset.add(data);
                return null;
            } else {
                for (SmsData data : dataset) {
                    body += data.body;
                }
            }
        }
        body += msg.getDisplayMessageBody();
        BackupLog.log(TAG, "final body:" + body);

        // ContentValues values = getAttribute();
        if (address == null){
            address="13800138000";
        }
        values.put(Sms.ADDRESS, address);
        values.remove(BaseColumns._ID);// we should remove it

        //if (values.getAsInteger(Sms.TYPE) != 3) {
            values.put(Sms.BODY, body);
            return values;
        //}
        //return null;
    }

    protected static class SmsData {
        public ConcatRef ref;
        public String body;
    }

    protected static class SmsDataComparator implements Comparator<SmsData> {
        @Override
        public int compare(SmsData data1, SmsData data2) {
            return data1.ref.seqNumber - data2.ref.seqNumber;
        }
    }
}
