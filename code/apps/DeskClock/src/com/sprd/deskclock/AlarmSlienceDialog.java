package com.sprd.deskclock;

import com.android.deskclock.AlarmClockFragment.AlarmItemAdapter.ItemHolder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.R;
import android.util.Log;

public class AlarmSlienceDialog extends DialogFragment {
    private Alarm mAlarm;
    private final String DEFAULT_ALARM_TIMEOUT_SETTING = "10";
    private static final String TAG = "AlarmSlienceDialog";
    private static final String KEY_SLIENCE_ALARM = "slience_alarm";

    public AlarmSlienceDialog() {
    }

    public static AlarmSlienceDialog newInstance(Alarm alarm) {
        AlarmSlienceDialog alarmSlienceDialog = new AlarmSlienceDialog();
        /* SPRD: Bug 530669 java.lang.NullPointerException, read alarm on a null object reference @{ */
        Bundle args = new Bundle();
        Log.d(TAG,"alarm = " + alarm);
        args.putParcelable(KEY_SLIENCE_ALARM, alarm);
        alarmSlienceDialog.setArguments(args);
        /* @} */
        return alarmSlienceDialog;
    }

    private final int[] AUTO_SILENCE = new int[] {
            1,
            5,
            10,
            15,
            20,
            25,
            30,
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /* SPRD: Bug 530669 java.lang.NullPointerException, read alarm on a null object reference @{ */
        Bundle bundle = getArguments();
        mAlarm = bundle.getParcelable(KEY_SLIENCE_ALARM);
        Log.d(TAG,"mAlarm = " + mAlarm);
        /* @} */

        // SPRD: Bug 527665 java.lang.NullPointerException
        final Context context = getActivity();
        Log.d(TAG,"onCreateDialog context = " + context);

        int defaultValue = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(mAlarm.id + "", DEFAULT_ALARM_TIMEOUT_SETTING));
        int markPosition = 0;
        for (int itemp = 0; itemp < AUTO_SILENCE.length; itemp++) {
            if (AUTO_SILENCE[itemp] == defaultValue) {
                markPosition = itemp;
                break;
            }
        }

        AlertDialog.Builder mAutoSilenceDialog = new AlertDialog.Builder(context);
        mAutoSilenceDialog.setTitle(R.string.auto_silence_title);
        mAutoSilenceDialog.setSingleChoiceItems(
                context.getResources().getStringArray(R.array.auto_silence), markPosition,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        PreferenceManager.getDefaultSharedPreferences(context).edit()
                                .putString(mAlarm.id + "", AUTO_SILENCE[which] + "")
                                .commit();
                        String setAutoSilenceTitle = context.getResources().getStringArray(
                                R.array.auto_silence)[which];
                        String autoSilenceTitle = context.getString(R.string.auto_silence_title)
                                + " " + setAutoSilenceTitle;
                        ShowAutoSilenceDialogListener listener = (ShowAutoSilenceDialogListener) getTargetFragment();
                        listener.autoSilenceDialoglistener(mAlarm);
                        /* SPRD: Bug 547647 java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState @{ */
                        try {
                            dialog.dismiss();
                        } catch (Exception e) {
                            Log.w(TAG, "ignore a exception that was found when executed dismiss,exception is:"+e.getMessage());
                        }
                        /* @} */
                    }
                });
        mAutoSilenceDialog.setNegativeButton(R.string.auto_silence_cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

       return mAutoSilenceDialog.create();
    }

    public interface ShowAutoSilenceDialogListener {
        public void autoSilenceDialoglistener(Alarm alarm);
    };

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss, exception is:" + e.getMessage());
        }
    }
}
