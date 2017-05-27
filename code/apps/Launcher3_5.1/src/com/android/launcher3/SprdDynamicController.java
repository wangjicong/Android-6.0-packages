package com.android.launcher3;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

/**
 * Note: this is a utility class to allow you dynamically change some switch state
 * in Launcher. It is primarily designed for tester to help he/she reproduce issues
 * in User release version. Also it can be used to disable some function by default,
 * and reopen when tester/developer need it without recompile the apk.
 *
 * Use the following pattern to enable/disable switch state:
 * <pre>
 * adb shell am broadcast -a <Intent> --ez "enable" <true|false>.
 * </pre>
 *
 * For example, use the following command to turn on the log switch:
 * <pre>
 * adb shell am broadcast -a "android.intent.action.sprd_switch_log" --ez "enable" <true|false>
 * </pre>
 *
 * Current supported switchs:
 * 1 Log "android.intent.action.sprd_switch_log"
 * 2 Stats "android.intent.action.sprd_switch_stats"
 *
 * And use the following pattern if you want to output the log when the switcher
 * is on:
 * <pre>
 * {@code
 * if (SprdDynamicController.getInstance().isLogSwitchOn()) {
 *     Log.d(TAG, "XXXXXX");
 * }
 * }
 * </pre>
 */
public class SprdDynamicController {

    private static final String TAG = "SprdDynamicController";

    // NOTE: this flag is to control the status of some switcher which deeply
    // affect the performance. It is also use to control some block of code that
    // deeply affect the usage of memory.
    // Currently if you set COMPILE_ENABLE to false, almost every function of
    // SprdDynamicController be disabled.
    private static final boolean COMPILE_ENABLE = true;

    private static final String DYNAMIC_SWITCHER_PREF = "dynamic_switcher_pref";
    private static final String KEY_SWITCH_LOG = "switch_log";
    private static final String KEY_SWITCH_STATS = "switch_stats";

    private static Context sContext;

    private static boolean sIsLogSwitchOn = false;
    private static boolean sIsStatsSwitchOn = false;

    private BroadcastReceiver mBroadcastReceiver;
    private WeakReference<Launcher> mLauncherRef;
    private HashMap<String, ArrayList<DynamicActionHandler>> mActionHandlers;

    private SprdDynamicController() {
        if (!COMPILE_ENABLE) {
            return;
        }

        if (sContext != null) {
            init();
        } else {
            Log.w(TAG, "Have not initial the application context.");
        }
    }

    private static class SingletonHolder {
        private static final SprdDynamicController INSTANCE = new SprdDynamicController();
    }

    public static SprdDynamicController getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private void init() {
        mActionHandlers = new HashMap<String, ArrayList<DynamicActionHandler>>();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleIntent(intent);
            }
        };

        // add action handlers
        new LogSwitcher(this);
        new StatsSwitcher(this);
        new PersistenceSwitcher(this);

        // NOTE: any new switcher need to be created before this line.

        // register receiver
        IntentFilter filter = new IntentFilter();
        for (String action : mActionHandlers.keySet()) {
            filter.addAction(action);
        }
        sContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public void registerActionHandler(String action, DynamicActionHandler handler) {
        if (action != null && !action.isEmpty() && handler != null) {
            if (!mActionHandlers.containsKey(action)) {
                mActionHandlers.put(action, new ArrayList<DynamicActionHandler>());
            }
            mActionHandlers.get(action).add(handler);
        } else {
            Log.w(TAG, "register dynamic action handler fail for action: " + action + ", handler: " + handler);
        }
    }

    /**
     * Note: no need to think synchronization on this method for log switcher is
     * rare usage.
     *
     * @return true if allow Launcher to log informations.
     */
    public boolean isLogSwitchOn() {
        return sIsLogSwitchOn;
    }

    public boolean isStatsSwitchOn() {
        return sIsStatsSwitchOn;
    }

    /**
     * Note: you need to call this method in LauncherApplication's onTerminate method
     * to not let broadcast leak.
     */
    public void onTerminate() {
        if (sContext != null && mBroadcastReceiver != null) {
            sContext.unregisterReceiver(mBroadcastReceiver);
        }
    }

    /**
     * Helper function to store a weak ref of Launcher instance. It is be called
     * in LauncherAppState's setLauncher(...) every time a new Launcher be created.
     * @param launcher
     */
    public void setLauncher(Launcher launcher) {
        if (!COMPILE_ENABLE) {
            return;
        }

        mLauncherRef = new WeakReference<Launcher>(launcher);
    }

    /**
     * Note: this method is to be called by LauncherApplication's onCreate.
     *
     * @param context instance of LauncherApplication.
     */
    public static void setApplicationContext(Context context) {
        sContext = context;
    }

    /**
     * Restore persisted data from shared preferences.
     * NOTE: this method usually takes about 30-80ms when device startup, and takes
     * 18ms when hot-startup Launcher.
     *
     * You can set COMPILE_ENABLE to false if the time comsume not acceptable.
     *
     */
    public static void restoreData() {
        if (!COMPILE_ENABLE) {
            return;
        }

        if (sContext != null) {
            // long startT = System.nanoTime();

            // TODO: May be we could use reflection technology to implement a more
            // looser coupling mechanism to restore data for new added Switchers.
            // But reflection may hurt the performance.

            SharedPreferences sp = sContext.getSharedPreferences(DYNAMIC_SWITCHER_PREF, Context.MODE_PRIVATE);

            sIsLogSwitchOn = sp.getBoolean(KEY_SWITCH_LOG, false);
            // NOTE: any change to the swithcher must do somthing, for example:
            LauncherModel.changeDebugLoadersState(sIsLogSwitchOn);

            sIsStatsSwitchOn = sp.getBoolean(KEY_SWITCH_STATS, false);
            // NOTE: any new switcher need to add restore logic here.

            //Log.d(TAG, "restoreData tasks " + ((System.nanoTime() - startT)/1000000) + " ms.");
        }
    }

    /**
     * Helper function to get launcher instance.
     * @return Launcher
     */
    public Launcher getLauncher() {
        if (mLauncherRef != null) {
            return mLauncherRef.get();
        }
        return null;
    }

    public void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (mActionHandlers.containsKey(action)) {
            ArrayList<DynamicActionHandler> handlers = mActionHandlers.get(action);
            for (DynamicActionHandler handler : handlers) {
                handler.handleAction(action, intent);
            }
        } else {
            Log.w(TAG, "not handler to change action[" + action + "].");
        }
    }

    private interface DynamicActionHandler {
        void registerAction();
        void handleAction(String action, Intent intent);
    }

    private class SwitcherPersistAsyncTask extends AsyncTask<Pair<String, Boolean>, Void, Void> {
        @Override
        protected Void doInBackground(Pair<String, Boolean>... params) {
            if (params.length != 1) return null;

            SharedPreferences sp = sContext.getSharedPreferences(DYNAMIC_SWITCHER_PREF, Context.MODE_PRIVATE);
            sp.edit().putBoolean(params[0].first, params[0].second).commit();
            return null;
        }
    }

    private abstract class Switcher implements DynamicActionHandler {

        public static final String EXTRA_KEY_ENABLE = "enable";

        protected static final String INTENT_ACTION_DO_PERSISTENCE = "android.intent.action.sprd_do_persistence";

        protected SprdDynamicController mController;
        public Switcher(SprdDynamicController controller) {
            mController = controller;
            registerAction();
        }

        @Override
        public void handleAction(String action, Intent intent) {
            boolean enable = false;
            if (intent.getExtras() != null) {
                enable = intent.getExtras().getBoolean(EXTRA_KEY_ENABLE, false);
            }
            onHandleAction(enable, action, intent);
        }

        public abstract void onHandleAction(boolean enable, String action, Intent intent);
    }

    private class LogSwitcher extends Switcher {
        private static final String INTENT_ACTION_LOG = "android.intent.action.sprd_switch_log";

        public LogSwitcher(SprdDynamicController controller) {
            super(controller);
        }

        @Override
        public void registerAction() {
            mController.registerActionHandler(INTENT_ACTION_LOG, this);
            mController.registerActionHandler(INTENT_ACTION_DO_PERSISTENCE, this);
        }

        @Override
        public void onHandleAction(boolean enable, String action, Intent intent) {
            if (INTENT_ACTION_LOG.equals(action)) {
                sIsLogSwitchOn = enable;
                LauncherModel.changeDebugLoadersState(enable);
            } else if (INTENT_ACTION_DO_PERSISTENCE.equals(action)) {
                SwitcherPersistAsyncTask task = new SwitcherPersistAsyncTask();
                Pair<String, Boolean> pair = new Pair<String, Boolean>(KEY_SWITCH_LOG, sIsLogSwitchOn);
                task.execute(pair);
            }
        }
    }

    private class StatsSwitcher extends Switcher {
        private static final String INTENT_ACTION_STATS = "android.intent.action.sprd_switch_stats";

        public StatsSwitcher(SprdDynamicController controller) {
            super(controller);
        }

        @Override
        public void registerAction() {
            mController.registerActionHandler(INTENT_ACTION_STATS, this);
            mController.registerActionHandler(INTENT_ACTION_DO_PERSISTENCE, this);
        }

        @Override
        public void onHandleAction(boolean enable, String action, Intent intent) {
            if (INTENT_ACTION_STATS.equals(action)) {
                if (sIsStatsSwitchOn != enable) {
                    sIsStatsSwitchOn = enable;
                    Launcher launcher = mController.getLauncher();
                    if (launcher != null) {
                        launcher.resetStats(enable);
                    }
                }
            } else if (INTENT_ACTION_DO_PERSISTENCE.equals(action)) {
                SwitcherPersistAsyncTask task = new SwitcherPersistAsyncTask();
                Pair<String, Boolean> pair = new Pair<String, Boolean>(KEY_SWITCH_STATS, sIsStatsSwitchOn);
                task.execute(pair);
            }
        }
    }

    private class PersistenceSwitcher extends Switcher {
        private static final String INTENT_ACTION_PERSISTENCE = "android.intent.action.sprd_persistence";

        public PersistenceSwitcher(SprdDynamicController controller) {
            super(controller);
        }

        @Override
        public void registerAction() {
            mController.registerActionHandler(INTENT_ACTION_PERSISTENCE, this);
        }

        @Override
        public void onHandleAction(boolean enable, String action, Intent intent) {
            if (INTENT_ACTION_PERSISTENCE.equals(action)) {
                if (enable) {
                    Intent doPersistenceIntent = new Intent(INTENT_ACTION_DO_PERSISTENCE);
                    mController.handleIntent(doPersistenceIntent);
                } else {
                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            SharedPreferences sp = sContext.getSharedPreferences(DYNAMIC_SWITCHER_PREF, Context.MODE_PRIVATE);
                            sp.edit().clear().commit();

                            // TODO: delete other than clear?
                            /*
                            Launcher launcher = mController.getLauncher();
                            if (launcher != null) {
                                File fileDir = launcher.getFilesDir();
                                String absPath = fileDir.getAbsolutePath();
                                // file path is like '/data/user/0/com.android.launcher3/files'
                                if (absPath != null && !absPath.isEmpty()) {
                                    int index = absPath.lastIndexOf("/");
                                    if (index != -1) {
                                        // delete last '/files'
                                        absPath = absPath.substring(0, index);
                                        // compose the final path, it is like '/data/user/0/com.android.launcher3/shared_prefs/dynamic_switcher_pref.xml'
                                        absPath += String.format("/shared_prefs/%s.xml", DYNAMIC_SWITCHER_PREF);
                                        File sharedF = new File(absPath);
                                        sharedF.delete();
                                    }
                                }
                            }
                            */

                            return null;
                        }
                    };
                    task.execute();
                }
            }
        }
    }
}
