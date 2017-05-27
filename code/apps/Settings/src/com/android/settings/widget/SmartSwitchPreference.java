package com.android.settings.widget;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import com.android.settings.R;

public class SmartSwitchPreference extends SwitchPreference {

    private Switch mSwitch;
    private boolean mChecked = false;
    private Context mContext;
    private OnPreferenceSwitchChangeListener mOnSwitchChangeListener;
    private OnViewClickedListener mOnViewClickedListener;

    public interface OnPreferenceSwitchChangeListener {
        public void onPreferenceSwitchChanged(boolean checked);
    }

    public interface OnViewClickedListener {
        public void OnViewClicked(View v);
    }

    public void setOnViewClickedListener(OnViewClickedListener listener) {
        mOnViewClickedListener = listener;
    }

    public void setOnPreferenceSwitchCheckedListener(OnPreferenceSwitchChangeListener listener) {
        mOnSwitchChangeListener = listener;
    }

    public SmartSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWidgetLayoutResource(R.layout.smart_switch_preference);
    }

    public SmartSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setWidgetLayoutResource(R.layout.smart_switch_preference);
    }

    public SmartSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.smart_switch_preference);
    }

    public SmartSwitchPreference(Context context) {
        super(context);
        mContext = context;
        setWidgetLayoutResource(R.layout.smart_switch_preference);
    }

    @Override
    protected void onBindView(View view) {
        mSwitch = (Switch) view.findViewById(R.id.prefrence_switch);
        view.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mOnViewClickedListener.OnViewClicked(v);
        }
        });

        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                mChecked = checked;
                mOnSwitchChangeListener.onPreferenceSwitchChanged(checked);
            }
        });
        }
        setChecked(mChecked);
        super.onBindView(view);
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

}
