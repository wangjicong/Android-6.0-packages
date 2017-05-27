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

public class MusicSwitchAnimation extends DialogFragment {
    private ImageView mMusicSwitchDisplay;
    private Button mMusicSwitchButton;
    private AnimationDrawable mAnimationDrawable;
    private static SmartSwitchPreference mPreference;
    private static final String TAG = "MusicSwitchAnimation";

    public static MusicSwitchAnimation newInstance(SmartSwitchPreference preference) {
        final MusicSwitchAnimation MusicSwitchDialog = new MusicSwitchAnimation();
        mPreference = preference;
        return MusicSwitchDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder mMusicSwitchAnimationDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater.inflate(R.layout.music_switch, null);

        mMusicSwitchDisplay = (ImageView) customView.findViewById(R.id.music_switch_display);

        mMusicSwitchDisplay.setImageResource(R.drawable.music_switch_anim);
        mAnimationDrawable = (AnimationDrawable) mMusicSwitchDisplay.getDrawable();
        mAnimationDrawable.start();

        mMusicSwitchAnimationDialog.setView(customView);

        mMusicSwitchAnimationDialog.setPositiveButton(R.string.smart_ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
                        if (mPreference != null) {
                            mPreference.setChecked(turnOn);
                        }
                    }
                });

       return mMusicSwitchAnimationDialog.create();
    }

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss,exception is:"+e.getMessage());
        }
    }
}