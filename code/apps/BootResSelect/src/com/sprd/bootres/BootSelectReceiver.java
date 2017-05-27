package com.sprd.bootres;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import com.sprd.android.config.OptConfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;//SUN:jicong.wang add 

public class BootSelectReceiver extends BroadcastReceiver {
	private static final String TAG = "BootSelectReceiver";
	public static String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

	@Override
	public void onReceive(Context arg0, Intent arg1) {
			String command="";
			if(arg1.getAction().equals(SECRET_CODE_ACTION)){
         command = arg1.getData().toString();
			}
			if(arg1.getAction().equals(SECRET_CODE_ACTION)&& command.equals("android_secret_code://123321") && SystemProperties.getInt("ro.SUN_MULTI_POWERONOFF_NUM",1)>1) {//SUN:jicong.wang add for bug 54549
			Intent intent = new Intent();
			intent.setClass(arg0, BootResSelectActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			arg0.startActivity(intent);
		}
	}
}
