package com.sprd.launcher3.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


import java.util.ArrayList;


public class SPRDShakeDetector implements SensorEventListener {

    private long mLastUpdateTime;
    private Context mContext;
    private SensorManager mSensorManager;
    private ArrayList<OnShakeListener> mListeners;
    private ArrayList list = new ArrayList();
    private static final int MAX = 5;
    private static final int MIN = -5;
    public static final int SENSOR_RIGHT_SHAKE = 1;
    public static final int SENSOR_LEFT_SHAKE = 2;
    public static final int SPEED_MIN = 3000;
    private static final int TIME_THRESHOLD = 80;

    private float mLastX;
    private float mLastY;
    private float mLastZ;


    public SPRDShakeDetector(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mListeners = new ArrayList<OnShakeListener>();
    }

    public interface OnShakeListener {
        void onShake(int direction);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int z = (int)sensorEvent.values[2];
        if (z == 1) {
            this.notifyListeners(SENSOR_LEFT_SHAKE);
        } else if (z == 2) {
            this.notifyListeners(SENSOR_RIGHT_SHAKE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void registerOnShakeListener(OnShakeListener listener) {
        if (mListeners.contains(listener)) {
            return;
        }
        mListeners.add(listener);
    }

    public void unregisterOnShakeListener(OnShakeListener listener) {
        mListeners.remove(listener);
    }

    public void start() {
        if (mSensorManager == null) {
            throw new UnsupportedOperationException();
        }
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SPRDHUB_SHAKE);
        if (sensor == null) {
            throw new UnsupportedOperationException();
        }
        boolean success = mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        if (!success) {
            throw new UnsupportedOperationException();
        }
    }

    public void stop() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    private void notifyListeners(int direction) {
        for (OnShakeListener listener : mListeners) {
            listener.onShake(direction);
        }
    }
}