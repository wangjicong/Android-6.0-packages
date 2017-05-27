
package com.sprd.engineermode.debuglog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.sprd.engineermode.EMSwitchPreference;
import com.sprd.engineermode.R;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

public class SystemSettingActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    private static final String TAG = "SystemSettingActivity";

    private static final String KEY_SYSTEM_SETTINGS = "system_settings";

    // sprd_monitor.conf
    private static final String FEATURE_PATH = "/data/local/tmp/sprd_monitor.conf";

    private List<String> sysSetList = new ArrayList<String>();
    private Handler uiThread = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_system_settings);
        setPreference();
    }
    
    @Override
    public void onResume() {
        if (!SystemProperties.getBoolean("persist.sys.slog.enabled", false)) {
            Log.d(TAG,"slog not start");
            Toast.makeText(SystemSettingActivity.this, "Slog stop", Toast.LENGTH_SHORT)
            .show();
            finish();
        }
        super.onResume();
    }

    public void setPreference() {
        // read sprd_monitor.conf
        BufferedReader reader = null;
        try {
            File file = new File(FEATURE_PATH);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sysSetList.add(line);
            }
        } catch (Exception e) {
            Log.d(TAG, "Read file error!!!");
            Toast.makeText(SystemSettingActivity.this, "Read file error!", Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        PreferenceGroup systemSettingsGroup = (PreferenceGroup) findPreference(KEY_SYSTEM_SETTINGS);
        for (int i = 1; i < sysSetList.size(); i++) {
            Log.d(TAG, "" + sysSetList.get(i));

            String[] str = sysSetList.get(i).split("\t");

            EMSwitchPreference switchPref = new EMSwitchPreference(this, null);
            switchPref.setOnPreferenceChangeListener(this);
            switchPref.setKey(str[0].trim());
            switchPref.setTitle(str[0].trim());

            Log.d(TAG, "status->" + str[str.length - 1].trim());
            if (str[str.length - 1].trim().contains("on")) {
                switchPref.setSummary("on");
                switchPref.setChecked(true);
            } else {
                switchPref.setSummary("off");
                switchPref.setChecked(false);
            }
            if (switchPref.getKey().contains("res-monitor")) {
                switchPref.setSummary(R.string.monitor_switch_warning);
                continue;
            }
            systemSettingsGroup.addPreference(switchPref);
        }
    }

    private boolean setSprdMonitor(String feature, String isOn) {
        Log.d(TAG, "setSprdMonitor->" + feature + "  " + isOn);

        boolean flag = true;
        PrintStream p = null;
        try {
            FileOutputStream out = new FileOutputStream(FEATURE_PATH);
            p = new PrintStream(out);
            for (int j = 0; j < sysSetList.size(); j++) {
                if (sysSetList.get(j).contains(feature)) {
                    String[] str = sysSetList.get(j).split("\t");
                    str[str.length - 1] = isOn;
                    sysSetList.set(j, str[0] + "\t" + str[str.length - 1]);
                }
                p.println(sysSetList.get(j));
            }
        } catch (Exception e) {
            Log.d(TAG, "setSprdMonitor[" + feature + "] error!");
            flag = false;
            e.printStackTrace();
        } finally {
            if (p != null) {
                try {
                    p.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object keyValue) {
        final EMSwitchPreference currPreference = (EMSwitchPreference) preference;

        if (currPreference.isChecked()) {
            boolean flag = setSprdMonitor(preference.getKey(), "off");
            Log.d(TAG, "" + flag);
            if (flag) {
                uiThread.post(new Runnable() {

                    @Override
                    public void run() {
                        currPreference.setSummary("off");
                        currPreference.setChecked(false);
                    }

                });
            } else {
                uiThread.post(new Runnable() {

                    @Override
                    public void run() {
                        currPreference.setSummary("on");
                        currPreference.setChecked(true);
                    }

                });
            }
            if (preference.getKey().contains("res-monitor")) {
                uiThread.post(new Runnable() {

                    @Override
                    public void run() {
                        currPreference.setSummary(R.string.monitor_switch_warning);
                    }

                });
            }
        } else {
            if (preference.getKey().contains("res-monitor")) {
                AlertDialog alertDialog = new AlertDialog.Builder(SystemSettingActivity.this)
                        .setTitle("res-monitor")
                        .setMessage(getString(R.string.monitor_dialog_warning))
                        .setPositiveButton(getString(R.string.alertdialog_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        boolean flag = setSprdMonitor(
                                                "res-monitor", "on");
                                        if (flag) {
                                            uiThread.post(new Runnable() {

                                                @Override
                                                public void run() {
                                                    currPreference
                                                            .setChecked(true);
                                                }

                                            });
                                        } else {
                                            uiThread.post(new Runnable() {

                                                @Override
                                                public void run() {
                                                    currPreference
                                                            .setChecked(false);

                                                }

                                            });
                                        }
                                    }
                                })
                        .setNegativeButton(R.string.alertdialog_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                    }
                                }).create();
                alertDialog.show();
            } else {
                boolean flag = setSprdMonitor(preference.getKey(), "on");
                if (flag) {
                    uiThread.post(new Runnable() {

                        @Override
                        public void run() {
                            currPreference.setSummary("on");
                            currPreference.setChecked(true);
                        }

                    });
                } else {
                    uiThread.post(new Runnable() {

                        @Override
                        public void run() {
                            currPreference.setSummary("off");
                            currPreference.setChecked(false);

                        }

                    });
                }
            }
        }
        return false;
    }
}
