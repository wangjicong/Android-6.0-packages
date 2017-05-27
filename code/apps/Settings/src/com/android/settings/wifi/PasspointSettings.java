/*
 *Copyright (C) 2015 SPRD Passpoint R1 Feature
 */

package com.android.settings.wifi;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.preference.Preference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wifi.AccessPointPreference.UserBadgeCache;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.AccessPoint.AccessPointListener;
import com.android.settingslib.wifi.WifiTracker.WifiListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Two types of UI are provided here.
 *
 * The first is for "usual Settings", appearing as any other Setup fragment.
 *
 * The second is for Setup Wizard, with a simplified interface that hides the action bar
 * and menus.
 */
public class PasspointSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, Indexable, WifiListener {

    private static final String TAG = "PasspointSettings";

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_ADD_NETWORK = Menu.FIRST + 1;
    private static final int MENU_ID_SAVED_CONFIG = Menu.FIRST + 2;

    // Instance state keys
    private static final String SAVE_DIALOG_EDIT_MODE = "edit_mode";

    private PasspointDialog mDialog;

    private TextView mEmptyView;

    private static final int PASSPOINT_DIALOG_ID = 0;

    // Save the dialog details
    private boolean mDlgEdit;
    private HandlerThread mBgThread;
    protected WifiManager mWifiManager;
    private WifiTracker mWifiTracker;
    private UserBadgeCache mUserBadgeCache;

    public PasspointSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.passpoint_settings);

        mUserBadgeCache = new UserBadgeCache(getPackageManager());
        mBgThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mBgThread.start();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiTracker =
                new WifiTracker(getActivity(), this, mBgThread.getLooper(), false, true, false);
        mWifiManager = mWifiTracker.getManager();

        mEmptyView = initEmptyView();
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mWifiTracker.startTracking();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWifiTracker.stopTracking();
    }

    @Override
    public void onDestroy() {
        mBgThread.quit();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the user is not allowed to configure wifi, do not show the menu.
        if (isUiRestricted()) return;

        addOptionsMenuItems(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * @param menu
     */
    void addOptionsMenuItems(Menu menu) {
        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(
                new int[] {R.attr.ic_menu_add, R.attr.ic_wps});

        menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.menu_stats_refresh)
                 .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_ADD_NETWORK, 0, R.string.passpoint_add_config)
                .setIcon(ta.getDrawable(0))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_SAVED_CONFIG, 0, R.string.passpoint_saved_config)
               .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        ta.recycle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            outState.putBoolean(SAVE_DIALOG_EDIT_MODE, mDlgEdit);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the user is not allowed to configure wifi, do not handle menu selections.
        if (isUiRestricted()) return false;

        switch (item.getItemId()) {
            case MENU_ID_SCAN:
                    mWifiTracker.forceScan();
                return true;
            case MENU_ID_ADD_NETWORK:
                    onAddNetworkPressed();
                return true;
            case MENU_ID_SAVED_CONFIG:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            SavedPassPointsConfigs.class.getCanonicalName(), null,
                            R.string.wifi_saved_passpoint_titlebar, null, this, 0);
                } else {
                    startFragment(this, SavedPassPointsConfigs.class.getCanonicalName(),
                            R.string.wifi_saved_passpoint_titlebar,
                            -1 /* Do not request a result */, null);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(PASSPOINT_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgEdit = edit;

        showDialog(PASSPOINT_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case PASSPOINT_DIALOG_ID:
                // If it's null, fine, it's for Add Network
                mDialog = new PasspointDialog(getActivity(), this, mDlgEdit);
                return mDialog;
        }
        return super.onCreateDialog(dialogId);
    }

    /**
     * Shows the latest access points available with supplemental information like
     * the strength of network and the security for it.
     * @param mWifiManager
     */
    private void updateAccessPoints() {
        // Safeguard from some delayed event handling
        if (getActivity() == null) return;
        Log.d(TAG,"fuzy updateAccessPoints");
        if (isUiRestricted()) {
            addMessagePreference(R.string.passpoint_empty_list_user_restricted);
            return;
        }

        final Collection<AccessPoint> accessPoints = mWifiTracker.getAccessPoints();
        getPreferenceScreen().removeAll();
        int index = 0;
        for (AccessPoint accessPoint : accessPoints) {
            // Ignore access points that are out of range.
            if (accessPoint.getLevel() != -1 && accessPoint.isPasspoint()) {
                AccessPointPreference preference = new AccessPointPreference(accessPoint,
                        getActivity(), mUserBadgeCache, false);
                preference.setOrder(index++);
                getPreferenceScreen().addPreference(preference);
            }
        }
        if (index == 0) {
            addMessagePreference(R.string.passpoint_empty_list_wifi_on);
        }
    }

    protected TextView initEmptyView() {
        TextView emptyView = (TextView) getActivity().findViewById(android.R.id.empty);
        getListView().setEmptyView(emptyView);
        return emptyView;
    }

    private void addMessagePreference(int messageId) {
        if (mEmptyView != null) mEmptyView.setText(messageId);
        getPreferenceScreen().removeAll();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == PasspointDialog.BUTTON_POSITIVE) {
            if (mDialog != null) {
                saveConfig();
            }
        }
    }

    /* package */ void saveConfig() {
        WifiManager wifiManager =
                (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG,"fuzy WifiPasspointConfig"+mDialog.getConfig().toString());
        wifiManager.addPasspointCred(mDialog.getConfig());
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        // No exact access point is selected.
        showDialog(null, true);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.WIFI_PASSPOINT_SETTINGS;
    }

    @Override
    public void onWifiStateChanged(int state) {
    }

    @Override
    public void onConnectedChanged() {
    }

    @Override
    public void onAccessPointsChanged() {
        updateAccessPoints();
    }
}
