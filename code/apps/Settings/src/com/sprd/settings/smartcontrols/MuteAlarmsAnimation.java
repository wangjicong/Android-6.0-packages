package com.sprd.settings.smartcontrols;

import android.app.DialogFragment;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.android.settings.R;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.app.Dialog;
import android.preference.Preference;
import com.android.settings.widget.SmartSwitchPreference;
import android.util.Log;

public class MuteAlarmsAnimation extends DialogFragment {
    private ImageView mMuteAlarmsDisplay;
    private Button mMuteAlarmsButton;
    private AnimationDrawable mAnimationDrawable;
    private static SmartSwitchPreference mPreference;
    private static final String TAG = "MuteAlarmsAnimation";

    public static MuteAlarmsAnimation newInstance(SmartSwitchPreference preference) {
        final MuteAlarmsAnimation MuteAlarmsDialog = new MuteAlarmsAnimation();
        mPreference = preference;
        return MuteAlarmsDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder mMuteAlarmsAnimationDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater.inflate(R.layout.mute_alarms, null);

        mMuteAlarmsDisplay = (ImageView) customView.findViewById(R.id.mute_alarms_display);

        mMuteAlarmsDisplay.setImageResource(R.drawable.mute_alarms_anim);
        mAnimationDrawable = (AnimationDrawable) mMuteAlarmsDisplay.getDrawable();
        mAnimationDrawable.start();

        mMuteAlarmsAnimationDialog.setView(customView);

        mMuteAlarmsAnimationDialog.setPositiveButton(R.string.smart_ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
                        if (mPreference != null) {
                            mPreference.setChecked(turnOn);
                        }
                    }
                });

       return mMuteAlarmsAnimationDialog.create();
    }

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss,exception is:"+e.getMessage());
        }
    }
}