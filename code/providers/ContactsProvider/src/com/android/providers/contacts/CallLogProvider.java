/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.providers.contacts;

import static com.android.providers.contacts.util.DbQueryUtils.checkForSupportedColumns;
import static com.android.providers.contacts.util.DbQueryUtils.getEqualityClause;
import static com.android.providers.contacts.util.DbQueryUtils.getInequalityClause;

import android.app.AppOpsManager;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.SearchIndexColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DbProperties;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import com.android.providers.contacts.SearchIndexManager.FtsQueryBuilder;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.UserUtils;
import com.google.common.annotations.VisibleForTesting;
import com.sprd.providers.contacts.PinYin;
import com.android.common.content.ProjectionMap;
import android.provider.ContactsContract;
import android.provider.ContactsContract.DisplayNameSources;

import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.CountDownLatch;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Call log content provider.
 */
public class CallLogProvider extends CallLogProviderParentEx /* SPRD: @orig ContentProvider*/ {
    private static final String TAG = CallLogProvider.class.getSimpleName();

    private static final int BACKGROUND_TASK_INITIALIZE = 0;
    private static final int BACKGROUND_TASK_ADJUST_PHONE_ACCOUNT = 1;

    /** Selection clause for selecting all calls that were made after a certain time */
    private static final String MORE_RECENT_THAN_SELECTION = Calls.DATE + "> ?";
    /** Selection clause to use to exclude voicemail records.  */
    private static final String EXCLUDE_VOICEMAIL_SELECTION = getInequalityClause(
            Calls.TYPE, Calls.VOICEMAIL_TYPE);
    /** Selection clause to exclude hidden records. */
    private static final String EXCLUDE_HIDDEN_SELECTION = getEqualityClause(
            Calls.PHONE_ACCOUNT_HIDDEN, 0);

    @VisibleForTesting
    static final String[] CALL_LOG_SYNC_PROJECTION = new String[] {
        Calls.NUMBER,
        Calls.NUMBER_PRESENTATION,
        Calls.TYPE,
        Calls.FEATURES,
        Calls.DATE,
        Calls.DURATION,
        Calls.DATA_USAGE,
        Calls.PHONE_ACCOUNT_COMPONENT_NAME,
        Calls.PHONE_ACCOUNT_ID,
        Calls.USERS_ID
    };

    static final String[] MINIMAL_PROJECTION = new String[] { Calls._ID };

    private static final int CALLS = 1;

    private static final int CALLS_ID = 2;

    private static final int CALLS_FILTER = 3;

    /* SPRD: DIALER SEARCH FEATURE @{ */
    private static final int SEARCH_SUGGEST = 4;
    private static final int CALLSGROUP = 5;
    /* @} */
    /* SPRD: Matching callLog when search in dialpad feature @{ */
    private static final int SMART_DIALMATCH = 6;
    private static final int REGULAR_DIALMATCH = 7;
    private static final int CALLABLE_DIALMATCH = 8;
    private static final int CALLS_ONLY = 9;
    private static final int CALLS_ONLY_DIALMATCH = 10;

    private static int DIAL_MATCH_MINMATCH = 7;
    /* @}  */
    // SPRD: add for bug493481
    public static final boolean DEBUG = "userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE);

    private static final String UNHIDE_BY_PHONE_ACCOUNT_QUERY =
            "UPDATE " + Tables.CALLS + " SET " + Calls.PHONE_ACCOUNT_HIDDEN + "=0 WHERE " +
            Calls.PHONE_ACCOUNT_COMPONENT_NAME + "=? AND " + Calls.PHONE_ACCOUNT_ID + "=?;";

    private static final String UNHIDE_BY_ADDRESS_QUERY =
            "UPDATE " + Tables.CALLS + " SET " + Calls.PHONE_ACCOUNT_HIDDEN + "=0 WHERE " +
            Calls.PHONE_ACCOUNT_ADDRESS + "=?;";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls", CALLS);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/#", CALLS_ID);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/filter/*", CALLS_FILTER);
        /* SPRD: DIALER SEARCH FEATURE @ { */
        sURIMatcher.addURI(CallLog.AUTHORITY, "callsgroup", CALLSGROUP);
        sURIMatcher.addURI(CallLog.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        sURIMatcher.addURI(CallLog.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        /* @} */
        /* SPRD: add uri for calllog search @{ */
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/dialmatch/*", SMART_DIALMATCH);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/regularmatch/*", REGULAR_DIALMATCH);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/callabledialmatch/*", CALLABLE_DIALMATCH);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls_only", CALLS_ONLY);
        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/calls_only_dialmatch/*", CALLS_ONLY_DIALMATCH);
        /* @} */
    }

    private static final HashMap<String, String> sCallsProjectionMap;
    /* SPRD: Matching callLog when search in dialpad feature @{ */
    private static ProjectionMap sSmartDialMatchContactMap;
    private static ProjectionMap sSmartDialMatchCallLogMap;
    /* @} */
    static {

        // Calls projection map
        sCallsProjectionMap = new HashMap<String, String>();
        sCallsProjectionMap.put(Calls._ID, Calls._ID);
        sCallsProjectionMap.put(Calls.NUMBER, Calls.NUMBER);
        sCallsProjectionMap.put(Calls.NUMBER_PRESENTATION, Calls.NUMBER_PRESENTATION);
        sCallsProjectionMap.put(Calls.DATE, Calls.DATE);
        sCallsProjectionMap.put(Calls.DURATION, Calls.DURATION);
        sCallsProjectionMap.put(Calls.DATA_USAGE, Calls.DATA_USAGE);
        sCallsProjectionMap.put(Calls.TYPE, Calls.TYPE);
        sCallsProjectionMap.put(Calls.FEATURES, Calls.FEATURES);
        sCallsProjectionMap.put(Calls.PHONE_ACCOUNT_COMPONENT_NAME, Calls.PHONE_ACCOUNT_COMPONENT_NAME);
        sCallsProjectionMap.put(Calls.PHONE_ACCOUNT_ID, Calls.PHONE_ACCOUNT_ID);
        sCallsProjectionMap.put(Calls.PHONE_ACCOUNT_ADDRESS, Calls.PHONE_ACCOUNT_ADDRESS);
        sCallsProjectionMap.put(Calls.NEW, Calls.NEW);
        sCallsProjectionMap.put(Calls.VOICEMAIL_URI, Calls.VOICEMAIL_URI);
        sCallsProjectionMap.put(Calls.TRANSCRIPTION, Calls.TRANSCRIPTION);
        sCallsProjectionMap.put(Calls.IS_READ, Calls.IS_READ);
        sCallsProjectionMap.put(Calls.CACHED_NAME, Calls.CACHED_NAME);
        sCallsProjectionMap.put(Calls.CACHED_NUMBER_TYPE, Calls.CACHED_NUMBER_TYPE);
        sCallsProjectionMap.put(Calls.CACHED_NUMBER_LABEL, Calls.CACHED_NUMBER_LABEL);
        sCallsProjectionMap.put(Calls.COUNTRY_ISO, Calls.COUNTRY_ISO);
        sCallsProjectionMap.put(Calls.GEOCODED_LOCATION, Calls.GEOCODED_LOCATION);
        sCallsProjectionMap.put(Calls.CACHED_LOOKUP_URI, Calls.CACHED_LOOKUP_URI);
        sCallsProjectionMap.put(Calls.CACHED_MATCHED_NUMBER, Calls.CACHED_MATCHED_NUMBER);
        sCallsProjectionMap.put(Calls.CACHED_NORMALIZED_NUMBER, Calls.CACHED_NORMALIZED_NUMBER);
        sCallsProjectionMap.put(Calls.CACHED_PHOTO_ID, Calls.CACHED_PHOTO_ID);
        sCallsProjectionMap.put(Calls.CACHED_PHOTO_URI, Calls.CACHED_PHOTO_URI);
        sCallsProjectionMap.put(Calls.CACHED_FORMATTED_NUMBER, Calls.CACHED_FORMATTED_NUMBER);
        sCallsProjectionMap.put(Calls.USERS_ID, Calls.USERS_ID);
        /* SPRD: DIALER SEARCH FEATURE @{ */
        sCallsProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_1, Calls.CACHED_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
        sCallsProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_2, Calls.NUMBER + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
        sCallsProjectionMap.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Calls._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);

        sCallsProjectionMap.put(ContactsDatabaseHelper.CACHED_NORMALIZED_SIMPLE_NAME,ContactsDatabaseHelper.CACHED_NORMALIZED_SIMPLE_NAME);
        sCallsProjectionMap.put(ContactsDatabaseHelper.CACHED_NORMALIZED_FULL_NAME,ContactsDatabaseHelper.CACHED_NORMALIZED_FULL_NAME);
        /* @} */
        /* SPRD: Matching callLog when search in dialpad feature @{ */
        initDialMatchProjectMap();
        /* @} */
    }

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private volatile CountDownLatch mReadAccessLatch;

    private ContactsDatabaseHelper mDbHelper;
    private DatabaseUtils.InsertHelper mCallsInserter;
    private boolean mUseStrictPhoneNumberComparation;
    private VoicemailPermissions mVoicemailPermissions;
    private CallLogInsertionHelper mCallLogInsertionHelper;

    @Override
    public boolean onCreate() {
        setAppOps(AppOpsManager.OP_READ_CALL_LOG, AppOpsManager.OP_WRITE_CALL_LOG);
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "CallLogProvider.onCreate start");
        }
        final Context context = getContext();
        mDbHelper = getDatabaseHelper(context);
        mUseStrictPhoneNumberComparation =
            context.getResources().getBoolean(
                    com.android.internal.R.bool.config_use_strict_phone_number_comparation);
        mVoicemailPermissions = new VoicemailPermissions(context);
        mCallLogInsertionHelper = createCallLogInsertionHelper(context);

        mBackgroundThread = new HandlerThread("CallLogProviderWorker",
                Process.THREAD_PRIORITY_BACKGROUND);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                performBackgroundTask(msg.what, msg.obj);
            }
        };

        mReadAccessLatch = new CountDownLatch(1);

        scheduleBackgroundTask(BACKGROUND_TASK_INITIALIZE, null);

        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "CallLogProvider.onCreate finish");
        }
        return true;
    }

    @VisibleForTesting
    protected CallLogInsertionHelper createCallLogInsertionHelper(final Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    @VisibleForTesting
    protected ContactsDatabaseHelper getDatabaseHelper(final Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        waitForAccess(mReadAccessLatch);
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(Tables.CALLS);
        qb.setProjectionMap(sCallsProjectionMap);
        qb.setStrict(true);

        final SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, true /*isQuery*/);
        selectionBuilder.addClause(EXCLUDE_HIDDEN_SELECTION);
        String groupBy = null; // DIALER SEARCH FEATURE

        final int match = sURIMatcher.match(uri);
        switch (match) {
            case CALLS:
                break;

            case CALLS_ID: {
                selectionBuilder.addClause(getEqualityClause(Calls._ID,
                        parseCallIdFromUri(uri)));
                break;
            }

            case CALLS_FILTER: {
                List<String> pathSegments = uri.getPathSegments();
                String phoneNumber = pathSegments.size() >= 2 ? pathSegments.get(2) : null;
                if (!TextUtils.isEmpty(phoneNumber)) {
                    qb.appendWhere("PHONE_NUMBERS_EQUAL(number, ");
                    qb.appendWhereEscapeString(phoneNumber);
                    qb.appendWhere(mUseStrictPhoneNumberComparation ? ", 1)" : ", 0)");
                } else {
                    qb.appendWhere(Calls.NUMBER_PRESENTATION + "!="
                            + Calls.PRESENTATION_ALLOWED);
                }
                break;
            }
            /* SPRD: DIALER SEARCH FEATURE @{ */
            case SEARCH_SUGGEST: {
                qb.setTables("calls");
                qb.setProjectionMap(sCallsProjectionMap);
                String[] columns = new String[] { BaseColumns._ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_TEXT_2,
                        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID };

                if (selectionArgs == null) {
                    throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
                }

                String query = selectionArgs[0];
                String select_mime = SearchManager.SUGGEST_COLUMN_TEXT_1 + " like ?" + " OR " +
                        SearchManager.SUGGEST_COLUMN_TEXT_2 + " like ?" + " OR " +
                        ContactsDatabaseHelper.CACHED_NORMALIZED_SIMPLE_NAME + " like ?" + " OR " +
                        ContactsDatabaseHelper.CACHED_NORMALIZED_FULL_NAME + " like ?";
                String[] selectArgForSearch = { "%" + query + "%", "%" + query + "%", "%" + query + "%", query + "%"};
                if(!TextUtils.isEmpty(query)){
                    return qb.query(mDbHelper.getWritableDatabase(), columns,select_mime, selectArgForSearch, null, null, sortOrder,null);
                }else {
                    return null;
                }
            }
            case CALLSGROUP: {
                groupBy = Calls.NUMBER;
                break;
            }
            /* SPRD: Matching callLog when search in dialpad feature @{ */
            case CALLS_ONLY_DIALMATCH: {
                String filterParam = null;
                String normalizeNumber = null;
                Cursor cursor = null;
                String callsSqlString = null;
                StringBuilder sb = new StringBuilder();
                SQLiteQueryBuilder callsQb = new SQLiteQueryBuilder();

                if (uri.getPathSegments().size() > 2) {
                    filterParam = uri.getLastPathSegment();
                }

                if (filterParam != null) {
                    normalizeNumber = PhoneNumberUtils.normalizeNumber(filterParam);
                }
                if (!TextUtils.isEmpty(normalizeNumber)) {
                    callsQb.setTables(Tables.CALLS);
                    callsQb.setProjectionMap(sSmartDialMatchCallLogMap);
                    callsQb.setStrict(true);
                    sb.append("(");
                    sb.append(Calls.NUMBER + " LIKE '");
                    sb.append("%");
                    sb.append(normalizeNumber);
                    sb.append("%'");

                    int lenth = normalizeNumber.length();
                    if (lenth >= DIAL_MATCH_MINMATCH) {
                        String minMatch = normalizeNumber.substring(lenth - DIAL_MATCH_MINMATCH,
                                lenth);
                        sb.append(" OR ");
                        sb.append(Calls.NUMBER + " LIKE '%");
                        sb.append(minMatch);
                        sb.append("'");
                    }
                    sb.append(")");
                    callsQb.appendWhere(sb);

                    callsSqlString = callsQb.buildQuery(projection, selection, "number", null,
                            sortOrder, null);
                    cursor = mDbHelper.getWritableDatabase().rawQuery(callsSqlString, null);
                }
                return cursor;
            }
            case CALLS_ONLY: {
                String calllogSqlString = null;
                Cursor cursor = null;
                SQLiteQueryBuilder calllogQb = new SQLiteQueryBuilder();

                calllogQb.setTables(Tables.CALLS);
                calllogQb.setProjectionMap(sSmartDialMatchCallLogMap);
                calllogQb.setStrict(true);

                calllogSqlString = calllogQb.buildQuery(projection, selection, "number", null,
                        sortOrder, null);
                cursor = mDbHelper.getWritableDatabase().rawQuery(calllogSqlString, null);
                return cursor;
            }
            case SMART_DIALMATCH:
            case REGULAR_DIALMATCH:
            case CALLABLE_DIALMATCH: {
                SQLiteQueryBuilder contactsQb = new SQLiteQueryBuilder();
                SQLiteQueryBuilder calllogQb = new SQLiteQueryBuilder();
                boolean isCalllogNeed = false;
                String filterParam = null;
                String calllogSqlString = null;
                String uinonSql = null;
                String contactSqlString = null;
                String normalizeNumber =null;
                String limit = ContactsProvider2.getLimitParam(uri);
                final String mimeTypeIsPhoneExpression = DataColumns.MIMETYPE_ID + "="
                        + mDbHelper.getMimeTypeIdForPhone();
                final String mimeTypeIsSipExpression = DataColumns.MIMETYPE_ID + "="
                        + mDbHelper.getMimeTypeIdForSip();
                StringBuilder sb = new StringBuilder();

                boolean orNeeded = false;
                boolean hasCondition = false;

                if (uri.getPathSegments().size() > 2) {
                    filterParam = uri.getLastPathSegment();
                }

                contactsQb.setTables(Views.DATA);
                contactsQb.setProjectionMap(sSmartDialMatchContactMap);
                contactsQb.setStrict(true);
                if (match == CALLABLE_DIALMATCH) {
                    contactsQb.appendWhere(" (" + mimeTypeIsPhoneExpression + " OR "
                            + mimeTypeIsSipExpression + ")");
                } else {
                    contactsQb.appendWhere(mimeTypeIsPhoneExpression);
                }

                if (filterParam != null) {
                    final String ftsMatchQuery = SearchIndexManager.getFtsMatchQuery(filterParam,
                            match == SMART_DIALMATCH ? SearchIndexManager.FtsQueryBuilder.UNSCOPED_NORMALIZING
                                    : FtsQueryBuilder.SCOPED_NAME_NORMALIZING);
                    sb.append(" AND (");
                    if (ftsMatchQuery.length() > 0) {
                        sb.append(Data.RAW_CONTACT_ID + " IN " + "(SELECT "
                                + RawContactsColumns.CONCRETE_ID + " FROM " + Tables.SEARCH_INDEX
                                + " JOIN " + Tables.RAW_CONTACTS + " ON (" + Tables.SEARCH_INDEX
                                + "." + SearchIndexColumns.CONTACT_ID + "="
                                + RawContactsColumns.CONCRETE_CONTACT_ID + ")" + " WHERE "
                                + SearchIndexColumns.NAME + " MATCH '");
                        sb.append(ftsMatchQuery);
                        sb.append("' AND " + RawContactsColumns.CONCRETE_DELETED + "=0 ");
                        sb.append(")");
                        orNeeded = true;
                        hasCondition = true;
                    }

                    normalizeNumber = PhoneNumberUtils.normalizeNumber(filterParam);
                    if (!TextUtils.isEmpty(normalizeNumber)) {
                        if (orNeeded) {
                            sb.append(" OR ");
                        }
                        if (match == SMART_DIALMATCH) {
                            sb.append(Data._ID + " IN (SELECT DISTINCT "
                                    + PhoneLookupColumns.DATA_ID + " FROM " + Tables.PHONE_LOOKUP
                                    + " JOIN " + Tables.RAW_CONTACTS + " ON ("
                                    + Tables.PHONE_LOOKUP + "." + Data.RAW_CONTACT_ID + "="
                                    + RawContactsColumns.CONCRETE_CONTACT_ID + ")" + " WHERE ( "
                                    + PhoneLookupColumns.NORMALIZED_NUMBER + " LIKE '");
                            sb.append("%");
                            sb.append(normalizeNumber);
                            sb.append("%'");
                            sb.append(" AND " + RawContactsColumns.CONCRETE_DELETED + "=0 ");
                            sb.append(")");
                        } else {
                            sb.append(Data._ID + " IN (SELECT DISTINCT "
                                    + PhoneLookupColumns.DATA_ID + " FROM " + Tables.PHONE_LOOKUP
                                    + " WHERE (" + PhoneLookupColumns.NORMALIZED_NUMBER + " LIKE '");
                            sb.append(normalizeNumber);
                            sb.append("%')");
                        }

                        int lenth = normalizeNumber.length();
                        if (lenth >= DIAL_MATCH_MINMATCH) {
                            String minMatch = normalizeNumber.substring(
                                    lenth - DIAL_MATCH_MINMATCH, lenth);
                            sb.append(" OR ");
                            sb.append(PhoneLookupColumns.NORMALIZED_NUMBER + " LIKE '%");
                            sb.append(minMatch);
                            sb.append("'");
                        }
                        sb.append(")");
                        hasCondition = true;
                    }

                    if (!TextUtils.isEmpty(ftsMatchQuery) && match == CALLABLE_DIALMATCH) {
                        if (hasCondition) {
                            sb.append(" OR ");
                        }
                        sb.append("(");
                        sb.append(mimeTypeIsSipExpression);
                        sb.append(" AND ((" + Data.DATA1 + " LIKE ");
                        DatabaseUtils.appendEscapedSQLString(sb, filterParam + '%');
                        sb.append(") OR (" + Data.DATA1 + " LIKE ");
                        // Users may want SIP URIs starting from "sip:"
                        DatabaseUtils.appendEscapedSQLString(sb, "sip:" + filterParam + '%');
                        sb.append(")))");
                        hasCondition = true;
                    }

                    if (!hasCondition) {
                        sb.append("0");
                    }

                    sb.append(")");
                    contactsQb.appendWhere(sb);
                }

                if (match == CALLABLE_DIALMATCH) {
                    // If the row is for a phone number that has a
                    // normalized form, we should use
                    // the normalized one as PHONES_FILTER does, while we
                    // shouldn't do that
                    // if the row is for a sip address.
                    String isPhoneAndHasNormalized = "(" + mimeTypeIsPhoneExpression + " AND "
                            + Phone.NORMALIZED_NUMBER + " IS NOT NULL)";
                    groupBy = "(CASE WHEN " + isPhoneAndHasNormalized + " THEN "
                            + Phone.NORMALIZED_NUMBER + " ELSE " + Phone.NUMBER + " END), "
                            + RawContacts.CONTACT_ID;
                } else {
                    groupBy = "(CASE WHEN " + Phone.NORMALIZED_NUMBER + " IS NOT NULL THEN "
                            + Phone.NORMALIZED_NUMBER + " ELSE " + Phone.NUMBER + " END), "
                            + RawContacts.CONTACT_ID;
                }

                if (!TextUtils.isEmpty(normalizeNumber)) {
                    calllogQb.setTables(Tables.CALLS);
                    calllogQb.setProjectionMap(sSmartDialMatchCallLogMap);
                    calllogQb.setStrict(true);
                    calllogQb.appendWhere(Calls.CACHED_NAME + " IS NULL ");
                    sb.setLength(0);// clear string builder
                    sb.append("AND (");
                    sb.append(Calls.NUMBER + " LIKE '");
                    sb.append("%");
                    sb.append(normalizeNumber);
                    sb.append("%'");

                    int lenth = normalizeNumber.length();
                    if (lenth >= DIAL_MATCH_MINMATCH) {
                        String minMatch = normalizeNumber.substring(lenth - DIAL_MATCH_MINMATCH,
                                lenth);
                        sb.append(" OR ");
                        sb.append(Calls.NUMBER + " LIKE '%");
                        sb.append(minMatch);
                        sb.append("'");
                    }
                    /* SPRD: add for bug504623 @{ */
                    sb.append(") ");
                    UserManager userManager = (UserManager) getContext()
                            .getSystemService(Context.USER_SERVICE);
                    if (!userManager.isSystemUser()) {
                        sb.append("AND (");
                        sb.append(String.format("%s = ", Calls.USERS_ID));
                        sb.append(Integer.toString(Calls.getUserId()));
                        sb.append(")");
                    }
                    /* @} */
                    hasCondition = true;
                    calllogQb.appendWhere(sb);
                    isCalllogNeed = true;
                }

                if (isCalllogNeed) {
                    contactSqlString = contactsQb.buildQuery(projection, selection, groupBy, null,
                            null, null);
                    groupBy = "number";
                    calllogSqlString = calllogQb.buildQuery(projection, selection, groupBy, null,
                            null, null);
                    String uinQueryArray[] = new String[] {
                            contactSqlString, calllogSqlString,

                    };

                    String orderBy = "item_type, date DESC";
                    uinonSql = qb.buildUnionQuery(uinQueryArray, orderBy, limit);
                } else {
                    contactSqlString = contactsQb.buildQuery(projection, selection, groupBy, null,
                            sortOrder, limit);
                    uinonSql = contactSqlString;
                }

                // SPRD: add log for bug493481
                if (DEBUG) {
                    Log.d("CallLogProvider", "query match SMART_DIALMATCH uinonSql is " + uinonSql);
                }
                Cursor cursor = null;
                Log.d("PerformanceTest", new SimpleDateFormat("yyyyMMdd-HH:mm:ss:SSS") .format(new Date()) + " CallLogProvider query before get cursor");
                cursor = mDbHelper.getWritableDatabase().rawQuery(uinonSql, null);
                Log.d("PerformanceTest", new SimpleDateFormat("yyyyMMdd-HH:mm:ss:SSS") .format(new Date()) + " CallLogProvider query before return cursor");
                return cursor;
            }
            /* @} */

            default:
//                throw new IllegalArgumentException("Unknown URL " + uri);
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
            /* @} */
        }

        final int limit = getIntParam(uri, Calls.LIMIT_PARAM_KEY, 0);
        final int offset = getIntParam(uri, Calls.OFFSET_PARAM_KEY, 0);
        String limitClause = null;
        if (limit > 0) {
            limitClause = offset + "," + limit;
        }

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        final Cursor c = qb.query(db, projection, selectionBuilder.build(), selectionArgs,/* SPRD: @orig:wq null,*/
                groupBy, null, sortOrder, limitClause);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), CallLog.CONTENT_URI);
        }
        return c;
    }

    /**
     * Gets an integer query parameter from a given uri.
     *
     * @param uri The uri to extract the query parameter from.
     * @param key The query parameter key.
     * @param defaultValue A default value to return if the query parameter does not exist.
     * @return The value from the query parameter in the Uri.  Or the default value if the parameter
     * does not exist in the uri.
     * @throws IllegalArgumentException when the value in the query parameter is not an integer.
     */
    private int getIntParam(Uri uri, String key, int defaultValue) {
        String valueString = uri.getQueryParameter(key);
        if (valueString == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            String msg = "Integer required for " + key + " parameter but value '" + valueString +
                    "' was found instead.";
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case CALLS:
                return Calls.CONTENT_TYPE;
            case CALLS_ID:
                return Calls.CONTENT_ITEM_TYPE;
            case CALLS_FILTER:
                return Calls.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        waitForAccess(mReadAccessLatch);
        checkForSupportedColumns(sCallsProjectionMap, values);
        // Inserting a voicemail record through call_log requires the voicemail
        // permission and also requires the additional voicemail param set.
        if (hasVoicemailValue(values)) {
            checkIsAllowVoicemailRequest(uri);
            mVoicemailPermissions.checkCallerHasWriteAccess(getCallingPackage());
        }
        if (mCallsInserter == null) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            mCallsInserter = new DatabaseUtils.InsertHelper(db, Tables.CALLS);
        }

        /* SPRD: add for call log match @{ */
        values.remove(SearchManager.SUGGEST_COLUMN_TEXT_1);
        values.remove(SearchManager.SUGGEST_COLUMN_TEXT_2);
        values.remove(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);

        String displayName = (String) values.get(Calls.CACHED_NAME);
        if (!TextUtils.isEmpty(displayName)) {
            String fullName = PinYin.getInstance(getContext()).getPinyinString(displayName);
            String spellNames[] = PinYin.getInstance(getContext()).getPinYinStringArray(displayName);
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < spellNames.length; i++) {
                sb.append(spellNames[i].charAt(0));
            }
            String simpleName = sb.toString();
            values.put(ContactsDatabaseHelper.CACHED_NORMALIZED_FULL_NAME, fullName);
            values.put(ContactsDatabaseHelper.CACHED_NORMALIZED_SIMPLE_NAME, simpleName);
        }
        /* @} */
         /* SPRD: Matching callLog when search in dialpad feature @{ */
        String lookupUriString = (String) values.get(Calls.CACHED_LOOKUP_URI);
        String number = (String) values.get(Calls.NUMBER);
        if (TextUtils.isEmpty(lookupUriString)) {
            Uri lookupUri = createTemporaryContactUri(number);
            values.put(Calls.CACHED_LOOKUP_URI, lookupUri.toString());
        }
        /* @} */
        ContentValues copiedValues = new ContentValues(values);

        // Add the computed fields to the copied values.
        mCallLogInsertionHelper.addComputedValues(copiedValues);

        long rowId = getDatabaseModifier(mCallsInserter).insert(copiedValues);
        if (rowId > 0) {
            return ContentUris.withAppendedId(uri, rowId);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        waitForAccess(mReadAccessLatch);
        checkForSupportedColumns(sCallsProjectionMap, values);
        // Request that involves changing record type to voicemail requires the
        // voicemail param set in the uri.
        if (hasVoicemailValue(values)) {
            checkIsAllowVoicemailRequest(uri);
        }

        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, false /*isQuery*/);

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
            case CALLS:
                break;

            case CALLS_ID:
                selectionBuilder.addClause(getEqualityClause(Calls._ID, parseCallIdFromUri(uri)));
                break;

            default:
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
        }

        return getDatabaseModifier(db).update(Tables.CALLS, values, selectionBuilder.build(),
                selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        waitForAccess(mReadAccessLatch);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, false /*isQuery*/);

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
            case CALLS:
                return getDatabaseModifier(db).delete(Tables.CALLS,
                        selectionBuilder.build(), selectionArgs);
            default:
                throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
        }
    }

    // Work around to let the test code override the context. getContext() is final so cannot be
    // overridden.
    protected Context context() {
        return getContext();
    }

    void adjustForNewPhoneAccount(PhoneAccountHandle handle) {
        scheduleBackgroundTask(BACKGROUND_TASK_ADJUST_PHONE_ACCOUNT, handle);
    }

    /**
     * Returns a {@link DatabaseModifier} that takes care of sending necessary notifications
     * after the operation is performed.
     */
    private DatabaseModifier getDatabaseModifier(SQLiteDatabase db) {
        return new DbModifierWithNotification(Tables.CALLS, db, context());
    }

    /**
     * Same as {@link #getDatabaseModifier(SQLiteDatabase)} but used for insert helper operations
     * only.
     */
    private DatabaseModifier getDatabaseModifier(DatabaseUtils.InsertHelper insertHelper) {
        return new DbModifierWithNotification(Tables.CALLS, insertHelper, context());
    }

    private static final Integer VOICEMAIL_TYPE = new Integer(Calls.VOICEMAIL_TYPE);
    private boolean hasVoicemailValue(ContentValues values) {
        return VOICEMAIL_TYPE.equals(values.getAsInteger(Calls.TYPE));
    }

    /**
     * Checks if the supplied uri requests to include voicemails and take appropriate
     * action.
     * <p> If voicemail is requested, then check for voicemail permissions. Otherwise
     * modify the selection to restrict to non-voicemail entries only.
     */
    private void checkVoicemailPermissionAndAddRestriction(Uri uri,
            SelectionBuilder selectionBuilder, boolean isQuery) {
        if (isAllowVoicemailRequest(uri)) {
            if (isQuery) {
                mVoicemailPermissions.checkCallerHasReadAccess(getCallingPackage());
            } else {
                mVoicemailPermissions.checkCallerHasWriteAccess(getCallingPackage());
            }
        } else {
            selectionBuilder.addClause(EXCLUDE_VOICEMAIL_SELECTION);
        }
    }

    /**
     * Determines if the supplied uri has the request to allow voicemails to be
     * included.
     */
    private boolean isAllowVoicemailRequest(Uri uri) {
        return uri.getBooleanQueryParameter(Calls.ALLOW_VOICEMAILS_PARAM_KEY, false);
    }

    /**
     * Checks to ensure that the given uri has allow_voicemail set. Used by
     * insert and update operations to check that ContentValues with voicemail
     * call type must use the voicemail uri.
     * @throws IllegalArgumentException if allow_voicemail is not set.
     */
    private void checkIsAllowVoicemailRequest(Uri uri) {
        if (!isAllowVoicemailRequest(uri)) {
            throw new IllegalArgumentException(
                    String.format("Uri %s cannot be used for voicemail record." +
                            " Please set '%s=true' in the uri.", uri,
                            Calls.ALLOW_VOICEMAILS_PARAM_KEY));
        }
    }

   /**
    * Parses the call Id from the given uri, assuming that this is a uri that
    * matches CALLS_ID. For other uri types the behaviour is undefined.
    * @throws IllegalArgumentException if the id included in the Uri is not a valid long value.
    */
    private long parseCallIdFromUri(Uri uri) {
        try {
            return Long.parseLong(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid call id in uri: " + uri, e);
        }
    }

    /**
     * Syncs any unique call log entries that have been inserted into the primary user's call log
     * since the last time the last sync occurred.
     */
    private void syncEntriesFromPrimaryUser(UserManager userManager) {
        final int userHandle = userManager.getUserHandle();
        if (userHandle == UserHandle.USER_OWNER
                || userManager.getUserInfo(userHandle).isManagedProfile()) {
            return;
        }

        final long lastSyncTime = getLastSyncTime();
        final Uri uri = ContentProvider.maybeAddUserId(CallLog.Calls.CONTENT_URI,
                UserHandle.USER_OWNER);
        final Cursor cursor = getContext().getContentResolver().query(
                uri,
                CALL_LOG_SYNC_PROJECTION,
                EXCLUDE_VOICEMAIL_SELECTION + " AND " + MORE_RECENT_THAN_SELECTION,
                new String[] {String.valueOf(lastSyncTime)},
                Calls.DATE + " DESC");
        if (cursor == null) {
            return;
        }
        try {
            final long lastSyncedEntryTime = copyEntriesFromCursor(cursor);
            if (lastSyncedEntryTime > lastSyncTime) {
                setLastTimeSynced(lastSyncedEntryTime);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Un-hides any hidden call log entries that are associated with the specified handle.
     *
     * @param handle The handle to the newly registered {@link android.telecom.PhoneAccount}.
     */
    private void adjustForNewPhoneAccountInternal(PhoneAccountHandle handle) {
        String[] handleArgs =
                new String[] { handle.getComponentName().flattenToString(), handle.getId() };

        // Check to see if any entries exist for this handle. If so (not empty), run the un-hiding
        // update. If not, then try to identify the call from the phone number.
        Cursor cursor = query(Calls.CONTENT_URI, MINIMAL_PROJECTION,
                Calls.PHONE_ACCOUNT_COMPONENT_NAME + " =? AND " + Calls.PHONE_ACCOUNT_ID + " =?",
                handleArgs, null);

        if (cursor != null) {
            try {
                if (cursor.getCount() >= 1) {
                    // run un-hiding process based on phone account
                    mDbHelper.getWritableDatabase().execSQL(
                            UNHIDE_BY_PHONE_ACCOUNT_QUERY, handleArgs);
                } else {
                    TelecomManager tm = TelecomManager.from(getContext());
                    if (tm != null) {

                        PhoneAccount account = tm.getPhoneAccount(handle);
                        if (account != null && account.getAddress() != null) {
                            // We did not find any items for the specific phone account, so run the
                            // query based on the phone number instead.
                            mDbHelper.getWritableDatabase().execSQL(UNHIDE_BY_ADDRESS_QUERY,
                                    new String[] { account.getAddress().toString() });
                        }

                    }
                }
            } finally {
                cursor.close();
            }
        }

    }

    /**
     * @param cursor to copy call log entries from
     *
     * @return the timestamp of the last synced entry.
     */
    @VisibleForTesting
    long copyEntriesFromCursor(Cursor cursor) {
        long lastSynced = 0;
        final ContentValues values = new ContentValues();
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final String[] args = new String[2];
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                values.clear();
                DatabaseUtils.cursorRowToContentValues(cursor, values);
                final String startTime = values.getAsString(Calls.DATE);
                final String number = values.getAsString(Calls.NUMBER);

                if (startTime == null || number == null) {
                    continue;
                }

                if (cursor.isLast()) {
                    try {
                        lastSynced = Long.valueOf(startTime);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Call log entry does not contain valid start time: "
                                + startTime);
                    }
                }

                // Avoid duplicating an already existing entry (which is uniquely identified by
                // the number, and the start time)
                args[0] = startTime;
                args[1] = number;
                if (DatabaseUtils.queryNumEntries(db, Tables.CALLS,
                        Calls.DATE + " = ? AND " + Calls.NUMBER + " = ?", args) > 0) {
                    continue;
                }

                db.insert(Tables.CALLS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return lastSynced;
    }

    private long getLastSyncTime() {
        try {
            return Long.valueOf(mDbHelper.getProperty(DbProperties.CALL_LOG_LAST_SYNCED, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setLastTimeSynced(long time) {
        mDbHelper.setProperty(DbProperties.CALL_LOG_LAST_SYNCED, String.valueOf(time));
    }

    private static void waitForAccess(CountDownLatch latch) {
        if (latch == null) {
            return;
        }

        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scheduleBackgroundTask(int task, Object arg) {
        mBackgroundHandler.obtainMessage(task, arg).sendToTarget();
    }

    private void performBackgroundTask(int task, Object arg) {
        if (task == BACKGROUND_TASK_INITIALIZE) {
            try {
                final Context context = getContext();
                if (context != null) {
                    final UserManager userManager = UserUtils.getUserManager(context);
                    if (userManager != null &&
                            !userManager.hasUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS)) {
                        syncEntriesFromPrimaryUser(userManager);
                    }
                }
            } finally {
                mReadAccessLatch.countDown();
                mReadAccessLatch = null;
            }
        } else if (task == BACKGROUND_TASK_ADJUST_PHONE_ACCOUNT) {
            adjustForNewPhoneAccountInternal((PhoneAccountHandle) arg);
        }

    }

    //--------------------------------- SPRD --------------------------------------
    /* SPRD: Matching callLog when search in dialpad feature @{ */
    private static void initDialMatchProjectMap() {
        sSmartDialMatchContactMap = ProjectionMap.builder()
                .add(Phone._ID, "MIN(" + Phone._ID + ")")
                .add(Phone.TYPE)
                .add(Phone.LABEL)
                .add(Phone.NUMBER)
                .add(Phone.CONTACT_ID)
                .add(Phone.LOOKUP_KEY)
                .add(Phone.PHOTO_ID)
                .add(Phone.DISPLAY_NAME_PRIMARY)
                .add(Phone.PHOTO_THUMBNAIL_URI)
                .add(Contacts.DISPLAY_ACCOUNT_TYPE)
                .add(Contacts.DISPLAY_ACCOUNT_NAME)
                .add(Phone.DISPLAY_NAME_ALTERNATIVE)
                .add(Contacts.DISPLAY_NAME_SOURCE)
                .add("item_type", "1")
                .add(Calls.DATE, "0")
                .add(Calls.TYPE, "0")
                .add(Calls.CACHED_LOOKUP_URI, "0")
                .add(RawContacts.SYNC1)
                .build();
        /*
         * in smart dial just select items that don't contain name so use number
         * as display name
         */

        sSmartDialMatchCallLogMap = ProjectionMap.builder()
                .add(Phone._ID, "MIN(" + Phone._ID + ")")
                .add(Phone.TYPE, "NULL")
                .add(Phone.LABEL, "NULL")
                .add(Phone.NUMBER, Calls.NUMBER)
                .add(Phone.CONTACT_ID, "-1")
                .add(Phone.LOOKUP_KEY,Calls.CACHED_LOOKUP_URI)
                .add(Phone.PHOTO_ID, "NULL")
                .add(Phone.DISPLAY_NAME_PRIMARY, Calls.NUMBER)
                .add(Phone.PHOTO_THUMBNAIL_URI, "NULL")
                .add(Contacts.DISPLAY_ACCOUNT_TYPE, "NULL")
                .add(Contacts.DISPLAY_ACCOUNT_NAME, "NULL")
                .add(Contacts.DISPLAY_NAME_SOURCE, "NULL")
                .add(Phone.DISPLAY_NAME_ALTERNATIVE, Calls.NUMBER)
                .add("item_type", "0")
                .add(Calls.DATE)
                .add(Calls.TYPE)
                .add(Calls.CACHED_LOOKUP_URI)
                .add(RawContacts.SYNC1,"NULL")
                .build();
    }

    private static Uri createTemporaryContactUri(String number) {
        try {
            final JSONObject contactRows = new JSONObject().put(Phone.CONTENT_ITEM_TYPE,
                    new JSONObject().put(Phone.NUMBER, number).put(Phone.TYPE, Phone.TYPE_CUSTOM));
            final String jsonString = new JSONObject().put(Contacts.DISPLAY_NAME, number)
                    .put(Contacts.DISPLAY_NAME_SOURCE, DisplayNameSources.PHONE)
                    .put(Contacts.CONTENT_ITEM_TYPE, contactRows).toString();
            return Contacts.CONTENT_LOOKUP_URI
                    .buildUpon()
                    .appendPath("encoded")
                    .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                            String.valueOf(Long.MAX_VALUE)).encodedFragment(jsonString).build();
        } catch (JSONException e) {
            return null;
        }
    }
    /* @} */
}
