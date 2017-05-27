package com.sprd.messaging.ui.smsmergeforward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.sms.MmsConfig;

class SmsMsgAdapter extends MutiSelectCursorAdapter {
    private static final String TAG = "SmsMsgAdapter";
    private Context context;

    public SmsMsgAdapter(Context context, Cursor c, boolean autoRequery,
            ListView listView) {
        super(context, c, autoRequery, listView, context.getResources()
                .getString(R.string.sms_forward));
        this.context = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(
                R.layout.messages_merge_forward, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (!(view instanceof SmsMsgListItem)) {
            return;
        }
        SmsMsgListItem smsli = (SmsMsgListItem) view;
        SmsMessageItem smsMsgItem = new SmsMessageItem(context, cursor);
        int position = cursor.getPosition();
        smsli.bind(smsMsgItem, position);
    }

    @Override
    public Object getItemKey(Cursor cursor) {
        try {
            SmsMessageItem item = new SmsMessageItem();
            item.mId = cursor.getInt(SmsMessageItem.PROJECTION_ID);
            item.mAddress = cursor.getString(SmsMessageItem.PROJECTION_ADDRESS);
            long date = cursor.getLong(SmsMessageItem.PROJECTION_DATE);
            item.mtimeStamp = item.formatTimeStampString(context, date);
            item.mBody = cursor.getString(SmsMessageItem.PROJECTION_BODY);
            item.mType = cursor.getInt(SmsMessageItem.PROJECTION_TYPE);
            return item;
        } catch (Exception e) {
            Log.e(TAG, "Exception e : " + e);
        }
        return null;
    }

    @Override
    public void confirmAction(HashMap<Integer,SmsMessageItem> set) {
        int selectCount = set.size();
        Log.d(TAG,"====sms merge forward======confirmAction=======selectCount: "+selectCount+"     min: "+MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSMSMergeForwardMinItems()+"   max: "+MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSMSMergeForwardMaxItems());
        if (selectCount >= MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSMSMergeForwardMinItems() && selectCount <= MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSMSMergeForwardMaxItems()) {
            ArrayList<SmsMessageItem> sms = new ArrayList<SmsMessageItem>();
            Iterator iter = set.entrySet().iterator();  
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();  
                Object key = entry.getKey();  
                Object val = entry.getValue();  
                SmsMessageItem item = (SmsMessageItem) val;
                sms.add(item);
            }  
            // Merge Sms
            String forwardSmsContent = mergeSms(sms);
            // Forward Sms
            forwardSms(forwardSmsContent);
        } else {
            showAlertDialog();
        }
    }

    private String mergeSms(ArrayList<SmsMessageItem> mergersms) {  
        int count = mergersms.size();
        // Construct the forwarded sms content.
        // The principle is as following: <forwardContent>
        /* <recipientName> >>> or <<< <myself> \n date \n body \n */
        // displayName date body
        // then append another msg
        String forwardContent = "";
        String displayName = "";

        String myName = "<"
                + context.getString(R.string.messagelist_sender_self) + ">";

        String mRecipientName =  mergersms.get(0).mAddress;
        for (int i = 0; i < count; i++) {
            String date = mergersms.get(i).mtimeStamp;;
            String body = mergersms.get(i).mBody;
            int type = mergersms.get(i).mType;
            if (type == MessageData.BUGLE_STATUS_INCOMING_COMPLETE) {
                displayName = "<" + mRecipientName + ">  >>>  " + myName;
            } else if(type == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE || type == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED){
                displayName = "<" + mRecipientName + ">  <<<  " + myName;
            }

            forwardContent = forwardContent + displayName + "\n" + date + "\n"
                    + body + "\n";
        }
      
        return forwardContent;
    }

    // Start ForwardMessageActivity to handle forwarding message
    private void forwardSms(String forwardContent) {
        Log.d(TAG,"====sms merge forward======forwardSms===maxByte: "+MmsConfig.getMaxMaxTxtFileSize());
        if(forwardContent.getBytes().length >= MmsConfig.getMaxMaxTxtFileSize()){
            new AlertDialog.Builder(context)
            .setTitle(R.string.sms_merge_dialog_title)
            .setMessage(String.format(context.getResources().getString(R.string.sms_merge_dialog_content_exceed), MmsConfig.getMaxMaxTxtFileSize()))
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
            return;
        }
        UIIntents.get().launchCreateNewConversationActivity(context, MessageData.createSharedMessage(forwardContent));
        ((Activity) context).finish();
    }

    private void showAlertDialog() {
        String sms_merge_dialog_content = String.format(context.getResources().getString(R.string.sms_merge_dialog_content1),MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSMSMergeForwardMinItems())+ String.format(context.getResources().getString(R.string.sms_merge_dialog_content2),MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSMSMergeForwardMaxItems());
        new AlertDialog.Builder(context)
                .setTitle(R.string.sms_merge_dialog_title)
                .setMessage(sms_merge_dialog_content)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
        return;
    }

}
