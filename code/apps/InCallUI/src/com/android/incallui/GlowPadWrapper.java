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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.telecom.VideoProfile;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.incallui.widget.multiwaveview.GlowPadView;
import com.sprd.incallui.IncomingThirdCallHelper;
import com.sprd.incallui.MultiPartCallHelper;

/**
 *
 */
public class GlowPadWrapper extends GlowPadView implements GlowPadView.OnTriggerListener {

    // Parameters for the GlowPadView "ping" animation; see triggerPing().
    private static final int PING_MESSAGE_WHAT = 101;
    private static final boolean ENABLE_PING_AUTO_REPEAT = true;
    private static final long PING_REPEAT_DELAY_MS = 1200;

    // SPRD: Add for Multi-Part-Call(MPC)
    public AlertDialog sDialog;

    private final Handler mPingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PING_MESSAGE_WHAT:
                    triggerPing();
                    break;
            }
        }
    };

    private AnswerListener mAnswerListener;
    private boolean mPingEnabled = true;
    private boolean mTargetTriggered = false;
    private int mVideoState = VideoProfile.STATE_BIDIRECTIONAL;

    public GlowPadWrapper(Context context) {
        super(context);
        Log.d(this, "class created " + this + " ");
    }

    public GlowPadWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(this, "class created " + this);
    }

    @Override
    protected void onFinishInflate() {
        Log.d(this, "onFinishInflate()");
        super.onFinishInflate();
        setOnTriggerListener(this);
    }

    public void startPing() {
        Log.d(this, "startPing");
        mPingEnabled = true;
        triggerPing();
    }

    public void stopPing() {
        Log.d(this, "stopPing");
        mPingEnabled = false;
        mPingHandler.removeMessages(PING_MESSAGE_WHAT);
    }

    private void triggerPing() {
        Log.d(this, "triggerPing(): " + mPingEnabled + " " + this);
        if (mPingEnabled && !mPingHandler.hasMessages(PING_MESSAGE_WHAT)) {
            ping();

            if (ENABLE_PING_AUTO_REPEAT) {
                mPingHandler.sendEmptyMessageDelayed(PING_MESSAGE_WHAT, PING_REPEAT_DELAY_MS);
            }
        }
    }

    @Override
    public void onGrabbed(View v, int handle) {
        Log.d(this, "onGrabbed()");
        stopPing();
    }

    @Override
    public void onReleased(View v, int handle) {
        Log.d(this, "onReleased()");
        if (mTargetTriggered) {
            mTargetTriggered = false;
        } else {
            startPing();
        }
    }

    @Override
    public void onTrigger(View v, int target) {
        Log.d(this, "onTrigger() view=" + v + " target=" + target);
        final int resId = getResourceIdForTarget(target);
        switch (resId) {
            case R.drawable.ic_lockscreen_answer:
                // mAnswerListener.onAnswer(VideoProfile.VideoState.AUDIO_ONLY, getContext());
                // mTargetTriggered = true;
                // SPRD: Multi-Part-Call(MPC)
                if (getContext() != null && MultiPartCallHelper.getInstance(
                        getContext()).isSupportMultiPartCall()) {
                    showAnswerOptions();
                } else if (getContext() != null && IncomingThirdCallHelper.getsInstance(
                        getContext()).isSupportIncomingThirdCall()
                        && CallList.getInstance().getBackgroundCall() != null
                        && CallList.getInstance().getActiveCall() != null
                        && CallList.getInstance().getIncomingCall() != null
                        && CallList.getInstance().getBackgroundCall()
                        .getState() == Call.State.ONHOLD
                        && CallList.getInstance().getActiveCall().getState() == Call.State.ACTIVE) {
                    mAnswerListener.onAnswer(MultiPartCallHelper.MPC_MODE_HF,
                            VideoProfile.STATE_AUDIO_ONLY, getContext());
                    mTargetTriggered = true;
                } else {
                    mAnswerListener.onAnswer(MultiPartCallHelper.MPC_MODE_HB,
                            VideoProfile.STATE_AUDIO_ONLY, getContext());
                    mTargetTriggered = true;
                }
                break;
            case R.drawable.ic_lockscreen_decline:
                mAnswerListener.onDecline(getContext());
                mTargetTriggered = true;
                break;
            case R.drawable.ic_lockscreen_text:
                mAnswerListener.onText();
                mTargetTriggered = true;
                break;
            case R.drawable.ic_videocam:
            case R.drawable.ic_lockscreen_answer_video:
                mAnswerListener.onAnswer(MultiPartCallHelper.MPC_MODE_HF, mVideoState, getContext());
                mTargetTriggered = true;
                break;
            case R.drawable.ic_lockscreen_decline_video:
                mAnswerListener.onDeclineUpgradeRequest(getContext());
                mTargetTriggered = true;
                break;
            default:
                // Code should never reach here.
                Log.e(this, "Trigger detected on unhandled resource. Skipping.");
        }
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {

    }

    @Override
    public void onFinishFinalAnimation() {

    }

    public void setAnswerListener(AnswerListener listener) {
        mAnswerListener = listener;
    }

    /**
     * Sets the video state represented by the "video" icon on the glow pad.
     *
     * @param videoState The new video state.
     */
    public void setVideoState(int videoState) {
        mVideoState = videoState;
    }

    public interface AnswerListener {
        void onAnswer(int mpcMode, int videoState, Context context);
        void onDecline(Context context);
        void onDeclineUpgradeRequest(Context context);
        void onText();
    }

///////////////////////////////////////SPRD////////////////////////////////////////
    /**
     * SPRD: Add for Multi-Part-Call(MPC)
     */
    private void showAnswerOptions() {
        final List<Map<String, String>> answerOptions = new ArrayList<Map<String, String>>();

        if (CallList.getInstance().getBackgroundCall() != null
                && CallList.getInstance().getActiveCall() != null
                && CallList.getInstance().getIncomingCall() != null
                && CallList.getInstance().getBackgroundCall().getState() == Call.State.ONHOLD
                && CallList.getInstance().getActiveCall().getState() == Call.State.ACTIVE) {
            String[] labelname = new String[] {
                    R.string.hangup_hold_and_answer + "",
                    R.string.hangup_active_and_answer + "" };
            String[] imageView = new String[] {
                    R.drawable.hangup_hold_and_answer_ex + "",
                    R.drawable.hangup_active_and_answer_ex + "" };
            String[] info = new String[2];
            final Call backgroundCall = CallList.getInstance()
                    .getBackgroundCall();
            final CallerInfo backgroundCallerInfo = CallerInfoUtils
                    .getCallerInfoForCall(getContext(), backgroundCall,
                            new FindInfoCallback(false));
            if (backgroundCallerInfo.name != null) {
                info[0] = backgroundCallerInfo.name + " "
                        + getCallNumberForSelectAnswer(backgroundCall) + " ("
                        + getContext().getString(R.string.onHold) + ")";
            } else {
                info[0] = getCallNumberForSelectAnswer(backgroundCall) + " ("
                        + getContext().getString(R.string.onHold) + ")";
            }
            final Call activeCall = CallList.getInstance().getActiveCall();
            final CallerInfo activeCallerInfo = CallerInfoUtils
                    .getCallerInfoForCall(getContext(), activeCall,
                            new FindInfoCallback(true));
            if (activeCallerInfo.name != null) {
                info[1] = activeCallerInfo.name + " "
                        + getCallNumberForSelectAnswer(activeCall) + " ("
                        + getContext().getString(R.string.onActive) + ")";
            } else {
                info[1] = getCallNumberForSelectAnswer(activeCall) + " ("
                        + getContext().getString(R.string.onActive) + ")";
            }
            for (int i = 0; i < 2; i++) {
                Map<String, String> infoMap = new HashMap<String, String>();
                infoMap.put("info", info[i]);
                infoMap.put("labelname", labelname[i]);
                infoMap.put("drawable", imageView[i]);
                answerOptions.add(infoMap);
            }
            SelectAnswerTypeListener listener = new SelectAnswerTypeListener() {
                @Override
                public void onAnswerTypeSelected(int type) {
                    switch (type) {
                    case 0:
                        mAnswerListener.onAnswer(
                                MultiPartCallHelper.MPC_MODE_HB,
                                VideoProfile.STATE_AUDIO_ONLY,
                                getContext());
                        break;
                    case 1:
                        mAnswerListener.onAnswer(
                                MultiPartCallHelper.MPC_MODE_HF,
                                VideoProfile.STATE_AUDIO_ONLY,
                                getContext());
                        break;
                    case 2:
                        // TODO: implement feature, like HANGUP ALL CALLS AND
                        // ANSWER
                    }
                }
            };
            showDialog(getContext(), answerOptions, listener);
        } else if (CallList.getInstance().getActiveCall() != null
                && CallList.getInstance().getIncomingCall() != null
                && CallList.getInstance().getActiveCall().getState() == Call.State.ACTIVE) {
            String[] labelname = new String[] {
                    R.string.hold_current_call_and_answer + "",
                    R.string.hangup_current_call_and_answer + "" };
            String[] imageView = new String[] {
                    R.drawable.hold_active_and_answer_ex + "",
                    R.drawable.hangup_active_and_answer_ex + "" };
            String info = new String();
            final Call activeCall = CallList.getInstance().getActiveCall();
            final CallerInfo activeCallerInfo = CallerInfoUtils
                    .getCallerInfoForCall(getContext(), activeCall,
                            new FindInfoCallback(true));
            if (activeCallerInfo.name != null) {
                info = activeCallerInfo.name + " "
                        + getCallNumberForSelectAnswer(activeCall) + " ("
                        + getContext().getString(R.string.onActive) + ")";
            } else {
                info = getCallNumberForSelectAnswer(activeCall) + " ("
                        + getContext().getString(R.string.onActive) + ")";
            }

            for (int i = 0; i < 2; i++) {
                Map<String, String> infoMap = new HashMap<String, String>();
                infoMap.put("info", info);
                infoMap.put("labelname", labelname[i]);
                infoMap.put("drawable", imageView[i]);
                answerOptions.add(infoMap);
            }
            SelectAnswerTypeListener listener = new SelectAnswerTypeListener() {
                @Override
                public void onAnswerTypeSelected(int type) {
                    switch (type) {
                    case 0:
                        mAnswerListener.onAnswer(
                                MultiPartCallHelper.MPC_MODE_HF,
                                VideoProfile.STATE_AUDIO_ONLY,
                                getContext());
                        break;
                    case 1:
                        mAnswerListener.onAnswer(
                                MultiPartCallHelper.MPC_MODE_HBF,
                                VideoProfile.STATE_AUDIO_ONLY,
                                getContext());
                        break;
                    }
                }
            };
            showDialog(getContext(), answerOptions, listener);
        } else {
            mAnswerListener.onAnswer(MultiPartCallHelper.MPC_MODE_HF,
                    VideoProfile.STATE_AUDIO_ONLY, getContext());
            mTargetTriggered = true;
        }
    }

    public static class SelectAnswerListAdapter extends
            ArrayAdapter<Map<String, String>> {
        private int mResId;

        public SelectAnswerListAdapter(Context context, int resource,
                List<Map<String, String>> answer) {
            super(context, resource, answer);
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.labelTextView = (TextView) rowView
                        .findViewById(R.id.label);
                holder.infoTextView = (TextView) rowView
                        .findViewById(R.id.info);
                holder.imageView = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            Map<String, String> namemap = getItem(position);
            Map<String, String> labelmap = getItem(position);
            Map<String, String> drawablemap = getItem(position);

            switch (position) {
            case 0:
                holder.labelTextView.setText(Integer.parseInt(labelmap
                        .get("labelname")));
                holder.infoTextView.setVisibility(View.VISIBLE);
                holder.infoTextView.setText(namemap.get("info"));
                holder.imageView.setImageResource(Integer.parseInt(drawablemap
                        .get("drawable")));
                break;
            case 1:
                holder.labelTextView.setText(Integer.parseInt(labelmap
                        .get("labelname")));
                holder.infoTextView.setVisibility(View.VISIBLE);
                holder.infoTextView.setText(namemap.get("info"));
                holder.imageView.setImageResource(Integer.parseInt(drawablemap
                        .get("drawable")));
                break;
            /*
             * TODO implement feature, like HANGUP ALL CALLS AND ANSWER case 2:
             * holder.labelTextView.setText(R.string.hangup_all_and_answer);
             * holder.infoTextView.setVisibility(View.GONE);
             * holder.imageView.setImageResource
             * (R.drawable.hangup_all_and_answer); break;
             */
            }
            return rowView;
        }

        private class ViewHolder {
            TextView labelTextView;
            TextView infoTextView;
            ImageView imageView;
        }
    }

    public void showDialog(Context context, List<Map<String, String>> answer,
            SelectAnswerTypeListener listener) {
        final List<Map<String, String>> mAnswer = answer;
        final SelectAnswerTypeListener mlistener = listener;
        final DialogInterface.OnClickListener selectionListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mlistener.onAnswerTypeSelected(which);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ListAdapter selectAnswerListAdapter = new SelectAnswerListAdapter(
                builder.getContext(), R.layout.select_answer_type_list_item_ex,
                mAnswer);

        sDialog = builder
                .setAdapter(selectAnswerListAdapter, selectionListener)
                .create();

        sDialog.show();
    }

    public interface SelectAnswerTypeListener {
        void onAnswerTypeSelected(int type);
    }

    private class FindInfoCallback implements
            CallerInfoAsyncQuery.OnQueryCompleteListener {
        private final boolean mIsIncoming;

        public FindInfoCallback(boolean isIncoming) {
            mIsIncoming = isIncoming;
        }

        @Override
        public void onQueryComplete(int token, Object cookie,
                CallerInfo callerInfo) {
            // null
        }
    }

    private String getCallNumberForSelectAnswer(Call call) {
        if (call.isConferenceCall()) {
            return getContext().getString(R.string.confCall);
        } else {
            return call.getNumber();
        }
    }
}
