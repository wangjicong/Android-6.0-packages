package com.sprd.validationtools;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

//import com.sprd.internal.telephony.IAtChannel;
import android.telephony.TelephonyManager;

public class IATUtils {
    private static final String TAG = "IATUtils";
    //private static IAtChannel mAtChannel = null;
    private static String strTmp = null;
    public static String AT_FAIL = "AT FAILED";
    public static String AT_OK = "OK";
    public static int mPhoneCount = 0;
/*
    public static IAtChannel getAtChannel(String serverName) {
        return mAtChannel = IAtChannel.Stub.asInterface(ServiceManager
                .getService(serverName));
    }
    */

    public static String sendATCmd(String cmd, String serverName) {
        try {
            String result = sendAtCmd(cmd);
            /*
            mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
            if (mPhoneCount == 1) {
                serverName = "atchannel";
            }
            strTmp = "error service can't get";
            mAtChannel = IAtChannel.Stub.asInterface(ServiceManager
                    .getService(serverName));
            if(serverName.contains("atchannel0")){
                Log.d(TAG, "<0> mAtChannel = " + mAtChannel + " , and cmd = " + cmd);
            }else if(serverName.contains("atchannel1")){
                Log.d(TAG, "<1> mAtChannel = " + mAtChannel + " , and cmd = " + cmd);
            }else{
                Log.d(TAG, "<?> mAtChannel = " + mAtChannel + " , and cmd = " + cmd);
            }
            if (mAtChannel == null) {
                Log.d(TAG, "error atchannel=null");
                strTmp = "error service can't get";
            } else {
                strTmp = mAtChannel.sendAt(cmd);
                if(serverName.contains("atchannel0")){
                    Log.d(TAG, "<0> AT response " + strTmp);
                }else if(serverName.contains("atchannel1")){
                    Log.d(TAG, "<1> AT response " + strTmp);  
                }else{
                    Log.d(TAG, "<?> AT response " + strTmp);
                }
            }
            return strTmp;
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            return strTmp;
        }*/
            return result;
        }catch (Exception e){
            Log.d("AT Exception",e.toString());
        }
        return "error service can't get";
    }

    public static String sendAtCmd(String cmd){
        String[] result = new String[1];
        String[] command = new String[1];
        command[0] = cmd;
        TelephonyManager mTelephonyManager = TelephonyManager.getDefault();
        if (mTelephonyManager.invokeOemRilRequestStrings(0,command,result) >= 0) {
            Log.d(TAG, "result = " + result[0]);
        } else {
           result[0] = IATUtils.AT_FAIL;
        }
	    Log.d(TAG, "result = " + result[0]);
        return result[0];
    }
}
