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

public class LockMusicSwitchAnimation extends DialogFragment {
    private ImageView mLockMusicSwitchDisplay;
    private Button mLockMusicSwitchButton;
    private AnimationDrawable mAnimationDrawable;
    private static SmartSwitchPreference mPreference;
    private static final String TAG = "LockMusicSwitchAnimation";

    public static LockMusicSwitchAnimation newInstance(SmartSwitchPreference preference) {
        final LockMusicSwitchAnimation LockMusicSwitchDialog = new LockMusicSwitchAnimation();
        mPreference = preference;
        return LockMusicSwitchDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder mLockMusicSwitchAnimationDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater.inflate(R.layout.lock_music_switch, null);

        mLockMusicSwitchDisplay = (ImageView) customView.findViewById(R.id.lock_music_switch_display);

        mLockMusicSwitchDisplay.setImageResource(R.drawable.lock_music_switch_anim);
        mAnimationDrawable = (AnimationDrawable) mLockMusicSwitchDisplay.getDrawable();
        mAnimationDrawable.start();

        mLockMusicSwitchAnimationDialog.setView(customView);

        mLockMusicSwitchAnimationDialog.setPositiveButton(R.string.smart_ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
                        if (mPreference != null) {
                            mPreference.setChecked(turnOn);
                        }
                    }
                });

       return mLockMusicSwitchAnimationDialog.create();
    }

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss,exception is:"+e.getMessage());
        }
    }
}