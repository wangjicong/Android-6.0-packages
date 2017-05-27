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
 * limitations under the License
 */

package com.android.incallui;

import java.lang.ref.WeakReference;

import android.content.Context;

import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.ContactInfoCache;
import com.android.incallui.CallCardPresenter.ContactLookupCallback;
import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.incallui.ContactInfoCache.ContactInfoCacheCallback;
import com.android.incallui.InCallPresenter;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.Log;
import com.android.incallui.Presenter;
import com.android.incallui.Ui;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.sprd.incallui.InCallUIPlugin;
import com.android.ims.ImsManager;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.content.pm.PackageManager;
/**
 * Logic for call buttons.
 */
public class ConferenceListPresenter
        extends Presenter<ConferenceListPresenter.ConferenceManagerUi>
        implements InCallStateListener {

    private static final int MAX_CALLERS_IN_CONFERENCE = 5;

    private int mNumCallersInConference;
    private String[] mCallerIds;
    private Context mContext;
    private boolean isVolteEnable;
    private boolean mOnlyDislpayActiveCall = InCallUIPlugin.getInstance().isOnlyDislpayActiveCall();

    @Override
    public void onUiReady(ConferenceManagerUi ui) {
        super.onUiReady(ui);

        // register for call state changes last
        InCallPresenter.getInstance().addListener(this);
    }

    @Override
    public void onUiUnready(ConferenceManagerUi ui) {
        super.onUiUnready(ui);

        InCallPresenter.getInstance().removeListener(this);
    }

    @Override
    public void onStateChange(InCallState oldState, InCallState newState, CallList callList) {
        Log.v(this, "onStateChange newState = " + newState + "  oldState = " + oldState);
        if (newState == InCallState.INCALL || newState == InCallState.OUTGOING || newState == InCallState.PENDING_OUTGOING) {
            final Call call = callList.getAllConferenceCall();
            if (call != null && call.isConferenceCall() && isVolteEnable && call.getState() != Call.State.ONHOLD) {
                InCallPresenter.getInstance().displayDialpad(false);
                Log.v(this, "Number of existing calls is " +
                        String.valueOf(call.getChildCallIds().size()));
                update(callList);
            } else {
                getUi().setVisible(false);
            }
        } else {
            getUi().setVisible(false);
        }
    }

    public void init(Context context, CallList callList) {
        mContext = Preconditions.checkNotNull(context);
        mContext = context;
        /*SPRD: add for bug532464{@*/
        if (mContext!=null && mContext.checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            isVolteEnable = ImsManager.isVolteEnabledByPlatform(mContext);
        }
        /*@}*/
        update(callList);
        /*SPRD: add for VoLTE{@*/
        if (CallList.getInstance().getAllConferenceCall() != null) {
            String[] mCallerIds;
            if (CallList.getInstance().getAllConferenceCall().getChildCallIds() != null) {
                mCallerIds = CallList.getInstance().getAllConferenceCall().getChildCallIds().toArray(new String[0]);
                for (int i = 0; i < mCallerIds.length; i++) {
                    Call conferenceCall = CallList.getInstance().getCallById(mCallerIds[i]);
                    if (conferenceCall != null && ContactInfoCache.getInstance(mContext).getInfo(mCallerIds[i]) == null) {
                        startContactInfoSearch(conferenceCall, true, conferenceCall.getState() == Call.State.INCOMING);
                    }
                }
            }
        }
        /*@}*/
    }

    /*SPRD: add for VoLTE{@*/
    private void startContactInfoSearch(final Call call, final boolean isPrimary,
            boolean isIncoming) {
        final ContactInfoCache cache = ContactInfoCache.getInstance(mContext);

        cache.findInfo(call, isIncoming, new ContactLookupCallback(this));
    }

    public static class ContactLookupCallback implements ContactInfoCacheCallback {
        private final WeakReference<ConferenceListPresenter> mConferenceListPresenter;

        public ContactLookupCallback(ConferenceListPresenter conferenceListPresenter) {
            mConferenceListPresenter = new WeakReference<ConferenceListPresenter>(conferenceListPresenter);
        }

        @Override
        public void onContactInfoComplete(String callId, ContactCacheEntry entry) {
            ConferenceListPresenter presenter = mConferenceListPresenter.get();
            if (presenter != null) {
                final CallList calls = CallList.getInstance();
                presenter.update(calls);
            }
        }

        @Override
        public void onImageLoadComplete(String callId, ContactCacheEntry entry) {
            // TODO Auto-generated method stub
        }
    }
    /*@}*/

    private void update(CallList callList) {
        if(callList == null){
            Log.w(this, "There is an error when update->callList is null!");
            return;
        } else if(callList.getAllConferenceCall() == null) {
            Log.w(this, "There is an error when update->callList.getAllConferenceCall is null!");
            return;
        } else if(callList.getAllConferenceCall().getChildCallIds() == null){
            Log.w(this, "There is an error when update->getChildCallIds is null!");
            return;
        } else if (getUi() == null) {
            Log.w(this, "There is an error when update->getUi() is null!");
            return;
        }
        mCallerIds = null;
        mCallerIds = callList.getAllConferenceCall().getChildCallIds().toArray(new String[0]);
        mNumCallersInConference = mCallerIds.length;
        Log.v(this, "Number of calls is " + String.valueOf(mNumCallersInConference));

        // Users can split out a call from the conference call if there either the active call
        // or the holding call is empty. If both are filled at the moment, users can not split out
        // another call.
        final boolean hasActiveCall = (callList.getActiveCall() != null);
        final boolean hasHoldingCall = (callList.getBackgroundCall() != null);

        for (int i = 0; i < MAX_CALLERS_IN_CONFERENCE; i++) {
            if (i < mNumCallersInConference) {
                // Fill in the row in the UI for this caller.

                final ContactCacheEntry contactCache = ContactInfoCache.getInstance(mContext).
                        getInfo(mCallerIds[i]);
                Call call = callList.getCallById(mCallerIds[i]);
                int callState = -1;
                if(call != null){
                    //SPRD: for bug531808
                    if(call.isGenericConferenceCall()){
                        callState = call.getGroupState();
                    } else {
                        callState = call.getState();
                    }
                }
                boolean isActive = (callState == Call.State.ACTIVE);
                if(contactCache != null){
                    setRowVisibleForActive(i, isActive);
                    updateManageConferenceRow(i, contactCache, callState);
                } else if(call != null && call.getNumber() != null) {
                    setRowVisibleForActive(i, isActive);
                    getUi().displayCallerInfoForConferenceRow(i, null,
                            call.getNumber(), null, callState);
                } else {
                    updateManageConferenceRow(i, contactCache, callState);
                }
            } else {
                // Blank out this row in the UI
                updateManageConferenceRow(i, null, -1);
            }
        }
    }

    /**
      * Updates a single row of the "Manage conference" UI.  (One row in this
      * UI represents a single caller in the conference.)
      *
      * @param i the row to update
      * @param contactCacheEntry the contact details corresponding to this caller.
      *        If null, that means this is an "empty slot" in the conference,
      *        so hide this row in the UI.
      */
    public void updateManageConferenceRow(final int i,
                                          final ContactCacheEntry contactCacheEntry,
                                          final int callStatus) {
        if (getUi() == null) {
            Log.w(this, "There is an error when updateManageConferenceRow->getUi() is null!");
            return;
        }

        if (contactCacheEntry != null) {
            // Activate this row of the Manage conference panel:
            boolean isActive = (callStatus == Call.State.ACTIVE);
            setRowVisibleForActive(i, isActive);

            final String name = contactCacheEntry.name;
            final String number = contactCacheEntry.number;
            boolean nameIsNumber = name != null && name.equals(number);

            // display the CallerInfo.
            if (nameIsNumber) {
                getUi().displayCallerInfoForConferenceRow(i, null, number, contactCacheEntry.label,
                        callStatus);
            } else {
                getUi().displayCallerInfoForConferenceRow(i, name, number, contactCacheEntry.label,
                        callStatus);
            }
        } else {
            // Disable this row of the Manage conference panel:
            getUi().setRowVisible(i, false);
        }
    }

    public int getMaxCallersInConference() {
        return MAX_CALLERS_IN_CONFERENCE;
    }

    public void setRowVisibleForActive(int i, boolean isActive) {
        if (mOnlyDislpayActiveCall) {
            getUi().setRowVisible(i, isActive);
        } else {
            getUi().setRowVisible(i, true);
        }
    }

    public interface ConferenceManagerUi extends Ui {
        void setVisible(boolean on);
        boolean isFragmentVisible();
        void setRowVisible(int rowId, boolean on);
        void displayCallerInfoForConferenceRow(int rowId, String callerName, String callerNumber,
                String callerNumberType, int callStatus);
        void startConferenceTime(long base);
        void stopConferenceTime();
    }
}
