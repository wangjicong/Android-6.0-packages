package com.android.messaging.datamodel.action;

import java.lang.Exception;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.Factory;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.LogUtil;

public class CertainTypeMessageAllDelAction extends Action implements Parcelable {
    private static final String TAG = "CertainTypeMessageAllDelAction";

    private static final String KEY_MESSAGES_ID     = "message_id";
    private static final String KEY_BOX_TYPE_MAX_IDS= "box_type_and_maxIds";
    private static final int MAX_TRANSACTION        = 100;

    //copy from SortMsgDataCollector.java
    public static final int MSG_UNKNOW              = -1;
    public static final int MSG_BOX_INBOX           = 0;
    public static final int MSG_BOX_SENT            = 1;
    public static final int MSG_BOX_OUTBOX          = 2;
    public static final int MSG_BOX_DRAFT           = 3;

    private CertainTypeMessageAllDelAction(final String box_type_and_ids, final String[] messagesId){
        super();
        actionParameters.putStringArray(KEY_MESSAGES_ID, messagesId);
        actionParameters.putString(KEY_BOX_TYPE_MAX_IDS, box_type_and_ids);
    }

    public static void deleteCertainTypeAll(final String box_type_and_ids, final String[] messagesId){
        LogUtil.i(TAG, "get in certain type all-delete messages.type:"+box_type_and_ids);
        CertainTypeMessageAllDelAction action = new CertainTypeMessageAllDelAction(box_type_and_ids, messagesId);
        action.start();
    }

    /**
     * Delete the messages in folder-view.
     */
    @Override
    protected Object executeAction() {
        requestBackgroundWork();
        return null;
    }

    // Doing this work in the background so that we're not competing with sync
    // which could bring the deleted message back to life between the time we deleted
    // it locally and deleted it in telephony (sync is also done on doBackgroundWork).
    //
    // Previously this block of code deleted from telephony first but that can be very
    // slow (on the order of seconds) so this was modified to first delete locally, trigger
    // the UI update, then delete from telephony.
    @Override
    protected Bundle doBackgroundWork() {
        LogUtil.i(TAG, "Action begin to delete type:.");
        final DatabaseWrapper db = DataModel.get().getDatabase();

        // First find the thread id for this conversation.
        final String[] messagesId = actionParameters.getStringArray(KEY_MESSAGES_ID);
        final String type_and_ids = actionParameters.getString(KEY_BOX_TYPE_MAX_IDS);

        //ArrayList conversations = getAllConversationIds(db, messagesId);

        //type_and_ids represent: box_type,messageMaxid,mmsMaxid,smsMaxid
        int box_type = Integer.valueOf(type_and_ids.split(",")[0]);
        String messagesMaxid = type_and_ids.split(",")[1];
        String maxMmsid = type_and_ids.split(",")[2];
        String maxSmsid = type_and_ids.split(",")[3];
        deleteCertainTypeMessage(db, box_type, messagesMaxid, maxMmsid, maxSmsid, getAllConversationIds(db, messagesId));
        LogUtil.d(TAG, "Action delete done.");

        return null;
    }


    /******************************************************************************************************
     * Delete All Message of one type
     * @param db
     * @param MessageID
     * @return
     *******************************************************************************************************/
    private int deleteCertainTypeMessage(final DatabaseWrapper dbWrapper,  int nType, final String messagesMaxid,
            final String szMaxMmsid, final String szMaxSmsid, ArrayList<String> conversations) {
        String szMessageCondition = getMessageStatusFromBugle(nType) +" and _id <= "+ messagesMaxid;
        LogUtil.d(TAG, "delete local db, SQL:"+szMessageCondition);
        dbWrapper.delete("messages" , szMessageCondition, null);
        //notify change, delete empty conversations in local db
        updateAllConversations(dbWrapper, conversations);
        MessagingContentProvider.notifyConversationListChanged();

        if( nType == 3){
            return 1;
        }

        final ContentResolver resolver = Factory.get().getApplicationContext().getContentResolver();
        String smsCondition = " _id <= "+ szMaxSmsid +" and " + getSmsTypeFromTele(nType);
        LogUtil.d(TAG, "delete sms from tele, SQL:"+smsCondition);
        String mmsCondition = " _id <= "+ szMaxMmsid + " and " + getMmsTypeFromTele(nType);
        LogUtil.d(TAG, "delete mms from tele, SQL:"+mmsCondition);

        if (Integer.valueOf(szMaxSmsid) > 0) {
            resolver.delete(Uri.parse("content://sms"), smsCondition, null);
        }

        if (Integer.valueOf(szMaxMmsid) > 0) {
            resolver.delete(Uri.parse("content://mms"), mmsCondition, null);
        }

//        //delete empty conversations in local db
//        updateAllConversations(dbWrapper, conversations);
//        MessagingContentProvider.notifyConversationListChanged();
        return 1;
    }

    private ArrayList<String> getAllConversationIds(DatabaseWrapper dbWrapper, String[] messagesId){
        if(messagesId.length > 0){
            ArrayList<String> conversationIds = new ArrayList<String>();
            dbWrapper.beginTransaction();
            try {
                for (int i = 0; i < messagesId.length; i++) {
                    final MessageData message = BugleDatabaseOperations.readMessage(dbWrapper, messagesId[i]);
                    if (message != null) {
                        final String conversationId = message.getConversationId();
                        conversationIds.add(conversationId);
                    }
                }
                dbWrapper.setTransactionSuccessful();
            } finally {
                dbWrapper.endTransaction();
            }
            LogUtil.d(TAG, "conversationIds' size is:"+conversationIds.size());
            return conversationIds;
        }
        return null;
    }

    private void updateAllConversations(DatabaseWrapper dbWrapper, ArrayList<String> conversations){
        if (conversations == null || conversations.size()==0) {
            LogUtil.d(TAG, "conversations' size is null or empty.");
            return;
        }
        dbWrapper.beginTransaction();
        try {
            LogUtil.d(TAG, "conversations' size is:"+conversations.size());
            for (int i = 0; i < conversations.size(); i++) {
                if (!BugleDatabaseOperations.deleteConversationIfEmptyInTransaction(dbWrapper, conversations.get(i))) {
                    // TODO: Should we leave the conversation sort timestamp alone?
                    LogUtil.d(TAG, "conversationId:"+conversations.get(i));
                    BugleDatabaseOperations.refreshConversationMetadataInTransaction(dbWrapper, conversations.get(i),
                            false/* shouldAutoSwitchSelfId */, false/*archived*/);
                }
            }
            dbWrapper.setTransactionSuccessful();
        } finally {
            dbWrapper.endTransaction();
        }
    }

    /*** 
     * type integer
     *  INBOX >= 100;
     *  SENT=1, 2;
     *  DRAFT=3;
     *  SENDING =4,5,6,7;
     *  FAILED = 8,9;
     *  
     ***/
    private String getMessageStatusFromBugle(int nType) {
        switch (nType) {
        case MSG_BOX_INBOX:
            return " message_status >= 100";
        case MSG_BOX_SENT:
            return "message_status in (1, 2)";
        case MSG_BOX_OUTBOX:
            return " message_status in (4, 5, 6, 7, 8, 9)";// sent failed and sending are in the same box:out_box
        case MSG_BOX_DRAFT:
            return " message_status = 3";
        default:
            return "0" ;
        }
    }

    /*** 
     * type integer
     *  ALL=0;
     *  INBOX=1;
     *  SENT=2;
     *  DRAFT=3;
     *  OUTBOX=4;
     *  FAILED=5;
     *  QUEUED=6;
     *  the messages unsent is (4,5,6)
     *  
     ***/
    private String getSmsTypeFromTele(int nType) {
        switch (nType) {
        case MSG_BOX_INBOX:
            return " type = 1";
        case MSG_BOX_SENT:
            return " type = 2";
        case MSG_BOX_OUTBOX:
            return "type in(4, 5, 6)" ;
        case MSG_BOX_DRAFT://the draft will not save in the mmssms.db
            return "0";
        default:
            return "0";
        }

    }

    /***
     * msg_box(integer): all:0
     * inbox:1
     * sent:2
     * draft:3
     * outbox:4
     * failed:5
     * the message unsent is 4
     * 
     ***/
    private String getMmsTypeFromTele(int nType) {
        switch (nType) {
        case MSG_BOX_INBOX:
            return " msg_box = 1";

        case MSG_BOX_SENT:
            return " msg_box = 2";
        case MSG_BOX_OUTBOX:
            return "msg_box in(4, 5)";
        case MSG_BOX_DRAFT://the draft will not save in the mmssms.db
            return "0";
        default:
            return "0";
        }
    }

    private CertainTypeMessageAllDelAction(final Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<CertainTypeMessageAllDelAction> CREATOR
            = new Parcelable.Creator<CertainTypeMessageAllDelAction>() {
        @Override
        public CertainTypeMessageAllDelAction createFromParcel(final Parcel in) {
            return new CertainTypeMessageAllDelAction(in);
        }

        @Override
        public CertainTypeMessageAllDelAction[] newArray(final int size) {
            return new CertainTypeMessageAllDelAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }

}
