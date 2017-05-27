
package com.android.deskclock.alarms;

import com.android.deskclock.AlarmInitReceiver;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.LogUtils;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.View.OnClickListener;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.R;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.support.annotation.NonNull;

public class AlarmShutDownActivity extends Activity implements OnClickListener {
    /**
     * AlarmActivity listens for this broadcast intent, so that other applications can snooze the
     * alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    /**
     * AlarmActivity listens for this broadcast intent, so that other applications can dismiss the
     * alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    private static final String LOGTAG = AlarmShutDownActivity.class.getSimpleName();
    private static final int PULSE_DURATION_MILLIS = 1000;
    private static final int ALARM_BOUNCE_DURATION_MILLIS = 500;
    private static final int ALERT_SOURCE_DURATION_MILLIS = 250;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_FADE_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2000;
    private static final Interpolator REVEAL_INTERPOLATOR =
            new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);

    private static final float BUTTON_SCALE_DEFAULT = 0.7f;
    private static final int BUTTON_DRAWABLE_ALPHA_DEFAULT = 165;
    private AlarmInstance mAlarmInstance;
    private int mCurrentHourColor;
    private String mVolumeBehavior;
    private boolean mAlarmHandled;
    private ViewGroup mContainerView;
    private ViewGroup mAlertView;
    private TextView mAlertTitleView;
    private TextView mAlertInfoView;

    private ViewGroup mContentView;
    private ImageButton mSnoozeButton;
    private LinearLayout mSnoozeLayout;
    private ImageButton mDismissCloseButton;
    private LinearLayout mDismissCloseLayout;
    private ImageButton mDismissOpenButton;
    private LinearLayout mDismissOpenLayout;
    private TextView mHintView;

    private ValueAnimator mSnoozeAnimator;
    private ValueAnimator mDismissCloseAnimator;
    private ValueAnimator mDismissOpenAnimator;
    private PowerManager mPm;

    private final Handler mHandler = new Handler();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtils.v(LOGTAG, "Received broadcast: %s", action);

            if (!mAlarmHandled) {
                switch (action) {
                    case ALARM_SNOOZE_ACTION:
                        snooze();
                        break;
                    case ALARM_DISMISS_ACTION:
                        dismissClose();
                        break;
                    case AlarmService.ALARM_DONE_ACTION:
                        SharedPreferences prefs =
                                PreferenceManager.getDefaultSharedPreferences(context);
                        prefs.edit().putString("type_shutdown_alarm", "").apply();
                        finish();
                        break;
                    default:
                        LogUtils.i(LOGTAG, "Unknown broadcast: %s", action);
                        break;
                }
            } else {
                LogUtils.v(LOGTAG, "Ignored broadcast: %s", action);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if(actionBar != null){
            actionBar.hide();
        }

        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (mAlarmInstance != null) {
            LogUtils.i(LOGTAG, "Displaying alarm for instance: %s", mAlarmInstance);
        } else {
            // The alarm got deleted before the activity got created, so just finish()
            LogUtils.e(LOGTAG, "Error displaying alarm for intent: %s", getIntent());
            finish();
            return;
        }
        // Get the volume/camera button behavior setting
        mVolumeBehavior = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mPm = (PowerManager) getSystemService(POWER_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        setContentView(R.layout.alarm_shut_down_activity);

        mContainerView = (ViewGroup) findViewById(android.R.id.content);

        mAlertView = (ViewGroup) mContainerView.findViewById(R.id.alert);
        mAlertTitleView = (TextView) mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = (TextView) mAlertView.findViewById(R.id.alert_info);

        mContentView = (ViewGroup) mContainerView.findViewById(R.id.content);
        mSnoozeButton = (ImageButton) mContentView.findViewById(R.id.snooze);
        mSnoozeLayout = (LinearLayout) mContainerView.findViewById(R.id.snooze_layout);
        mDismissCloseButton = (ImageButton) mContentView.findViewById(R.id.dismiss_close);
        mDismissCloseLayout = (LinearLayout) mContainerView.findViewById(R.id.dismiss_close_layout);
        mDismissOpenButton = (ImageButton) mContentView.findViewById(R.id.dismiss_open);
        mDismissOpenLayout = (LinearLayout)  mContainerView.findViewById(R.id.dismiss_open_layout);
        mHintView = (TextView) mContentView.findViewById(R.id.hint);

        final TextView titleView = (TextView) findViewById(R.id.title);
        final TextClock digitalClock = (TextClock) findViewById(R.id.digital_clock);

        titleView.setText(mAlarmInstance.getLabelOrDefault(this));
        Utils.setTimeFormat(this,digitalClock,
                getResources().getDimensionPixelSize(R.dimen.main_ampm_font_size));

        mCurrentHourColor = Utils.getCurrentHourColor();
        mContainerView.setBackgroundColor(mCurrentHourColor);

        mSnoozeButton.setOnClickListener(this);
        mSnoozeLayout.setOnClickListener(this);
        mDismissCloseButton.setOnClickListener(this);
        mDismissCloseLayout.setOnClickListener(this);
        mDismissOpenButton.setOnClickListener(this);
        mDismissOpenLayout.setOnClickListener(this);

        mSnoozeAnimator = getButtonAnimator(mSnoozeButton, Color.WHITE);
        mDismissCloseAnimator = getButtonAnimator(mDismissCloseButton, mCurrentHourColor);
        mDismissOpenAnimator = getButtonAnimator(mDismissOpenButton, mCurrentHourColor);

        setAnimatedFractions(0.0f /* snoozeFraction */, 0.0f /* dismissFraction */);

        final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // If the alarm instance is null the receiver was never registered and calling
        // unregisterReceiver will throw an exception.
        if (mAlarmInstance != null) {
            unregisterReceiver(mReceiver);
        }
    }

    private ValueAnimator getButtonAnimator(ImageButton button, int tintColor) {
        return ObjectAnimator.ofPropertyValuesHolder(button,
                PropertyValuesHolder.ofFloat(View.SCALE_X, BUTTON_SCALE_DEFAULT, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, BUTTON_SCALE_DEFAULT, 1.0f),
                PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255),
                PropertyValuesHolder.ofInt(AnimatorUtils.DRAWABLE_ALPHA,
                        BUTTON_DRAWABLE_ALPHA_DEFAULT, 255),
                PropertyValuesHolder.ofObject(AnimatorUtils.DRAWABLE_TINT,
                        AnimatorUtils.ARGB_EVALUATOR, Color.WHITE, tintColor));
    }

    private void setAnimatedFractions(float snoozeFraction, float dismissFraction) {
        final float alarmFraction = Math.max(snoozeFraction, dismissFraction);
        AnimatorUtils.setAnimatedFraction(mSnoozeAnimator, snoozeFraction);
        AnimatorUtils.setAnimatedFraction(mDismissCloseAnimator, dismissFraction);
        AnimatorUtils.setAnimatedFraction(mDismissOpenAnimator, dismissFraction);
    }

    private Animator getAlertAnimator(final View source, final int titleResId,
            final String infoText, final int revealColor, final int backgroundColor) {
        final ViewGroupOverlay overlay = mContainerView.getOverlay();

        // Create a transient view for performing the reveal animation.
        final View revealView = new View(this);
        revealView.setRight(mContainerView.getWidth());
        revealView.setBottom(mContainerView.getHeight());
        revealView.setBackgroundColor(revealColor);
        overlay.add(revealView);

        // Add the source to the containerView's overlay so that the animation can occur under the
        // status bar, the source view will be automatically positioned in the overlay so that
        // it maintains the same relative position on screen.
        overlay.add(source);

        final int centerX = Math.round((source.getLeft() + source.getRight()) / 2.0f);
        final int centerY = Math.round((source.getTop() + source.getBottom()) / 2.0f);
        final float startRadius = Math.max(source.getWidth(), source.getHeight()) / 2.0f;

        final int xMax = Math.max(centerX, mContainerView.getWidth() - centerX);
        final int yMax = Math.max(centerY, mContainerView.getHeight() - centerY);
        final float endRadius = (float) Math.sqrt(Math.pow(xMax, 2.0) + Math.pow(yMax, 2.0));

        final ValueAnimator sourceAnimator = ObjectAnimator.ofFloat(source, View.ALPHA, 0.0f);
        sourceAnimator.setDuration(ALERT_SOURCE_DURATION_MILLIS);
        sourceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(source);
            }
        });

        final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(
                revealView, centerX, centerY, startRadius, endRadius);
        revealAnimator.setDuration(ALERT_REVEAL_DURATION_MILLIS);
        revealAnimator.setInterpolator(REVEAL_INTERPOLATOR);
        revealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mAlertView.setVisibility(View.VISIBLE);
                mAlertTitleView.setText(titleResId);
                if (infoText != null) {
                    mAlertInfoView.setText(infoText);
                    mAlertInfoView.setVisibility(View.VISIBLE);
                }
                mContentView.setVisibility(View.GONE);
                mContainerView.setBackgroundColor(backgroundColor);
            }
        });

        final ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        fadeAnimator.setDuration(ALERT_FADE_DURATION_MILLIS);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(revealView);
            }
        });

        final AnimatorSet alertAnimator = new AnimatorSet();
        alertAnimator.play(revealAnimator).with(sourceAnimator).before(fadeAnimator);
        alertAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, ALERT_DISMISS_DELAY_MILLIS);
            }
        });

        return alertAnimator;
    }

    private void snooze() {
        mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Snoozed: %s", mAlarmInstance);
        mDismissOpenButton.setClickable(false);
        mDismissOpenLayout.setClickable(false);
        mDismissCloseButton.setClickable(false);
        mDismissCloseLayout.setClickable(false);
        final int accentColor = Utils.obtainStyledColor(this, R.attr.colorAccent, Color.RED);
        setAnimatedFractions(1.0f /* snoozeFraction */, 0.0f /* dismissFraction */);
        String snoozeMinutes = getResources().getQuantityString(R.plurals.alarm_alert_snooze_duration,
                AlarmStateManager.getSnoozedMinutes(this), AlarmStateManager.getSnoozedMinutes(this));
        getAlertAnimator(mSnoozeButton, R.string.alarm_alert_snoozed_text,
                snoozeMinutes, accentColor, accentColor).start();
        AlarmStateManager.setSnoozeState(this, mAlarmInstance, false /* showToast */);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPm.shutdownForAlarm();
            }
        }, ALERT_DISMISS_DELAY_MILLIS);
    }

    private void dismissClose() {
        mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Dismissed: %s", mAlarmInstance);

        setAnimatedFractions(0.0f /* snoozeFraction */, 1.0f /* dismissFraction */);
        getAlertAnimator(mDismissCloseButton, R.string.alarm_alert_off_text, null /* infoText */,
                Color.WHITE, mCurrentHourColor).start();
        AlarmStateManager.setDismissState(this, mAlarmInstance);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPm.shutdownForAlarm();
            }
        }, ALERT_DISMISS_DELAY_MILLIS);
    }

    private void dismissOpen() {
        mAlarmHandled = true;
        mDismissCloseButton.setClickable(false);
        mDismissCloseLayout.setClickable(false);
        LogUtils.v(LOGTAG, "Dismissed: %s", mAlarmInstance);

        setAnimatedFractions(0.0f /* snoozeFraction */, 1.0f /* dismissFraction */);
        getAlertAnimator(mDismissOpenButton, R.string.alarm_alert_off_text, null /* infoText */,
                Color.WHITE, mCurrentHourColor).start();
        AlarmStateManager.setDismissState(this, mAlarmInstance);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("type_shutdown_alarm", "").apply();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.snooze:
            case R.id.snooze_layout:
                snooze();
                break;
            case R.id.dismiss_close:
            case R.id.dismiss_close_layout:
                dismissClose();
                break;
            case R.id.dismiss_open:
            case R.id.dismiss_open_layout:
                dismissOpen();
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) {
        LogUtils.v(LOGTAG, "dispatchKeyEvent: %s", keyEvent);
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                switch (mVolumeBehavior) {
                    case SettingsActivity.VOLUME_BEHAVIOR_SNOOZE:
                        snooze();
                        break;
                    case SettingsActivity.VOLUME_BEHAVIOR_DISMISS:
                        dismissOpen();
                        break;
                    default:
                        break;
                }
                return true;
            default:
                return super.dispatchKeyEvent(keyEvent);
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }
}
