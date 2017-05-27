
package com.sprd.validationtools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.SystemProperties;
import android.util.Log;
import android.view.ViewConfiguration;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.Camera.CameraInfo;

import com.sprd.validationtools.itemstest.*;

import com.sprd.validationtools.itemstest.FMTest;

/*SUN:jicong.wang add for SUN_BREATHINGLIGHT start {@*/
import com.sprd.validationtools.itemstest.LedTest;
/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @}*/
import com.sprd.android.config.OptConfig;//qiuyaobo,20160712

public class Const {
    private static String TAG = "Const";

    public static boolean DEBUG = true;

    public static final String ENG_ENGTEST_DB = "/productinfo/engtest.db";
    public static final String ENG_STRING2INT_TABLE = "str2int";
    public static final String ENG_STRING2INT_NAME = "name";
    public static final String ENG_STRING2INT_VALUE = "value";
    public static final String ENG_GROUPID_VALUE = "groupid";
    public static final int ENG_ENGTEST_VERSION = 1;
    public static final String RESULT_TEST_NAME = "result";

    public static final String INTENT_PARA_TEST_NAME = "testname";
    public static final String INTENT_PARA_TEST_INDEX = "testindex";
    public static final String INTENT_BACKGROUND_TEST_RESULT = "bgtestresult";
    public static final String INTENT_RESULT_TYPE = "resulttype";

    public static final int RESULT_TYPE_FOR_SYSTEMTEST = 0;
    public static final int RESULT_TYPE_NORMAL = 1;

    public final static int TEST_ITEM_DONE = 0;
    public static final boolean IS_SUPPORT_LED_TEST = fileIsExists()||rgb_gpio_fileIsExists();

    public static final int[] ALL_TEST_ITEM_STRID = {
            R.string.otg_test,
            R.string.version_test,
            R.string.rf_cali_test,
            R.string.rtc_test,
            R.string.backlight_test,
            R.string.lcd_test,
            R.string.touchpoint_test,
            R.string.muti_touchpoint_test,
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            R.string.vibrator_test,
            R.string.ring_test,
            R.string.receiver_test,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            R.string.phone_loopback_test,
            R.string.phone_call_test,
            R.string.gravity_sensor_test,
            R.string.oritention_sensor_test,
            R.string.proximity_sensor_test,
            R.string.front_camera_title_text,
            R.string.back_camera_title_text,
            R.string.key_test,
            R.string.battery_title_text,
            R.string.headset_test,
            R.string.fm_test,
            /*SUN:jicog.wang modify 20160908 start {@            
             R.string.status_indicator_red,
             R.string.status_indicator_green,
             R.string.status_indicator_blue,
            SUN:jicong.wang modify 20160908 end @}*/             
            R.string.bt_test,
            R.string.wifi_test,
            R.string.gps_test,
            R.string.sdcard_test,
            R.string.sim_test,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT start {@*/
			R.string.led_test,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @}*/
            R.string.TestResultTitleString,
    };
    // SPRD: Add for bug464743,Some phones don't support Led lights test.
    public static final int[] ALL_TEST_ITEM_STRID2 = {
            R.string.otg_test, 
            R.string.version_test, 
            R.string.rf_cali_test, 
            R.string.rtc_test,
            R.string.backlight_test, 
            R.string.lcd_test, 
            R.string.touchpoint_test,
            R.string.muti_touchpoint_test, 
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            R.string.vibrator_test,
            R.string.ring_test,
            R.string.receiver_test,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */            
            R.string.vibrator_test, 
            R.string.phone_loopback_test,
            R.string.phone_call_test, 
            R.string.gravity_sensor_test,
            R.string.oritention_sensor_test, 
            R.string.proximity_sensor_test,
            R.string.front_camera_title_text, 
            R.string.back_camera_title_text, 
            R.string.key_test,
            R.string.battery_title_text, 
            R.string.headset_test, 
            R.string.fm_test, 
            R.string.bt_test,
            R.string.wifi_test, 
            R.string.gps_test, 
            R.string.sdcard_test, 
            R.string.sim_test,
            R.string.TestResultTitleString,
    };

    public static final String[] ALL_TEST_ITEM_NAME = {
            "OTG test",
            "Version",
            "RF CALI",
            "RTC test",
            "Backlight test",
            "Lcd test",
            "TP test",
            "Multi-TP test",
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //"Melody test", 
            "Vibrator test",
            "Ring test",
            "Receiver test",
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            "Phone loopback test",
            "PhoneCall test",
            "Gsensor test",
            "Msensor test",
            "Proximity test",
            "FrontCamera test",
            "Camera test",
            "Key test",
            "Charger test",
            "Headset test",
            "FM test",
            /*SUN:jicog.wang modify 20160908 start {@
             "RedLed test",
             "GreenLed test",
             "BlueLed test",
             SUN:jicong.wang modify 20160908 end @}*/
            "Bluetooth test",
            "Wifi test",
            "Gps test",
            "SDcard test",
            "SIMcard test",
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT start {@ */
			"Led test",
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @} */            
            RESULT_TEST_NAME
    };
    // SPRD: Add for bug464743,Some phones don't support Led lights test.
    public static final String[] ALL_TEST_ITEM_NAME2 = {
            "OTG test", 
            "Version", 
            "RF CALI", 
            "RTC test", 
            "Backlight test", 
            "Lcd test", 
            "TP test",
            "Multi-TP test", 
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //"Melody test", 
            "Vibrator test",
            "Ring test",
            "Receiver test",
            /* Sunvov:lzz 20150618 modify for NewTest end @} */            
            "Melody test", 
            "Phone loopback test", 
            "PhoneCall test",
            "Gsensor test", 
            "Msensor test", 
            "Proximity test", 
            "FrontCamera test", 
            "Camera test",
            "Key test", 
            "Charger test", 
            "Headset test", 
            "FM test", 
            "Bluetooth test", 
            "Wifi test",
            "Gps test", 
            "SDcard test", 
            "SIMcard test", 
            RESULT_TEST_NAME
    };

    public static final Class[] ALL_TEST_ITEM = {
            OTGTest.class,
            SystemVersionTest.class,
            RFCALITest.class,
            RTCTest.class,
            BackLightTest.class,
            ScreenColorTest.class,
            SingleTouchPointTest.class,
            MutiTouchTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            PhoneLoopBackTest.class,
            PhoneCallTestActivity.class,
            GsensorTestActivity.class,
            CompassTestActivity.class,
            PsensorTestActivity.class,
            FrontCameraTestActivity.class,
            CameraTestActivity.class,
            KeyTestActivity.class,
            ChargerTest.class,
            HeadSetTest.class,
            FMTest.class,
            /*SUN:jicog.wang modify 20160908 start {@            
             RedLightTest.class,
             GreenLightTest.class,
             BlueLightTest.class,
            SUN:jicong.wang modify 20160908 end @}*/             
            BluetoothTestActivity.class,
            WifiTestActivity.class,
            GpsTestActivity.class,
            SDCardTest.class,
            SIMCardTestActivity.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT {@ */
            LedTest.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @} */
            TestResultActivity.class
    };
    // SPRD: Add for bug464743,Some phones don't support Led lights test.
    public static final Class[] ALL_TEST_ITEM2 = {
            OTGTest.class, 
            SystemVersionTest.class, 
            RFCALITest.class, 
            RTCTest.class,
            BackLightTest.class, 
            ScreenColorTest.class, 
            SingleTouchPointTest.class,
            MutiTouchTest.class, 
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */            
            MelodyTest.class, 
            PhoneLoopBackTest.class,
            PhoneCallTestActivity.class, 
            GsensorTestActivity.class, 
            CompassTestActivity.class,
            PsensorTestActivity.class, 
            FrontCameraTestActivity.class, 
            CameraTestActivity.class,
            KeyTestActivity.class, 
            ChargerTest.class, 
            HeadSetTest.class, 
            FMTest.class,
            BluetoothTestActivity.class, 
            WifiTestActivity.class, 
            GpsTestActivity.class,
            SDCardTest.class, 
            SIMCardTestActivity.class,
            TestResultActivity.class
    };

    public static final Class[] DEFAULT_UNIT_TEST_ITEMS = {
            OTGTest.class,
            SystemVersionTest.class,
            RFCALITest.class,
            RTCTest.class,
            BackLightTest.class,
            ScreenColorTest.class,
            SingleTouchPointTest.class,
            MutiTouchTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            PhoneLoopBackTest.class,
            PhoneCallTestActivity.class,
            GsensorTestActivity.class,
            CompassTestActivity.class,
            PsensorTestActivity.class,
            FrontCameraTestActivity.class,
            CameraTestActivity.class,
            KeyTestActivity.class,
            ChargerTest.class,
            HeadSetTest.class,
            FMTest.class,
            /*SUN:jicog.wang modify 20160908 start {@            
             RedLightTest.class,
             GreenLightTest.class,
             BlueLightTest.class,
            SUN:jicong.wang modify 20160908 end @}*/             
            BluetoothTestActivity.class,
            WifiTestActivity.class,
            GpsTestActivity.class,
            SDCardTest.class,
            SIMCardTestActivity.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT {@ */
            LedTest.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @} */
            TestResultActivity.class
    };
    // SPRD: Add for bug464743,Some phones don't support Led lights test.
    public static final Class[] DEFAULT_UNIT_TEST_ITEMS2 = {
            OTGTest.class, 
            SystemVersionTest.class, 
            RFCALITest.class, 
            RTCTest.class,
            BackLightTest.class, 
            ScreenColorTest.class, 
            SingleTouchPointTest.class,
            MutiTouchTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            PhoneLoopBackTest.class,
            PhoneCallTestActivity.class, 
            GsensorTestActivity.class, 
            CompassTestActivity.class,
            PsensorTestActivity.class, 
            FrontCameraTestActivity.class, 
            CameraTestActivity.class,
            KeyTestActivity.class, 
            ChargerTest.class, 
            HeadSetTest.class, 
            FMTest.class,
            BluetoothTestActivity.class, 
            WifiTestActivity.class, 
            GpsTestActivity.class,
            SDCardTest.class, 
            SIMCardTestActivity.class, 
            TestResultActivity.class
    };

    public static final Class[] DEFAULT_AUTO_TEST_ITEMS = {
            BackLightTest.class,
            ScreenColorTest.class,
            SingleTouchPointTest.class,
            MutiTouchTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            PhoneLoopBackTest.class,
            MelodyTest.class,
            GsensorTestActivity.class,
            CompassTestActivity.class,
            PsensorTestActivity.class,
            FrontCameraTestActivity.class,
            CameraTestActivity.class,
            KeyTestActivity.class,
            ChargerTest.class,
            HeadSetTest.class,
            FMTest.class,
            /*SUN:jicog.wang modify 20160908 start {@            
             RedLightTest.class,
             GreenLightTest.class,
             BlueLightTest.class,
            SUN:jicong.wang modify 20160908 end @}*/             
             //qiuyaobo,20160727,begin
             PhoneCallTestActivity.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT {@ */
            LedTest.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @} */
             //TestResultActivity.class
             //qiuyaobo,20160727,end
    };
    // SPRD: Add for bug464743,Some phones don't support Led lights test.
    public static final Class[] DEFAULT_AUTO_TEST_ITEMS2 = {
            BackLightTest.class, 
            ScreenColorTest.class, 
            SingleTouchPointTest.class,
            MutiTouchTest.class, 
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */            
            PhoneLoopBackTest.class, 
            MelodyTest.class,
            GsensorTestActivity.class, 
            CompassTestActivity.class, 
            PsensorTestActivity.class,
            FrontCameraTestActivity.class, 
            CameraTestActivity.class, 
            KeyTestActivity.class,
            ChargerTest.class, 
            HeadSetTest.class, 
            FMTest.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT {@ */
            LedTest.class,
			/*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @} */			
            //qiuyaobo,20160727,begin
            PhoneCallTestActivity.class,
            //TestResultActivity.class  
            //qiuyaobo,20160727,end          
    };

    public static final Class[] DEFAULT_SYSTEM_TEST_ITEMS = {
            BackLightTest.class,
            ScreenColorTest.class,
            SingleTouchPointTest.class,
            MutiTouchTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest start @{ */
            //MelodyTest.class,
            VibratorTest.class,
            RingTest.class,
            ReceiverTest.class,
            /* Sunvov:lzz 20150618 modify for NewTest end @} */
            PhoneLoopBackTest.class,
            MelodyTest.class,
            GsensorTestActivity.class,
            CompassTestActivity.class,
            PsensorTestActivity.class,
            FrontCameraTestActivity.class,
            CameraTestActivity.class,
            KeyTestActivity.class,
            ChargerTest.class,
            HeadSetTest.class,
            FMTest.class,
            PhoneCallTestActivity.class,
            BluetoothTestActivity.class,
            WifiTestActivity.class,
            GpsTestActivity.class,
            SDCardTest.class,
            SIMCardTestActivity.class,
    };

    // add status for test item
    public static final int FAIL = 0;
    public static final int SUCCESS = 1;
    public static final int DEFAULT = 2;

    // add the filter here
    private static boolean isSupport(Class className, Context context) {
        if (FrontCameraTestActivity.class == className) {
            int mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
            CameraInfo[] mInfo = new CameraInfo[mNumberOfCameras];
            for (int i = 0; i < mNumberOfCameras; i++) {
                mInfo[i] = new CameraInfo();
                android.hardware.Camera.getCameraInfo(i, mInfo[i]);
                if (mInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT) {
                    return true;
                }
            }

            return false;
        } else if (CameraTestActivity.class == className) {
            int mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
            CameraInfo[] mInfo = new CameraInfo[mNumberOfCameras];
            for (int i = 0; i < mNumberOfCameras; i++) {
                mInfo[i] = new CameraInfo();
                android.hardware.Camera.getCameraInfo(i, mInfo[i]);
                if (mInfo[i].facing == CameraInfo.CAMERA_FACING_BACK) {
                    return true;
                }
            }

            return false;
        } else if (OTGTest.class == className) {
            BufferedReader bReader = null;
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(
                        "/sys/devices/platform/dwc_otg.0/driver/is_support_otg");
                bReader = new BufferedReader(new InputStreamReader(inputStream));
                String str = bReader.readLine();
                if (str.contains("0")) {
                    return false;
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getSupportList()  Exception happens:");
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                Log.e(TAG, "getSupportList()  Exception happens:");
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (bReader != null) {
                        bReader.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getSupportList()  Exception happens:");
                }

                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getSupportList()  Exception happens:");
                }
            }
        } else if (CompassTestActivity.class == className) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null){
                return false;
            }
        } else if (PsensorTestActivity.class == className) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) == null){
                return false;
            }
            //qiuyaobo,20160712,begin
             if (OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
                return false;
            }           
            //qiuyaobo,20160712,end
        }
        //qiuyaobo,20160712,begin
        else if (GpsTestActivity.class == className) {
            if (OptConfig.SUNVOV_NO_GPS){
                return false;
            }
        }
        //qiuyaobo,20160712,end
        /*SUN:jicong.wang add for SUN_BREATHINGLIGHT start {@ */
        else if (LedTest.class == className) {
            if(!OptConfig.SUN_BREATHINGLIGHT)
            {
            	return false;
            }
        }
        /*SUN:jicong.wang add for SUN_BREATHINGLIGHT end @} */		
        else if (ReceiverTest.class == className) {
            if (OptConfig.SUNVOV_CUSTOM_C7356_TYC_WVGA || OptConfig.SUNVOV_CUSTOM_C7356_TYC_S418_WVGA){
                return false;
            }
        }
        
        return true;
    }

    /* SPRD: Modify for bug464743,Some phones don't support Led lights test.{@ */
    public static ArrayList<TestItem> getSupportList(boolean withResultActivity, Context context) {
        ArrayList<TestItem> supportArray = new ArrayList<TestItem>();

        if(IS_SUPPORT_LED_TEST){
            for (int i = 0; i < DEFAULT_UNIT_TEST_ITEMS.length; i++) {
                for (int j = 0; j < ALL_TEST_ITEM.length; j++) {
                    if (DEFAULT_UNIT_TEST_ITEMS[i].hashCode() == ALL_TEST_ITEM[j]
                            .hashCode() && isSupport(ALL_TEST_ITEM[j], context)) {
                        if (!withResultActivity
                                && ALL_TEST_ITEM[j]
                                        .equals(TestResultActivity.class)) {
                            continue;
                        }
                        TestItem item = new TestItem(j);
                        supportArray.add(item);
                    }
                }
            }
        }else{
            for (int i = 0; i < DEFAULT_UNIT_TEST_ITEMS2.length; i++) {
                for (int j = 0; j < ALL_TEST_ITEM2.length; j++) {
                    if (DEFAULT_UNIT_TEST_ITEMS2[i].hashCode() == ALL_TEST_ITEM2[j]
                            .hashCode() && isSupport(ALL_TEST_ITEM2[j], context)) {
                        if (!withResultActivity
                                && ALL_TEST_ITEM2[j]
                                        .equals(TestResultActivity.class)) {
                            continue;
                        }
                        TestItem item = new TestItem(j);
                        supportArray.add(item);
                    }
                }
            }
        }
        return supportArray;
    }

    public static ArrayList<TestItem> getSupportAutoTestList(Context context) {
        ArrayList<TestItem> supportArray = new ArrayList<TestItem>();
        if(IS_SUPPORT_LED_TEST){
            for (int i = 0; i < DEFAULT_AUTO_TEST_ITEMS.length; i++) {
                for (int j = 0; j < ALL_TEST_ITEM.length; j++) {
                    if (DEFAULT_AUTO_TEST_ITEMS[i].hashCode() == ALL_TEST_ITEM[j]
                            .hashCode() && isSupport(ALL_TEST_ITEM[j], context)) {
                        TestItem item = new TestItem(j);
                        supportArray.add(item);
                    }
                }
            }
        }else{
            for (int i = 0; i < DEFAULT_AUTO_TEST_ITEMS2.length; i++) {
                for (int j = 0; j < ALL_TEST_ITEM2.length; j++) {
                    if (DEFAULT_AUTO_TEST_ITEMS2[i].hashCode() == ALL_TEST_ITEM2[j]
                            .hashCode() && isSupport(ALL_TEST_ITEM2[j], context)) {
                        TestItem item = new TestItem(j);
                        supportArray.add(item);
                    }
                }
            }
        }
        return supportArray;
    }
    public static ArrayList<TestItem> getSupportSystemTestList(Context context) {
        ArrayList<TestItem> supportArray = new ArrayList<TestItem>();

        for (int i = 0; i < DEFAULT_SYSTEM_TEST_ITEMS.length; i++) {
            for (int j = 0; j < ALL_TEST_ITEM2.length; j++) {
                if (DEFAULT_SYSTEM_TEST_ITEMS[i].hashCode() == ALL_TEST_ITEM2[j]
                        .hashCode() && isSupport(ALL_TEST_ITEM2[j], context)) {
                    TestItem item = new TestItem(j);
                    supportArray.add(item);
                }
            }
        }
        return supportArray;
    }
    /* @} */

    public static boolean isCameraSupport() {
        int mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
        CameraInfo[] mInfo = new CameraInfo[mNumberOfCameras];
        for (int i = 0; i < mNumberOfCameras; i++) {
            mInfo[i] = new CameraInfo();
            android.hardware.Camera.getCameraInfo(i, mInfo[i]);
            if (mInfo[i].facing == CameraInfo.CAMERA_FACING_BACK) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCmmbSupport() {
        boolean isSupport = SystemProperties.getBoolean(
                "ro.config.hw.cmmb_support", true);
        Log.d(TAG, "hw cmmb is support:" + isSupport);
        return isSupport;
    }
    public static boolean isHomeSupport(Context context) {
/* SPRD: modify 20140529 Spreadtrum of 305634 MMI test,lack of button which is on the right 0f "Home" @{ */
        boolean isSupport = ViewConfiguration.get(context).hasPermanentMenuKey();
        //boolean isSupport = SystemProperties.getBoolean("ro.config.hw.home_support", true);
/* @} */
        Log.d(TAG, "hw home is support:" + isSupport);
        /* SPRD: modify 20150527 Spreadtrum of 440597  @{ */
        //return isSupport;
        return true;
        /* @} */
    }

    public static boolean isBackSupport(Context context) {
/* SPRD: modify 20140529 Spreadtrum of 305634 MMI test,lack of button which is on the right 0f "Home" @{ */
       boolean isSupport = ViewConfiguration.get(context).hasPermanentMenuKey();
       // boolean isSupport = SystemProperties.getBoolean("ro.config.hw.back_support", true);
/* @} */
       Log.d(TAG, "hw Back is support:" + isSupport);
        /* SPRD: modify 20150527 Spreadtrum of 440597  @{ */
        //return isSupport;
        return true;
        /* @} */
    }

    public static boolean isMenuSupport(Context context) {
        //boolean isSupport = SystemProperties.getBoolean("ro.config.hw.menu_support", true);
        boolean isSupport = ViewConfiguration.get(context).hasPermanentMenuKey();
        Log.d(TAG, "hw menu is support:" + isSupport);
        /* SPRD: modify 20150527 Spreadtrum of 440597  @{ */
        //return isSupport;
        return true;
        /* @} */
    }

    public static boolean isVolumeUpSupport() {
        boolean isSupport = SystemProperties.getBoolean(
                "ro.config.hw.vol_up_support", true);
        Log.d(TAG, "hw VolumeUp is support:" + isSupport);
        return isSupport;
    }

    public static boolean isVolumeDownSupport() {
        boolean isSupport = SystemProperties.getBoolean(
                "ro.config.hw.vol_down_support", true);
        Log.d(TAG, "hw VolumeDown is support:" + isSupport);
        return isSupport;
    }

    /*
     * SPRD:Modify Bug 464743, check LED light /sys/class/leds/red/brightness
     * /sys/class/leds/green/brightness /sys/class/leds/blue/brightness
     * @{
     */
    public static boolean fileIsExists() {
        try {
            File file = new File("/sys/class/leds/red/brightness");
            Log.d(TAG, "brightness fileIsExists");
            if (!file.exists()) {
                Log.d(TAG, " brightness fileIsExists false");
                return false;
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.d(TAG, " brightness fileIsExists Exception e = " + e);
            return false;
        }
        Log.d(TAG, "brightness fileIsExists true");
        return true;
    }
    /* @} */
    /*SUN:jicong.wang add for rgb grio start {@*/
    public static boolean rgb_gpio_fileIsExists() {
        try {
            File file = new File("/sys/class/rgb_gpio/rgb_ctl/device");
            Log.d(TAG, "/sys/class/rgb_gpio/rgb_ctl/device fileIsExists");
            if (!file.exists()) {
                Log.d(TAG, " /sys/class/rgb_gpio/rgb_ctl/device fileIsExists false");
                return false;
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.d(TAG, " /sys/class/rgb_gpio/rgb_ctl/device fileIsExists Exception e = " + e);
            return false;
        }
        Log.d(TAG, "/sys/class/rgb_gpio/rgb_ctl/device fileIsExists true");
        return true;
    }    
    /*SUN:jicong.wang add for rgb gpio end @}*/

}
