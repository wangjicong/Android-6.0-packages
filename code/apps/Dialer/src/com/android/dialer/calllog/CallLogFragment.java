/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.calllog;

import static android.Manifest.permission.READ_CALL_LOG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.VoicemailContract.Status;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.contacts.common.util.ViewUtil;
import com.android.dialer.DialerApplication;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;
import com.android.dialer.list.ListsFragment.HostInterface;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.EmptyLoader;
import com.android.dialer.voicemail.VoicemailPlaybackPresenter;
import com.android.dialer.voicemail.VoicemailStatusHelper;
import com.android.dialer.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.dialer.voicemail.VoicemailStatusHelperImpl;
import com.android.dialer.widget.EmptyContentView;
import com.android.dialer.widget.EmptyContentView.OnEmptyViewActionButtonClickedListener;
import com.android.dialerbind.ObjectFactory;
import com.android.internal.telephony.TeleUtils;
import com.sprd.dialer.CallLogFilterFragment;
import com.sprd.dialer.calllog.CallLogClearActivity;
import com.sprd.dialer.utils.SourceUtils;

import java.util.List;

/**
 * Displays a list of call log entries. To filter for a particular kind of call
 * (all, missed or voicemails), specify it in the constructor.
 */
public class CallLogFragment extends Fragment implements CallLogQueryHandler.Listener,
        CallLogAdapter.CallFetcher, OnEmptyViewActionButtonClickedListener {
    private static final String TAG = "CallLogFragment";

    /**
     * ID of the empty loader to defer other fragments.
     */
    private static final int EMPTY_LOADER_ID = 0;

    private static final String KEY_FILTER_TYPE = "filter_type";
    private static final String KEY_LOG_LIMIT = "log_limit";
    private static final String KEY_DATE_LIMIT = "date_limit";

    // No limit specified for the number of logs to show; use the CallLogQueryHandler's default.
    private static final int NO_LOG_LIMIT = -1;
    // No date-based filtering.
    private static final int NO_DATE_LIMIT = 0;

    private static final int READ_CALL_LOG_PERMISSION_REQUEST_CODE = 1;

    private TextView mSimNameText;
    private TelephonyManager mTelephonyManager;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private CallLogAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private VoicemailPlaybackPresenter mVoicemailPlaybackPresenter;
    private boolean mScrollToTop;

    /** Whether there is at least one voicemail source installed. */
    private boolean mVoicemailSourcesAvailable = false;

    private EmptyContentView mEmptyListView;
    private KeyguardManager mKeyguardManager;

    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;
    private boolean mVoicemailStatusFetched;
    /* SPRD: FILTER CALL LOGS BY SIM FEATURE. @{ */
    private int mShowType = CallLogFilterFragment.TYPE_ALL;
    /* @} */
    // SPRD: add for bug538611
    private boolean mIsMultiSimActive = false;

    private final Handler mHandler = new Handler();

    private class CustomContentObserver extends ContentObserver {
        public CustomContentObserver() {
            super(mHandler);
        }
        @Override
        public void onChange(boolean selfChange) {
            mRefreshDataRequired = true;
        }
    }

    // See issue 6363009
    private final ContentObserver mCallLogObserver = new CustomContentObserver();
    private final ContentObserver mContactsObserver = new CustomContentObserver();
    private final ContentObserver mVoicemailStatusObserver = new CustomContentObserver();
    private boolean mRefreshDataRequired = true;

    private boolean mHasReadCallLogPermission = false;

    // Exactly same variable is in Fragment as a package private.
    private boolean mMenuVisible = true;

    // Default to all calls.
    private int mCallTypeFilter = CallLogQueryHandler.CALL_TYPE_ALL;

    // Log limit - if no limit is specified, then the default in {@link CallLogQueryHandler}
    // will be used.
    private int mLogLimit = NO_LOG_LIMIT;

    // Date limit (in millis since epoch) - when non-zero, only calls which occurred on or after
    // the date filter are included.  If zero, no date-based filtering occurs.
    private long mDateLimit = NO_DATE_LIMIT;

    /*
     * True if this instance of the CallLogFragment is the Recents screen shown in
     * DialtactsActivity.
     */
    private boolean mIsRecentsFragment;

    public interface HostInterface {
        public void showDialpad();
    }

    public CallLogFragment() {
        this(CallLogQueryHandler.CALL_TYPE_ALL, NO_LOG_LIMIT);
    }

    public CallLogFragment(int filterType) {
        this(filterType, NO_LOG_LIMIT);
    }

    public CallLogFragment(int filterType, int logLimit) {
        this(filterType, logLimit, NO_DATE_LIMIT);
    }

    /**
     * Creates a call log fragment, filtering to include only calls of the desired type, occurring
     * after the specified date.
     * @param filterType type of calls to include.
     * @param dateLimit limits results to calls occurring on or after the specified date.
     */
    public CallLogFragment(int filterType, long dateLimit) {
        this(filterType, NO_LOG_LIMIT, dateLimit);
    }

    /**
     * Creates a call log fragment, filtering to include only calls of the desired type, occurring
     * after the specified date.  Also provides a means to limit the number of results returned.
     * @param filterType type of calls to include.
     * @param logLimit limits the number of results to return.
     * @param dateLimit limits results to calls occurring on or after the specified date.
     */
    public CallLogFragment(int filterType, int logLimit, long dateLimit) {
        mCallTypeFilter = filterType;
        mLogLimit = logLimit;
        mDateLimit = dateLimit;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            mCallTypeFilter = state.getInt(KEY_FILTER_TYPE, mCallTypeFilter);
            mLogLimit = state.getInt(KEY_LOG_LIMIT, mLogLimit);
            mDateLimit = state.getLong(KEY_DATE_LIMIT, mDateLimit);
        }

        mIsRecentsFragment = mLogLimit != NO_LOG_LIMIT;

        final Activity activity = getActivity();
        mTelephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        final ContentResolver resolver = activity.getContentResolver();
        String currentCountryIso = GeoUtil.getCurrentCountryIso(activity);
        mCallLogQueryHandler = new CallLogQueryHandler(activity, resolver, this, mLogLimit);
        mKeyguardManager =
                (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        resolver.registerContentObserver(CallLog.CONTENT_URI, true, mCallLogObserver);
        resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true,
                mContactsObserver);
        resolver.registerContentObserver(Status.CONTENT_URI, true, mVoicemailStatusObserver);
        /** SPRD: FILTER CALL LOGS BY SIM FEATURE.
         * @orig
         * setHasOptionsMenu(true);
         * @{ */
        if (getActivity() instanceof DialtactsActivity) {
            // DialtactsActivity has its own options menu, so don't need to create new one
            setHasOptionsMenu(false);
        } else {
            setHasOptionsMenu(true);
        }

        mShowType = CallLogFilterFragment.getCallLogShowType(getActivity());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(mShowCalllogListener);
        /* @} */
        if (mCallTypeFilter == Calls.VOICEMAIL_TYPE) {
            mVoicemailPlaybackPresenter = VoicemailPlaybackPresenter
                    .getInstance(activity, state);
        }
    }

    /** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public boolean onCallsFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            // Return false; we did not take ownership of the cursor
            return false;
        }
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);
        // This will update the state of the "Clear call log" menu item.
        getActivity().invalidateOptionsMenu();

        boolean showListView = cursor != null && cursor.getCount() > 0;
        mRecyclerView.setVisibility(showListView ? View.VISIBLE : View.GONE);
        mEmptyListView.setVisibility(!showListView ? View.VISIBLE : View.GONE);

        if (mScrollToTop) {
            // The smooth-scroll animation happens over a fixed time period.
            // As a result, if it scrolls through a large portion of the list,
            // each frame will jump so far from the previous one that the user
            // will not experience the illusion of downward motion.  Instead,
            // if we're not already near the top of the list, we instantly jump
            // near the top, and animate from there.
            if (mLayoutManager.findFirstVisibleItemPosition() > 5) {
                // TODO: Jump to near the top, then begin smooth scroll.
                mRecyclerView.smoothScrollToPosition(0);
            }
            // Workaround for framework issue: the smooth-scroll doesn't
            // occur if setSelection() is called immediately before.
            mHandler.post(new Runnable() {
               @Override
               public void run() {
                   if (getActivity() == null || getActivity().isFinishing()) {
                       return;
                   }
                   mRecyclerView.smoothScrollToPosition(0);
               }
            });

            mScrollToTop = false;
        }
        mCallLogFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
        return true;
    }

    /**
     * Called by {@link CallLogQueryHandler} after a successful query to voicemail status provider.
     */
    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        mVoicemailStatusFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
    }

    private void destroyEmptyLoaderIfAllDataFetched() {
        if (mCallLogFetched && mVoicemailStatusFetched && mEmptyLoaderRunning) {
            mEmptyLoaderRunning = false;
            getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.call_log_fragment, container, false);

        /* SPRD:add for bug450956 @{ */
        mSimNameText = (TextView) view.findViewById(R.id.sim_name);
        String simName = "";
        if (mShowType != CallLogFilterFragment.TYPE_ALL) {
            simName = mTelephonyManager.getSimOperatorNameForPhoneEx(mShowType);
        }
        if (TextUtils.isEmpty(simName)) {
            mSimNameText.setVisibility(View.GONE);
        } else {
            simName = TeleUtils.updateOperator(simName, "spn");
            mSimNameText.setText(simName);
            mSimNameText.setVisibility(View.VISIBLE);
        }
        /* @} */


        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mEmptyListView = (EmptyContentView) view.findViewById(R.id.empty_list_view);
        mEmptyListView.setImage(R.drawable.empty_call_log);
        mEmptyListView.setActionClickedListener(this);

        String currentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());
        boolean isShowingRecentsTab = mLogLimit != NO_LOG_LIMIT || mDateLimit != NO_DATE_LIMIT;
        mAdapter = ObjectFactory.newCallLogAdapter(
                getActivity(),
                this,
                new ContactInfoHelper(getActivity(), currentCountryIso),
                mVoicemailPlaybackPresenter,
                isShowingRecentsTab);
        mRecyclerView.setAdapter(mAdapter);

        fetchCalls();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateEmptyMessage(mCallTypeFilter);
        mAdapter.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        // Start the empty loader now to defer other fragments.  We destroy it when both calllog
        // and the voicemail status are fetched.
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
                new EmptyLoader.Callback(getActivity()));
        mEmptyLoaderRunning = true;
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        final boolean hasReadCallLogPermission =
                PermissionsUtil.hasPermission(getActivity(), READ_CALL_LOG);
        if (!mHasReadCallLogPermission && hasReadCallLogPermission) {
            // We didn't have the permission before, and now we do. Force a refresh of the call log.
            // Note that this code path always happens on a fresh start, but mRefreshDataRequired
            // is already true in that case anyway.
            mRefreshDataRequired = true;
            updateEmptyMessage(mCallTypeFilter);
        }
        mHasReadCallLogPermission = hasReadCallLogPermission;
        /* SPRD: modify for bug538611 @{ */
        if (hasReadCallLogPermission) {
            final SubscriptionManager subScriptionManager = SubscriptionManager.from(getActivity());
            List<SubscriptionInfo> subInfos = subScriptionManager.getActiveSubscriptionInfoList();
            if (subInfos != null && subInfos.size() > 1) {
                mIsMultiSimActive = true;
            } else {
                mIsMultiSimActive = false;
            }
            if (mShowType > CallLogFilterFragment.TYPE_ALL && !mIsMultiSimActive) {
                Log.i(TAG, "Reset callLog filterType to TYPE_ALL");
                CallLogFilterFragment.setCallLogShowType(getActivity(),
                        CallLogFilterFragment.TYPE_ALL);
            }
        }
        /* @} */
        refreshData();
    }

    @Override
    public void onPause() {
        if (mVoicemailPlaybackPresenter != null) {
            mVoicemailPlaybackPresenter.onPause();
        }
        mAdapter.pauseCache();
        super.onPause();
    }

    @Override
    public void onStop() {
        updateOnTransition(false /* onEntry */);

        super.onStop();
    }

    @Override
    public void onDestroy() {
        mAdapter.pauseCache();
        mAdapter.changeCursor(null);

        if (mVoicemailPlaybackPresenter != null) {
            mVoicemailPlaybackPresenter.onDestroy();
        }

        getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
        getActivity().getContentResolver().unregisterContentObserver(mContactsObserver);
        getActivity().getContentResolver().unregisterContentObserver(mVoicemailStatusObserver);
        /* SPRD: FILTER CALL LOGS BY SIM FEATURE. @{ */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mShowCalllogListener);
        /* @} */
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_FILTER_TYPE, mCallTypeFilter);
        outState.putInt(KEY_LOG_LIMIT, mLogLimit);
        outState.putLong(KEY_DATE_LIMIT, mDateLimit);

        mAdapter.onSaveInstanceState(outState);

        if (mVoicemailPlaybackPresenter != null) {
            mVoicemailPlaybackPresenter.onSaveInstanceState(outState);
        }
    }

    @Override
    public void fetchCalls() {
        /* SPRD: FILTER CALL LOGS BY SIM FEATURE. @{ */
        mCallLogQueryHandler.setShowType(mShowType);
        mCallLogQueryHandler.fetchCalls(mCallTypeFilter, mDateLimit);
        /* @} */
    }

    private void updateEmptyMessage(int filterType) {
        final Context context = getActivity();
        if (context == null) {
            return;
        }

        if (!PermissionsUtil.hasPermission(context, READ_CALL_LOG)) {
            mEmptyListView.setDescription(R.string.permission_no_calllog);
            mEmptyListView.setActionLabel(R.string.permission_single_turn_on);
            /* SPRD: add for bug540651 @{ */
            if (context != null) {
                DialerApplication dialerApplication = (DialerApplication)
                        context.getApplicationContext();
                dialerApplication.stopMonitor();
            }
            /* @} */
            return;
        }

        final int messageId;
        switch (filterType) {
            case Calls.MISSED_TYPE:
                messageId = R.string.recentMissed_empty;
                break;
            case Calls.VOICEMAIL_TYPE:
                messageId = R.string.recentVoicemails_empty;
                break;
            case CallLogQueryHandler.CALL_TYPE_ALL:
                messageId = R.string.recentCalls_empty;
                break;
            /* SPRD: FILTER CALL LOG TYPE FEATURE. @{ */
            case Calls.OUTGOING_TYPE:
                messageId = R.string.recentOutgoing_empty;
                break;
            case Calls.INCOMING_TYPE:
                messageId = R.string.recentIncoming_empty;
                break;
            /* @} */
            default:
                throw new IllegalArgumentException("Unexpected filter type in CallLogFragment: "
                        + filterType);
        }
        mEmptyListView.setDescription(messageId);
        if (mIsRecentsFragment) {
            mEmptyListView.setActionLabel(R.string.recentCalls_empty_action);
        } else {
            mEmptyListView.setActionLabel(EmptyContentView.NO_LABEL);
        }
    }

    CallLogAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (!menuVisible) {
                updateOnTransition(false /* onEntry */);
            } else if (isResumed()) {
                refreshData();
            }
        }
    }

    /** Requests updates to the data to be shown. */
    private void refreshData() {
        // Prevent unnecessary refresh.
        if (mRefreshDataRequired) {
            // Mark all entries in the contact info cache as out of date, so they will be looked up
            // again once being shown.
            mAdapter.invalidateCache();
            mAdapter.setLoading(true);

            fetchCalls();
            mCallLogQueryHandler.fetchVoicemailStatus();

            updateOnTransition(true /* onEntry */);
            mRefreshDataRequired = false;
        } else {
            // Refresh the display of the existing data to update the timestamp text descriptions.
            mAdapter.notifyDataSetChanged();
        }
        mAdapter.startCache();
    }

    /**
     * Updates the call data and notification state on entering or leaving the call log tab.
     *
     * If we are leaving the call log tab, mark all the missed calls as read.
     *
     * TODO: Move to CallLogActivity
     */
    private void updateOnTransition(boolean onEntry) {
        // We don't want to update any call data when keyguard is on because the user has likely not
        // seen the new calls yet.
        // This might be called before onCreate() and thus we need to check null explicitly.
        if (mKeyguardManager != null && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
            // On either of the transitions we update the missed call and voicemail notifications.
            // While exiting we additionally consume all missed calls (by marking them as read).
            mCallLogQueryHandler.markNewCallsAsOld();
            if (!onEntry) {
                mCallLogQueryHandler.markMissedCallsAsRead();
            }
            CallLogNotificationsHelper.removeMissedCallNotifications(getActivity());
            CallLogNotificationsHelper.updateVoicemailNotifications(getActivity());
        }
    }

    @Override
    public void onEmptyViewActionButtonClicked() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (!PermissionsUtil.hasPermission(activity, READ_CALL_LOG)) {
            requestPermissions(new String[] {READ_CALL_LOG}, READ_CALL_LOG_PERMISSION_REQUEST_CODE);
        } else if (mIsRecentsFragment) {
            // Show dialpad if we are the recents fragment.
            ((HostInterface) activity).showDialpad();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == READ_CALL_LOG_PERMISSION_REQUEST_CODE) {
            if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                // Force a refresh of the data since we were missing the permission before this.
                /* SPRD: add for bug540651 @{ */
                final Context context = getActivity();
                if (context != null) {
                    DialerApplication dialerApplication = (DialerApplication)
                            context.getApplicationContext();
                    dialerApplication.startMonitor();
                }
                /* @} */
                mRefreshDataRequired = true;
            }
        }
    }

    // --------------------------------- SPRD --------------------------------------
    /**
     * SPRD: FILTER CALL LOGS BY SIM FEATURE.
     * Override this method here, moved from CallLogActivity. @{
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.delete_all:
                /* SPRD: Porting CLEAR CALL LOG FEATURE. @{
                * @orig
                ClearCallLogDialog.show(getFragmentManager()); */
                Intent clearIntent = new Intent();
                clearIntent.putExtra(SourceUtils.CALL_LOG_TYPE_EXTRA, mCallTypeFilter);
                clearIntent.setClass(this.getActivity(), CallLogClearActivity.class);
                startActivity(clearIntent);
                /* @} */
                return true;
            case R.id.view_setting:
                CallLogFilterFragment.show(getFragmentManager());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.call_log_options, menu);
        /* SPRD: modify for bug538611 @{ */
        if (!mIsMultiSimActive) {
            menu.removeItem(R.id.view_setting);
        }
        /* @} */
    }

    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem itemDeleteAll = menu.findItem(R.id.delete_all);
        if (itemDeleteAll != null && mAdapter != null) {
            itemDeleteAll.setVisible(!mAdapter.isEmpty());
        }
        return;
    }

    SharedPreferences.OnSharedPreferenceChangeListener mShowCalllogListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (CallLogFilterFragment.SHOW_TYPE.equals(key)) {
                mShowType = sharedPreferences.getInt(key, CallLogFilterFragment.TYPE_ALL);
                Log.d(TAG, "Preference changed type = " + mShowType);
                fetchCalls();
                /* SPRD:add for bug450956 @{ */
                String simName = "";
                if (mShowType != CallLogFilterFragment.TYPE_ALL) {
                    simName = mTelephonyManager.getSimOperatorNameForPhoneEx(mShowType);
                }
                if (TextUtils.isEmpty(simName)) {
                    mSimNameText.setVisibility(View.GONE);
                } else {
                    simName = TeleUtils.updateOperator(simName, "spn");
                    mSimNameText.setText(simName);
                    mSimNameText.setVisibility(View.VISIBLE);
                }

                /* @} */

            }
        }
    };
    /* @} */
}
