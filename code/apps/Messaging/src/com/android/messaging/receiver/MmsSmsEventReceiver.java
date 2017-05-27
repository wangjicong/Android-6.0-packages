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

package com.android.messaging.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.messaging.util.LogUtil;

import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import com.android.messaging.sms.MmsUtils;

/**
 * Class that receives event from messaging.
 */
public final class MmsSmsEventReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSmsEventReceiver";
    private Handler mHandler = new Handler();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        LogUtil.d(TAG, "onReceive");
        new MmsSmsEventPushTask(context).execute(intent);
    }

    private class MmsSmsEventPushTask extends AsyncTask<Intent, Void, Void> {
        private Context mContext;

        public MmsSmsEventPushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];
            String action = intent.getAction();

            LogUtil.d(TAG, "MmsSmsEventReceiver received:    action  = " + action);

            if (action == null) {
                return null;
            }
            if (action.equals(MmsUtils.NOTIFY_SHOW_MMS_SMS_REPORT_ACTION)) {

                final String report = intent.getStringExtra("report");
                if (report != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d(TAG, "Toast.makeText report:" + report);

                            Toast.makeText(mContext, report, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            return null;
        }
    }
}
