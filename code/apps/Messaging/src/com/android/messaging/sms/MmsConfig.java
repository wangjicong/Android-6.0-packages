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

package com.android.messaging.sms;

import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v7.mms.CarrierConfigValuesLoader;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.GsmAlphabet;
import com.android.messaging.Factory;
import com.android.messaging.OperatorFactory;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.util.Assert;
import com.android.messaging.util.FdnUtil;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.SafeAsyncTask;
import com.android.messaging.util.BuglePrefs;//Add for Bug:533513
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

/**
 * MMS configuration.
 *
 * This is now a wrapper around the BugleCarrierConfigValuesLoader, which does
 * the actual loading and stores the values in a Bundle. This class provides getter
 * methods for values used in the app, which is easier to use than the raw loader
 * class.
 */
public class MmsConfig {
    private static final String TAG = LogUtil.BUGLE_TAG;

    private static final int DEFAULT_MAX_TEXT_LENGTH = 2000;

    /*
     * Key types
     */
    public static final String KEY_TYPE_INT = "int";
    public static final String KEY_TYPE_BOOL = "bool";
    public static final String KEY_TYPE_STRING = "string";

    private static final Map<String, String> sKeyTypeMap = Maps.newHashMap();

    static {
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLED_MMS, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLED_TRANS_ID, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLED_NOTIFY_WAP_MMSC, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ALIAS_ENABLED, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ALLOW_ATTACH_AUDIO, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLE_MULTIPART_SMS, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_DELIVERY_REPORTS,
                KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLE_GROUP_MMS, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION,
                KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_CELL_BROADCAST_APP_LINKS, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES,
                KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLE_MMS_READ_REPORTS, KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ENABLE_MMS_DELIVERY_REPORTS,
                KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_SUPPORT_HTTP_CHARSET_HEADER,
                KEY_TYPE_BOOL);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_SIZE, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_MAX_IMAGE_HEIGHT, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_MAX_IMAGE_WIDTH, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_RECIPIENT_LIMIT, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_HTTP_SOCKET_TIMEOUT, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ALIAS_MIN_CHARS, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_ALIAS_MAX_CHARS, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_SMS_TO_MMS_TEXT_THRESHOLD, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD,
                KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_TEXT_SIZE, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_MAX_SUBJECT_LENGTH, KEY_TYPE_INT);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_UA_PROF_TAG_NAME, KEY_TYPE_STRING);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_HTTP_PARAMS, KEY_TYPE_STRING);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_EMAIL_GATEWAY_NUMBER, KEY_TYPE_STRING);
        sKeyTypeMap.put(CarrierConfigValuesLoader.CONFIG_NAI_SUFFIX, KEY_TYPE_STRING);
    }

    // A map that stores all MmsConfigs, one per active subscription. For pre-LMSim, this will
    // contain just one entry with the default self sub id; for LMSim and above, this will contain
    // all active sub ids but the default subscription id - the default subscription id will be
    // resolved to an active sub id during runtime.
    private static final Map<Integer, MmsConfig> sSubIdToMmsConfigMap = Maps.newHashMap();
    // The fallback values
    private static final MmsConfig sFallback =
            new MmsConfig(ParticipantData.DEFAULT_SELF_SUB_ID, new Bundle());

    // Per-subscription configuration values.
    private final Bundle mValues;
    private final int mSubId;
    private static int mEnableSendingBlankSMSandDialog = 1;
    private static boolean mEnabledSmsValidity=true;
    private static boolean mEnabledMmsValidity=true;
    // sprd :562194 fixed  start
    private static int mDefaultNumberLength = 10;
    private static boolean mFdnContactsEnable = false;
    // sprd :562194 fixed  end

    //489220 begin
    private static boolean mMmsReadReportsEnable = true;
    //489220 end
    private final static String KEY_SP_SINATURE = "k-sp-signature";//Add for Bug:533513
    private final static String KEY_SP_ENCODETYPE = "k-sp-encodetype";//Add for Encode Type feature bug:562203

    /**
     * Retrieves the MmsConfig instance associated with the given {@code subId}
     */
    public static MmsConfig get(final int subId) {
        final int realSubId = PhoneUtils.getDefault().getEffectiveSubId(subId);
        synchronized (sSubIdToMmsConfigMap) {
            final MmsConfig mmsConfig = sSubIdToMmsConfigMap.get(realSubId);
            if (mmsConfig == null) {
                // The subId is no longer valid. Fall back to the default config.
                LogUtil.e(LogUtil.BUGLE_TAG, "Get mms config failed: invalid subId. subId=" + subId
                        + ", real subId=" + realSubId
                        + ", map=" + sSubIdToMmsConfigMap.keySet());
                return sFallback;
            }
            return mmsConfig;
        }
    }

    private MmsConfig(final int subId, final Bundle values) {
        mSubId = subId;
        mValues = values;
        OperatorFactory.setParamter(mValues);
    }

    /**
     * Same as load() but doing it using an async thread from SafeAsyncTask thread pool.
     */
    public static void loadAsync() {
        SafeAsyncTask.executeOnThreadPool(new Runnable() {
            @Override
            public void run() {
                load();
            }
        });
    }

    /**
     * Reload the device and per-subscription settings.
     */
    public static synchronized void load() {
        final BugleCarrierConfigValuesLoader loader = Factory.get().getCarrierConfigValuesLoader();
        // Rebuild the entire MmsConfig map.
        sSubIdToMmsConfigMap.clear();
        loader.reset();
        if (OsUtil.isAtLeastL_MR1()) {
            final List<SubscriptionInfo> subInfoRecords =
                    PhoneUtils.getDefault().toLMr1().getActiveSubscriptionInfoList();
            if (subInfoRecords == null) {
                LogUtil.w(TAG, "Loading mms config failed: no active SIM");
                return;
            }
            for (SubscriptionInfo subInfoRecord : subInfoRecords) {
                final int subId = subInfoRecord.getSubscriptionId();
                final Bundle values = loader.get(subId);
                addMmsConfig(new MmsConfig(subId, values));
            }
        } else {
            final Bundle values = loader.get(ParticipantData.DEFAULT_SELF_SUB_ID);
            addMmsConfig(new MmsConfig(ParticipantData.DEFAULT_SELF_SUB_ID, values));
        }
    }

    private static void addMmsConfig(MmsConfig mmsConfig) {
        Assert.isTrue(OsUtil.isAtLeastL_MR1() !=
                (mmsConfig.mSubId == ParticipantData.DEFAULT_SELF_SUB_ID));
        sSubIdToMmsConfigMap.put(mmsConfig.mSubId, mmsConfig);
    }

    /**Add from Bug 489223, It can use for the CMCC features*/
    public static boolean getIsCMCC() {
        return "cmcc".equalsIgnoreCase(SystemProperties.get("ro.operator")) ||
               "spec3".equalsIgnoreCase(SystemProperties.get("ro.operator.version"));
    }

    public int getSmsToMmsTextThreshold() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_SMS_TO_MMS_TEXT_THRESHOLD,
                CarrierConfigValuesLoader.CONFIG_SMS_TO_MMS_TEXT_THRESHOLD_DEFAULT);
    }

    public int getSmsToMmsTextLengthThreshold() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD,
                CarrierConfigValuesLoader.CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD_DEFAULT);
    }

    public int getMaxMessageSize() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_SIZE,
                CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_SIZE_DEFAULT);
    }

    //add new function for empty msg begin
    /**
     * Return the value flag for sending Empty sms and Whether show the dialog or not
     * show : 0x00000001  no show : 0x00010000
     */
    public static int getFinalSendEmptyMessageFlag() {
        return  mEnableSendingBlankSMSandDialog ;
    }

    //add new function for empty msg end
    //489220 begin
    public static boolean getmMmsReadReportsEnable(){
        return mMmsReadReportsEnable;
    }
    //489220 end

    // sprd :562194 fixed  start
    public static int getDefaultFdnKeyPhoneNumbeLength(){
        return mDefaultNumberLength;
    }

    public static boolean getFdnContactFittingEnable(){
        int flag = SystemProperties.getInt("ro.messag.fdnfilter", 1);
        ArrayList<Integer> subidList = FdnUtil.getActivitySubidListForOut();
        if(subidList == null){
            return false;
        }
        if(subidList.size() == 1){
            if(FdnUtil.getFdnEnable(subidList.get(0))){
                if(flag == 0){
                    mFdnContactsEnable =  true;
                }else if(flag == 1){
                    mFdnContactsEnable =  false;
                }
            }else{
                mFdnContactsEnable = false;
            }

        }
        return mFdnContactsEnable;
    }
    // sprd :562194 fixed  end

    /**
     * Return the largest MaxMessageSize for any subid
     */
    public static int getMaxMaxMessageSize() {
        int maxMax = 0;
        for (MmsConfig config : sSubIdToMmsConfigMap.values()) {
            maxMax = Math.max(maxMax, config.getMaxMessageSize());
        }
        return maxMax > 0 ? maxMax : sFallback.getMaxMessageSize();
    }

    public boolean getTransIdEnabled() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ENABLED_TRANS_ID,
                CarrierConfigValuesLoader.CONFIG_ENABLED_TRANS_ID_DEFAULT);
    }

    public String getEmailGateway() {
        return mValues.getString(CarrierConfigValuesLoader.CONFIG_EMAIL_GATEWAY_NUMBER,
                CarrierConfigValuesLoader.CONFIG_EMAIL_GATEWAY_NUMBER_DEFAULT);
    }

    public int getMaxImageHeight() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_MAX_IMAGE_HEIGHT,
                CarrierConfigValuesLoader.CONFIG_MAX_IMAGE_HEIGHT_DEFAULT);
    }

    public int getMaxImageWidth() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_MAX_IMAGE_WIDTH,
                CarrierConfigValuesLoader.CONFIG_MAX_IMAGE_WIDTH_DEFAULT);
    }

    private static final int MAX_RECIPIENT_LIMIT = 50;
    public int getRecipientLimit() {
        int limit = mValues.getInt(CarrierConfigValuesLoader.CONFIG_RECIPIENT_LIMIT,
                CarrierConfigValuesLoader.CONFIG_RECIPIENT_LIMIT_DEFAULT);
        if(limit > MAX_RECIPIENT_LIMIT) {
            limit = MAX_RECIPIENT_LIMIT;
        }
        return limit < 0 ? MAX_RECIPIENT_LIMIT : limit;
    }

    public int getMaxTextLimit() {
        final int max = mValues.getInt(CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_TEXT_SIZE,
                CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_TEXT_SIZE_DEFAULT);
        return max > -1 ? max : DEFAULT_MAX_TEXT_LENGTH;
    }

    public boolean getMultipartSmsEnabled() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ENABLE_MULTIPART_SMS,
                CarrierConfigValuesLoader.CONFIG_ENABLE_MULTIPART_SMS_DEFAULT);
    }

    public boolean getSendMultipartSmsAsSeparateMessages() {
        return mValues.getBoolean(
                CarrierConfigValuesLoader.CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES,
                CarrierConfigValuesLoader.CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES_DEFAULT);
    }

    public boolean getSMSDeliveryReportsEnabled() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_DELIVERY_REPORTS,
                CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_DELIVERY_REPORTS_DEFAULT);
    }

    public boolean getNotifyWapMMSC() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ENABLED_NOTIFY_WAP_MMSC,
                CarrierConfigValuesLoader.CONFIG_ENABLED_NOTIFY_WAP_MMSC_DEFAULT);
    }

    public boolean isAliasEnabled() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ALIAS_ENABLED,
                CarrierConfigValuesLoader.CONFIG_ALIAS_ENABLED_DEFAULT);
    }

    public int getAliasMinChars() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_ALIAS_MIN_CHARS,
                CarrierConfigValuesLoader.CONFIG_ALIAS_MIN_CHARS_DEFAULT);
    }

    public int getAliasMaxChars() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_ALIAS_MAX_CHARS,
                CarrierConfigValuesLoader.CONFIG_ALIAS_MAX_CHARS_DEFAULT);
    }

    public boolean getAllowAttachAudio() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ALLOW_ATTACH_AUDIO,
                CarrierConfigValuesLoader.CONFIG_ALLOW_ATTACH_AUDIO_DEFAULT);
    }

    public int getMaxSubjectLength() {
        return mValues.getInt(CarrierConfigValuesLoader.CONFIG_MAX_SUBJECT_LENGTH,
                CarrierConfigValuesLoader.CONFIG_MAX_SUBJECT_LENGTH_DEFAULT);
    }

    public boolean getGroupMmsEnabled() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_ENABLE_GROUP_MMS,
                CarrierConfigValuesLoader.CONFIG_ENABLE_GROUP_MMS_DEFAULT);
    }

    public boolean getSupportMmsContentDisposition() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION,
                CarrierConfigValuesLoader.CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION_DEFAULT);
    }

    public boolean getShowCellBroadcast() {
        return mValues.getBoolean(CarrierConfigValuesLoader.CONFIG_CELL_BROADCAST_APP_LINKS,
                CarrierConfigValuesLoader.CONFIG_CELL_BROADCAST_APP_LINKS_DEFAULT);
    }

    // sprd for smsc begin
    public boolean getSmscShowEnabled() {
        return true;
    }

    // sprd for smsc end

    /*modify by SPRD for Bug:533513  2016.03.10 Start */
    /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
    public static boolean getSignatureEnabled() {
        //return true;
        final BuglePrefs prefs = Factory.get().getApplicationPrefs();
        return prefs.getBoolean(KEY_SP_SINATURE,false);
    }
    /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */

    public static void setSignatureEnable(boolean signatureEnable) {
        final BuglePrefs prefs = Factory.get().getApplicationPrefs();
        prefs.putBoolean(KEY_SP_SINATURE, signatureEnable);
    }
    /*modify by SPRD for Bug:533513  2016.03.10 End */

    /*Add by SPRD for Bug:562203 Encode Type feature  Start */
    public static String getEncodeType() {
        final BuglePrefs prefs = Factory.get().getApplicationPrefs();
        return prefs.getString(KEY_SP_ENCODETYPE, "0");
    }

    public static void setEncodeType(String encode) {
        final BuglePrefs prefs = Factory.get().getApplicationPrefs();
        prefs.putString(KEY_SP_ENCODETYPE, encode);
    }

    public static int[] calculateLength(CharSequence text) {
        int count = 0;
        int[] params = new int[2];
        int encodeType = Integer.parseInt(getEncodeType());
        if (encodeType == 1) {// 7bit coding
            for (int i = 0; i < text.length(); i++) {
                try {
                    count += GsmAlphabet.countGsmSeptets(text.charAt(i), false);
                } catch (/*Encode*/Exception ex) {
                    // this should never happen
                }
            }
            if (count > SmsMessage.MAX_USER_DATA_SEPTETS) {
                params[0] = (count + (SmsMessage.MAX_USER_DATA_SEPTETS_WITH_HEADER - 1))
                    / SmsMessage.MAX_USER_DATA_SEPTETS_WITH_HEADER;
                params[1] = (params[0] * SmsMessage.MAX_USER_DATA_SEPTETS_WITH_HEADER)
                    - count;
            } else {
                params[0] = 1;
                params[1] = SmsMessage.MAX_USER_DATA_SEPTETS - count;
            }
        } else if (encodeType == 3) {// 16bit coding
            count = text.length() * 2;
            if (count > SmsMessage.MAX_USER_DATA_BYTES) {
                params[0] = (count + (SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER - 1))
                    / SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER;
                params[1] = ((params[0] * SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER) - count) / 2;
            } else {
                params[0] = 1;
                params[1] = (SmsMessage.MAX_USER_DATA_BYTES - count) / 2;
            }
        }
        return params;
    }
    /*Add by SPRD for Bug:562203 Encode Type feature  End */

    public Object getValue(final String key) {
        return mValues.get(key);
    }

    public Set<String> keySet() {
        return mValues.keySet();
    }

    public static String getKeyType(final String key) {
        return sKeyTypeMap.get(key);
    }

    public void update(final String type, final String key, final String value) {
        BugleCarrierConfigValuesLoader.update(mValues, type, key, value);
    }

    /* SPRD: add for Bug 498629 begin */
    public static boolean getBeepOnCallStateEnabled() {
        return false;
    }
    /* SPRD: add for Bug 498629 end */

    /* SPRD: Modify for bug 509830 begin */
    public static boolean getKeepOrgSoundVibrate() {
        return false;
    }
    /* SPRD: Modify for bug 509830 end */

    public boolean getSMSRetryTimesEnabled() {
        return mValues
                .getBoolean(
                        CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_RETRY_TIMES,
                        CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_RETRY_TIMES_DEFAULT);
    }

    /**
     * Return the largest MaxTxtFileSize for any subid
     */
    public static int getMaxMaxTxtFileSize() {
        int maxMax = 0;
        for (MmsConfig config : sSubIdToMmsConfigMap.values()) {
            maxMax = Math.max(maxMax, config.getMaxTxtFileSize());
        }
        return maxMax > 0 ? maxMax : sFallback.getMaxTxtFileSize();
    }

    public int getMaxTxtFileSize() {
        return mValues.getInt(
                CarrierConfigValuesLoader.CONFIG_MAX_TXT_FILE_SIZE,
                CarrierConfigValuesLoader.CONFIG_MAX_TXT_FILE_SIZE_DEFAULT);
    }

    public static boolean getValiditySmsEnabled() {
        return mEnabledSmsValidity;
    }

    public static boolean getValidityMmsEnabled() {
        return mEnabledMmsValidity;
    }

    /* And by SPRD for Bug:530742 2016.02.02 Start */
    public int getSharedImageLimit() {
        final int limit = mValues.getInt(CarrierConfigValuesLoader.CONFIG_SHARED_IMAGE_LIMIT,
                CarrierConfigValuesLoader.CONFIG_SHARED_IMAGE_LIMIT_DEFAULT);
        return limit < 0 ? Integer.MAX_VALUE : limit;
    }
    /* And by SPRD for Bug:530742 2016.02.02 Start */

    /* Sprd add for sms merge forward begin */
    public int getSMSMergeForwardMaxItems() {
        return mValues
                .getInt(
                        CarrierConfigValuesLoader.CONFIG_SMS_MERGE_FORWARD_MAX_TIMES,
                        CarrierConfigValuesLoader.CONFIG_SMS_MERGE_FORWARD_MAX_DEFAULT);
    }

    public int getSMSMergeForwardMinItems() {
        return mValues
                .getInt(
                        CarrierConfigValuesLoader.CONFIG_SMS_MERGE_FORWARD_MIN_TIMES,
                        CarrierConfigValuesLoader.CONFIG_SMS_MERGE_FORWARD_MIN_DEFAULT);
    }
    /* Sprd add for sms merge forward end */
    // sprd #542214 start
    public boolean getSmsSaveSimEnabled() {
        return mValues
                .getBoolean(
                        CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_SAVE_TO_SIM,
                        CarrierConfigValuesLoader.CONFIG_ENABLE_SMS_SAVE_TO_SIM_DEFAULT);
    }

    // sprd #542214 end

    // sprd 554003 start 2016/4/21
    public static HashMap<String, String> mHashmap = new HashMap<String, String>();

    public void setSmsModemStorage(String szSubId, String smsModemStorage) {
        mHashmap.put(szSubId, smsModemStorage);
    }

    public String getSmsModemStorage(String szSubid) {
        if (mHashmap.containsKey(szSubid)) {
            return mHashmap.get(szSubid);
        } else {
            return "";
        }
    }
    // sprd 554003 end 2016/4/21
}
