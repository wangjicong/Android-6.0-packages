
package com.spreadst.validator;

import java.io.File;
import java.lang.reflect.Method;
//import src.com.sprd.engineermode.String;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioFeatures;
import android.telephony.TelephonyManager.RadioCapbility;

import com.android.internal.telephony.TelephonyIntents;

import android.os.ServiceManager;
import com.spreadst.validator.R;
import android.os.SystemProperties;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

public class ValidateService extends Service {
    private static final String TAG = "Validator_ValidateService";

    public static final String ID = "id";

    public static final String TEST_CASE = "case";

    public static final String RE_TEST = "re_test";

    public static final int BUTTON_OK = 0;

    public static final int BUTTON_CANCEL = 1;

    public static final int BUTTON_NEXT = 2;

    public static final int BUTTON_RETRY = 3;

    public static final int BUTTON_FAILED = 4;

    public static final int BUTTON_NO = 5;

    public static final int LAUNCHER_ON_TOP = 8;

    public static final int BOOT_COMPLETED = 9;

    public static final int USER_START_APK = 10;

    public static final int STOP_RECEIVER = 11;

    public static final int APP_NULL = 0;

    public static final int APP_VIDEO = 1;

    public static final int APP_CAM_DC = 2;

    public static final int APP_CAM_DV = 3;

    public static final int APP_BENCHMARK = 4;

    public static final int APP_VOICE_CYCLE = 5;

    public static final int APP_TEL_TD = 7;

    public static final int APP_TEL_GSM = 6;

    public static final int APP_TEL_LTE = 8;

    public static final int APP_BT = 9;

    public static final int APP_WIFI = 10;

    public static final int APP_RESULT = 11;

    public static final int APP_END = 12;

    private static final String COMMAND_SU = "su";

    private static final String COMMAND_MONKEY_RUN = "monkey -f %s 1";

    private static final String SCRIPT_DC = "cam_dc";

    private static final String SCRIPT_DV = "cam_dv";

    private static final String SCRIPT_BENCHMARK = "benchmark";

    private boolean mRunning = false;

    public static final String HOME_LOAD_COMPLETED = "HOME_LOAD_COMPLETED";

    public static final String CURRENT_APP_IS_EXIT = "CURRENT_APP_IS_EXIT";

    public static final int NT_MODE_TD_SCDMA_ONLY = 15;

    public static final int NT_MODE_GSM_ONLY = 13;

    private static final int NT_MODE_WCDMA_ONLY = 14;

    private static final int NT_MODE_LTE_ONLY = 16;

    private static final int RE_SET_MODE = 17;

    private int mSimindex = 0;

    private int mSubID = -1;

    private String atCmd;

    private static final String ENG_AT_NETMODE  = "AT^SYSCONFIG=";

    private static final String RE_SET_MODE_AT = "AT+RESET=1";

    private ITelephony mITelephony = null; 

    private TelephonyManager mTelephonyManager = null;

    //private IAtChannel mAtChannel = null;

    private static final String SERVER_NAME = "atchannel";

    private RadioFeatures mSetRadioFeature = null;
    
    private IATUtils iATUtils;
    
    private String serverName;

    private boolean[] mHasSelectTestCase;

    private static final int WIFI_ACTIVITY = 0;

    private static final int BT_ACTIVITY = 1;

    private static final int PING_ACTIVITY = 2;

    private static final int RESULT_ACTIVITY = 3;

    private boolean mStopReceiver = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "MyReceiver onReceive intent:" + intent);
            Log.d(TAG, "MyReceiver onReceive action:" + intent.getAction());

            if (CURRENT_APP_IS_EXIT.equals(intent.getAction())) {
                Intent it = new Intent(context, ValidateService.class);
                it.putExtra(ID, LAUNCHER_ON_TOP);
                context.startService(it);
            }
        }
    };

    private int mCurrentApp = APP_NULL;

    private Process mProcess = null;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d(TAG, "xixi ");
        Log.d(TAG, "onStartCommand intent:" + intent);
        int id = intent.getIntExtra(ID, -1);
        if (id == BUTTON_OK) {
            mHasSelectTestCase = intent.getBooleanArrayExtra(TEST_CASE);
        }
        boolean reTest = intent.getBooleanExtra(RE_TEST, false);
        if (reTest) {
            mCurrentApp = APP_NULL;
        }
        processRequest(id);
        return START_STICKY;
        }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter("CURRENT_APP_IS_EXIT");
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    long timeBefore = 0;

    private void processRequest(int id) {
        Log.i(TAG, "processRequest id= " + id);
        switch (id) {
            case BUTTON_CANCEL:
                validateCancel();
                break;
            case BUTTON_FAILED:
            case BUTTON_NO:
                validateComplete();
                break;
            case BUTTON_OK:
            case BUTTON_NEXT:
                try {
                    if (mProcess != null) {
                        mProcess.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCurrentApp++;
                runApp();
                break;
            case LAUNCHER_ON_TOP:
                // user may press back or home key when in testing
                // so we should kill monkey process and show dialog to user
                // judge 2000s, because UIDialog may exit before the next app be
                // shown
                if (System.currentTimeMillis() - timeBefore > 2000 && mCurrentApp != (APP_END - 1) && !mStopReceiver) {
                    Log.d(TAG, "processRequest timeBefore > 2000");
                    try {
                        if (mProcess != null) {
                            mProcess.destroy();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (mCurrentApp == APP_NULL) {
                        showDialog(UIDialog.DIALOG_BEGIN);
                    } else {
                        showDialog(UIDialog.DIALOG_CONFIRM);
                    }
                } else
                    Log.d(TAG, "processRequest timeBefore <= 2000, so passby");
                break;
            case BUTTON_RETRY:
                runApp();
                break;
            case BOOT_COMPLETED:
                SharedPreferences sltValue = getSharedPreferences(UIDialog.SLT_VALUE, Context.MODE_PRIVATE);
                boolean bootStart = sltValue.getBoolean(UIDialog.BOOT_COMPLETE_START, false);
                if (bootStart) {
                    showDialog(UIDialog.DIALOG_BEGIN);
                } else {
                    showDialog(UIDialog.DIALOG_FINISH);
                }
                break;
            case USER_START_APK:
                if (mCurrentApp == APP_NULL || mCurrentApp == APP_END) {
                    showDialog(UIDialog.DIALOG_BEGIN);
                } else {
                    showDialog(UIDialog.DIALOG_CONFIRM);
                }
                break;
            case STOP_RECEIVER:
                stopReceiver();
                break;
            default:
                break;
        }
        timeBefore = System.currentTimeMillis();
    }

    private void runApp() {
        Log.i(TAG, "runApp mCurrentApp =" + mCurrentApp);
        mRunning = true;
        switch (mCurrentApp) {
            case APP_BENCHMARK:
                if (mHasSelectTestCase[UIDialog.BENCHMARK]) {
                    runMonkeyScriptThread(SCRIPT_BENCHMARK);
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_CAM_DC:
                if (mHasSelectTestCase[UIDialog.CAM_DC]) {
                    runMonkeyScriptThread(SCRIPT_DC);
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_CAM_DV:
                if (mHasSelectTestCase[UIDialog.CAM_DV]) {
                    runMonkeyScriptThread(SCRIPT_DV);
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_END:
                validateComplete();
                break;
            case APP_TEL_GSM:
                // Set the modem network mode
            	
            	Log.d(TAG, "case APP_TEL_GSM ");
                if (mHasSelectTestCase[UIDialog.TEL_GSM]) {
                    if (!hasSimCard()) {
                        return;
                    }
                    if (!isSupportLte()) {
                        Log.d(TAG, "case APP_TEL_GSM  !isSupportLte");
                        atCmd = ENG_AT_NETMODE + "13,3,2,4";
                        String atRsp = iATUtils.sendATCmd(atCmd, "atchannel" + mSimindex);
                        Log.d(TAG,"atRsp:" + atRsp);
                        if (atRsp.contains(iATUtils.AT_OK)) {
                            Log.d(TAG, "case APP_TEL_GSM !isSupportLte  switch network success : " );
                            Toast.makeText(this, getResources().getString(R.string.set_feature_success),
                                  Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            Log.d(TAG, "case APP_TEL_GSM !isSupportLte  switch network fail : " );
                            Toast.makeText(this, getResources().getString(R.string.switch_modem_fail),
                                  Toast.LENGTH_LONG).show();
                            return;
                        }

//                        if (!switchTeleNetwork(Phone.NT_MODE_GSM_ONLY)) {
//                            Log.d(TAG, "!switchTeleNetwork(Phone.NT_MODE_GSM_ONLY):"+(!switchTeleNetwork(Phone.NT_MODE_GSM_ONLY)));
//                            Log.d(TAG, "case APP_TEL_GSM !isSupportLte  switch network fail : " + Phone.NT_MODE_GSM_ONLY);
//                            Toast.makeText(this, getResources().getString(R.string.switch_modem_fail),
//                                    Toast.LENGTH_LONG).show();
//                            return;
//                        }
                    } else {
                        mSetRadioFeature = selectRadioFeatures(NT_MODE_GSM_ONLY);
                        Log.d(TAG, "case APP_TEL_GSM !isSupportLte  NT_MODE_GSM_ONLY " + NT_MODE_GSM_ONLY);
                        resetModem(false);
                    }
                    startActivity(Properties.getDialorIntent());
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_TEL_TD:
                // Set the modem network mode
            	
            	Log.d(TAG, "case APP_TEL_TD ");
                if (mHasSelectTestCase[UIDialog.TEL_TD]) {
                    if (!hasSimCard()) {
                        return;
                    }
                    if (!isSupportLte()) {
                        Log.d(TAG, "case APP_TEL_TD  !isSupportLte");
                        atCmd = ENG_AT_NETMODE + "14,3,2,4";
                        String atRsp = iATUtils.sendATCmd(atCmd, "atchannel" + mSimindex);
                        Log.d(TAG,"atRsp:" + atRsp);
                        if (atRsp.contains(iATUtils.AT_OK)) {
                            Log.d(TAG, "case APP_TEL_TD !isSupportLte  switch network success : " );
                            Toast.makeText(this, getResources().getString(R.string.set_feature_success),
                                  Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            Log.d(TAG, "case APP_TEL_TD !isSupportLte  switch network fail : " );
                            Toast.makeText(this, getResources().getString(R.string.switch_modem_fail),
                                  Toast.LENGTH_LONG).show();
                            return;
                        }
//                        if (!switchTeleNetwork(Phone.NT_MODE_WCDMA_ONLY)) {
//                        	Log.d(TAG, "case APP_TEL_TD !isSupportLte  switch network fail : " + Phone.NT_MODE_WCDMA_ONLY);
//                            Toast.makeText(this, getResources().getString(R.string.switch_modem_fail),
//                                    Toast.LENGTH_LONG).show();
//                            return;
//                        }
                    } else {
                        if (isCmcc()) {
                            Log.d(TAG, "case APP_TEL_TD isCmcc NT_MODE_TD_SCDMA_ONLY ");
                            mSetRadioFeature = selectRadioFeatures(NT_MODE_TD_SCDMA_ONLY);
                            resetModem(false);
                        } else {
                            mSetRadioFeature = selectRadioFeatures(NT_MODE_WCDMA_ONLY);
                            Log.d(TAG, "case APP_TEL_TD !isCmcc NT_MODE_WCDMA_ONLY ");
                            resetModem(false);
                        }
                    }
                    startActivity(Properties.getDialorIntent());
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_VIDEO:
                if (mHasSelectTestCase[UIDialog.VIDEO_TEST]) {
                    startActivity(Properties.getVedioIntent());
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_VOICE_CYCLE:
                if (mHasSelectTestCase[UIDialog.VOICE_CYCLE]) {
                    startActivity(Properties.getVoiceCircleIntent());
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_WIFI:
                if (mHasSelectTestCase[UIDialog.WIFI]) {
                    startTestActivity(WIFI_ACTIVITY);
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_BT:
                if (mHasSelectTestCase[UIDialog.BT]) {
                    startTestActivity(BT_ACTIVITY);
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_TEL_LTE:
                if (mHasSelectTestCase[UIDialog.TEL_LTE]) {
                    if (!hasSimCard()) {
                        return;
                    }
                    if (isSupportLte()) {
                        mSetRadioFeature = selectRadioFeatures(NT_MODE_LTE_ONLY);
                        resetModem(true);
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.has_no_lte),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    startTestActivity(PING_ACTIVITY);
                } else {
                    mCurrentApp++;
                    runApp();
                }
                break;
            case APP_RESULT:
                startTestActivity(RESULT_ACTIVITY);
                break;
            default:
                break;
        }
    }

    private boolean runMonkeyScript(String scripFile) {
        try {
            Log.d(TAG, "execMonkey enter");
            mProcess = new ProcessBuilder()
                    .command("monkey", "-f", Properties.SDCARD_PATH + scripFile, "1")
                    .redirectErrorStream(true).start();

            int exitValue = mProcess.waitFor();
            Log.d(TAG, "Process.waitFor() return " + exitValue);
            Log.e(TAG, "execMonkey exit");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                mProcess.destroy();
                mProcess = null;
                mRunning = false;
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        return true;
    }

    private void validateComplete() {
//        showDialog(UIDialog.DIALOG_END);
        Toast.makeText(this, getResources().getString(R.string.test_complete),
                Toast.LENGTH_LONG).show();
        stopSelf();
    }

    private void validateCancel() {
        stopSelf();
    }

    private void runMonkeyScriptThread(final String scriptName) {
        File monkeyFile = new File(Properties.SDCARD_PATH + scriptName);
        Log.d(TAG, "monkeyFile = "+monkeyFile.exists());
        if (!monkeyFile.exists()) {
            Toast.makeText(this, getResources().getString(R.string.has_no_monkey_file),
                    Toast.LENGTH_LONG).show();
            return ;
        }
        new Thread(new Runnable() {
            public void run() {
                runMonkeyScript(scriptName);
            }
        }).start();
    }

    private void showDialog(int id) {
        Log.i(TAG, "showDialog id = " + id);
        Intent i = new Intent();
        i.setClass(this.getApplicationContext(), UIDialog.class);
        i.putExtra(UIDialog.ACTION_ID, id);
        if (id == UIDialog.DIALOG_CONFIRM) {
            i.putExtra(UIDialog.CURRENT_APP, mCurrentApp);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.getApplicationContext().startActivity(i);
    }

        private void initPhone() {
        if (mITelephony != null) {
            return;
        }
        Method method;
        try {
//            method = Class.forName("android.os.ServiceManager").getMethod("getService",
//                    String.class);
//            Log.d(TAG, "method: " +method);
//            IBinder iBinder = (IBinder) method.invoke(null, new Object[] {
//                    "sprd_phone"
//            });
//            Log.d(TAG, "iBinder: " +iBinder);
            mITelephony = ITelephony.Stub.asInterface(ServiceManager
    				.getService(Context.TELEPHONY_SERVICE));
            Log.d(TAG, "mITelephony valueL: " +mITelephony);
            Log.d(TAG, "mITelephony creat successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "can not get ITelephony instance.");
            e.printStackTrace();
            mITelephony = null;
            Log.d(TAG, "mITelephony creat fail");
        }
    }

      private boolean switchTeleNetwork(int networkType) {
        initPhone();
        Log.d(TAG, "initPhone sucessfully");

        if (mITelephony == null) {
        	Log.d(TAG, "mITelephony == null");
            return false;
        }

        
        int[] subId = SubscriptionManager.getSubId(mSimindex);
        if (subId != null && subId.length > 0) {
             mSubID = subId[0];
             Log.d(TAG, "mSubID:" + mSubID);
         }else{
             Log.e(TAG, "onSettingsChange, subIds null or length 0 for mSimindex " + mSimindex);
             return false;
         }

        try {
            Log.d(TAG, "return value: " +(mITelephony.setPreferredNetworkType(mSubID,networkType)));
            return mITelephony.setPreferredNetworkType(mSubID,networkType); 
            
        } catch (RemoteException e) {
            Log.e(TAG, "switchTeleNetwork find RemoteException : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private static final Class[] mStartActivity = {
        WifiTestActivity.class,
        BluetoothTestActivity.class,
        PingActivity.class,
        TestResultActivity.class
    };

    private void startTestActivity(int index) {
        Log.d(TAG,"startWifiTestActivity, index = "+index);
        Intent i = new Intent();
        i.setClass(this.getApplicationContext(), mStartActivity[index]);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.getApplicationContext().startActivity(i);
    }

    private boolean hasSimCard() {
        Log.d(TAG,"hasSimCard founction, phone count = " +TelephonyManager.from(this).getPhoneCount());
        for (int i = 0; i < TelephonyManager.from(this).getPhoneCount(); i++) {
            if (TelephonyManager.from(this).getSimState(i) == TelephonyManager.SIM_STATE_READY) {
//            if (TelephonyManager.from(this).hasIccCard(i)) {
                mSimindex = i;
                return true;
            }
        }
        Toast.makeText(this, getResources().getString(R.string.has_no_simcard),
                Toast.LENGTH_LONG).show();
        return false;
    }

    private void resetModem(boolean lte) {
        Log.d(TAG, "resetModem, lte = " + lte);
        RadioCapbility mCurrentCapbility = TelephonyManager.getRadioCapbility();
        Log.d(TAG, "mCurrentCapbility = "+mCurrentCapbility);
        if (mCurrentCapbility == RadioCapbility.TDD_SVLTE) {
            if (switchFeatures(mSetRadioFeature)) {
            	ValidationsendATCmd();
            }
        } else {
            final RadioInteraction radioInteraction = new RadioInteraction(
                    getApplicationContext(), mSimindex);
            radioInteraction.setCallBack(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"run powerOffRadio callback...");
                    if (switchFeatures(mSetRadioFeature)) {
                        radioInteraction.powerOnRadio();
                    }
                }
            });
            radioInteraction.powerOffRadio(35000);
        }
    }
    
    public int slotIdToSubId(int phoneId) {
    	SubscriptionManager mSubscriptionManager = (SubscriptionManager) SubscriptionManager.from(this);
    	        int subId;
    	        SubscriptionInfo mSubscriptionInfo = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(phoneId);
    	        if (mSubscriptionInfo != null) {
    	            subId = mSubscriptionInfo.getSubscriptionId();
    	        } else {
    	            subId = SubscriptionManager.getDefaultVoiceSubId();
    	        }
    	        return subId;
    	     }

    private void ValidationsendATCmd() {
        serverName = getServerName(mSimindex);
        
//        mAtChannel = IAtChannel.Stub.asInterface(ServiceManager
//                .getService(serverName));
        new Thread(new Runnable() {
            @Override
            public void run() {
//                try{
                	
                	iATUtils.sendATCmd(RE_SET_MODE_AT,serverName);
//                    mAtChannel.sendAt(RE_SET_MODE_AT);
 //               } catch (RemoteException e) {
 //                   e.printStackTrace();
 //               }
            }
        }).start();
    }

    private String getServerName(int index) {
        Log.d(TAG, "getServerName, index = "+index);
        if (1 == TelephonyManager.from(this).getPhoneCount()) {
            return SERVER_NAME;
        } else {
            return SERVER_NAME + index;
        }
    }

    private boolean isCmcc() {
    
    	String modeTypestr = SystemProperties.get("ril.radio.modemtype");
    	int modeType = Integer.parseInt(modeTypestr.trim());
//    	  0：MODEM_TYPE_GSM  
//    	   1：MODEM_TYPE_TDSCDMA         
//    	   2：MODEM_TYPE_WCDMA            
//    	   3：MODEM_TYPE_LTE
    	Log.d("TAG", "modeType = " + modeType);
        if (modeType == 2) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isSupportLte() {
    	String modeTypestr = SystemProperties.get("ril.radio.modemtype");
    	Log.d("TAG", "modeTypestr = " + modeTypestr);
    	int modeType = Integer.parseInt(modeTypestr.trim());  	
//  	  0：MODEM_TYPE_GSM  
//  	   1：MODEM_TYPE_TDSCDMA         
//  	   2：MODEM_TYPE_WCDMA            
//  	   3：MODEM_TYPE_LTE
        Log.d("TAG", "modeType = " + modeType);
        if (modeType == 3) {
            return true;
        } else {
            return false;
        }
    }

    private boolean switchFeatures(RadioFeatures setRadioFeature) {
        Log.d(TAG, "switchFeatures, setRadioFeature ="+setRadioFeature);
        boolean success = false;
        String messege = null;
        mTelephonyManager = TelephonyManager.from(ValidateService.this);
        int result = mTelephonyManager.switchRadioFeatures(this, setRadioFeature);
        if (result == 0) {
            success = true;
            messege = getResources().getString(R.string.set_feature_success);
        } else {
            messege = getResources().getString(R.string.set_feature_fail);
        }
        Toast.makeText(this, messege, Toast.LENGTH_LONG).show();
        return success;
    }

    private RadioFeatures selectRadioFeatures(int mode) {
        Log.d(TAG, "selectRadioFeatures, mode ="+mode);
        RadioFeatures setRadioFeature = null;
        switch (mode) {
        case NT_MODE_GSM_ONLY:
            setRadioFeature = TelephonyManager.RadioFeatures.GSM_ONLY;
            break;
        case NT_MODE_WCDMA_ONLY:
            setRadioFeature = TelephonyManager.RadioFeatures.WCDMA_ONLY;
            break;
        case NT_MODE_TD_SCDMA_ONLY:
            setRadioFeature = TelephonyManager.RadioFeatures.TD_ONLY;
            break;
        case NT_MODE_LTE_ONLY:
            RadioCapbility mCurrentCapbility = TelephonyManager.getRadioCapbility();
            Log.d(TAG, "mCurrentCapbility = "+mCurrentCapbility);
            if (mCurrentCapbility == RadioCapbility.TDD_SVLTE) {
                setRadioFeature = TelephonyManager.RadioFeatures.SVLET;
            } else {
                setRadioFeature = TelephonyManager.RadioFeatures.TD_LTE;
            }
            break;
        }
        return setRadioFeature;
    }

    public static class RadioInteraction {
        private static final int MSG_POWER_OFF_RADIO = 1;
        private static final int MSG_POWER_OFF_ICC = 2;

        private TelephonyManager mTelephonyManager;
        private int mPhoneId;

        private volatile Looper mMsgLooper;
        private volatile MessageHandler mMsgHandler;

        private Runnable mRunnable;

        public RadioInteraction(Context context, int phoneId) {
            mPhoneId = phoneId;
            mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            /*
             * It is safer for UI than using thread. We have to {@link
             * #destroy()} the looper after quit this UI.
             */
            HandlerThread thread = new HandlerThread("RadioInteraction[" + phoneId + "]");
            thread.start();

            mMsgLooper = thread.getLooper();
            mMsgHandler = new MessageHandler(mMsgLooper);
        }

        public void setCallBack(Runnable callback) {
            mRunnable = callback;
        }

        private final class MessageHandler extends Handler {
            public MessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                Log.i(TAG, "MessageHandler handleMessage " + msg);
                int timeout = Integer.parseInt(String.valueOf(msg.obj));
                switch (msg.what) {
                    case MSG_POWER_OFF_RADIO:
                        powerOffRadioInner(timeout);
                        break;
                    case MSG_POWER_OFF_ICC:
//                        powerOffIccCardInner(timeout);
                        break;
                    default:
                        break;
                }

            }
        }

        public void destoroy() {
            mMsgLooper.quit();
        }

        /*
         * The interface of ITelephony.setRadioPower is a-synchronized handler.
         * But some case should be synchronized handler. A method to power off
         * the radio.
         */
        public void powerOffRadio(int timeout) {
            Log.i(TAG, "powerOffRadio for Phone" + mPhoneId);
            mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MSG_POWER_OFF_RADIO, timeout));
        }

        private void powerOffRadioInner(int timeout) {
            Log.i(TAG, "powerOffRadioInner for Phone" + mPhoneId);
            final long endTime = SystemClock.elapsedRealtime() + timeout;
            boolean radioOff = false;

            final ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            
            
 //           Itelephony.Stub.asInterface().isRadioOn( String callingPackage)
            try {
                radioOff = phone == null || !phone.isRadioOn("com.spreadst.validator");
                Log.w(TAG, "Powering off radio...");
                if (!radioOff) {
                    phone.setRadio(false);
                }
            } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during radio poweroff", ex);
                radioOff = true;
            }

            Log.i(TAG, "Waiting for radio poweroff...");

            while (SystemClock.elapsedRealtime() < endTime) {
                if (!radioOff) {
                    try {
                        radioOff = phone == null || !phone.isRadioOn("com.spreadst.validator");
                    } catch (RemoteException ex) {
                        Log.e(TAG, "RemoteException during radio poweroff", ex);
                        radioOff = true;
                    }
                    if (radioOff) {
                        Log.i(TAG, "Radio turned off.");
                        break;
                    }
                }
                // To give a chance for CPU scheduler
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mRunnable != null) {
                Log.i(TAG, "Run the callback.");
                mRunnable.run();
            }
        }

        /*
         * The interface of ITelephony.setIccCard is a-synchronized handler. But
         * some case should be synchronized handler. A method to power off the
         * IccCard.
         */
/*        public void powerOffIccCard(int timeout) {
            Log.i(TAG, "powerOffIccCard for Phone" + mPhoneId);
            mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MSG_POWER_OFF_ICC, timeout));
        }

        private void powerOffIccCardInner(int timeout) {
            Log.i(TAG, "powerOffIccCardInner for Phone" + mPhoneId);
            final long endTime = SystemClock.elapsedRealtime() + timeout;
            boolean IccOff = false;

            final ISprdTelephony phone = ISprdTelephony.Stub.asInterface(ServiceManager
                    .getService(SprdPhoneSupport.getServiceName(Context.SPRD_TELEPHONY_SERVICE,
                            mPhoneId)));
            try {
                IccOff = phone == null || !mTelephonyManager.hasIccCard();
                Log.w(TAG, "Powering off IccCard...");
                if (!IccOff) {
                    phone.setIccCard(false);
                }
            } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during IccCard poweroff", ex);
                IccOff = true;
            }

            Log.i(TAG, "Waiting for radio poweroff...");

            while (SystemClock.elapsedRealtime() < endTime) {
                if (!IccOff) {
                    IccOff = phone == null || !mTelephonyManager.hasIccCard();
                    if (IccOff) {
                        Log.i(TAG, "IccCard turned off.");
                        break;
                    }
                }
                // To give a chance for CPU scheduler
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SystemClock.sleep(500);
            if (mRunnable != null) {
                Log.i(TAG, "Run the callback.");
                mRunnable.run();
            }
        }
*/
        /*
         * A wrapper for interface of ITelephony.setIccCard();
         */
/*        public void powerOnIccCard() {
            Log.i(TAG, "powerOnIccCard for Phone" + mPhoneId);
            final ISprdTelephony phone = ISprdTelephony.Stub.asInterface(ServiceManager
                    .getService(SprdPhoneSupport.getServiceName(Context.SPRD_TELEPHONY_SERVICE,
                            mPhoneId)));
            try {
                Log.i(TAG, "Powering on IccCard...");
                phone.setIccCard(true);
            } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during IccCard powerOn", ex);
            }
            SystemClock.sleep(500);
        }
*/
        /*
         * A wrapper for interface of ITelephony.setRadio();
         */
        public void powerOnRadio() {
            Log.i(TAG, "powerOnRadio for Phone" + mPhoneId);
            final ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            try {
                Log.i(TAG, "Powering on radio...");
                phone.setRadio(true);
            } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during IccCard powerOn", ex);
            }
            SystemClock.sleep(500);
        }
    }

    private void stopReceiver() {
        mStopReceiver = true;
    }
}
