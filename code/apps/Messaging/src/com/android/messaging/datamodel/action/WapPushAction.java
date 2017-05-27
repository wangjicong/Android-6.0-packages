package com.android.messaging.datamodel.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.BugleNotifications;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.SyncManager;
import com.android.messaging.datamodel.action.DeleteMessageAction;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.mmslib.SqliteWrapper;
import com.android.messaging.receiver.SmsReceiver;
import com.android.messaging.receiver.WapPushDeleteReceiver;
import com.android.messaging.sms.MmsSmsUtils;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.wappush.WapPushMessageShowActivity;
import com.android.messaging.wappush.WapPushMsg;
import com.android.messaging.wappush.WapPushParser;
import com.android.messaging.wappush.SerializeUtil;

import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.provider.Telephony.TextBasedSmsColumns;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;


public class WapPushAction extends Action implements Parcelable {
	public  static final String TAG = "WapPushAction";
    private static final String SUB_ID = "subId";

    public final static String WAP_PUSH_EXPRIRE_DELETE_ACTION = "com.android.mms.transaction.wappush_expire_delete";

    private String CONTENT_MIME_TYPE_B_PUSH_SI = "application/vnd.wap.sic";
    private String CONTENT_MIME_TYPE_B_PUSH_SL = "application/vnd.wap.slc";

    public WapPushAction(int subId,Intent intent) {
        super();
        intent.putExtra(SUB_ID, subId);
        actionParameters.putParcelable("wap_push_intent", intent);
    }

    @Override
    protected Object executeAction() {
        Intent intent = actionParameters.getParcelable("wap_push_intent");
        handleWapPushReceived(intent);
        return super.executeAction();
    }

    private WapPushAction(final Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<WapPushAction> CREATOR = new Parcelable.Creator<WapPushAction>() {
        @Override
        public WapPushAction createFromParcel(final Parcel in) {
            return new WapPushAction(in);
        }

        @Override
        public WapPushAction[] newArray(final int size) {
            return new WapPushAction[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
          writeActionToParcel(parcel, flags);
    }

    private void handleWapPushReceived(Intent intent) {
        String pushBody;
        String expired = "";
        String action = "";
        String si_id = "";
        String href = "";
        int error = intent.getIntExtra("errorCode", 0);
        byte[] pushData = intent.getByteArrayExtra("data");
        WapPushParser pushParser = new WapPushParser(pushData);
        WapPushMsg pushMsg = null;
        Log.i(TAG,"======wap push=====handleWapPushReceived()--> Wap push type is " + intent.getType());
        if (CONTENT_MIME_TYPE_B_PUSH_SI.equals(intent.getType())) {
            pushMsg = pushParser.parse(WapPushMsg.WAP_PUSH_TYPE_SI);
            if (null == pushMsg)
                return;
            href = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_HREF);
            String sitext = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_SI_TEXT);
            expired = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_SI_EXPIRED);
            if (expired != null && !"".equals(expired)) {
                // Implement the expired length to align to the system time.
                int lenthDiff = 14 - expired.length();
                if (lenthDiff > 0) {
                    for (int i = 0; i < lenthDiff; i++) {
                        expired += "0";
                    }
                }
            }
            action = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_PRIOR);
            si_id = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_SI_ID);
            if(!TextUtils.isEmpty(sitext))
                pushBody = sitext + "\n Url:" + href;
            else
                pushBody = "Url:" + href;
            Log.d(TAG,"======wap push====handleWapPushReceived()========href: " + href+"    pushBody: "+pushBody+"    sitext: "+sitext);
        } else if (CONTENT_MIME_TYPE_B_PUSH_SL.equals(intent.getType())) {
            pushMsg = pushParser.parse(WapPushMsg.WAP_PUSH_TYPE_SL);
            if (null == pushMsg) return;
            href = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_HREF);
            action = pushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_PRIOR);
            pushBody = "Url:"+href;
            Log.i(TAG,"======wap push===handleWapPushReceived()========pushBody: "+pushBody);
        } else {
            Log.i(TAG,"wap push non support type");
            return;
        }

        boolean isNeedNotify = false;
        boolean isNeedStore = false;
        boolean isShowDialog = false;
        int actiontype = WapPushParser.getPushAttrValue(action);
        Log.d(TAG, "====wap push====handleWapPushReceived()-->       actiontype = " + actiontype);
        if (actiontype == WapPushMsg.WAP_PUSH_PRIO_NONE.intValue() 
                || actiontype == WapPushMsg.WAP_PUSH_SL_PRIO_CACHE.intValue()) {
            isNeedNotify = false;
            isNeedStore = false;
        } else if (actiontype == WapPushMsg.WAP_PUSH_PRIO_LOW.intValue()
                || actiontype == WapPushMsg.WAP_PUSH_SL_PRIO_LOW.intValue()) {
            isNeedNotify = false;
            isNeedStore = true;
        } else if (actiontype == WapPushMsg.WAP_PUSH_PRIO_MEDIUM.intValue()) {
            isNeedNotify = true;
            isNeedStore = true;
        } else if (actiontype == WapPushMsg.WAP_PUSH_PRIO_HIGH.intValue()
                || actiontype == WapPushMsg.WAP_PUSH_SL_PRIO_LOW.intValue()
                || actiontype == WapPushMsg.WAP_PUSH_SL_PRIO_HIGH.intValue()) {
            isNeedNotify = true;
            isNeedStore = true;
            isShowDialog = true;
        } else if (actiontype == WapPushMsg.WAP_PUSH_PRIO_DELETE.intValue()) {
            isNeedNotify = false;
            isNeedStore = false;
            deleteWapPushMessageBySIID(si_id);
        }

        Log.d(TAG, "=====wap push=======handleWapPushReceived()-->  si_id=" + si_id + ",  expired = " + expired
                + ", isNeedStore = " + isNeedStore + ", isNeedNotify = " + isNeedNotify+"   isShowDialog: "+isShowDialog
                + ", Wap Push Body = " + pushBody);

        Uri messageUri = null;
        int subId = intent.getIntExtra(SUB_ID, -1);
        Log.d(TAG, "===wap push====handleWapPushReceived() subId==" + subId);
        String adddress = null;
        if (isNeedStore) {
            android.telephony.SmsMessage[] messages = Sms.Intents.getMessagesFromIntent(intent);
            messageUri = storePushMessage(Factory.get().getApplicationContext(), messages, pushBody,error,expired, si_id,subId,isNeedNotify);
            adddress = messages[0].getDisplayOriginatingAddress();
        }

        if (isShowDialog) {
            // Need to show wap push message in dialog?
            messageUri = (messageUri == null) ? Uri.parse("") : messageUri;
            Intent sendintent = new Intent("android.intent.action.ShowWapPush");
            sendintent.setClass(Factory.get().getApplicationContext(), WapPushMessageShowActivity.class);
            sendintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            sendintent.putExtra("href", href);
            sendintent.putExtra("messageUri", messageUri.toString());
            sendintent.putExtra("pushBody", pushBody);
            Factory.get().getApplicationContext().startActivity(sendintent);
        }
        Log.d(TAG, "========wap push=======handleWapPushReceived====expired: " + expired
                + "   isNeedStore: " + isNeedStore + "  messageUri: "
                + (messageUri != null ? messageUri.toString() : " null")+"     adddress: "+adddress);
        setExpireAlarm(expired, isNeedStore, messageUri);
    }

    private void deleteWapPushMessageBySIID(final String si_id) {
        Log.d(TAG, "===wap push====1===delete wap push message by si_id ='"+ si_id + "'");
        if (si_id == null || si_id.trim().length() <= 0) {
            return;
        }
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"===wap push====2===delete wap push message by si_id ='"+ si_id + "'");
                    Cursor cursor = cr.query(Sms.CONTENT_URI,
                                    new String[] { "_id" }, "si_id = '" + si_id + "'",null, null);
                    if (cursor == null || cursor.getCount() == 0)
                        return;
                    ArrayList<Integer> smsIdSets = new ArrayList<Integer>();
                    if (cursor.moveToFirst()) {
                        try {
                            do {
                                smsIdSets.add(cursor.getInt(0));
                            } while (cursor.moveToNext());
                         } finally {
                             cursor.close();
                             cursor = null;
                         }
                    }
                    String[] smsUriSets = new String[smsIdSets.size()];
                    for (int i = 0; i < smsIdSets.size(); i++) {
                        smsUriSets[i] = "content://sms/" + smsIdSets.get(i);
                        Log.d(TAG,"=======wap push==3==deleteWapPushMessageBySIID====uri: "+ smsUriSets[i]);
                    }
                    final DatabaseWrapper dbWrapper = DataModel.get().getDatabase();

                    String where = "";
                    for (int i = 0; i < smsUriSets.length; i++) {
                        if (i < smsUriSets.length - 1)
                            where += "sms_message_uri = '" + smsUriSets[i] + "' or ";
                        else
                            where += "sms_message_uri = '" + smsUriSets[i] + "'";
                    }
                    Log.d(TAG,"=======wap push==4==deleteWapPushMessageBySIID====where: "+ where);
                    cursor = dbWrapper.query(DatabaseHelper.MESSAGES_TABLE,
                                             new String[] { "_id" }, where, null, null, null,null);

                    if (cursor == null || cursor.getCount() == 0)
                        return;
                    if (cursor.moveToFirst()) {
                        try {
                            do {
                                String messageId = String.valueOf(cursor.getInt(0));
                                Log.d(TAG,"===wap push===5====deleteExpiredWapPushMessage messageId: "+ messageId);
                                DeleteMessageAction.deleteMessage(messageId);
                            } while (cursor.moveToNext());
                        } finally {
                            cursor.close();
                            cursor = null;
                        }
                    }
                    Log.d(TAG,"===wap push===6====deleteExpiredWapPushMessage======");
                 }
            }).start();
        } catch (Exception e) {
               Log.e(TAG,"====wap push===process deleteWapPushMessageBySIID happened exception!",e);
        }
    }

    /**
     * Store wap push message if it is needed.
     * @param context
     * @param msgs Wap push message
     * @param pushBody Wap push message body parsed by WapPushParser.
     * @param error
     * @param expired
     * @param si_id
     * @return
     */
    private Uri storePushMessage(Context context, android.telephony.SmsMessage[] messages, String pushBody, int error,
            String expired, String si_id,int sub_id,boolean isNeedNotify) {
        final ContentValues messageValues =
                MmsUtils.parseReceivedSmsMessage(context, messages, error);

        Log.i(TAG, "====wap push==storePushMessage===begin====");
        Uri messageUri = null;

        final long nowInMillis =  System.currentTimeMillis();
        final long receivedTimestampMs = MmsUtils.getMessageDate(messages[0], nowInMillis);

        messageValues.put(Sms.Inbox.DATE, receivedTimestampMs);
        // Default to unread and unseen for us but ReceiveSmsMessageAction will override
        // seen for the telephony db.
        if (OsUtil.isAtLeastL_MR1()) {
            messageValues.put(Sms.SUBSCRIPTION_ID, sub_id);
        }
        messageValues.put(Sms.Inbox.READ, 0);
        messageValues.put(Sms.Inbox.SEEN, 0);
        messageValues.put("si_id", si_id);
        messageValues.put(Sms.BODY, pushBody);
        messageValues.put("wap_push", 1);
        if ((expired != null) && (!"".equals(expired))) {
            messageValues.put("expired", expired);
        } else {
            messageValues.put("expired", 0);
        }

        final DatabaseWrapper db = DataModel.get().getDatabase();

        // Get the SIM subscription ID
        Integer subId = messageValues.getAsInteger(Sms.SUBSCRIPTION_ID);
        if (subId == null) {
            subId = ParticipantData.DEFAULT_SELF_SUB_ID;
        }
        // Make sure we have a sender address
        String address = messageValues.getAsString(Sms.ADDRESS);
        if (TextUtils.isEmpty(address)) {
            LogUtil.w(TAG, "Received an SMS without an address; using unknown sender.");
            address = ParticipantData.getUnknownSenderDestination();
            messageValues.put(Sms.ADDRESS, address);
        }
        final ParticipantData rawSender = ParticipantData.getFromRawPhoneBySimLocale(
                address, subId);

        // TODO: Should use local timestamp for this?
        final long received = messageValues.getAsLong(Sms.DATE);
        // Inform sync that message has been added at local received timestamp
        final SyncManager syncManager = DataModel.get().getSyncManager();
        syncManager.onNewMessageInserted(received);

        // Make sure we've got a thread id
        final long threadId = MmsSmsUtils.Threads.getOrCreateThreadId(context, address);
        messageValues.put(Sms.THREAD_ID, threadId);
        final boolean blocked = BugleDatabaseOperations.isBlockedDestination(
                db, rawSender.getNormalizedDestination());
        final String conversationId = BugleDatabaseOperations.
                getOrCreateConversationFromRecipient(db, threadId, blocked, rawSender);

        final boolean messageInFocusedConversation =
                DataModel.get().isFocusedConversation(conversationId);
        final boolean messageInObservableConversation =
                DataModel.get().isNewMessageObservable(conversationId);

        MessageData message = null;
        // Only the primary user gets to insert the message into the telephony db and into bugle's
        // db. The secondary user goes through this path, but skips doing the actual insert. It
        // goes through this path because it needs to compute messageInFocusedConversation in order
        // to calculate whether to skip the notification and play a soft sound if the user is
        // already in the conversation.
        if (!OsUtil.isSecondaryUser()) {
            final boolean read = messageValues.getAsBoolean(Sms.Inbox.READ)
                    || messageInFocusedConversation;
            // If you have read it you have seen it
            final boolean seen = read || messageInObservableConversation || blocked;
            messageValues.put(Sms.Inbox.READ, read ? Integer.valueOf(1) : Integer.valueOf(0));

            // incoming messages are marked as seen in the telephony db
            messageValues.put(Sms.Inbox.SEEN, 1);

            // Insert into telephony
            messageUri = context.getContentResolver().insert(Sms.Inbox.CONTENT_URI,
                    messageValues);

            if (messageUri != null) {
                if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
                    LogUtil.d(TAG, "ReceiveSmsMessageAction: Inserted SMS message into telephony, "
                            + "uri = " + messageUri);
                }
            } else {
                LogUtil.e(TAG, "ReceiveSmsMessageAction: Failed to insert SMS into telephony!");
            }

            final String text = messageValues.getAsString(Sms.BODY);
            final String subject = messageValues.getAsString(Sms.SUBJECT);
            final long sent = messageValues.getAsLong(Sms.DATE_SENT);
            final ParticipantData self = ParticipantData.getSelfParticipant(subId);
            final Integer pathPresent = messageValues.getAsInteger(Sms.REPLY_PATH_PRESENT);
            final String smsServiceCenter = messageValues.getAsString(Sms.SERVICE_CENTER);
            String conversationServiceCenter = null;
            // Only set service center if message REPLY_PATH_PRESENT = 1
            if (pathPresent != null && pathPresent == 1 && !TextUtils.isEmpty(smsServiceCenter)) {
                conversationServiceCenter = smsServiceCenter;
            }
            db.beginTransaction();
            try {
                final String participantId =
                        BugleDatabaseOperations.getOrCreateParticipantInTransaction(db, rawSender);
                final String selfId =
                        BugleDatabaseOperations.getOrCreateParticipantInTransaction(db, self);

                message = MessageData.createReceivedSmsMessage(messageUri, conversationId,
                        participantId, selfId, text, subject, sent, received, seen, read);

                BugleDatabaseOperations.insertNewMessageInTransaction(db, message);

                BugleDatabaseOperations.updateConversationMetadataInTransaction(db, conversationId,
                        message.getMessageId(), message.getReceivedTimeStamp(), blocked,
                        conversationServiceCenter, true, isNeedNotify);

                final ParticipantData sender = ParticipantData.getFromId(db, participantId);
                BugleActionToasts.onMessageReceived(conversationId, sender, message);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            LogUtil.i(TAG, "ReceiveSmsMessageAction: Received SMS message " + message.getMessageId()
                    + " in conversation " + message.getConversationId()
                    + ", uri = " + messageUri);

            ProcessPendingMessagesAction.scheduleProcessPendingMessagesAction(false, this);
        } else {
            if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
                LogUtil.d(TAG, "ReceiveSmsMessageAction: Not inserting received SMS message for "
                        + "secondary user.");
            }
        }
        // Show a notification to let the user know a new message has arrived
        /* SPRD: modified for bug 499870 begin */
        BugleNotifications.update(false/*silent*/, conversationId, BugleNotifications.UPDATE_ALL, subId);
        /* SPRD: modified for bug 499870 end */

        MessagingContentProvider.notifyMessagesChanged(conversationId);
        MessagingContentProvider.notifyPartsChanged();
 
        return messageUri;
    }


    private void setExpireAlarm(String expired, boolean isNeedStore, Uri messageUri) {
        if (!isNeedStore || TextUtils.isEmpty(expired)) {
            return;
        }
        Context context = Factory.get().getApplicationContext();
        long retryAt = changDate(expired);
        Intent intent = new Intent(WAP_PUSH_EXPRIRE_DELETE_ACTION, null, context, WapPushDeleteReceiver.class);
        intent.putExtra("uri", messageUri.toString());
        PendingIntent operation = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Log.d(TAG, "======wap push=====setExpireAlarm====expired: " + expired + "   retryAt: "
                + retryAt);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, retryAt, operation);
        Log.v(TAG,
                "===wap push====delete expired push message at"
                        + (retryAt - System.currentTimeMillis()) + "ms from now");
    }

    private long changDate(String date) {
        long currentTime = 0;
        Pattern p = Pattern.compile("(\\d{4})(\\d{1,2})(\\d{1,2})(\\d{1,2})(\\d{1,2})(\\d{1,2})");
        Matcher m = p.matcher(date);
        if (m.find()) {
            try {
                String result = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + " " + m.group(4)
                        + ":" + m.group(5) + ":" + m.group(6);
                SimpleDateFormat fomat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date time = fomat.parse(result);
                currentTime = time.getTime();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return currentTime;
    }

}
