
package com.android.gallery3d.app;

import java.io.File;
import android.content.ContentResolver;
import android.database.Cursor;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.provider.MediaStore;

public class FloatPlayerService extends Service {

    FloatMoviePlayer mFloatMoviePlayer;
    private Uri mUri;
    private int mPosition;
    private ScreenBroadcastReceiver mScreenReceiver;
    public int mOldState;

    private static final String TAG = "FloatPlayerService";
    public static final String STOP_PLAY_VIDEO = "stop.play.video";
    private static String ACTION_SHUTFLOAT="com.android.gallery3d.app.SHUT_FLOATWINDOW";
    private boolean mIsHeadSetMount = false;
    private boolean mIsUnmounted = false;//SPRD:modify by old bug487272
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d(TAG, "onCreate");
        mScreenReceiver = new ScreenBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(ACTION_SHUTFLOAT);
        filter.addAction(STOP_PLAY_VIDEO);
        filter.addAction("com.wx.hall");//Bug49967
        registerReceiver(mScreenReceiver, filter);

        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        /** modify by old Bug487272 @{ */
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        /**@}*/
        intentFilter.setPriority(1000);
        intentFilter.addDataScheme("file");
        registerReceiver(mExternalMountedReceiver, intentFilter);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        Log.i(TAG, "oncreate");
    }

    PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d(TAG, "onCallStateChanged   state = " + state);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mFloatMoviePlayer != null) {
                        // mFloatMoviePlayer.closeWindow();
                        mFloatMoviePlayer.setPhoneState(true);
                    }
                    break;
                // bug 350950 begin
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (mFloatMoviePlayer != null) {
                        // mFloatMoviePlayer.closeWindow();
                        mFloatMoviePlayer.setPhoneState(true);
                    }
                    break;
                // bug 350950 end
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mFloatMoviePlayer != null) {
                        mFloatMoviePlayer.setPhoneState(false);
                    }
                    break;
                default:
                    break;
            }
        };
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
        } else {
            mUri = intent.getData();
            Log.d(TAG, "FloatPlayerService onStartCommand Uri = " + mUri);
            mPosition = intent.getIntExtra("position", 0);
            int state = intent.getIntExtra("currentstate", 0);
            createFloatView();
            mFloatMoviePlayer.setDataIntent(intent);
            Log.d(TAG, "onStartCommand setVideoUri state=" + state);
            mFloatMoviePlayer.setVideoUri(mUri);
            if (mPosition > 0) {
                Log.d(TAG, "onStartCommand seekTo");
                mFloatMoviePlayer.seekTo(mPosition-2500);//SPRD:modify by old Bug490436
            }
            // if (state != FloatMoviePlayer.STATE_PAUSED) {
            Log.d(TAG, "onStartCommand start");
            mFloatMoviePlayer.start();
            // }
            mFloatMoviePlayer.initTitle();
        }
        /*
         * if(state == FloatMoviePlayer.STATE_PAUSED){ mFloatMoviePlayer.pause(); }
         */
        Log.d(TAG, "onStartCommand end");
        return super.onStartCommand(intent, flags, startId);
    }

    private void createFloatView() {
        if (mFloatMoviePlayer == null) {
            mFloatMoviePlayer = new FloatMoviePlayer(this);
        }
        if (mFloatMoviePlayer.mFloatLayout == null) {
            mFloatMoviePlayer.addToWindow();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mScreenReceiver);
        unregisterReceiver(mExternalMountedReceiver);
        if (mFloatMoviePlayer != null) {
            mFloatMoviePlayer.removeFromWindow();
            mFloatMoviePlayer = null;
        }
        super.onDestroy();
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (mFloatMoviePlayer == null) {
                return;
            }
            int state = mFloatMoviePlayer.getCurrentState();
            Log.d(TAG, "onReceive action:" + action + " state=" + state);
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mOldState = state;
                if (state == FloatMoviePlayer.STATE_PLAYING) {
                    mFloatMoviePlayer.pause();
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (state == FloatMoviePlayer.STATE_PAUSED
                        && mOldState == FloatMoviePlayer.STATE_PLAYING) {
                    if(mFloatMoviePlayer.getPhoneState()){
                        mFloatMoviePlayer.setState(mOldState);
                        mOldState = 0;
                        return;
                    }
                    mOldState = 0;
                    mFloatMoviePlayer.start();
                }
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                if (intent.getIntExtra("state", 0) == 1) {
                    mIsHeadSetMount = true;
                    Log.d(TAG, "intent.getIntExtra() " + 1 + " " + mIsHeadSetMount);
                } else if (intent.getIntExtra("state", 0) == 0) {
                    Log.d(TAG, "intent.getIntExtra() " + 0 + " " + mIsHeadSetMount);
                    if (mIsHeadSetMount == true) {
                        mIsHeadSetMount = false;
                        mFloatMoviePlayer.pause();
                    }
                }
            } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                mFloatMoviePlayer.closeWindow();
            } else if (STOP_PLAY_VIDEO.equals(action)) {
                mFloatMoviePlayer.closeWindow();
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                mFloatMoviePlayer.closeWindow();
            /**SPRD:Bug542760 the MovieActivity and the floatwindow play together@{*/
            }else if(ACTION_SHUTFLOAT.equals(action)){
                mFloatMoviePlayer.closeWindow();
            }
            /**@}*/
            else if(action.equals("com.wx.hall")){//Bug49967
                mFloatMoviePlayer.pause();
            }
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    private BroadcastReceiver mExternalMountedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            /** modify by old Bug487272 @{ */
            // String path = VideoDrmUtils.getInstance().getFilePathByUri(mUri,context);
//            String path = getFilePathFromUri(mUri);
//            if (path == null) {
//                /* stopSelf(); */
//                mFloatMoviePlayer.closeWindow();
//            } else {
//                File file = new File(path);
//                if (file == null || !file.exists()) {
//                    /* stopSelf(); */
//                    mFloatMoviePlayer.closeWindow();
//                }
//            }
            String action = intent.getAction();
            Log.d(TAG, action);
            String path = getFilePathFromUri(mUri);
            String sdPath = Environment.getExternalStoragePath().toString();
            if (path == null || path.startsWith(sdPath)) {
                if(!mIsUnmounted){
                    mFloatMoviePlayer.stop();
                    Log.d(TAG, "float movie player has stopped");
                    mIsUnmounted = true;
                    mFloatMoviePlayer.closeWindow();
                }
            }
         /**@}*/
        }
    };

    public String getFilePathFromUri(Uri uri) {
        /** modify by old Bug480381 @{ */
        if (uri.getScheme().toString().compareTo("file") == 0) {
            String path = uri.toString().replace("file://", "");
            return path;
        } else {
            /**@}*/
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(uri, null, null, null, null);
            // Log.d("info", ""+cursor.toString()+"---"+cursor.getCount()+"行");
            // int column_index=cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            if (cursor != null) {//SPRD:modify by old bug510904
                if (cursor.getCount() == 0) {
                    cursor.close();
                    return null;
                } else {

                    cursor.moveToFirst();
                    String path = cursor.getString(1);
                    cursor.close();
                    return path;
                }
            } else {
                return "uselessUri";
            }
        }
    }

}
