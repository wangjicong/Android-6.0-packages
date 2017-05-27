package com.sprd.music.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.FloatMath;
import android.util.Log;

public class SPRDSwitchDetector implements SensorEventListener {
   private static final String TAG = "SPRDSwitchpDetector";
   private float mData;
   private Context mContext;
   private SensorManager mSensorManager;
   private ArrayList<OnSwitchListener> mListeners;
   private final Object mLock = new Object();

   // senserhub flip
   private boolean mFirstIn = true;
   private boolean mDeviceDirBefore = false;
   private boolean mActionStart = false;


   public SPRDSwitchDetector(Context context) {
       mContext = context;
       mSensorManager = (SensorManager) context
               .getSystemService(Context.SENSOR_SERVICE);
       mListeners = new ArrayList<OnSwitchListener>();
   }

   public interface OnSwitchListener {
       void onFlip();
   }

   public void registerOnSwitchListener(OnSwitchListener listener) {
       if (mListeners.contains(listener))
           return;
       mListeners.add(listener);
   }

   public void unregisterOnSwitchListener(OnSwitchListener listener) {
       mListeners.remove(listener);
   }

   public void start() {
       if (mSensorManager == null) {
           throw new UnsupportedOperationException();
       }
       Sensor sensor = mSensorManager
               .getDefaultSensor(Sensor.TYPE_SPRDHUB_FACE_UP_DOWN);
       if (sensor == null) {
           throw new UnsupportedOperationException();
       }
       boolean success = mSensorManager.registerListener(this, sensor,
               SensorManager.SENSOR_DELAY_NORMAL);
       if (!success) {
           throw new UnsupportedOperationException();
       }
   }

   public void stop() {
       if (mSensorManager != null)
           mSensorManager.unregisterListener(this);
   }

   @Override
   public void onAccuracyChanged(Sensor sensor, int accuracy) {
   }

   @Override
   public void onSensorChanged(SensorEvent event) {
       synchronized (mLock) {
            mData = event.values[SensorManager.DATA_X];
            if (mContext != null) {
                Log.e(TAG, "SPRDSwitchDetector mData=" + mData);
                if (mData == 0.0 || mData == 1.0) {
                    this.notifyListeners_Switch();
                }
            }
       }
   }

   private void notifyListeners_Switch() {
       for (OnSwitchListener listener : mListeners) {
           listener.onFlip();
       }
   }
}

