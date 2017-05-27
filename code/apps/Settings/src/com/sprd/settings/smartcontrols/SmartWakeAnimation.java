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

public class SmartWakeAnimation extends DialogFragment {
    private ImageView mSmartWakeDisplay;
    private Button mSmartWakeButton;
    private AnimationDrawable mAnimationDrawable;
    private static SmartSwitchPreference mPreference;
    private static final String TAG = "SmartWakeAnimation";

    public static SmartWakeAnimation newInstance(SmartSwitchPreference preference) {
        final SmartWakeAnimation smartWakeDialog = new SmartWakeAnimation();
        mPreference = preference;
        return smartWakeDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder mSmartWakeAnimationDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater.inflate(R.layout.smart_wake, null);

        mSmartWakeDisplay = (ImageView) customView.findViewById(R.id.smart_wake_display);

        mSmartWakeDisplay.setImageResource(R.drawable.smart_wake_anim);
        mAnimationDrawable = (AnimationDrawable) mSmartWakeDisplay.getDrawable();
        mAnimationDrawable.start();

        mSmartWakeAnimationDialog.setView(customView);

        mSmartWakeAnimationDialog.setPositiveButton(R.string.smart_ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
                        if (mPreference != null) {
                            mPreference.setChecked(turnOn);
                        }
                    }
                });

       return mSmartWakeAnimationDialog.create();
    }

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss,exception is:"+e.getMessage());
        }
    }
}
