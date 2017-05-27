/*
 * Copyright (C) 2015 SPRD Passpoint R1 Feature
 */

package com.android.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;


public class PasspointPreference extends SwitchPreference {
    /**
     * Construct a new PasspointPreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes A resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     */
    public PasspointPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Construct a new PasspointPreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public PasspointPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    /**
     * Construct a new PasspointPreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     */
    public PasspointPreference(Context context, AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.switchPreferenceStyle);
    }

    /**
     * Construct a new PasspointPreference with default style options.
     *
     * @param context The Context that will style this preference
     */
    public PasspointPreference(Context context) {
        super(context, null);
    }
    @Override
    protected void onClick() {
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        View checkableView = view.findViewById(com.android.internal.R.id.switchWidget);
        final Switch switchView = (Switch)checkableView;
        switchView.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            }
        });
    }
}
