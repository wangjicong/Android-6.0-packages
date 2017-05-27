
package com.sprd.messaging.ui.folderview;

import android.net.Uri;
import android.util.Log;

public class SortMsgDefinitionCollector {

    /**
     * provider def
     */
    public static final String AUTHORITY = "com.android.messaging.datamodel.MessagingContentProvider";
    private static final String CONTENT_AUTHORITY = "content://" + AUTHORITY + '/';
    private static final String MESSAGE_LIST_VIEW_QUERY = "message_list_view_query";
    private static final String MESSAGES_QUERY = "messages";
    private static final String MESSAGES_UPDATE = "messages_update";
    private static final String MESSAGES_DELETE = "messages_delete";
    public static final Uri MESSAGE_LIST_VIEW_URI = Uri.parse(CONTENT_AUTHORITY
            + MESSAGE_LIST_VIEW_QUERY);
    public static final Uri MESSAGE_SPECIFY_UPDATE_URI = Uri.parse(CONTENT_AUTHORITY
            + MESSAGES_UPDATE);
    public static final Uri MESSAGE_SPECIFY_DELETE_URI = Uri.parse(CONTENT_AUTHORITY
            + MESSAGES_DELETE);

    public static final String MESSAGE_LIST_VIEW_STATUS_COLUMN = "message_status";
    public static final String PARTICIPANT_COUNT = "participant_count";
    public static final String OTHER_PARTICIPANT_NORMALIZED_DESTINATION = "participant_normalized_destination";
    public static final String DISPLAY_DESTINATION = "display_destination";
    public static final String RECEIVED_TIMESTAMP = "received_timestamp";
    public static final String MESSAGE_READ = "read";
    public static final String MESSAGE_SEEN = "seen";

    /**
     * Sort type and order key
     */
    public static final String MESSAGE_SORT_TYPE = "msg_sort_type";

    public static final String KEY_BOX_INBOX_ORDER_BY = "inbox_order_by";
    public static final String KEY_BOX_SENT_ORDER_BY = "sent_order_by";
    public static final String KEY_BOX_OUTBOX_ORDER_BY = "outbox_order_by";
    public static final String KEY_BOX_DRAFT_ORDER_BY = "draft_order_by";
    public static final String KEY_BOX_ERROR_ORDER_BY_UNKONE = "error_order_by_unknow";

    public static final int LOADER_ID_DEFAULT = 100;
    public static final int LOADER_ID_TIME_DESC = 101;
    public static final int LOADER_ID_TIME_ASC = 102;
    public static final int LOADER_ID_PHONE_NUM_DESC = 103;
    public static final int LOADER_ID_PHONE_NUM_ASC = 104;

    public static String ORDER_BY_PHONE_NUMBER_DESC = DISPLAY_DESTINATION
            + " DESC";;
    public static String ORDER_BY_PHONE_NUMBER_ASC = DISPLAY_DESTINATION
            + " ASC";
    public static String ORDER_BY_TIME_DESC = RECEIVED_TIMESTAMP + " DESC";
    public static String ORDER_BY_TIME_ASC = RECEIVED_TIMESTAMP + " ASC";

    public static String getMsgOrderKey(int sortType) {
        switch (sortType) {
            case MSG_BOX_INBOX:
                return KEY_BOX_INBOX_ORDER_BY;
            case MSG_BOX_SENT:
                return KEY_BOX_SENT_ORDER_BY;
            case MSG_BOX_OUTBOX:
                return KEY_BOX_OUTBOX_ORDER_BY;
            case MSG_BOX_DRAFT:
                return KEY_BOX_DRAFT_ORDER_BY;
            default:
                Log.d(TAG, "getMsgOrderKey is null, sortType=" + sortType);
                break;
        }
        return KEY_BOX_ERROR_ORDER_BY_UNKONE;
    }

    public static final int MSG_UNKNOW = 0;
    public static final int MSG_BOX_INBOX = 1;
    public static final int MSG_BOX_SENT = 2;
    public static final int MSG_BOX_OUTBOX = 3;
    public static final int MSG_BOX_DRAFT = 4;

    public static final String ORDER_BY_PHONE_NUMBER = "order_by_phone_number";
    public static final String ORDER_BY_RECEIVED_TIME = "order_by_received_time";

    public static final int ORDER_DEFAULT = -1;
    public static final int ORDER_DESC = 0;
    public static final int ORDER_ASC = 1;

    public static String getOrderByPhoneNumberDesc() {
        return ORDER_BY_PHONE_NUMBER_DESC;
    }

    public static String getOrderByPhoneNumberAsc() {
        return ORDER_BY_PHONE_NUMBER_ASC;
    }

    public static String getOrderByReceivedTimeDesc() {
        return ORDER_BY_TIME_DESC;
    }

    public static String getOrderByReceivedTimeAsc() {
        return ORDER_BY_TIME_ASC;
    }

    public static int getSortTypeByStatus(int status) {

        switch (status) {
        // Received:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_DELIVERED:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_COMPLETE:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED:
            case SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE:
                return MSG_BOX_INBOX;
                // Sent:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_COMPLETE:
                return MSG_BOX_SENT;
                // Sending & Fail:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_YET_TO_SEND:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_SENDING:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_RESENDING:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_AWAITING_RETRY:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_FAILED:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER:
                return MSG_BOX_OUTBOX;
                // Draft:
            case SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_DRAFT:
                return MSG_BOX_DRAFT;
            default:
                Log.e(TAG, "Unknow message status, ContactIconView will be null");
                return MSG_UNKNOW;
        }
    }

    /**
     * status field types
     */
    public static final int BUGLE_STATUS_UNKNOWN = 0;
    // Outgoing
    public static final int BUGLE_STATUS_OUTGOING_COMPLETE = 1;
    public static final int BUGLE_STATUS_OUTGOING_DELIVERED = 2;
    // Transitions to either YET_TO_SEND or SEND_AFTER_PROCESSING depending
    // attachments.
    public static final int BUGLE_STATUS_OUTGOING_DRAFT = 3;
    public static final int BUGLE_STATUS_OUTGOING_YET_TO_SEND = 4;
    public static final int BUGLE_STATUS_OUTGOING_SENDING = 5;
    public static final int BUGLE_STATUS_OUTGOING_RESENDING = 6;
    public static final int BUGLE_STATUS_OUTGOING_AWAITING_RETRY = 7;
    public static final int BUGLE_STATUS_OUTGOING_FAILED = 8;
    public static final int BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER = 9;
    // Incoming
    public static final int BUGLE_STATUS_INCOMING_COMPLETE = 100;
    public static final int BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD = 101;
    public static final int BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD = 102;
    public static final int BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING = 103;
    public static final int BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD = 104;
    public static final int BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING = 105;
    public static final int BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED = 106;
    public static final int BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE = 107;
    // All incoming messages expect to have status >=
    // BUGLE_STATUS_FIRST_INCOMING
    public static final int BUGLE_STATUS_FIRST_INCOMING = BUGLE_STATUS_INCOMING_COMPLETE;
    private static final String TAG = "MessagingDataDef";

    public static boolean getIsIncoming(int status) {
        return (status >= SortMsgDefinitionCollector.BUGLE_STATUS_FIRST_INCOMING);
    }

    public static String getOrderQueryWhereAppend(String orderName, int orderType) {
        String append = "";
        switch (orderName) {
            case ORDER_BY_PHONE_NUMBER:
                append = getPhoneNumberAppend(orderType);
                break;
            case ORDER_BY_RECEIVED_TIME:
                append = getReceivedTimeAppend(orderType);
                break;
            default:
                Log.e(TAG, "Have no this order name:" + orderName);
                break;
        }
        return append;
    }

    private static String getPhoneNumberAppend(int orderType) {
        String append = "";
        switch (orderType) {
            case ORDER_ASC:
                append = " ORDER BY " + DISPLAY_DESTINATION + " ASC";
                break;
            case ORDER_DESC:
                append = " ORDER BY " + DISPLAY_DESTINATION + " DESC";
                break;
            default:
                Log.e(TAG, "Have no this order type:" + orderType);
                break;
        }
        return append;
    }

    private static String getReceivedTimeAppend(int orderType) {
        String append = "";
        switch (orderType) {
            case ORDER_ASC:
                append = " ORDER BY " + RECEIVED_TIMESTAMP + " ASC";
                break;
            case ORDER_DESC:
                append = " ORDER BY " + RECEIVED_TIMESTAMP + " DESC";
                break;
            default:
                Log.e(TAG, "Have no this order type:" + orderType);
                break;
        }
        return append;
    }

    public static String getSortTypeQueryWhere(int sort_type) {
        String where = null;
        switch (sort_type) {
            case MSG_BOX_INBOX:
                where = getInboxQueryWhere();
                break;
            case MSG_BOX_SENT:
                where = getSentBoxQueryWhere();
                break;
            case MSG_BOX_OUTBOX:
                where = getOutboxQueryWhere();
                break;
            case MSG_BOX_DRAFT:
                where = getDraftboxQueryWhere();
                break;
            default:
                Log.e(TAG, "Have no this sort_type:" + sort_type);
                break;
        }
        return where;
    }

    private static String getDraftboxQueryWhere() {
        return "(" + SortMsgDefinitionCollector.MESSAGE_LIST_VIEW_STATUS_COLUMN + "="
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_DRAFT + " AND "
                + SortMsgDefinitionCollector.PARTICIPANT_COUNT + " = " + 1 + ") ";
    }

    private static String getOutboxQueryWhere() {
        return "(" + SortMsgDefinitionCollector.MESSAGE_LIST_VIEW_STATUS_COLUMN + " IN " + "("
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_YET_TO_SEND + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_SENDING + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_RESENDING + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_AWAITING_RETRY + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_FAILED + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER + ")" + " AND "
                + SortMsgDefinitionCollector.PARTICIPANT_COUNT + " = " + 1 + ") ";
    }

    private static String getSentBoxQueryWhere() {
        return "(" + SortMsgDefinitionCollector.MESSAGE_LIST_VIEW_STATUS_COLUMN + "="
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_COMPLETE + " AND "
                + SortMsgDefinitionCollector.PARTICIPANT_COUNT + " = " + 1 + ") ";
    }

    private static String getInboxQueryWhere() {
        return "(" + SortMsgDefinitionCollector.MESSAGE_LIST_VIEW_STATUS_COLUMN + " IN " + "("
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_COMPLETE + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_OUTGOING_DELIVERED + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED + ", "
                + SortMsgDefinitionCollector.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE + ")" + ") ";
    }

    /**
     * X-Mms-Response-Status field types.
     */
    public static final int RESPONSE_STATUS_OK = 0x80;
    public static final int RESPONSE_STATUS_ERROR_UNSPECIFIED = 0x81;
    public static final int RESPONSE_STATUS_ERROR_SERVICE_DENIED = 0x82;
    public static final int RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT = 0x83;
    public static final int RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED = 0x84;
    public static final int RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND = 0x85;
    public static final int RESPONSE_STATUS_ERROR_NETWORK_PROBLEM = 0x86;
    public static final int RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED = 0x87;
    public static final int RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE = 0x88;
    public static final int RESPONSE_STATUS_ERROR_TRANSIENT_FAILURE = 0xC0;
    public static final int RESPONSE_STATUS_ERROR_TRANSIENT_SENDNG_ADDRESS_UNRESOLVED = 0xC1;
    public static final int RESPONSE_STATUS_ERROR_TRANSIENT_MESSAGE_NOT_FOUND = 0xC2;
    public static final int RESPONSE_STATUS_ERROR_TRANSIENT_NETWORK_PROBLEM = 0xC3;
    public static final int RESPONSE_STATUS_ERROR_TRANSIENT_PARTIAL_SUCCESS = 0xC4;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_FAILURE = 0xE0;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_SERVICE_DENIED = 0xE1;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_FORMAT_CORRUPT = 0xE2;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_SENDING_ADDRESS_UNRESOLVED = 0xE3;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND = 0xE4;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_CONTENT_NOT_ACCEPTED = 0xE5;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_LIMITATIONS_NOT_MET = 0xE6;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_REQUEST_NOT_ACCEPTED = 0xE7;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_FORWARDING_DENIED = 0xE8;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_NOT_SUPPORTED = 0xE9;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_ADDRESS_HIDING_NOT_SUPPORTED = 0xEA;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_LACK_OF_PREPAID = 0xEB;
    public static final int RESPONSE_STATUS_ERROR_PERMANENT_END = 0xFF;

    /**
     * PROTOCOL Values
     */
    public static final int PROTOCOL_UNKNOWN = -1; // Unknown type
    public static final int PROTOCOL_SMS = 0; // SMS message
    public static final int PROTOCOL_MMS = 1; // MMS message
    public static final int PROTOCOL_MMS_PUSH_NOTIFICATION = 2; // MMS WAP push
                                                                // notification

    public static boolean getIsSms(int protocol) {
        return (protocol == SortMsgDefinitionCollector.PROTOCOL_SMS);
    }

    public static boolean getIsMms(int protocol) {
        return (getIsMmsRetrieveConf(protocol) || getIsMmsNotificationInd(protocol));
    }

    public static boolean getIsMmsRetrieveConf(int protocol) {
        return (protocol == SortMsgDefinitionCollector.PROTOCOL_MMS);
    }

    public static boolean getIsMmsNotificationInd(int protocol) {
        return (protocol == SortMsgDefinitionCollector.PROTOCOL_MMS_PUSH_NOTIFICATION);
    }

    public static final int RAW_TELEPHONY_STATUS_MESSAGE_TOO_BIG = 10000;

    /**
     * sim process
     * */
   // We always use -1 as default/invalid sub id although system may give us anything negative
    public static final int DEFAULT_SELF_SUB_ID = -1;
    // Active slot ids are non-negative. Using -1 to designate to inactive self participants.
    public static final int INVALID_SLOT_ID = -1;

    public static final String SHOW_MESSAGE_BY_SUB_ID = "show_which_sim";
    public static final int SHOW_ALL_MESSAGE = 0;

    /**
     * mutil-media
     * */
    // Default value for unknown dimension of image
    public static final int UNSPECIFIED_SIZE = -1;
    
    /**
     * folder view and messaging common service, cancle notification, resend message,
     * */
    public static final String ACTION_FOLDER_VIEW_MESSAGING_COMM = "com.android.action.FOLDER_VIEW_MESSAGING_COMM";
    public static final String KEY_NOTIFICATION = "key_notification";
    public static final String KEY_COMM = "key_comm";
    public static final int INVAIL_NOTIFICATION_ID = -1;

    public static final int KEY_SMS_NOTIFICATION_ID = 0;
    public static final int KEY_MESSAGE_RESEND = 1;
    public static final int KEY_MSG_SEND_ERROR = 2;
    public static final int KEY_PARTICIPANTS_REFRESH = 3;    
}
