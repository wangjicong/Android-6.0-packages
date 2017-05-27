/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.dialer.dialpad;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader.ForceLoadContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.android.contacts.common.list.PhoneNumberListAdapter.PhoneQuery;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.database.DialerDatabaseHelper.ContactNumber;
import com.android.dialer.database.DialerDatabaseHelper.DialMatchInfo;
import com.android.dialerbind.DatabaseHelperManager;

import java.util.ArrayList;

/**
 * Implements a Loader<Cursor> class to asynchronously load SmartDial search results.
 */
public class SmartDialCursorLoader extends AsyncTaskLoader<Cursor> {

    private final String TAG = SmartDialCursorLoader.class.getSimpleName();
    private final boolean DEBUG = false;

    private final Context mContext;

    private Cursor mCursor;

    private String mQuery;
    private SmartDialNameMatcher mNameMatcher;

    private ForceLoadContentObserver mObserver;
    /* SPRD: Matching callLog when search in dialpad feature @{ */
    private boolean mIsDialMatchMode;
    /* @} */

    public SmartDialCursorLoader(Context context) {
        super(context);
        mContext = context;
        /* SPRD: Matching callLog when search in dialpad feature @{ */
        mIsDialMatchMode = false;
        /* @} */
    }

    /* SPRD: Matching callLog when search in dialpad feature @{ */
    public SmartDialCursorLoader(Context context, boolean isDialMatchMode) {
        super(context);
        mContext = context;
        mIsDialMatchMode = isDialMatchMode;
    }
    /* @} */

    /**
     * Configures the query string to be used to find SmartDial matches.
     * @param query The query string user typed.
     */
    public void configureQuery(String query) {
        if (DEBUG) {
            Log.v(TAG, "Configure new query to be " + query);
        }
        mQuery = SmartDialNameMatcher.normalizeNumber(query, SmartDialPrefix.getMap());

        /** Constructs a name matcher object for matching names. */
        mNameMatcher = new SmartDialNameMatcher(mQuery, SmartDialPrefix.getMap());
    }

    /**
     * Queries the SmartDial database and loads results in background.
     * @return Cursor of contacts that matches the SmartDial query.
     */
    @Override
    public Cursor loadInBackground() {
        if (DEBUG) {
            Log.v(TAG, "Load in background " + mQuery);
        }

        if (!PermissionsUtil.hasContactsPermissions(mContext)) {
            return new MatrixCursor(PhoneQuery.PROJECTION_PRIMARY);
        }

        /** Loads results from the database helper. */
        final DialerDatabaseHelper dialerDatabaseHelper = DatabaseHelperManager.getDatabaseHelper(
                mContext);
        /* SPRD: Matching callLog when search in dialpad feature @{
        * @orig
        * final ArrayList<ContactNumber> allMatches = dialerDatabaseHelper.getLooseMatches(mQuery,
        *        mNameMatcher); */
        // SPRD: modify for bug517323
        MatrixCursor cursor = null;
        Log.d(TAG, "loadInBackground mIsDialMatchMode = " + mIsDialMatchMode);
        if (mIsDialMatchMode) {
            // SPRD: modify for bug517323
            cursor = new MatrixCursor(PhoneQuery.PROJECTION_DIALMATCH);
            final ArrayList<DialMatchInfo> allMatches = dialerDatabaseHelper
                    .getLooseMatchesForDialMatchInfo(mQuery, mNameMatcher);
        /* @} */

        if (DEBUG) {
            Log.v(TAG, "Loaded matches " + String.valueOf(allMatches.size()));
        }

        /** Constructs a cursor for the returned array of results. */
        /* SPRD: Matching callLog when search in dialpad feature @{
        * @orig
        final MatrixCursor cursor = new MatrixCursor(PhoneQuery.PROJECTION_PRIMARY);
        Object[] row = new Object[PhoneQuery.PROJECTION_PRIMARY.length];
        for (ContactNumber contact : allMatches) {
            row[PhoneQuery.PHONE_ID] = contact.dataId;
            row[PhoneQuery.PHONE_NUMBER] = contact.phoneNumber;
            row[PhoneQuery.CONTACT_ID] = contact.id;
            row[PhoneQuery.LOOKUP_KEY] = contact.lookupKey;
            row[PhoneQuery.PHOTO_ID] = contact.photoId;
            row[PhoneQuery.DISPLAY_NAME] = contact.displayName;
            /* SPRD: DIALER SEARCH FEATURE @{ *//*
            row[PhoneQuery.CONTACT_DISPLAY_ACCOUNT_TYPE] = contact.accountType;
            row[PhoneQuery.CONTACT_DISPLAY_ACCOUNT_NAME] = contact.accountName;
            /* @} *//*
            cursor.addRow(row);
        } */
            Object[] row = new Object[PhoneQuery.PROJECTION_DIALMATCH.length];
            for (DialMatchInfo info : allMatches) {
                row[PhoneQuery.PHONE_ID] = info.dataId;
                row[PhoneQuery.PHONE_NUMBER] = info.phoneNumber;
                row[PhoneQuery.CONTACT_ID] = info.id;
                row[PhoneQuery.LOOKUP_KEY] = info.lookupKey;
                row[PhoneQuery.PHOTO_ID] = info.photoId;
                row[PhoneQuery.DISPLAY_NAME] = info.displayName;
                /** SPRD: DIALER SEARCH FEATURE BEGIN @{ */
                row[PhoneQuery.CONTACT_DISPLAY_ACCOUNT_TYPE] = info.accountType;
                row[PhoneQuery.CONTACT_DISPLAY_ACCOUNT_NAME] = info.accountName;
                /** END @} */
                row[PhoneQuery.ITEM_TYPE] = info.itemType;
                row[PhoneQuery.CALLS_DATE] = info.callsDate;
                row[PhoneQuery.CALLS_TYPE] = info.callsType;
                row[PhoneQuery.CACHED_LOOKUP_URI] = info.callsCachedLookupUri;
                row[PhoneQuery.SYNC1] = info.SYNC1;
                cursor.addRow(row);
            }
        } else {
            // SPRD: modify for bug517323
            cursor = new MatrixCursor(PhoneQuery.PROJECTION_PRIMARY);
            final ArrayList<ContactNumber> allMatches = dialerDatabaseHelper.getLooseMatches(
                    mQuery, mNameMatcher);

            if (DEBUG) {
                Log.v(TAG, "Loaded matches " + String.valueOf(allMatches.size()));
            }

            /** Constructs a cursor for the returned array of results. */
            Object[] row = new Object[PhoneQuery.PROJECTION_PRIMARY.length];
            for (ContactNumber contact : allMatches) {
                row[PhoneQuery.PHONE_ID] = contact.dataId;
                row[PhoneQuery.PHONE_NUMBER] = contact.phoneNumber;
                row[PhoneQuery.CONTACT_ID] = contact.id;
                row[PhoneQuery.LOOKUP_KEY] = contact.lookupKey;
                row[PhoneQuery.PHOTO_ID] = contact.photoId;
                row[PhoneQuery.DISPLAY_NAME] = contact.displayName;
                row[PhoneQuery.CONTACT_DISPLAY_ACCOUNT_TYPE] = contact.accountType;
                row[PhoneQuery.CONTACT_DISPLAY_ACCOUNT_NAME] = contact.accountName;
                cursor.addRow(row);
            }
        }
        /* @} */

        return cursor;
    }

    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            /** The Loader has been reset; ignore the result and invalidate the data. */
            releaseResources(cursor);
            return;
        }

        /** Hold a reference to the old data so it doesn't get garbage collected. */
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (mObserver == null) {
            mObserver = new ForceLoadContentObserver();
            mContext.getContentResolver().registerContentObserver(
                    DialerDatabaseHelper.SMART_DIAL_UPDATED_URI, true, mObserver);
        }

        if (isStarted()) {
            /** If the Loader is in a started state, deliver the results to the client. */
            super.deliverResult(cursor);
        }

        /** Invalidate the old data as we don't need it any more. */
        if (oldCursor != null && oldCursor != cursor) {
            releaseResources(oldCursor);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            /** Deliver any previously loaded data immediately. */
            deliverResult(mCursor);
        }
        if (mCursor == null) {
            /** Force loads every time as our results change with queries. */
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        /** The Loader is in a stopped state, so we should attempt to cancel the current load. */
        cancelLoad();
    }

    @Override
    protected void onReset() {
        /** Ensure the loader has been stopped. */
        onStopLoading();

        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }

        /** Release all previously saved query results. */
        if (mCursor != null) {
            releaseResources(mCursor);
            mCursor = null;
        }
    }

    @Override
    public void onCanceled(Cursor cursor) {
        super.onCanceled(cursor);

        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }

        /** The load has been canceled, so we should release the resources associated with 'data'.*/
        releaseResources(cursor);
    }

    private void releaseResources(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
