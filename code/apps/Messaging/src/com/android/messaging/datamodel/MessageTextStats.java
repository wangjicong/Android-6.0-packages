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

import android.content.Context;
import android.telephony.SmsMessage;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.LogUtil;

public class MessageTextStats {
    private boolean mMessageLengthRequiresMms;
    private int mMessageCount;
    private int mCodePointsRemainingInCurrentMessage;

    public MessageTextStats() {
        mCodePointsRemainingInCurrentMessage = Integer.MAX_VALUE;
    }

    public int getNumMessagesToBeSent() {
        return mMessageCount;
    }

    public int getCodePointsRemainingInCurrentMessage() {
        return mCodePointsRemainingInCurrentMessage;
    }

    public boolean getMessageLengthRequiresMms() {
        return mMessageLengthRequiresMms;
    }

    public void updateMessageTextStats(final int selfSubId, final String messageText) {
        /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
        final Context context = Factory.get().getApplicationContext();
        final String prefKey = context.getString(R.string.signature_pref_key);
        String signatureText = BuglePrefs.getApplicationPrefs().getString(prefKey, "");
        if (MmsConfig.getSignatureEnabled() && !("".equals(signatureText.trim()))) {
            signatureText = "--" + signatureText;
        } else {
            signatureText = "";
        }
        /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */

        /*Modify by SPRD for Bug:562203 Encode type feature  Start */
        String msgText = messageText + signatureText;
        /*final */int[] params;
        int encodeType = Integer.parseInt(MmsConfig.getEncodeType());
        LogUtil.d(LogUtil.BUGLE_TAG+"-encodeTypeLog", "MessageTextStats  encodeType:"+encodeType);
        if (encodeType != 0) {
            params = MmsConfig.calculateLength(msgText);
            mMessageCount = params[0];
            mCodePointsRemainingInCurrentMessage = params[1];
            LogUtil.d(LogUtil.BUGLE_TAG+"-encodeTypeLog", "[updateMessageTextStats] one mMessageCount:"
            +mMessageCount+", mCodePointsRemainingInCurrentMessage:"+mCodePointsRemainingInCurrentMessage);
        } else {
            params = SmsMessage.calculateLength(msgText, false);
            /* SmsMessage.calculateLength returns an int[4] with:
             *   int[0] being the number of SMS's required,
             *   int[1] the number of code points used,
             *   int[2] is the number of code points remaining until the next message.
             *   int[3] is the encoding type that should be used for the message.
             */
            mMessageCount = params[0];
            mCodePointsRemainingInCurrentMessage = params[2];
            LogUtil.d(LogUtil.BUGLE_TAG+"-encodeTypeLog", "[updateMessageTextStats] two mMessageCount:"
            +mMessageCount+", mCodePointsRemainingInCurrentMessage:"+mCodePointsRemainingInCurrentMessage);
        }
        /*Modify by SPRD for Bug:562203 Encode Type feature  End */

        final MmsConfig mmsConfig = MmsConfig.get(selfSubId);
        if (!mmsConfig.getMultipartSmsEnabled() &&
                !mmsConfig.getSendMultipartSmsAsSeparateMessages()) {
            // The provider doesn't support multi-part sms's and we should use MMS to
            // send multi-part sms, so as soon as the user types
            // an sms longer than one segment, we have to turn the message into an mms.
            mMessageLengthRequiresMms = mMessageCount > 1;
        } else {
            final int threshold = mmsConfig.getSmsToMmsTextThreshold();
            mMessageLengthRequiresMms = threshold > 0 && mMessageCount > threshold;
        }
        // Some carriers require any SMS message longer than 80 to be sent as MMS
        // see b/12122333
        int smsToMmsLengthThreshold = mmsConfig.getSmsToMmsTextLengthThreshold();
        if (smsToMmsLengthThreshold > 0) {
            final int usedInCurrentMessage = params[1];
            /*
             * A little hacky way to find out if we should count characters in double bytes.
             * SmsMessage.calculateLength counts message code units based on the characters
             * in input. If all of them are ascii, the max length is
             * SmsMessage.MAX_USER_DATA_SEPTETS (160). If any of them are double-byte, like
             * Korean or Chinese, the max length is SmsMessage.MAX_USER_DATA_BYTES (140) bytes
             * (70 code units).
             * Here we check if the total code units we can use is smaller than 140. If so,
             * we know we should count threshold in double-byte, so divide the threshold by 2.
             * In this way, we will count Korean text correctly with regard to the length threshold.
             */
            if (usedInCurrentMessage + mCodePointsRemainingInCurrentMessage
                    < SmsMessage.MAX_USER_DATA_BYTES) {
                smsToMmsLengthThreshold /= 2;
            }
            if (usedInCurrentMessage > smsToMmsLengthThreshold) {
                mMessageLengthRequiresMms = true;
            }
        }
    }

}
