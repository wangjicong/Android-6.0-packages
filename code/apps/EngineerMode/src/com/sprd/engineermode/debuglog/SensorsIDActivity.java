
package com.sprd.engineermode.debuglog;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import com.sprd.engineermode.R;
import com.sprd.engineermode.engconstents;

public class SensorsIDActivity extends Activity {

    private static final String TAG = "SensorsIDActivity";
    
    private static final String LCD_ID = "sys/module/sprd_phinfo/parameters/SPRD_LCDInfo";
    private static final String TP_VERSION = "sys/module/sprd_phinfo/parameters/SPRD_TPInfo";
    private static final String GSENSOR = "sys/module/sprd_phinfo/parameters/SPRD_GsensorInfo";
    private static final String LSENSOR = "sys/module/sprd_phinfo/parameters/SPRD_LsensorInfo";

    private TextView mCameraID, mLcdID, mTpVersion, mGsensorID, mLsensorID ,mCameraTitle, mLcdTitle, mTpTitle, mGsensorTitle, mLsensorTitle;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors_id);
        
        mCameraID = (TextView) findViewById(R.id.camera_id);
        mLcdID = (TextView) findViewById(R.id.lcd_id);
        mTpVersion = (TextView) findViewById(R.id.tp_version);
        mGsensorID = (TextView) findViewById(R.id.gsensor_id);
        mLsensorID = (TextView) findViewById(R.id.lsensor_id);
        
        mCameraTitle = (TextView) findViewById(R.id.camera_id_title);
        mLcdTitle = (TextView) findViewById(R.id.lcd_id_title);
        mTpTitle = (TextView) findViewById(R.id.tp_version_title);
        mGsensorTitle = (TextView) findViewById(R.id.gsensor_id_title);
        mLsensorTitle = (TextView) findViewById(R.id.lsensor_id_title);
        
        mCameraID.setText("back camera id:" + getBackCameraID() + "\n" + "front camera id:" + getFrontCameraID());
        mLcdID.setText(getLcdID());
        mTpVersion.setText(getTpVersion());
        mGsensorID.setText(getGsensorID());
        //mLsensorID.setText(getLsensorID());
        if(true){
            mLsensorID.setVisibility(View.GONE);
            mLsensorTitle.setVisibility(View.GONE);
        }
    }

	private String getLsensorID() {
        return getFileValue(LSENSOR);
	}

	private String getGsensorID() {
        return getFileValue(GSENSOR);
	}
	private String getTpVersion() {
        return getFileValue(TP_VERSION);
    }

	private String getLcdID() {
        return getFileValue(LCD_ID);
	}
	
    private String getFileValue(String path) {
        BufferedReader bReader = null;
        StringBuffer sBuffer = new StringBuffer();

        try {
            FileInputStream fi = new FileInputStream(path);
            bReader = new BufferedReader(new InputStreamReader(fi));
            String str = bReader.readLine();

            while (str != null) {
                sBuffer.append(str + "\n");
                str = bReader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sBuffer.toString();
    }

    private native String getBackCameraID();
    private native String getFrontCameraID();

    static {
        System.loadLibrary("jni_engineermode");
    }

}
