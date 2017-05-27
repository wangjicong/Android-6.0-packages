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

import com.android.incallui.BaseFragment;
import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.Log;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Chronometer;
import android.widget.TextView;
import com.android.incallui.R;

/**
 * Fragment for call control buttons
 */
public class ConferenceListFragment
        extends BaseFragment<ConferenceListPresenter,
                ConferenceListPresenter.ConferenceManagerUi>
        implements ConferenceListPresenter.ConferenceManagerUi {

    private View mButtonManageConferenceDone;
    private ViewGroup[] mConferenceCallList;
    private Chronometer mConferenceTime;

    // sprd: modify access permission for vt
    @Override
    public ConferenceListPresenter createPresenter() {
        // having a singleton instance.
        return new ConferenceListPresenter();
    }
    // sprd: modify access permission for vt
    @Override
    public ConferenceListPresenter.ConferenceManagerUi getUi() {
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View parent;
        parent = inflater.inflate(R.layout.conference_manager_list_sprd, container, false);
        mConferenceStub = (ViewStub) parent.findViewById(R.id.conferenceStub);

        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void setVisible(boolean on) {
        if(getView() == null) return;
        if (on) {
            if(mConferenceStub != null && mConferenceView == null){
                initConferenceViews();
            } else if(mConferenceView != null){
                mConferenceView.setVisibility(View.VISIBLE);
            }
            if(getActivity() != null){
                final CallList calls = CallList.getInstance();
                getPresenter().init(getActivity(), calls);
            }
            getView().setVisibility(View.VISIBLE);
        } else {
            if(mConferenceView != null){
                mConferenceView.setVisibility(View.GONE);
            }
            getView().setVisibility(View.GONE);
        }

    }

    @Override
    public boolean isFragmentVisible() {
        return isVisible();
    }

    @Override
    public void setRowVisible(int rowId, boolean on) {
        if((mConferenceCallList == null) || (mConferenceCallList[rowId] == null)){
            Log.i(this, "setRowVisible-> rowId:" + rowId+ " on:"+ on + " (mConferenceCallList == null)?" + (mConferenceCallList == null));
            return;
        }
        Log.i(this, "setRowVisible-> rowId:" + rowId+ " on:"+on);
        if (on) {
            mConferenceCallList[rowId].setVisibility(View.VISIBLE);
        } else {
            mConferenceCallList[rowId].setVisibility(View.GONE);
        }
    }

    /**
     * Helper function to fill out the Conference Call(er) information
     * for each item in the "Manage Conference Call" list.
     */
    @Override
    public final void displayCallerInfoForConferenceRow(int rowId, String callerName,
            String callerNumber, String callerNumberType, int callStatus) {
        if((mConferenceCallList == null) || (mConferenceCallList[rowId] == null)){
            Log.i(this, "displayCallerInfoForConferenceRow-> rowId:" + rowId + " (mConferenceCallList == null)?" + (mConferenceCallList == null));
            return;
        }
        Log.i(this, "displayCallerInfoForConferenceRow-> rowId:" + rowId
                +" callerName:"+callerName
                + " callerNumber:"+callerNumber
                +" callerNumberType:"+callerNumberType
                + " callStatus:"+callStatus);

        final TextView nameTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerName);
        final TextView numberTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerNumber);
        final TextView numberTypeTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerNumberType);
        final TextView callStatusTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerStatus);


        // set the caller name
        nameTextView.setText(callerName);
        callStatusTextView.setText(getCallStateLabelFromState(callStatus));

        // set the caller number in subscript, or make the field disappear.
        if (TextUtils.isEmpty(callerNumber)) {
            numberTextView.setVisibility(View.GONE);
            numberTypeTextView.setVisibility(View.GONE);
        } else {
            numberTextView.setVisibility(View.VISIBLE);
            numberTextView.setText(callerNumber);
            numberTypeTextView.setVisibility(View.VISIBLE);
            numberTypeTextView.setText(callerNumberType);
        }
    }

    /**
     * Starts the "conference time" chronometer.
     */
    @Override
    public void startConferenceTime(long base) {
        if (mConferenceTime != null) {
            mConferenceTime.setBase(base);
            mConferenceTime.start();
        }
    }

    /**
     * Stops the "conference time" chronometer.
     */
    @Override
    public void stopConferenceTime() {
        if (mConferenceTime != null) {
            mConferenceTime.stop();
        }
    }

    /* SPRD: Add for Universe UI @{ */
    private ViewStub mConferenceStub;
    private View mConferenceView;

    private void initConferenceViews(){
        View parent = mConferenceStub.inflate();
        mConferenceView = parent.findViewById(R.id.manageConferencePanel);
        // set up the Conference Call chronometer
        mConferenceTime = (Chronometer) parent.findViewById(R.id.manageConferencePanelHeader);
        mConferenceTime.setFormat(getActivity().getString(R.string.caller_manage_header));

        // Create list of conference call widgets
        mConferenceCallList = new ViewGroup[getPresenter().getMaxCallersInConference()];

        final int[] viewGroupIdList = { R.id.caller0, R.id.caller1, R.id.caller2,
                R.id.caller3, R.id.caller4 };
        for (int i = 0; i < getPresenter().getMaxCallersInConference(); i++) {
            mConferenceCallList[i] =
                    (ViewGroup) parent.findViewById(viewGroupIdList[i]);
        }
        mConferenceView.setVisibility(View.VISIBLE);
    }
    /* @}
     */
    private String getCallStateLabelFromState(int state) {
        final Context context = getView().getContext();
        String callStateLabel = " ";  // Label to display as part of the call banner

        switch(state){
            case Call.State.IDLE:
                break;
            case Call.State.ACTIVE:
            case Call.State.CONFERENCED:
                callStateLabel = context.getString(R.string.card_title_in_call);
                break;
            case Call.State.ONHOLD:
                callStateLabel = context.getString(R.string.card_title_on_hold);
                break;
            case Call.State.DIALING:
                callStateLabel = context.getString(R.string.card_title_dialing);
                break;
            case Call.State.REDIALING:
                callStateLabel = context.getString(R.string.card_title_redialing);
                break;
            case Call.State.INCOMING:
                callStateLabel = context.getString(R.string.card_title_incoming_call);
                break;
            case Call.State.DISCONNECTING:
                callStateLabel = context.getString(R.string.card_title_hanging_up);
                break;
            default:
                Log.wtf(this, "updateCallStateWidgets: unexpected call: " + state);
        }

        return callStateLabel;
    }
}