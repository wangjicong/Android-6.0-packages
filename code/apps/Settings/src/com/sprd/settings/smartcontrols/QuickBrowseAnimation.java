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

public class QuickBrowseAnimation extends DialogFragment {
    private ImageView mQuickBrowseDisplay;
    private Button mQuickBrowseButton;
    private AnimationDrawable mAnimationDrawable;
    private static SmartSwitchPreference mPreference;
    private static final String TAG = "QuickBrowseAnimation";

    public static QuickBrowseAnimation newInstance(SmartSwitchPreference preference) {
        final QuickBrowseAnimation QuickBrowseDialog = new QuickBrowseAnimation();
        mPreference = preference;
        return QuickBrowseDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder mQuickBrowseAnimationDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater.inflate(R.layout.quick_browse, null);

        mQuickBrowseDisplay = (ImageView) customView.findViewById(R.id.quick_browse_display);

        mQuickBrowseDisplay.setImageResource(R.drawable.quick_browse_anim);
        mAnimationDrawable = (AnimationDrawable) mQuickBrowseDisplay.getDrawable();
        mAnimationDrawable.start();

        mQuickBrowseAnimationDialog.setView(customView);

        mQuickBrowseAnimationDialog.setPositiveButton(R.string.smart_ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
                        if (mPreference != null) {
                            mPreference.setChecked(turnOn);
                        }
                    }
                });

       return mQuickBrowseAnimationDialog.create();
    }

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss,exception is:"+e.getMessage());
        }
    }
}