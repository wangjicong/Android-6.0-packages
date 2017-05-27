package com.sprd.phone;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;

import com.android.internal.util.Preconditions;
import com.android.services.telephony.Log;

/**
 * Plays the default ringtone. Uses {@link Ringtone} in a separate thread so that this class can be
 * used from the main thread.
 */
class AsyncRingtonePlayer {
    private static final String TAG = AsyncRingtonePlayer.class.getSimpleName();
    // Message codes used with the ringtone thread.
    private static final int EVENT_PLAY = 1;
    private static final int EVENT_STOP = 2;
    private static final int EVENT_REPEAT = 3;
    // SPRD: Fade down ringtone to vibrate.
    private static final int EVENT_FADEDOWN_RINGTONE = 4;

    // The interval in which to restart the ringer.
    private static final int RESTART_RINGER_MILLIS = 3000;

    /** Handler running on the ringtone thread. */
    private Handler mHandler;

    /** The current ringtone. Only used by the ringtone thread. */
    private Ringtone mRingtone;

    /**
     * The context.
     */
    private final Context mContext;
    /* SPRD: Fade down ringtone to vibrate. @{*/
    /** Indicate that we want the pattern to repeat at the step which turns on vibration. */
    private static final int VIBRATION_PATTERN_REPEAT = 1;
    private static final long[] VIBRATION_PATTERN = new long[] {
    /** Indicate that we want the pattern to repeat at the step which turns on vibration. */
            0, // No delay before starting
            1000, // How long to vibrate
            1000, // How long to wait before vibrating again
    };
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();
    private final Vibrator mVibrator;
    private boolean mIsVibrating = false;
    /* @} */
    // MaxRingingVolume and Vibrate.
    private int mVolumeIndex;
    private boolean mIsMaxRingingVolumeOn = false;

    AsyncRingtonePlayer(Context context) {
        mContext = context;
        // SPRD: Fade down ringtone to vibrate.
        mVibrator = new SystemVibrator(mContext);
    }

    /** Plays the ringtone. */
    void play(Uri ringtone) {
        Log.d(this, "Posting play.");
        postMessage(EVENT_PLAY, true /* shouldCreateHandler */, ringtone);
    }

    /** Stops playing the ringtone. */
    void stop() {
        Log.d(this, "Posting stop.");
        postMessage(EVENT_STOP, false /* shouldCreateHandler */, null);
    }

    /**
     * Posts a message to the ringtone-thread handler. Creates the handler if specified by the
     * parameter shouldCreateHandler.
     *
     * @param messageCode The message to post.
     * @param shouldCreateHandler True when a handler should be created to handle this message.
     */
    private void postMessage(int messageCode, boolean shouldCreateHandler, Uri ringtone) {
        synchronized(this) {
            if (mHandler == null && shouldCreateHandler) {
                mHandler = getNewHandler();
            }

            if (mHandler == null) {
                Log.d(this, "Message %d skipped because there is no handler.", messageCode);
            } else {
                mHandler.obtainMessage(messageCode, ringtone).sendToTarget();
            }
        }
    }

    /**
     * Creates a new ringtone Handler running in its own thread.
     */
    private Handler getNewHandler() {
        Preconditions.checkState(mHandler == null);

        HandlerThread thread = new HandlerThread("ringtone-player");
        thread.start();

        return new Handler(thread.getLooper()) {
            // SPRD: Fade down ringtone to vibrate.
            float mCurrentVolume = 1.0f;
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case EVENT_PLAY:
                        handlePlay((Uri) msg.obj);
                        break;
                    case EVENT_REPEAT:
                        handleRepeat();
                        break;
                    case EVENT_STOP:
                        handleStop();
                        break;
                    /* SPRD: Fade down ringtone to vibrate. @{ */
                    case EVENT_FADEDOWN_RINGTONE:
                        mCurrentVolume -= .05f;
                        if (mCurrentVolume > .05f) {
                            mHandler.sendEmptyMessageDelayed(EVENT_FADEDOWN_RINGTONE, 500);
                        } else if (!mIsVibrating) {
                            mCurrentVolume = 0f;
                            mVibrator.vibrate(VIBRATION_PATTERN, VIBRATION_PATTERN_REPEAT,
                                    VIBRATION_ATTRIBUTES);
                            mIsVibrating = true;
                        }
                        if (mRingtone != null) {
                            mRingtone.setVolume(mCurrentVolume);
                        }
                        break;
                    /* @} */
                }
            }
        };
    }

    /**
     * Starts the actual playback of the ringtone. Executes on ringtone-thread.
     */
    private void handlePlay(Uri ringtoneUri) {
        // don't bother with any of this if there is an EVENT_STOP waiting.
        if (mHandler.hasMessages(EVENT_STOP)) {
            return;
        }

        if (!(Looper.getMainLooper() == Looper.myLooper())) {
            Log.wtf(TAG, new IllegalStateException(), "Must be on the main thread!");
        }
        Log.i(this, "Play ringtone.");

        if (mRingtone == null) {
            mRingtone = getRingtone(ringtoneUri);

            // Cancel everything if there is no ringtone.
            if (mRingtone == null) {
                handleStop();
                return;
            }
        }

        handleRepeat();
    }

    private void handleRepeat() {
        if (mRingtone == null) {
            return;
        }

        if (mRingtone.isPlaying()) {
            Log.d(this, "Ringtone already playing.");
        } else {
            mRingtone.play();
            Log.i(this, "Repeat ringtone.");
        }

        // Repost event to restart ringer in {@link RESTART_RINGER_MILLIS}.
        synchronized(this) {
            if (!mHandler.hasMessages(EVENT_REPEAT)) {
                mHandler.sendEmptyMessageDelayed(EVENT_REPEAT, RESTART_RINGER_MILLIS);
            }
        }
    }

    /**
     * Stops the playback of the ringtone. Executes on the ringtone-thread.
     */
    private void handleStop() {
        if (!(Looper.getMainLooper() == Looper.myLooper())) {
            Log.wtf(TAG, new IllegalStateException(), "Must be on the main thread!");
        }
        Log.i(this, "Stop ringtone.");

        if (mRingtone != null) {
            Log.d(this, "Ringtone.stop() invoked.");
            mRingtone.stop();
            mRingtone = null;
        }
        /* SPRD: Fade down ringtone to vibrate. @{ */
        if (mIsVibrating) {
            mVibrator.cancel();
            mIsVibrating = false;
        }
        /* @} */
        /* SPRD: MaxRingingVolume and Vibrate. @{  */
        if (mIsMaxRingingVolumeOn) {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, mVolumeIndex, 0);
        }
        mIsMaxRingingVolumeOn = false;
        /* @} */

        synchronized(this) {
            // At the time that STOP is handled, there should be no need for repeat messages in the
            // queue.
            mHandler.removeMessages(EVENT_REPEAT);

            if (mHandler.hasMessages(EVENT_PLAY)) {
                Log.v(this, "Keeping alive ringtone thread for subsequent play request.");
            } else {
                mHandler.removeMessages(EVENT_STOP);
                mHandler.getLooper().quitSafely();
                mHandler = null;
                Log.v(this, "Handler cleared.");
            }
        }
    }

    private Ringtone getRingtone(Uri ringtoneUri) {
        if (ringtoneUri == null) {
            ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
        }

        Ringtone ringtone = RingtoneManager.getRingtone(mContext, ringtoneUri);
        if (ringtone != null) {
            ringtone.setStreamType(AudioManager.STREAM_RING);
        }
        return ringtone;
    }

    /* SPRD: Fade down ringtone to vibrate. @{ */
    void fadeDownRingtone() {
        Log.d(this, "fadeDownRingtone.");
        postMessage(EVENT_FADEDOWN_RINGTONE, false /* shouldCreateHandler */, null);
    }
    /* @} */

    /* SPRD: MaxRingingVolume and Vibrate. @{ */
    void handleMaxRingingVolume() {
        Log.d(this, "maxRingingVolume.");
        mIsMaxRingingVolumeOn = true;
        if (!mIsVibrating) {
            mVibrator.vibrate(VIBRATION_PATTERN, VIBRATION_PATTERN_REPEAT,
                    VIBRATION_ATTRIBUTES);
            mIsVibrating = true;
        }
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVolumeIndex = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        audioManager.setStreamVolume(AudioManager.STREAM_RING,7,0);
    }
    /* @} */
}
