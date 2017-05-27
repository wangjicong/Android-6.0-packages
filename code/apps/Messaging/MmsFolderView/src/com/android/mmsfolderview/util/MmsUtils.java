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

package com.android.mmsfolderview.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Patterns;

import com.android.mmsfolderview.R;
import com.android.mmsfolderview.data.SortMsgDataCollector;

/**
 * Utils for sending sms/mms messages.
 */
public class MmsUtils {

    private static String[] sNoSubjectStrings;

    public static final Pattern NAME_ADDR_EMAIL_PATTERN =
            Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    public static int mapRawStatusToErrorResourceId(final int bugleStatus, final int rawStatus) {
        int stringResId = R.string.msg_status_send_failed;
        switch (rawStatus) {
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_SERVICE_DENIED:
                /*Add by SPRD for bug550315  2016.04.12 Start*/
                stringResId = R.string.user_not_exist;
                break;
                /*Add by SPRD for bug550315  2016.04.12 End*/
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_PERMANENT_SERVICE_DENIED:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_LIMITATIONS_NOT_MET:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_REQUEST_NOT_ACCEPTED:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_FORWARDING_DENIED:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_PERMANENT_REPLY_CHARGING_NOT_SUPPORTED:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_PERMANENT_ADDRESS_HIDING_NOT_SUPPORTED:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_PERMANENT_LACK_OF_PREPAID:
                stringResId = R.string.mms_failure_outgoing_service;
                break;
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED:
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_TRANSIENT_SENDNG_ADDRESS_UNRESOLVED:
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_PERMANENT_SENDING_ADDRESS_UNRESOLVED:
                stringResId = R.string.mms_failure_outgoing_address;
                break;
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT:
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_FORMAT_CORRUPT:
                stringResId = R.string.mms_failure_outgoing_corrupt;
                break;
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED:
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_PERMANENT_CONTENT_NOT_ACCEPTED:
                stringResId = R.string.mms_failure_outgoing_content;
                break;
            case SortMsgDataCollector.RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND:
            //case MessagingDataDef.RESPONSE_STATUS_ERROR_TRANSIENT_MESSAGE_NOT_FOUND:
                stringResId = R.string.mms_failure_outgoing_unsupported;
                break;
            case SortMsgDataCollector.RAW_TELEPHONY_STATUS_MESSAGE_TOO_BIG:
                stringResId = R.string.mms_failure_outgoing_too_large;
                break;
        }
        return stringResId;
    }

    /**
     * cleanseMmsSubject will take a subject that's says,
     * "<Subject: no subject>", and return a null string. Otherwise it will
     * return the original subject string.
     * 
     * @param resources So the function can grab string resources
     * @param subject the raw subject
     * @return
     */
    public static String cleanseMmsSubject(final Resources resources, final String subject) {
        if (TextUtils.isEmpty(subject)) {
            return null;
        }
        if (sNoSubjectStrings == null) {
            sNoSubjectStrings = resources.getStringArray(R.array.empty_subject_strings);
        }
        for (final String noSubjectString : sNoSubjectStrings) {
            if (subject.equalsIgnoreCase(noSubjectString)) {
                return null;
            }
        }
        return subject;
    }
    
    /**
     * Returns true if the address is an email address
     *
     * @param address the input address to be tested
     * @return true if address is an email address
     */
    public static boolean isEmailAddress(final String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        final String s = extractAddrSpec(address);
        final Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
        return match.matches();
    }
    
    public static String extractAddrSpec(final String address) {
        final Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }

    public static int tanslateSubIdToPhoneId(Context context, int subId) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        int phoneId = tm.getPhoneId(subId);
        return phoneId;
    }
}
