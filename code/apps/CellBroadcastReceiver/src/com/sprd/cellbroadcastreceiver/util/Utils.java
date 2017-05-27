package com.sprd.cellbroadcastreceiver.util;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.ISub;
import com.android.internal.telephony.SubscriptionController;
import com.sprd.cellbroadcastreceiver.provider.CommonSettingTableDefine;
import com.sprd.cellbroadcastreceiver.util.SystemProperties;
import com.sprd.android.config.OptConfig;


public class Utils {

    public final static boolean DEBUG           = true;

    public static int DISABLE_CHANNEL           = 1;
    public static int OPERATION_ADD             = 2;
    public static int OPERATION_EDIT            = 3;
    public static int OPERATION_DEL             = 4;
    public static int SET_CHANNEL               = 5;
    public static int SET_LANGUAGE              = 6;
    public static int PADDING                   = 0xffff;

    public static String TAG                    = "Utils";
    public static String SETTING_TYPE           = "setting_type";
    public static String CHANNEL_SETTING        = "channel_setting";
    public static String LANGUAGE_SETTING       = "language_setting";

    public static String MATCHED                = "matched";
    public static String OPERATION              = "operation";
    public static String INDEXOFARRAY           = "indexOfArray";
    public static String DEFAULT_RINGTONE       = Settings.System.DEFAULT_NOTIFICATION_URI.toString();

    public static Uri mLangUri                  = Uri.parse("content://cellbroadcasts/lang_map");
    public static Uri mChannelUri               = Uri.parse("content://cellbroadcasts/channel");
    public static Uri mViewLangUri              = Uri.parse("content://cellbroadcasts/view_lang");
    public static Uri mCommonSettingUri         = Uri.parse("content://cellbroadcasts/common_setting");

    //used subid
    public static boolean USE_SUBID             = isUseSubId();//SystemProperties.get("use_subid", true);
    //the ringtone depend on channel or slot
    public static boolean DEPEND_ON_SLOT         = isDependOnSlot();//SystemProperties.get("depend_on_sim", false);

    private static boolean isUseSubId(){
        if (SystemProperties.get("ro.cb_config") == null || SystemProperties.get("ro.cb_config").length()<1) {
            return true;//true
        } else {
            return (Integer.parseInt(SystemProperties.get("ro.cb_config")) & 0x01)==0;
        }
    }

    private static boolean isDependOnSlot(){
        if (SystemProperties.get("ro.cb_config") == null || SystemProperties.get("ro.cb_config").length()<1) {
            return false;//false
        } else {
            Log.d(TAG, "ro.cb_config of SystemProperties is:"+ SystemProperties.get("ro.cb_config"));
            return (Integer.parseInt(SystemProperties.get("ro.cb_config")) & 0x02)!=0;
        }
    }

    public static boolean hasActiveSim(Context context) {
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        int phoneCount = subInfoList!=null ? subInfoList.size():0;//added for coverity 107975
        Log.d(TAG, "--check the active sim, phoneCount is:"+ phoneCount);
        if (phoneCount >= 1) {
            return true;
        } else {
            return false;
        }
    }

    public static int tanslatePhoneIdToSubId(Context context, int phoneId){
        if (!(phoneId >= 0 && phoneId < TelephonyManager.getDefault().getSimCount())) {
            Log.d(TAG, "this phoneId:"+phoneId+" is invalid.");
            return -1;
        }
        int[] subId = null;
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                subId = iSub.getSubId(phoneId);
            }
        } catch (RemoteException ex) {
            // ignore it
        }

        return subId[0];
    }

    public static int tanslateSubIdToPhoneId(Context context, int subId){
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        int phoneId = tm.getPhoneId(subId);
        return phoneId;
    }

    public static boolean sendATCmd(Context context, int channel_id, int enabled, int subId, int type){
        int sub_id;
        if (USE_SUBID) {
            sub_id = subId;
        } else {
            sub_id = tanslatePhoneIdToSubId(context, subId);
        }
        SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(sub_id);
        if(enabled == 1 && type==SET_CHANNEL){
            Log.d(TAG, "Send ATCommand. Enabled channel:"+ channel_id);
            return manager.enableCellBroadcastRange(channel_id, channel_id, SmsManager.CELL_BROADCAST_RAN_TYPE_GSM);
        } else {
            return sendCustomAT(sub_id, getConfigArray(channel_id, enabled, type));
        }
    }

    private static int[] getConfigArray(int channel_id, int enabled, int type){
        int[] channelConfig = new int[5];
        if (type == SET_CHANNEL) {
            Log.d(TAG, "Send ATCommand. Disabled channel:"+ channel_id);
            channelConfig[0] = channel_id;
            channelConfig[1] = channel_id;
            channelConfig[2] = PADDING;
            channelConfig[3] = PADDING;
            channelConfig[4] = 1;
        } else {//if type is SET_LANGUAGE, channel_id means language_id
            Log.d(TAG, "Send ATCommand. Language:"+ channel_id+" enabled:"+enabled);
            channelConfig[0] = PADDING;
            channelConfig[1] = PADDING;
            channelConfig[2] = channel_id;
            channelConfig[3] = channel_id;
            channelConfig[4] = enabled==1?0:1;
        }
        return channelConfig;
    }

    public static boolean sendLangAT(Context context, ArrayList<Integer> languageIds, int subId, boolean enable){
        int sub_id;
        if (USE_SUBID) {
            sub_id = subId;
        } else {
            sub_id = tanslatePhoneIdToSubId(context, subId);
        }
        
        return sendLangAT(languageIds, subId, enable);
    }

    public static boolean sendLangAT(ArrayList<Integer> languageIds, int sub_id, boolean enable) {

        int[] langConfig = new int[/*languageIds.size() */ 5];
        //int j = 0;
        for (int i = 0; i < languageIds.size(); i ++) {
            langConfig[0] = PADDING;
            langConfig[1] = PADDING;
            langConfig[2] = languageIds.get(i);
            langConfig[3] = languageIds.get(i);
            langConfig[4] = enable ? 0 : 1;
            //j++;
            sendCustomAT(sub_id, langConfig);
        }

        return true;
    }

    private static boolean sendCustomAT(int sub_id, int[] data) {
        boolean success = false;
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms != null) {
                success = iccISms.commonInterfaceForMessaging(DISABLE_CHANNEL,
                        sub_id, null, data);
            }
        } catch (RemoteException ex) {
            // ignore it
        }
        return success;
    }

    public static boolean queryEnabledCheckBox(Context context, int subId) {
        String[] select_column = {
            CommonSettingTableDefine.ENABLED_CELLBROADCAST
        };
        Log.d(TAG, "the subId send in is:" + subId);
        int sub_id;
        if (!USE_SUBID) {
            sub_id = Utils.tanslateSubIdToPhoneId(context, subId);
        } else {
            sub_id = subId;
        }
        Cursor cursor = context.getContentResolver().query(mCommonSettingUri,
                select_column, CommonSettingTableDefine.SUBID + "=" + sub_id,
                null, null);
        Log.d(TAG, "---queryEnabledCheckBox---subId=" + sub_id
                + " and cursor.getcount=" + cursor.getCount());
        if (cursor == null || cursor.getCount() == 0) {
            ContentValues cv = new ContentValues(2);
            cv.put(CommonSettingTableDefine.SUBID, sub_id);
            if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){//wangxing add
                cv.put(CommonSettingTableDefine.ENABLED_CELLBROADCAST, 0);
            }else{
                cv.put(CommonSettingTableDefine.ENABLED_CELLBROADCAST, 1);
            }

            context.getContentResolver().insert(mCommonSettingUri, cv);
            if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){//wangxing add
                return false;
            }else{
                return true;
            }
        }   
        cursor.moveToFirst();
        int index = cursor
                .getColumnIndex(CommonSettingTableDefine.ENABLED_CELLBROADCAST);
        int enabled = cursor.getInt(index);
        return enabled == 1 ? true : false;
    }
}
