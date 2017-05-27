/*
 * Copyright (C) 2006 The Android Open Source Project
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
  /*billy _modify_20140723 -----edit
 */

package com.android.incallui;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.os.SystemProperties;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.sprd.android.config.OptConfig;

public class IncallFlash {
    private static final String TAG = "IncallFlash";
    private static final int SLEEP_TIME = (OptConfig.SUNVOV_S7358_HWD_COMMON) ? 800 : 150;
    private static IncallFlash sInstance;
    private Camera mCamera = null;
    private Camera.Parameters mParameters = null;  
    private  boolean isFlashOpen=false;
    private  boolean isThreadRuning=false;
    private  boolean isVideoIncomingCall=false;
    public static  SurfaceTexture gSurfaceTexture=new SurfaceTexture(0);
    public static IncallFlash init() {
        synchronized (IncallFlash.class) {
            if (sInstance == null) {
                sInstance = new IncallFlash();
            } else {
                Log.v(TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }
    private IncallFlash() {
        /*do nothing*/
    }
    
    
    public void OpenLightOn(){    
    	Log.v("dlj","dlj OpenLightOn()");
    if(!SystemProperties.getBoolean("persist.sys.incallflash",false))return;
   
		if(isVideoIncomingCall) return;
				
        if (isThreadRuning==false){
            isThreadRuning=true;
            Log.v("dlj", "dlj OpenLightOn");
            handler.post(startThread);
        }
    }
    
    public void CloseLightOff(){
    	if(!SystemProperties.getBoolean("persist.sys.incallflash",false))return;
        try
        {
        	Log.v("dlj", "dlj CloseLightOff");
            destroyAll();
        }catch (Exception localIOException){
            Log.v(TAG,"IncallFlash--CloseLightOff"+localIOException);
        }
    }

    private void flashopen() {
    	Log.e("xuhui", "flashopen");
        try
        {
            if(mCamera==null){
                mCamera = Camera.open(); 
            }
            if (null == mParameters){
                mParameters = mCamera.getParameters();
            }	
            isFlashOpen=true;
            mCamera.setPreviewTexture(gSurfaceTexture);
            mCamera.startPreview();
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParameters);
        }catch (Exception localIOException){
            Log.v(TAG,"IncallFlash--flashopen"+localIOException);
            isFlashOpen=false;
            isThreadRuning=false;
            mCamera=null;
        }
    }  


    private void flashclose() {
        try
        {
            if(mCamera==null){
                mCamera = mCamera.open(); 
            }
            if (null == mParameters){
                mParameters = mCamera.getParameters();
            }	
            isFlashOpen=false;
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParameters);
        }catch (Exception localIOException){
            Log.v(TAG,"IncallFlash--flashclose"+localIOException); 
            isFlashOpen=false;
            isThreadRuning=false;
            mCamera=null;
        }
    }

	
    Runnable startThread = new Runnable(){  
        public void run(){  
            try {
                if (isThreadRuning==false){
                    destroyAll();
                    return;
                }else{
                    if (isFlashOpen){
                        flashclose();
                    }else{
                        flashopen();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*SUN:jicong.wang add for flash time start {@*/
            if (Integer.parseInt(SystemProperties.get("ro.SUN_INCALL_FLASH_TIME", "150"))>0){
                handler.postDelayed(startThread, Long.parseLong(SystemProperties.get("ro.SUN_INCALL_FLASH_TIME", "150")));
            } else {
                handler.postDelayed(startThread, SLEEP_TIME);
            }
            /*SUN:jicong.wang add for flash time end @}*/
        }
    }; 
    
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private void destroyAll(){
        if (handler!=null && startThread!=null){
            handler.removeCallbacks(startThread);
        }
        
        if (isFlashOpen==true){
            flashclose();
        }
        
        if (mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
        isThreadRuning=false;
        isVideoIncomingCall=false;
    }

    public void setVideoIncomingCall(boolean flag){
        isVideoIncomingCall=flag;
    }
   
}
