
package com.sprd.engineermode.slidesettings;

import java.util.HashSet;
import java.util.Set;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.os.SystemProperties;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface.OnClickListener;
import android.text.InputFilter.LengthFilter;
import android.text.InputFilter;

import com.sprd.engineermode.R;

public class ValueItem {

    private static final String TAG = "SlideSettings";

    /**
     * the systempropety will be write when the value has changed by
     * EngineerMode
     */

    // launcher damping, real value is input value
    static final String LA_DELAY_TIME_KEY = "persist.sys.la_delay_time";
    // launcher page roll back time, real value is input value
    static final String LA_SLIP_VELOCITY_KEY = "persist.sys.la_slip_velocity";
    // launcher the sliding speed of flipping page, real value is input value*50
    static final String LA_MIN_SNAP_VELOCITY_KEY = "persist.sys.la_min_snap_v";
    // launcher Trigger sliding length,real value is input value
    static final String LA_TOUCH_SLOP_KEY = "persist.sys.la_touch_slop";
    // launcher Trigger sliding length1,real value is input value*10
    static final String LA_MIN_FLING_VELOCITY_KEY = "persist.sys.la_min_fling_v";
    // launcher Trigger sliding length2,real value is input value*10, the value
    // must greater then Trigger sliding length1
    static final String LA_FLING_THRESHOLD_VELOCITY_KEY = "persist.sys.la_fling_th_v";

    // listview coefficient of friction, real value is input value
    static final String LV_FRICTION_KEY = "persist.sys.lv_friction";
    // listview sliding velocity, real value is input value
    static final String LV_VELOCITY_KEY = "persist.sys.lv_velocity";
    // listview Trigger sliding length, real value is input value
    static final String LV_TOUCH_SLOP_KEY = "persist.sys.lv_touchslop_th";
    // listview Trigger sliding speed, real value is input value
    static final String LV_MIN_VELOCITY_KEY = "persist.sys.lv_minvelo_th";

    public static final int REMOVE_TASK_KILL_PROCESS = 0x0001;

    static Set<String> launcherProcessName;

    static {
        launcherProcessName = new HashSet<String>();
        launcherProcessName.add("com.android.launcher3");
        launcherProcessName.add("com.android.sprdlauncher1");
        launcherProcessName.add("com.android.sprdlauncher2");
        launcherProcessName.add("com.android.launcher");
    }

    private boolean killLauncher;
    private Config config;
    private Context mContext;
    private int curRealyValue;
    private int curTextValue;
    private boolean isTitle;
    private ActivityManager mAm;
    private SettingsAdapter mAdapter;

    public ValueItem(SettingsAdapter adapter, int index) {
        mAdapter = adapter;
        mContext = adapter.getContext();
        init(index);
    }

    private void init(int index) {
        if (index == SettingsAdapter.LAUNCHER_SET_INDEX
                || index == SettingsAdapter.LISTVIEW_SET_INDEX) {
            isTitle = true;
            return;
        }
        killLauncher = index >= SettingsAdapter.LA_DELAY_TIME_INDEX
                && index <= SettingsAdapter.LA_FLING_THRESHOLD_VELOCITY_INDEX;
        switch (index) {
            case SettingsAdapter.LA_DELAY_TIME_INDEX:
                config = new Config(LA_DELAY_TIME_KEY, 65, 1, 100, 1f, R.string.delay_time_slide,
                        R.string.default_delay_time, R.string.delay_time_explain);
                break;
            case SettingsAdapter.LA_SLIP_VELOCITY_INDEX:
                config = new Config(LA_SLIP_VELOCITY_KEY, 600, 150, 1500, 1f,
                        R.string.slip_velocity,
                        R.string.default_slip_velocity, R.string.slip_velocity_explain);
                break;
            case SettingsAdapter.LA_MIN_SNAP_VELOCITY_INDEX:
                config = new Config(LA_MIN_SNAP_VELOCITY_KEY, 50, 5, 100, 50f,
                        R.string.set_la_snap_velocity,
                        R.string.default_la_snap_velocity, R.string.la_snap_velocity_explain);
                break;
            case SettingsAdapter.LA_TOUCH_SLOP_INDEX:
                config = new Config(LA_TOUCH_SLOP_KEY, 8, 1, 30, 1f, R.string.set_la_touchslop,
                        R.string.default_la_touchslop, R.string.la_touchslop_explain);
                break;
            case SettingsAdapter.LA_MIN_FLING_VELOCITY_INDEX:
                config = new Config(LA_MIN_FLING_VELOCITY_KEY, 25, 1, 60, 10f,
                        R.string.set_la_minvelocity,
                        R.string.default_la_minvelocity, R.string.la_minvelocity_explain);
                break;
            case SettingsAdapter.LA_FLING_THRESHOLD_VELOCITY_INDEX:
                config = new Config(LA_FLING_THRESHOLD_VELOCITY_KEY, 50, 10, 80, 10f,
                        R.string.set_la_velocity,
                        R.string.default_la_velocity, R.string.la_velocity_explain);
                break;
            case SettingsAdapter.LV_FRICTION_INDEX:
                config = new Config(LV_FRICTION_KEY, 170, 1, 1000, 1f, R.string.set_lv_friction,
                        R.string.default_lv_friction, R.string.lv_friction_explain);
                break;
            case SettingsAdapter.LV_VELOCITY_INDEX:
                config = new Config(LV_VELOCITY_KEY, 1000, 1, 2000, 1f, R.string.set_lv_velocity,
                        R.string.default_lv_velocity, R.string.lv_velocity_explain);
                break;
            case SettingsAdapter.LV_TOUCH_SLOP_INDEX:
                config = new Config(LV_TOUCH_SLOP_KEY, 8, 1, 30, 1f, R.string.set_lv_touchslop,
                        R.string.default_lv_touchslop, R.string.lv_touchslop_explain);
                break;
            case SettingsAdapter.LV_MIN_VELOCITY_INDEX:
                config = new Config(LV_MIN_VELOCITY_KEY, 50, 10, 100, 1f,
                        R.string.set_lv_minvelocity,
                        R.string.default_lv_minvelocity, R.string.lv_minvelocity_explain);
                break;
        }
        curRealyValue = SystemProperties.getInt(config.key,
                (int) (config.defaultValue * config.rate));
        curTextValue = (int) (curRealyValue / config.rate);
    }

    public boolean isTitle() {
        return isTitle;
    }

    public Config getConfig() {
        return config;
    }

    public String getCurText() {
        return curTextValue + "";
    }

    public int getRealyValue() {
        return curRealyValue;
    }

    private void dealClick(String inputStr) {
        int inputValue;
        if (inputStr == null || inputStr.isEmpty()) {
            inputValue = 0;
        } else {
            inputValue = Integer.parseInt(inputStr);
            if (inputValue < config.minValue || inputValue > config.maxValue) {
                inputValue = ~inputValue;
            }
        }
        if (inputValue > 0) {
            int realValue = (int) (inputValue * config.rate);
            if (realValue == curRealyValue) {
                return;
            }
            if (config.key == LA_MIN_FLING_VELOCITY_KEY ||
                    config.key == LA_FLING_THRESHOLD_VELOCITY_KEY) {
                boolean isFirst = config.key == LA_MIN_FLING_VELOCITY_KEY;
                ValueItem vi = (ValueItem) mAdapter.getItem(isFirst ?
                        SettingsAdapter.LA_FLING_THRESHOLD_VELOCITY_INDEX
                        : SettingsAdapter.LA_MIN_FLING_VELOCITY_INDEX);
                int otherValue = vi.getRealyValue();
                if ((isFirst && otherValue <= realValue) ||
                        (!isFirst && otherValue >= realValue)) {
                    Toast.makeText(mContext, R.string.warn_la_velocity, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            curRealyValue = realValue;
            curTextValue = inputValue;
            SystemProperties.set(config.key, realValue + "");
            Toast.makeText(mContext, "set value " + inputStr + " succeed", Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "set " + config.key + ": " + realValue + " killLauncher: " + killLauncher);
            goingKillLauncher();
        } else {
            Toast.makeText(mContext,
                    "you input value " + (inputValue == 0 ? 0 : ~inputValue) + " is invalidly",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onItemClick() {
        if (isTitle) {
            return;
        }
        final EditText inputEditText = new EditText(mContext);
        inputEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        int maxLength = String.valueOf(config.maxValue).length();
        InputFilter[] filters = {
                new LengthFilter(maxLength)  
        };
        inputEditText.setFilters(filters);

        new AlertDialog.Builder(mContext)
                .setTitle(config.setText)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .setMessage(config.defaultText)
                .setView(inputEditText)
                .setPositiveButton(R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dealClick(inputEditText.getText().toString());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel_slide, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void goingKillLauncher() {
        if (!killLauncher) {
            return;
        }
        if (mAm == null) {
            mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        }
        for (RunningTaskInfo rt : mAm.getRunningTasks(10)) {
            String packageName = rt.topActivity.getPackageName();
            if (launcherProcessName.contains(packageName)) {
                Log.d(TAG, "set launcher slide argument, remove launcher task, taskId: " + rt.id
                        + " packageName: " + packageName);
                mAm.removeTask(rt.id);
                mAm.forceStopPackage(packageName);
            }
        }
    }

    class Config {
        String key;
        int defaultValue;
        int minValue;
        int maxValue;
        float rate;

        String setText;
        String defaultText;
        String explainText;

        public Config(String key, int defaultValue, int minValue, int maxValue, float rate,
                int setTextId, int defaultTextId, int explainTextId) {
            super();
            this.key = key;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.rate = rate;
            setText = mContext.getString(setTextId);
            defaultText = mContext.getString(defaultTextId, defaultValue, minValue, maxValue);
            explainText = mContext.getString(explainTextId);
        }

        @Override
        public String toString() {
            return "Config [setText=" + setText + ", defaultText=" + defaultText + ", explainText="
                    + explainText + "]";
        }
    }

}
