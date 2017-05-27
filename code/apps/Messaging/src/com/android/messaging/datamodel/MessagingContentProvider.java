/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.messaging.datamodel;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.android.messaging.BugleApplication;
import com.android.messaging.Factory;
import com.android.messaging.datamodel.action.CertainTypeMessageAllDelAction;
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns;
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns;
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.util.Assert;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.widget.BugleWidgetProvider;
import com.android.messaging.widget.WidgetConversationProvider;
import com.google.common.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

// bug 478514: Add for MmsFolderView Feature -- Begin
import android.content.ContentUris;
import android.util.Log;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.action.DeleteMessageAction;
import com.android.messaging.sms.MmsUtils;
// bug 478514: Add for MmsFolderView Feature -- End
import android.os.Message;

/**
 * A centralized provider for Uris exposed by Bugle.
 *  */
public class MessagingContentProvider extends ContentProvider {
    private static final String TAG = LogUtil.BUGLE_TAG;

    @VisibleForTesting
    public static final String AUTHORITY =
            "com.android.messaging.datamodel.MessagingContentProvider";
    private static final String CONTENT_AUTHORITY = "content://" + AUTHORITY + '/';

    // Conversations query
    private static final String CONVERSATIONS_QUERY = "conversations";

    public static final Uri CONVERSATIONS_URI = Uri.parse(CONTENT_AUTHORITY + CONVERSATIONS_QUERY);
    static final Uri PARTS_URI = Uri.parse(CONTENT_AUTHORITY + DatabaseHelper.PARTS_TABLE);

    // bug 495194 : add for search feature begin
    private static final String QUERY_CONVERSATIONS_TABLE = "conversation";
    public static final Uri QUERY_CONVERSATIONS_URI = Uri
            .parse(CONTENT_AUTHORITY + QUERY_CONVERSATIONS_TABLE);
    public static final Uri URI_TEMP_CONVERSATION = Uri
            .parse("content://mms-sms/temp_conversation");
    // bug 495194 : add for search feature end
    // Messages query
    private static final String MESSAGES_QUERY = "messages";

    static final Uri MESSAGES_URI = Uri.parse(CONTENT_AUTHORITY + MESSAGES_QUERY);

    public static final Uri CONVERSATION_MESSAGES_URI = Uri.parse(CONTENT_AUTHORITY +
            MESSAGES_QUERY + "/conversation");

    // Conversation participants query
    private static final String PARTICIPANTS_QUERY = "participants";

    static class ConversationParticipantsQueryColumns extends ParticipantColumns {
        static final String CONVERSATION_ID = ConversationParticipantsColumns.CONVERSATION_ID;
    }

    static final Uri CONVERSATION_PARTICIPANTS_URI = Uri.parse(CONTENT_AUTHORITY +
            PARTICIPANTS_QUERY + "/conversation");

    public static final Uri PARTICIPANTS_URI = Uri.parse(CONTENT_AUTHORITY + PARTICIPANTS_QUERY);

    // Conversation images query
    private static final String CONVERSATION_IMAGES_QUERY = "conversation_images";

    public static final Uri CONVERSATION_IMAGES_URI = Uri.parse(CONTENT_AUTHORITY +
            CONVERSATION_IMAGES_QUERY);

    private static final String DRAFT_IMAGES_QUERY = "draft_images";

    public static final Uri DRAFT_IMAGES_URI = Uri.parse(CONTENT_AUTHORITY +
            DRAFT_IMAGES_QUERY);

    public static final Uri ICC_URI = Uri.parse("content://sms/icc");
    private static final String SIM_MESSAGES_QUERY = "sim_messages";
    static final Uri SIM_MESSAGES_URI = Uri.parse(CONTENT_AUTHORITY + SIM_MESSAGES_QUERY);

    /**
     * Notifies that <i>all</i> data exposed by the provider needs to be refreshed.
     * <p>
     * <b>IMPORTANT!</b> You probably shouldn't be calling this. Prefer to notify more specific
     * uri's instead. Currently only sync uses this, because sync can potentially update many
     * different tables at once.
     */
    public static void notifyEverythingChanged() {
        final Uri uri = Uri.parse(CONTENT_AUTHORITY);
        final Context context = Factory.get().getApplicationContext();
        final ContentResolver cr = context.getContentResolver();
        cr.notifyChange(uri, null);

        // Notify any conversations widgets the conversation list has changed.
        BugleWidgetProvider.notifyConversationListChanged(context);

        // Notify all conversation widgets to update.
        WidgetConversationProvider.notifyMessagesChanged(context, null /*conversationId*/);
    }

    /**
     * Build a participant uri from the conversation id.
     */
    public static Uri buildConversationParticipantsUri(final String conversationId) {
        final Uri.Builder builder = CONVERSATION_PARTICIPANTS_URI.buildUpon();
        builder.appendPath(conversationId);
        return builder.build();
    }

    public static void notifyParticipantsChanged(final String conversationId) {
        final Uri uri = buildConversationParticipantsUri(conversationId);
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        cr.notifyChange(uri, null);
    }

    public static void notifyAllMessagesChanged() {
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        cr.notifyChange(CONVERSATION_MESSAGES_URI, null);
    }

    public static void notifyAllParticipantsChanged() {
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        cr.notifyChange(CONVERSATION_PARTICIPANTS_URI, null);
    }

    // Default value for unknown dimension of image
    public static final int UNSPECIFIED_SIZE = -1;

    // Internal
    private static final int CONVERSATIONS_QUERY_CODE = 10;

    private static final int CONVERSATION_QUERY_CODE = 20;
    private static final int CONVERSATION_MESSAGES_QUERY_CODE = 30;
    private static final int CONVERSATION_PARTICIPANTS_QUERY_CODE = 40;
    private static final int CONVERSATION_IMAGES_QUERY_CODE = 50;
    private static final int DRAFT_IMAGES_QUERY_CODE = 60;
    private static final int PARTICIPANTS_QUERY_CODE = 70;

    // bug 478514: Add for MmsFolderView Feature -- Begin
    private static final String MESSAGES_UPDATE = "messages_update";
    private static final String MESSAGES_DELETE = "messages_delete";
    private static final String MESSAGE_LIST_VIEW_QUERY = "message_list_view_query";
    private static final int MESSAGE_LIST_VIEW_QUERY_CODE = 80;
    private static final int FOLDER_VIEW_SPECIFY_MESSAGE_UPDATE_CODE = 90;
    private static final int FOLDER_VIEW_SPECIFY_MESSAGE_DELETE_CODE = 100;
    //add for bug 559631
    private static final int FOLDER_VIEW_SPECIFY_MESSAGE_MULTI_DELETE_CODE = 101;
    private static final int FOLDER_VIEW_MESSAGE_QUERY_CODE = 102;
    //add for bug 559631
    private static final int MESSAGES_MESSAGE_QUERY_CODE = 110;
    // bug 478514: Add for MmsFolderView Feature -- End
    // bug 495194 : add for search feature begin
    private static final int QUERY_CONERSATIONS_CODE = 120;
    // bug 495194 : add for search feature end
    private static final String SIM_MESSAGE_LIST_VIEW_QUERY = "sim_message_list_view_query";
    private static final int SIM_MESSAGE_LIST_VIEW_QUERY_CODE = 130;
    private static final String CLEAR_SIM_MESSAGES = "clear_sim_sms_query";
    private static final int CLEAR_SIM_MESSAGES_CODE = 140;

    // TODO: Move to a better structured URI namespace.
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, CONVERSATIONS_QUERY, CONVERSATIONS_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, CONVERSATIONS_QUERY + "/*", CONVERSATION_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, MESSAGES_QUERY + "/conversation/*",
                CONVERSATION_MESSAGES_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, PARTICIPANTS_QUERY + "/conversation/*",
                CONVERSATION_PARTICIPANTS_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, PARTICIPANTS_QUERY, PARTICIPANTS_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, CONVERSATION_IMAGES_QUERY + "/*",
                CONVERSATION_IMAGES_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, DRAFT_IMAGES_QUERY + "/*",
                DRAFT_IMAGES_QUERY_CODE);
        // bug 478514: Add for MmsFolderView Feature -- Begin
        sURIMatcher.addURI(AUTHORITY, MESSAGE_LIST_VIEW_QUERY, MESSAGE_LIST_VIEW_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, MESSAGES_UPDATE + "/*",
                FOLDER_VIEW_SPECIFY_MESSAGE_UPDATE_CODE);
        sURIMatcher.addURI(AUTHORITY, MESSAGES_DELETE + "/*",
                FOLDER_VIEW_SPECIFY_MESSAGE_DELETE_CODE);
        //add for bug559631 begin
        sURIMatcher.addURI(AUTHORITY, MESSAGES_DELETE,
                FOLDER_VIEW_SPECIFY_MESSAGE_MULTI_DELETE_CODE);
        sURIMatcher.addURI(AUTHORITY, "messages",
                FOLDER_VIEW_MESSAGE_QUERY_CODE);
        //add for bug559631 end
        sURIMatcher.addURI(AUTHORITY, MESSAGES_QUERY + "/message/*", MESSAGES_MESSAGE_QUERY_CODE);
        // bug 478514: Add for MmsFolderView Feature -- End

        /*Sim Message begin*/
        sURIMatcher.addURI(AUTHORITY, SIM_MESSAGE_LIST_VIEW_QUERY, SIM_MESSAGE_LIST_VIEW_QUERY_CODE);
        sURIMatcher.addURI(AUTHORITY, CLEAR_SIM_MESSAGES, CLEAR_SIM_MESSAGES_CODE);
        /*Sim Message end*/
        // bug 495194 : add for search feature begin
        sURIMatcher.addURI(AUTHORITY, QUERY_CONVERSATIONS_TABLE,
                QUERY_CONERSATIONS_CODE);
        // bug 495194 : add for search feature end
    }

    /**
     * Build a messages uri from the conversation id.
     */
    public static Uri buildConversationMessagesUri(final String conversationId) {
        final Uri.Builder builder = CONVERSATION_MESSAGES_URI.buildUpon();
        builder.appendPath(conversationId);
        return builder.build();
    }

    public static void notifyMessagesChanged(final String conversationId) {
        final Uri uri = buildConversationMessagesUri(conversationId);
        final Context context = Factory.get().getApplicationContext();
        final ContentResolver cr = context.getContentResolver();
        cr.notifyChange(uri, null);
        notifyConversationListChanged();

        // Notify the widget the messages changed
        WidgetConversationProvider.notifyMessagesChanged(context, conversationId);
    }

    /**
     * Build a conversation metadata uri from a conversation id.
     */
    public static Uri buildConversationMetadataUri(final String conversationId) {
        final Uri.Builder builder = CONVERSATIONS_URI.buildUpon();
        builder.appendPath(conversationId);
        return builder.build();
    }

    public static void notifyConversationMetadataChanged(final String conversationId) {
        final Uri uri = buildConversationMetadataUri(conversationId);
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        cr.notifyChange(uri, null);
        notifyConversationListChanged();
    }

    public static void notifyPartsChanged() {
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        cr.notifyChange(PARTS_URI, null);
    }

    public static void notifyConversationListChanged() {
        final Context context = Factory.get().getApplicationContext();
        final ContentResolver cr = context.getContentResolver();
        cr.notifyChange(CONVERSATIONS_URI, null);

        // bug 478514: Add for MmsFolderView Feature -- Begin
        notifyMessageListViewChanged();
        // bug 478514: Add for MmsFolderView Feature -- End
        // Notify the widget the conversation list changed
        BugleWidgetProvider.notifyConversationListChanged(context);
    }

    // bug 478514: Add for MmsFolderView Feature -- Begin
    public static final Uri MESSAGE_LIST_VIEW_URI = Uri.parse(CONTENT_AUTHORITY
            + MESSAGE_LIST_VIEW_QUERY);
    public static final Uri SINGLE_MESSAGES_URI = Uri.parse(CONTENT_AUTHORITY + MESSAGES_QUERY
            + "/message");
    public static void notifyMessageListViewChanged() {
        final Context context = Factory.get().getApplicationContext();
        final ContentResolver cr = context.getContentResolver();
        cr.notifyChange(MESSAGE_LIST_VIEW_URI, null);
    }
    public static Uri buildSingleMessageUri(final String messageId) {
        final Uri.Builder builder = SINGLE_MESSAGES_URI.buildUpon();
        builder.appendPath(messageId);
        return builder.build();
    }
    private Cursor querySingleMessage(final String messageId, final Uri notifyUri) {
        final String[] queryArgs = {
                messageId
        };
        Log.d("tim_sql_", "querySingleMessage-sql:" + ConversationMessageData.getSingleMessageQuerySql());
        final Cursor cursor = getDatabaseWrapper().rawQuery(
                ConversationMessageData.getSingleMessageQuerySql(), queryArgs);
        cursor.setNotificationUri(getContext().getContentResolver(), notifyUri);
        return cursor;
    }
    // bug 478514: Add for MmsFolderView Feature -- End
    /**
     * Build a conversation images uri from a conversation id.
     */
    public static Uri buildConversationImagesUri(final String conversationId) {
        final Uri.Builder builder = CONVERSATION_IMAGES_URI.buildUpon();
        builder.appendPath(conversationId);
        return builder.build();
    }

    /**
     * Build a draft images uri from a conversation id.
     */
    public static Uri buildDraftImagesUri(final String conversationId) {
        final Uri.Builder builder = DRAFT_IMAGES_URI.buildUpon();
        builder.appendPath(conversationId);
        return builder.build();
    }

    private DatabaseHelper mDatabaseHelper;
    private DatabaseWrapper mDatabaseWrapper;

    public MessagingContentProvider() {
        super();
    }

    @VisibleForTesting
    public void setDatabaseForTest(final DatabaseWrapper db) {
        Assert.isTrue(BugleApplication.isRunningTests());
        mDatabaseWrapper = db;
    }

    private DatabaseWrapper getDatabaseWrapper() {
        if (mDatabaseWrapper == null) {
            mDatabaseWrapper = mDatabaseHelper.getDatabase();
        }
        return mDatabaseWrapper;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, String selection,
            final String[] selectionArgs, String sortOrder) {

        // Processes other than self are allowed to temporarily access the media
        // scratch space; we grant uri read access on a case-by-case basis. Dialer app and
        // contacts app would doQuery() on the vCard uri before trying to open the inputStream.
        // There's nothing that we need to return for this uri so just No-Op.
        //if (isMediaScratchSpaceUri(uri)) {
        //    return null;
        //}

        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        String[] queryArgs = selectionArgs;
        final int match = sURIMatcher.match(uri);
        String groupBy = null;
        String limit = null;
        switch (match) {
            case CONVERSATIONS_QUERY_CODE:
                queryBuilder.setTables(ConversationListItemData.getConversationListView());
                // Hide empty conversations (ones with 0 sort_timestamp)
                queryBuilder.appendWhere(ConversationColumns.SORT_TIMESTAMP + " > 0 ");
                break;
            case CONVERSATION_QUERY_CODE:
                queryBuilder.setTables(ConversationListItemData.getConversationListView());
                if (uri.getPathSegments().size() == 2) {
                    queryBuilder.appendWhere(ConversationColumns._ID + "=?");
                    // Get the conversation id from the uri
                    queryArgs = prependArgs(queryArgs, uri.getPathSegments().get(1));
                } else {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
                break;
            case CONVERSATION_PARTICIPANTS_QUERY_CODE:
                queryBuilder.setTables(DatabaseHelper.PARTICIPANTS_TABLE);
                if (uri.getPathSegments().size() == 3 &&
                        TextUtils.equals(uri.getPathSegments().get(1), "conversation")) {
                    queryBuilder.appendWhere(ParticipantColumns._ID + " IN ( " + "SELECT "
                            + ConversationParticipantsColumns.PARTICIPANT_ID + " AS "
                            + ParticipantColumns._ID
                            + " FROM " + DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE
                            + " WHERE " + ConversationParticipantsColumns.CONVERSATION_ID
                            + " =? UNION SELECT " + ParticipantColumns._ID + " FROM "
                            + DatabaseHelper.PARTICIPANTS_TABLE + " WHERE "
                            + ParticipantColumns.SUB_ID + " != "
                            + ParticipantData.OTHER_THAN_SELF_SUB_ID + " )");
                    // Get the conversation id from the uri
                    queryArgs = prependArgs(queryArgs, uri.getPathSegments().get(2));
                } else {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
                break;
            case PARTICIPANTS_QUERY_CODE:
                queryBuilder.setTables(DatabaseHelper.PARTICIPANTS_TABLE);
                if (uri.getPathSegments().size() != 1) {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
                break;
            case CONVERSATION_MESSAGES_QUERY_CODE:
                if (uri.getPathSegments().size() == 3 &&
                    TextUtils.equals(uri.getPathSegments().get(1), "conversation")) {
                    // Get the conversation id from the uri
                    final String conversationId = uri.getPathSegments().get(2);

                    // We need to handle this query differently, instead of falling through to the
                    // generic query call at the bottom. For performance reasons, the conversation
                    // messages query is executed as a raw query. It is invalid to specify
                    // selection/sorting for this query.

                    if (selection == null && selectionArgs == null && sortOrder == null) {
                        return queryConversationMessages(conversationId, uri);
                    } else {
                        throw new IllegalArgumentException(
                                "Cannot set selection or sort order with this query");
                    }
                } else {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
            case CONVERSATION_IMAGES_QUERY_CODE:
                queryBuilder.setTables(ConversationImagePartsView.getViewName());
                if (uri.getPathSegments().size() == 2) {
                    // Exclude draft.
                    queryBuilder.appendWhere(
                            ConversationImagePartsView.Columns.CONVERSATION_ID + " =? AND " +
                                    ConversationImagePartsView.Columns.STATUS + "<>" +
                                    MessageData.BUGLE_STATUS_OUTGOING_DRAFT);
                    // Get the conversation id from the uri
                    queryArgs = prependArgs(queryArgs, uri.getPathSegments().get(1));
                } else {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
                break;
            case DRAFT_IMAGES_QUERY_CODE:
                queryBuilder.setTables(ConversationImagePartsView.getViewName());
                if (uri.getPathSegments().size() == 2) {
                    // Draft only.
                    queryBuilder.appendWhere(
                            ConversationImagePartsView.Columns.CONVERSATION_ID + " =? AND " +
                                    ConversationImagePartsView.Columns.STATUS + "=" +
                                    MessageData.BUGLE_STATUS_OUTGOING_DRAFT);
                    // Get the conversation id from the uri
                    queryArgs = prependArgs(queryArgs, uri.getPathSegments().get(1));
                } else {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
                break;
            // bug 478514: Add for MmsFolderView Feature -- Begin
            case MESSAGE_LIST_VIEW_QUERY_CODE:
                queryBuilder.setTables(DatabaseHelper.MESSAGE_LIST_VIEW);
                Log.d("tim_V6", "MessagingContentProvider:MESSAGE_LIST_VIEW_QUERY_CODE->where="
                        + selection);
                break;
            case MESSAGES_MESSAGE_QUERY_CODE:
                if (uri.getPathSegments().size() == 3 && TextUtils.equals(uri.getPathSegments().get(1), "message")) {
                    final String messageId = uri.getPathSegments().get(2);
                    return querySingleMessage(messageId, uri);
                } else {
                    throw new IllegalArgumentException("Malformed URI " + uri);
                }
            case SIM_MESSAGE_LIST_VIEW_QUERY_CODE:
                queryBuilder.setTables(DatabaseHelper.SIM_MESSAGE_LIST_VIEW);
                Log.d("SimMessage", "MessagingContentProvider:SIM_MESSAGE_LIST_VIEW_QUERY_CODE->where="
                        + selection);
                break;
            // bug 495194 : add for search feature begin
            case QUERY_CONERSATIONS_CODE:
                System.out.println("enter QUERY_CONERSATIONS_CODE");
                queryBuilder.setTables(DatabaseHelper.CONVERSATIONS_TABLE);
                break;
            // bug 495194 : add for search feature end
            //add for bug 559631 begin
            case FOLDER_VIEW_MESSAGE_QUERY_CODE:
                Log.d(TAG, "query the max _id in messages in bugle.db");
                queryBuilder.setTables(DatabaseHelper.MESSAGES_TABLE);
                break;
            //add for bug 559631 end
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        final Cursor cursor = getDatabaseWrapper().query(queryBuilder, projection, selection,
                queryArgs, groupBy, null, sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private Cursor queryConversationMessages(final String conversationId, final Uri notifyUri) {
        final String[] queryArgs = { conversationId };
        final Cursor cursor = getDatabaseWrapper().rawQuery(
                ConversationMessageData.getConversationMessagesQuerySql(), queryArgs);
        cursor.setNotificationUri(getContext().getContentResolver(), notifyUri);
        return cursor;
    }

    @Override
    public String getType(final Uri uri) {
        final StringBuilder sb = new
                StringBuilder("vnd.android.cursor.dir/vnd.android.messaging.");

        switch (sURIMatcher.match(uri)) {
            case CONVERSATIONS_QUERY_CODE: {
                sb.append(CONVERSATIONS_QUERY);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
        return sb.toString();
    }

    protected DatabaseHelper getDatabase() {
        return DatabaseHelper.getInstance(getContext());
    }

    @Override
    public ParcelFileDescriptor openFile(final Uri uri, final String fileMode)
            throws FileNotFoundException {
        throw new IllegalArgumentException("openFile not supported: " + uri);
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        throw new IllegalStateException("Insert not supported " + uri);
    }

    @Override
    public int  delete(final Uri uri, final String selection, final String[] selectionArgs) {
        // bug 478514: Add for MmsFolderView Feature -- Begin
        final int match = sURIMatcher.match(uri);
        switch (match) {
            //add for bug 559631 begin
            case FOLDER_VIEW_SPECIFY_MESSAGE_MULTI_DELETE_CODE:
                if (selection != null) {
                    CertainTypeMessageAllDelAction.deleteCertainTypeAll(selection, selectionArgs);
                }
                break;
            //add for bug 559631 end
            case FOLDER_VIEW_SPECIFY_MESSAGE_DELETE_CODE:
                long messageId = ContentUris.parseId(uri);
                String id = Long.toString(messageId);
                DeleteMessageAction.deleteMessage(id);
                break;
            case CLEAR_SIM_MESSAGES_CODE:
                final DatabaseWrapper db = DataModel.get().getDatabase();
                db.execSQL("delete from sim_messages");
                Factory.get().getApplicationContext().getContentResolver().notifyChange(SIM_MESSAGE_LIST_VIEW_URI, null);
            default:
                break;
        }
        return 1;
        // throw new IllegalArgumentException("Delete not supported: " + uri);
        // bug 478514: Add for MmsFolderView Feature -- End
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        // bug 478514: Add for MmsFolderView Feature -- Begin
        int count = 0;
        final DatabaseWrapper db = DataModel.get().getDatabase();
        final int match = sURIMatcher.match(uri);
        switch (match) {
            case FOLDER_VIEW_SPECIFY_MESSAGE_UPDATE_CODE:
                long messageId = ContentUris.parseId(uri);
                String id = Long.toString(messageId);
                db.beginTransaction();
                try {
                    count = db.update(DatabaseHelper.MESSAGES_TABLE, values, MessageColumns._ID
                            + "=?", new String[] {
                        id
                    });
                    if (count > 0) {
                        MessagingContentProvider.notifyMessageListViewChanged();
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                break;
        }
        return count;
        // throw new IllegalArgumentException("Update not supported: " + uri);
        // bug 478514: Add for MmsFolderView Feature -- End
    }

    /**
     * Prepends new arguments to the existing argument list.
     *
     * @param oldArgList The current list of arguments. May be {@code null}
     * @param args The new arguments to prepend
     * @return A new argument list with the given arguments prepended
     */
    private String[] prependArgs(final String[] oldArgList, final String... args) {
        if (args == null || args.length == 0) {
            return oldArgList;
        }
        final int oldArgCount = (oldArgList == null ? 0 : oldArgList.length);
        final int newArgCount = args.length;

        final String[] newArgs = new String[oldArgCount + newArgCount];
        System.arraycopy(args, 0, newArgs, 0, newArgCount);
        if (oldArgCount > 0) {
            System.arraycopy(oldArgList, 0, newArgs, newArgCount, oldArgCount);
        }
        return newArgs;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void dump(final FileDescriptor fd, final PrintWriter writer, final String[] args) {
        // First dump out the default SMS app package name
        String defaultSmsApp = PhoneUtils.getDefault().getDefaultSmsApp();
        if (TextUtils.isEmpty(defaultSmsApp)) {
            if (OsUtil.isAtLeastKLP()) {
                defaultSmsApp = "None";
            } else {
                defaultSmsApp = "None (pre-Kitkat)";
            }
        }
        writer.println("Default SMS app: " + defaultSmsApp);
        // Now dump logs
        LogUtil.dump(writer);
    }

    @Override
    public boolean onCreate() {
        // This is going to wind up calling into createDatabase() below.
        mDatabaseHelper = (DatabaseHelper) getDatabase();

        this.getContext().getContentResolver().registerContentObserver(ICC_URI, true, new SimmessageUpdateObserver());
        // We cannot initialize mDatabaseWrapper yet as the Factory may not be initialized
        return true;
    }
    /* Bug 489223 begin*/
    public static final Uri SIM_MESSAGE_LIST_VIEW_URI = Uri.parse(CONTENT_AUTHORITY
            + SIM_MESSAGE_LIST_VIEW_QUERY);

    class SimmessageUpdateObserver extends ContentObserver {
        public SimmessageUpdateObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
          //bug 554851 :Begin
            if(mHandler.hasMessages(EVENT_ONCHAGE_START)){
                mHandler.removeMessages(EVENT_ONCHAGE_START);
            }
            Message msg = mHandler.obtainMessage(EVENT_ONCHAGE_START);
            mHandler.sendMessage(msg);
           // onSimMessageChage();
        }
    }

    public static void onSimMessageChage(){
        /* Modify by SPRD for Bug:556394 Start */
        if (!OsUtil.hasRequiredPermissions()) {
            LogUtil.d(TAG, "onSimMessageChange and one or all the permissions has been closed!");
        /* Modify by SPRD for Bug:556394 End */
            return;
        }
        final DatabaseWrapper db = DataModel.get().getDatabase();
        db.execSQL("delete from sim_messages");
        LogUtil.d(TAG, "local deleted");
        // insert sim record ,
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        Cursor cursor = cr.query(ICC_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0 &&
            cursor.moveToFirst()) {
            int i = 0;
            boolean insetsuc = false;
            do {
                insertICCToLocal(db, cursor);
            } while (cursor.moveToNext());
        }
        // Notify Sim Message Change
        cr.notifyChange(SIM_MESSAGE_LIST_VIEW_URI, null);
    }

    private static boolean insertICCToLocal(DatabaseWrapper db, Cursor cursor) {
        // N.B.: These calls must appear in the same order as the
        // columns appear in ICC_COLUMNS.
        // modify translation
        int operate_flag = cursor.getInt(cursor.getColumnIndex(OPERATE_FLAG));
        if (operate_flag == OPERATE_DELETE) {
            return false;
        }
        ContentValues values = new ContentValues();

        String address = "" + cursor.getString(cursor.getColumnIndex("address"));
        String name = getContactNameByPhoneNumber(Factory.get().getApplicationContext(), address);
        if (name == null) {
            name = address;
        }
        int status = cursor.getInt(cursor.getColumnIndex("status"));
        int message_status = 100;
        int read = 0;
        if (status == 1) {
            read = 1;
        } else if (status == 5) {
            read = 1;
            message_status = 2;
        } else if (status == 7) {
            read = 1;
            message_status = status;
        }

        values.put(DISPLAY_DESTINATION, address);
        values.put(TEXT, "" + cursor.getString(cursor.getColumnIndex("body")));
        values.put(RECEIVED_TIMESTAMP, "" + cursor.getString(cursor.getColumnIndex("date")));
        values.put(SUB_ID, "" + cursor.getString(cursor.getColumnIndex("sub_id")));
        values.put(NAME, "" + name);
        values.put(READ, read);
        values.put(MESSAGE_STATUS, message_status);
        values.put(CONVERSATION_ID, cursor.getInt(cursor.getColumnIndex("index_on_icc")));
        values.put(ID, cursor.getInt(cursor.getColumnIndex("_id")));
        long rowID = db.insert(TB_SIM_MSG, null, values);
        return (rowID > 0);
    }

    public final static String TB_SIM_MSG = "sim_messages";
    public final static String DISPLAY_DESTINATION = "display_destination";
    public final static String TEXT = "text";
    public final static String RECEIVED_TIMESTAMP = "received_timestamp";
    public final static String SUB_ID = "sub_id";
    public final static String NAME = "name";
    public static final String READ = "read";
    public static final String MESSAGE_STATUS = "message_status";
    public static final String ID = "_id";
    public static final int OPERATE_DELETE = 0x00000004;
    public final static String OPERATE_FLAG = "operate_flag";
    public static final String CONVERSATION_ID = "conversation_id";

    public static String getContactNameByPhoneNumber(Context context,
                                                     String address) {
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

      //bug 554851 :Begin
        Cursor cursor = null;
        try{
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, // Which columns to return.
                    ContactsContract.CommonDataKinds.Phone.NUMBER + " = '"
                            + address + "'", // WHERE clause.
                    null, // WHERE clause value substitution
                    null); // Sort order.
        }catch(NullPointerException e){
            Log.d(TAG, "getContactNameByPhoneNumber :"+e);
        }
      //bug 554851 :End

        if (cursor == null) {
            Log.d(TAG, "getContactNameByPhoneNumber getPeople null");
            return null;
        }
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);

            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            String name = cursor.getString(nameFieldColumnIndex);
            return name;
        }
        return null;
    }
    /* Bug 489223 end */

  //bug 554851 :Begin
    private static final int EVENT_ONCHAGE_START = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_ONCHAGE_START:
                Log.d(TAG, "handler onSimMessageChage");
                onSimMessageChage();
                break;
                }
            }
        };
  //bug 554851 :End
}
