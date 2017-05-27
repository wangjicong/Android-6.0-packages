/**
 * Copyright (c) 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;
import android.annotation.NonNull;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.service.carrier.CarrierIdentifier;
import android.service.carrier.CarrierService;
import android.service.carrier.ICarrierService;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.ICarrierConfigLoader;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.FastXmlSerializer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * CarrierConfigLoader binds to privileged carrier apps to fetch carrier config overlays.
 */

public class CarrierConfigLoader extends ICarrierConfigLoader.Stub {
    private static final String LOG_TAG = "CarrierConfigLoader";
    // Package name for default carrier config app, bundled with system image.
    private static final String DEFAULT_CARRIER_CONFIG_PACKAGE = "com.android.carrierconfig";

    /** The singleton instance. */
    private static CarrierConfigLoader sInstance;
    // The context for phone app, passed from PhoneGlobals.
    private Context mContext;
    // Carrier configs from default app, indexed by phoneID.
    private PersistableBundle[] mConfigFromDefaultApp;
    // Carrier configs from privileged carrier config app, indexed by phoneID.
    private PersistableBundle[] mConfigFromCarrierApp;
    // Service connection for binding to config app.
    private CarrierServiceConnection[] mServiceConnection;

    // SPRD: [bug475223] Net configs from default app, indexed by phoneID.
    private PersistableBundle[] mNetPreferredConfigFromDefaultApp;
    // SPRD: [bug475223] Feature configs from default app, include area config,country config and operator config.
    private PersistableBundle mFeatureConfigFromDefaultApp;

    // Broadcast receiver for SIM and pkg intents, register intent filter in constructor.
    private final BroadcastReceiver mReceiver = new ConfigLoaderBroadcastReceiver();

    // Message codes; see mHandler below.
    // Request from SubscriptionInfoUpdater when SIM becomes absent or error.
    private static final int EVENT_CLEAR_CONFIG = 0;
    // Has connected to default app.
    private static final int EVENT_CONNECTED_TO_DEFAULT = 3;
    // Has connected to carrier app.
    private static final int EVENT_CONNECTED_TO_CARRIER = 4;
    // Config has been loaded from default app.
    private static final int EVENT_LOADED_FROM_DEFAULT = 5;
    // Config has been loaded from carrier app.
    private static final int EVENT_LOADED_FROM_CARRIER = 6;
    // Attempt to fetch from default app or read from XML.
    private static final int EVENT_FETCH_DEFAULT = 7;
    // Attempt to fetch from carrier app or read from XML.
    private static final int EVENT_FETCH_CARRIER = 8;
    // A package has been installed, uninstalled, or updated.
    private static final int EVENT_PACKAGE_CHANGED = 9;
    // Bind timed out for the default app.
    private static final int EVENT_BIND_DEFAULT_TIMEOUT = 10;
    // Bind timed out for a carrier app.
    private static final int EVENT_BIND_CARRIER_TIMEOUT = 11;
    // Check if the system fingerprint has changed.
    private static final int EVENT_CHECK_SYSTEM_UPDATE = 12;

    // SPRD: [bug475223] Need to clear network preferred config when no network registered.
    private static final int EVENT_CLEAR_NET_PRE_CONFIG = 20;
    // SPRD: [bug475223] Need to clear feature config when no feature config info set.
    private static final int EVENT_CLEAR_FEATURE_CONFIG = 21;
    // SPRD: [bug475223] Attempt to fetch from default app or read from XML to get network preferred config.
    private static final int EVENT_FETCH_DEFAULT_FOR_NET_PRE_CONFIG = 22;
    // SPRD: [bug475223] Has connected to default app for network preferred config.
    private static final int EVENT_CONNECTED_TO_DEFAULT_FOR_NET_PRE_CONFIG = 23;
    // SPRD: [bug475223] Attempt to fetch from default app or read from XML to get feature configs.
    private static final int EVENT_FETCH_DEFAULT_FOR_FEATURE_CONFIG = 24;
    // SPRD: [bug475223] Has connected to default app for feature configs.
    private static final int EVENT_CONNECTED_TO_DEFAULT_FOR_FEATURE_CONFIG = 25;

    private static final int BIND_TIMEOUT_MILLIS = 10000;

    // Tags used for saving and restoring XML documents.
    private static final String TAG_DOCUMENT = "carrier_config";
    private static final String TAG_VERSION = "package_version";
    private static final String TAG_BUNDLE = "bundle_data";

    // SharedPreferences key for last known build fingerprint.
    private static final String KEY_FINGERPRINT = "build_fingerprint";

    private String[] mLastNetNumerics;

    // Handler to process various events.
    //
    // For each phoneId, the event sequence should be:
    //     fetch default, connected to default, loaded from default,
    //     fetch carrier, connected to carrier, loaded from carrier.
    //
    // If there is a saved config file for either the default app or the carrier app, we skip
    // binding to the app and go straight from fetch to loaded.
    //
    // At any time, at most one connection is active. If events are not in this order, previous
    // connection will be unbound, so only latest event takes effect.
    //
    // We broadcast ACTION_CARRIER_CONFIG_CHANGED after:
    // 1. loading from carrier app (even if read from a file)
    // 2. loading from default app if there is no carrier app (even if read from a file)
    // 3. clearing config (e.g. due to sim removal)
    // 4. encountering bind or IPC error
    private Handler mHandler = new Handler() {
            @Override
        public void handleMessage(Message msg) {
            int phoneId = msg.arg1;
            log("mHandler: " + msg.what + " phoneId: " + phoneId);
            String iccid;
            CarrierIdentifier carrierId;
            String carrierPackageName;
            CarrierServiceConnection conn;
            PersistableBundle config;
            switch (msg.what) {
                case EVENT_CLEAR_CONFIG:
                    if (mConfigFromDefaultApp[phoneId] == null &&
                        mConfigFromCarrierApp[phoneId] == null)
                        break;
                    mConfigFromDefaultApp[phoneId] = null;
                    mConfigFromCarrierApp[phoneId] = null;
                    mServiceConnection[phoneId] = null;
                    broadcastConfigChangedIntent(phoneId);
                    break;

                /* SPRD: [bug475223] Clear net configs when no network registered. @{ */
                case EVENT_CLEAR_NET_PRE_CONFIG:
                    if (mNetPreferredConfigFromDefaultApp[phoneId] == null) break;
                    mNetPreferredConfigFromDefaultApp[phoneId] = null;
                    broadcastConfigChangedIntent(phoneId);
                    break;
                /* @} */

                /* SPRD: [bug475223] Clear feature configs when on feature config info set. @{ */
                case EVENT_CLEAR_FEATURE_CONFIG:
                    if (mFeatureConfigFromDefaultApp == null) break;
                    mFeatureConfigFromDefaultApp = null;
                    broadcastConfigChangedIntent(phoneId);
                    break;
                /* @} */

                case EVENT_PACKAGE_CHANGED:
                    carrierPackageName = (String) msg.obj;
                    // Only update if there are cached config removed to avoid updating config
                    // for unrelated packages.
                    if (clearCachedConfigForPackage(carrierPackageName)) {
                        int numPhones = TelephonyManager.from(mContext).getPhoneCount();
                        for (int i = 0; i < numPhones; ++i) {
                            updateConfigForPhoneId(i);
                        }
                    }
                    break;

                case EVENT_FETCH_DEFAULT:
                    iccid = getIccIdForPhoneId(phoneId);
                    config = restoreConfigFromXml(DEFAULT_CARRIER_CONFIG_PACKAGE, iccid);
                    if (config != null) {
                        log("Loaded config from XML. package=" + DEFAULT_CARRIER_CONFIG_PACKAGE
                                + " phoneId=" + phoneId);
                        mConfigFromDefaultApp[phoneId] = config;
                        Message newMsg = obtainMessage(EVENT_LOADED_FROM_DEFAULT, phoneId, -1);
                        newMsg.getData().putBoolean("loaded_from_xml", true);
                        mHandler.sendMessage(newMsg);
                    } else {
                        if (bindToConfigPackage(DEFAULT_CARRIER_CONFIG_PACKAGE,
                                phoneId, EVENT_CONNECTED_TO_DEFAULT)) {
                            sendMessageDelayed(obtainMessage(EVENT_BIND_DEFAULT_TIMEOUT, phoneId, -1),
                                    BIND_TIMEOUT_MILLIS);
                        } else {
                            // Send bcast if bind fails
                            broadcastConfigChangedIntent(phoneId);
                        }
                    }
                    break;

                /* SPRD: [bug475223] Add for network preferred config and feature configs @{ */
                case EVENT_FETCH_DEFAULT_FOR_NET_PRE_CONFIG:
                case EVENT_FETCH_DEFAULT_FOR_FEATURE_CONFIG:
                    fetchDefaultConfig(msg);
                    break;

                case EVENT_CONNECTED_TO_DEFAULT_FOR_NET_PRE_CONFIG:
                case EVENT_CONNECTED_TO_DEFAULT_FOR_FEATURE_CONFIG:
                    connectToDefaultApp(msg);
                    break;
                /* @} */

                case EVENT_CONNECTED_TO_DEFAULT:
                    removeMessages(EVENT_BIND_DEFAULT_TIMEOUT);
                    carrierId = getCarrierIdForPhoneId(phoneId);
                    conn = (CarrierServiceConnection) msg.obj;
                    // If new service connection has been created, unbind.
                    if (mServiceConnection[phoneId] != conn || conn.service == null) {
                        mContext.unbindService(conn);
                        break;
                    }
                    try {
                        ICarrierService carrierService = ICarrierService.Stub
                                .asInterface(conn.service);
                        config = carrierService.getCarrierConfig(carrierId);
                        iccid = getIccIdForPhoneId(phoneId);
                        saveConfigToXml(DEFAULT_CARRIER_CONFIG_PACKAGE, iccid, config);
                        mConfigFromDefaultApp[phoneId] = config;
                        sendMessage(obtainMessage(EVENT_LOADED_FROM_DEFAULT, phoneId, -1));
                    } catch (RemoteException ex) {
                        loge("Failed to get carrier config: " + ex.toString());
                    } finally {
                        mContext.unbindService(mServiceConnection[phoneId]);
                    }
                    break;

                case EVENT_BIND_DEFAULT_TIMEOUT:
                    mContext.unbindService(mServiceConnection[phoneId]);
                    broadcastConfigChangedIntent(phoneId);
                    break;

                case EVENT_LOADED_FROM_DEFAULT:
                    // If we attempted to bind to the app, but the service connection is null, then
                    // config was cleared while we were waiting and we should not continue.
                    if (!msg.getData().getBoolean("loaded_from_xml", false)
                            && mServiceConnection[phoneId] == null) {
                        break;
                    }
                    carrierPackageName = getCarrierPackageForPhoneId(phoneId);
                    if (carrierPackageName != null) {
                        log("Found carrier config app: " + carrierPackageName);
                        sendMessage(obtainMessage(EVENT_FETCH_CARRIER, phoneId));
                    } else {
                        broadcastConfigChangedIntent(phoneId);
                    }
                    break;

                case EVENT_FETCH_CARRIER:
                    carrierPackageName = getCarrierPackageForPhoneId(phoneId);
                    iccid = getIccIdForPhoneId(phoneId);
                    config = restoreConfigFromXml(carrierPackageName, iccid);
                    if (config != null) {
                        log("Loaded config from XML. package=" + carrierPackageName + " phoneId="
                                + phoneId);
                        mConfigFromCarrierApp[phoneId] = config;
                        Message newMsg = obtainMessage(EVENT_LOADED_FROM_CARRIER, phoneId, -1);
                        newMsg.getData().putBoolean("loaded_from_xml", true);
                        sendMessage(newMsg);
                    } else {
                        if (carrierPackageName != null
                            && bindToConfigPackage(carrierPackageName, phoneId,
                                    EVENT_CONNECTED_TO_CARRIER)) {
                            sendMessageDelayed(obtainMessage(EVENT_BIND_CARRIER_TIMEOUT, phoneId, -1),
                                    BIND_TIMEOUT_MILLIS);
                        } else {
                            // Send bcast if bind fails
                            broadcastConfigChangedIntent(phoneId);
                        }
                    }
                    break;

                case EVENT_CONNECTED_TO_CARRIER:
                    removeMessages(EVENT_BIND_CARRIER_TIMEOUT);
                    carrierId = getCarrierIdForPhoneId(phoneId);
                    conn = (CarrierServiceConnection) msg.obj;
                    // If new service connection has been created, unbind.
                    if (mServiceConnection[phoneId] != conn ||
                            conn.service == null) {
                        mContext.unbindService(conn);
                        break;
                    }
                    try {
                        ICarrierService carrierService = ICarrierService.Stub
                                .asInterface(conn.service);
                        config = carrierService.getCarrierConfig(carrierId);
                        carrierPackageName = getCarrierPackageForPhoneId(phoneId);
                        iccid = getIccIdForPhoneId(phoneId);
                        saveConfigToXml(carrierPackageName, iccid, config);
                        mConfigFromCarrierApp[phoneId] = config;
                        sendMessage(obtainMessage(EVENT_LOADED_FROM_CARRIER, phoneId, -1));
                    } catch (RemoteException ex) {
                        loge("Failed to get carrier config: " + ex.toString());
                    } finally {
                        mContext.unbindService(mServiceConnection[phoneId]);
                    }
                    break;

                case EVENT_BIND_CARRIER_TIMEOUT:
                    mContext.unbindService(mServiceConnection[phoneId]);
                    broadcastConfigChangedIntent(phoneId);
                    break;

                case EVENT_LOADED_FROM_CARRIER:
                    // If we attempted to bind to the app, but the service connection is null, then
                    // config was cleared while we were waiting and we should not continue.
                    if (!msg.getData().getBoolean("loaded_from_xml", false)
                            && mServiceConnection[phoneId] == null) {
                        break;
                    }
                    broadcastConfigChangedIntent(phoneId);
                    break;

                case EVENT_CHECK_SYSTEM_UPDATE:
                    SharedPreferences sharedPrefs =
                            PreferenceManager.getDefaultSharedPreferences(mContext);
                    final String lastFingerprint = sharedPrefs.getString(KEY_FINGERPRINT, null);
                    if (!Build.FINGERPRINT.equals(lastFingerprint)) {
                        log("Build fingerprint changed. old: "
                                + lastFingerprint + " new: " + Build.FINGERPRINT);
                        clearCachedConfigForPackage(null);
                        sharedPrefs.edit().putString(KEY_FINGERPRINT, Build.FINGERPRINT).apply();
                    }
                    break;
            }
        }
    };

    /**
     * Constructs a CarrierConfigLoader, registers it as a service, and registers a broadcast
     * receiver for relevant events.
     */
    private CarrierConfigLoader(Context context) {
        mContext = context;

        // Register for package updates. Update app or uninstall app update will have all 3 intents,
        // in the order or removed, added, replaced, all with extra_replace set to true.
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pkgFilter.addDataScheme("package");
        context.registerReceiverAsUser(mReceiver, UserHandle.ALL, pkgFilter, null, null);

        /* SPRD: [bug475223] Add for receiving broadcast SERVICE_STATE_CHANGED to update network preferred config @{ */
        IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        context.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);
        /* @} */

        int numPhones = TelephonyManager.from(context).getPhoneCount();
        mConfigFromDefaultApp = new PersistableBundle[numPhones];
        mConfigFromCarrierApp = new PersistableBundle[numPhones];
        mNetPreferredConfigFromDefaultApp = new PersistableBundle[numPhones];
        mLastNetNumerics = new String[numPhones];
        mServiceConnection = new CarrierServiceConnection[numPhones];
        // Make this service available through ServiceManager.
        ServiceManager.addService(Context.CARRIER_CONFIG_SERVICE, this);
        log("CarrierConfigLoader has started");
        mHandler.sendEmptyMessage(EVENT_CHECK_SYSTEM_UPDATE);

        /* SPRD: [bug475223] Update feature configs when phone app starts.Feature configs are fixed unless set manually.
         So there's no need to wait for sim loaded or network registered. @{ */
        updateFeatureConfigs();
        /* @} */
    }

    /**
     * Initialize the singleton CarrierConfigLoader instance.
     *
     * This is only done once, at startup, from {@link com.android.phone.PhoneApp#onCreate}.
     */
    /* package */
    static CarrierConfigLoader init(Context context) {
        synchronized (CarrierConfigLoader.class) {
            if (sInstance == null) {
                sInstance = new CarrierConfigLoader(context);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    private void broadcastConfigChangedIntent(int phoneId) {
        Intent intent = new Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE,
                UserHandle.USER_ALL);
    }

    /** Binds to the default or carrier config app. */
    private boolean bindToConfigPackage(String pkgName, int phoneId, int eventId) {
        log("Binding to " + pkgName + " for phone " + phoneId);
        Intent carrierService = new Intent(CarrierService.CARRIER_SERVICE_INTERFACE);
        carrierService.setPackage(pkgName);
        mServiceConnection[phoneId] = new CarrierServiceConnection(phoneId, eventId);
        try {
            return mContext.bindService(carrierService, mServiceConnection[phoneId],
                    Context.BIND_AUTO_CREATE);
        } catch (SecurityException ex) {
            return false;
        }
    }

    private CarrierIdentifier getCarrierIdForPhoneId(int phoneId) {
        String mcc = "";
        String mnc = "";
        String imsi = "";
        String gid1 = "";
        String gid2 = "";
        String spn = "";
//        String spn = TelephonyManager.from(mContext).getSimOperatorNameForPhone(phoneId);
        String simOperator = TelephonyManager.from(mContext).getSimOperatorNumericForPhone(phoneId);
        // A valid simOperator should be 5 or 6 digits, depending on the length of the MNC.
        if (simOperator != null && simOperator.length() >= 3) {
            mcc = simOperator.substring(0, 3);
            mnc = simOperator.substring(3);
        }
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            imsi = phone.getSubscriberId();
            gid1 = phone.getGroupIdLevel1();
            gid2 = phone.getGroupIdLevel2();
        }

        /* SPRD: [bug475223] Add for mvno match type PNN @{ */
        String pnn = "";
        UiccCard uiccCard =phone.getUiccCard();
        if (uiccCard != null) {
            UiccCardApplication uiccCardApp = uiccCard.getApplication(UiccController.APP_FAM_3GPP);
            if (uiccCardApp != null) {
                IccRecords records = uiccCardApp.getIccRecords();
                if (records != null && records instanceof SIMRecords) {
                    pnn = ((SIMRecords)records).getPnnHomeName();
                    // SPRD: [bug475223] Make sure spn value is loaded from sim and haven't been covered.
                    spn = ((SIMRecords)records).getOriginalServiceProviderName();
                }
            }
        }

        return new CarrierIdentifier(mcc, mnc, spn, imsi, gid1, gid2, pnn);
        /* @} */
    }

    /** Returns the package name of a priveleged carrier app, or null if there is none. */
    private String getCarrierPackageForPhoneId(int phoneId) {
        List<String> carrierPackageNames = TelephonyManager.from(mContext)
                .getCarrierPackageNamesForIntentAndPhone(
                        new Intent(CarrierService.CARRIER_SERVICE_INTERFACE), phoneId);
        if (carrierPackageNames != null && carrierPackageNames.size() > 0) {
            return carrierPackageNames.get(0);
        } else {
            return null;
        }
    }

    private String getIccIdForPhoneId(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            return null;
        }
        return phone.getIccSerialNumber();
    }

    /**
     * Writes a bundle to an XML file.
     *
     * The bundle will be written to a file named after the package name and ICCID, so that it can
     * be restored later with {@link @restoreConfigFromXml}. The XML output will include the bundle
     * and the current version of the specified package.
     *
     * In case of errors or invalid input, no file will be written.
     *
     * @param packageName the name of the package from which we fetched this bundle.
     * @param iccid the ICCID of the subscription for which this bundle was fetched.
     * @param config the bundle to be written. Null will be treated as an empty bundle.
     */
    private void saveConfigToXml(String packageName, String iccid, PersistableBundle config) {
        if (packageName == null || iccid == null) {
            loge("Cannot save config with null packageName or iccid.");
            return;
        }
        if (config == null) {
          config = new PersistableBundle();
        }

        final String version = getPackageVersion(packageName);
        if (version == null) {
            loge("Failed to get package version for: " + packageName);
            return;
        }

        FileOutputStream outFile = null;
        try {
            outFile = new FileOutputStream(
                    new File(mContext.getFilesDir(), getFilenameForConfig(packageName, iccid)));
            FastXmlSerializer out = new FastXmlSerializer();
            out.setOutput(outFile, "utf-8");
            out.startDocument("utf-8", true);
            out.startTag(null, TAG_DOCUMENT);
            out.startTag(null, TAG_VERSION);
            out.text(version);
            out.endTag(null, TAG_VERSION);
            out.startTag(null, TAG_BUNDLE);
            config.saveToXml(out);
            out.endTag(null, TAG_BUNDLE);
            out.endTag(null, TAG_DOCUMENT);
            out.endDocument();
            out.flush();
            outFile.close();
        }
        catch (IOException e) {
            loge(e.toString());
        }
        catch (XmlPullParserException e) {
            loge(e.toString());
        }
    }

    /**
     * Reads a bundle from an XML file.
     *
     * This restores a bundle that was written with {@link #saveConfigToXml}. This returns the saved
     * config bundle for the given package and ICCID.
     *
     * In case of errors, or if the saved config is from a different package version than the
     * current version, then null will be returned.
     *
     * @param packageName the name of the package from which we fetched this bundle.
     * @param iccid the ICCID of the subscription for which this bundle was fetched.
     * @return the bundle from the XML file. Returns null if there is no saved config, the saved
     *         version does not match, or reading config fails.
     */
    private PersistableBundle restoreConfigFromXml(String packageName, String iccid) {
        final String version = getPackageVersion(packageName);
        if (version == null) {
            loge("Failed to get package version for: " + packageName);
            return null;
        }
        if (packageName == null || iccid == null) {
            loge("Cannot restore config with null packageName or iccid.");
            return null;
        }

        PersistableBundle restoredBundle = null;
        FileInputStream inFile = null;
        try {
            inFile = new FileInputStream(
                    new File(mContext.getFilesDir(), getFilenameForConfig(packageName, iccid)));
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(inFile, "utf-8");

            int event;
            while (((event = parser.next()) != XmlPullParser.END_DOCUMENT)) {

                if (event == XmlPullParser.START_TAG && TAG_VERSION.equals(parser.getName())) {
                    String savedVersion = parser.nextText();
                    if (!version.equals(savedVersion)) {
                        log("Saved version mismatch: " + version + " vs " + savedVersion);
                        break;
                    }
                }

                if (event == XmlPullParser.START_TAG && TAG_BUNDLE.equals(parser.getName())) {
                    restoredBundle = PersistableBundle.restoreFromXml(parser);
                }
            }
            inFile.close();
        }
        catch (FileNotFoundException e) {
            loge(e.toString());
        }
        catch (XmlPullParserException e) {
            loge(e.toString());
        }
        catch (IOException e) {
            loge(e.toString());
        }

        return restoredBundle;
    }

    /**
     * Clears cached carrier config.
     * This deletes all saved XML files associated with the given package name. If packageName is
     * null, then it deletes all saved XML files.
     *
     * @param packageName the name of a carrier package, or null if all cached config should be
     *                    cleared.
     * @return true iff one or more files were deleted.
     */
    private boolean clearCachedConfigForPackage(final String packageName) {
        File dir = mContext.getFilesDir();

        if (dir == null) {
            return false;
        }

        File[] packageFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                if (packageName != null) {
                    return filename.startsWith("carrierconfig-" + packageName + "-");
                } else {
                    return filename.startsWith("carrierconfig-");
                }
            }
        });
        if (packageFiles == null || packageFiles.length < 1) return false;
        for (File f : packageFiles) {
            log("deleting " + f.getName());
            f.delete();
        }
        return true;
    }

    /** Builds a canonical file name for a config file. */
    private String getFilenameForConfig(@NonNull String packageName, @NonNull String iccid) {
        return "carrierconfig-" + packageName + "-" + iccid + ".xml";
    }

    /** Return the current version code of a package, or null if the name is not found. */
    private String getPackageVersion(String packageName) {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(packageName, 0);
            return Integer.toString(info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Read up to date config.
     *
     * This reads config bundles for the given phoneId. That means getting the latest bundle from
     * the default app and a privileged carrier app, if present. This will not bind to an app if we
     * have a saved config file to use instead.
     */
    private void updateConfigForPhoneId(int phoneId) {
        // Clear in-memory cache for carrier app config, so when carrier app gets uninstalled, no
        // stale config is left.
        if (mConfigFromCarrierApp[phoneId] != null &&
                getCarrierPackageForPhoneId(phoneId) == null) {
            mConfigFromCarrierApp[phoneId] = null;
        }
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_FETCH_DEFAULT, phoneId, -1));
    }

    @Override public
    @NonNull
    PersistableBundle getConfigForSubId(int subId) {
        /* SPRD: [bug475223] To avoid duplicate codes @{ */
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return getConfigForPhoneId(phoneId);
        /* @} */
    }

    @Override
    public void notifyConfigChangedForSubId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            log("Ignore invalid phoneId: " + phoneId + " for subId: " + subId);
            return;
        }
        String callingPackageName = mContext.getPackageManager().getNameForUid(
                Binder.getCallingUid());
        // TODO: Check that the calling packages is privileged for subId specifically.
        int privilegeStatus = TelephonyManager.from(mContext).checkCarrierPrivilegesForPackage(
                callingPackageName);
        if (privilegeStatus != TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
            throw new SecurityException(
                    "Package is not privileged for subId=" + subId + ": " + callingPackageName);
        }

        // This method should block until deleting has completed, so that an error which prevents us
        // from clearing the cache is passed back to the carrier app. With the files successfully
        // deleted, this can return and we will eventually bind to the carrier app.
        clearCachedConfigForPackage(callingPackageName);
        updateConfigForPhoneId(phoneId);
    }

    @Override
    public void updateConfigForPhoneId(int phoneId, String simState) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.MODIFY_PHONE_STATE, null);
        log("update config for phoneId: " + phoneId + " simState: " + simState);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return;
        }
        // requires Java 7 for switch on string.
        switch (simState) {
            case IccCardConstants.INTENT_VALUE_ICC_ABSENT:
            case IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR:
            case IccCardConstants.INTENT_VALUE_ICC_UNKNOWN:
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_CLEAR_CONFIG, phoneId, -1));
                break;
            case IccCardConstants.INTENT_VALUE_ICC_LOADED:
            case IccCardConstants.INTENT_VALUE_ICC_LOCKED:
                updateConfigForPhoneId(phoneId);
                break;
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump carrierconfig from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("CarrierConfigLoader: " + this);
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            pw.println("  Phone Id=" + i);
            pw.println("  mConfigFromDefaultApp=" + mConfigFromDefaultApp[i]);
            pw.println("  mConfigFromCarrierApp=" + mConfigFromCarrierApp[i]);
        }
    }

    private class CarrierServiceConnection implements ServiceConnection {
        int phoneId;
        int eventId;
        IBinder service;

        public CarrierServiceConnection(int phoneId, int eventId) {
            this.phoneId = phoneId;
            this.eventId = eventId;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("Connected to config app: " + name.flattenToString());
            this.service = service;
            mHandler.sendMessage(mHandler.obtainMessage(eventId, phoneId, -1, this));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.service = null;
        }
    }

    private class ConfigLoaderBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /* SPRD: [bug475223] Update network preferred config after receive broadcast ACTION_SERVICE_STATE_CHANGED @{ */
            if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                updateNetConfigForPhoneId(phoneId);
                /* @} */
            } else {
                boolean replace = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                // If replace is true, only care ACTION_PACKAGE_REPLACED.
                if (replace && !Intent.ACTION_PACKAGE_REPLACED.equals(action))
                    return;

                switch (action) {
                    case Intent.ACTION_PACKAGE_ADDED:
                    case Intent.ACTION_PACKAGE_REMOVED:
                    case Intent.ACTION_PACKAGE_REPLACED:
                        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
                        String packageName = mContext.getPackageManager().getNameForUid(uid);
                        if (packageName != null) {
                            // We don't have a phoneId for arg1.
                            mHandler.sendMessage(
                                    mHandler.obtainMessage(EVENT_PACKAGE_CHANGED, packageName));
                        }
                        break;
                }
            }
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    /* SPRD: [bug475223] Add for network preferred config and feature configs @{ */
    private void fetchDefaultConfig(Message msg) {
        int phoneId = msg.arg1;
        boolean forFeatureConfig = msg.what == EVENT_FETCH_DEFAULT_FOR_FEATURE_CONFIG;
        PersistableBundle config = restoreConfigFromXml(DEFAULT_CARRIER_CONFIG_PACKAGE,
                forFeatureConfig ? getFeatureConfigsInfo() : getNetworkNumeric(phoneId));
        if (config != null) {
            log("Loaded config from XML[" + msg.what + "]. package=" + DEFAULT_CARRIER_CONFIG_PACKAGE
                    + " phoneId=" + phoneId);
            if (forFeatureConfig) {
                mFeatureConfigFromDefaultApp = config;
            } else {
                mNetPreferredConfigFromDefaultApp[phoneId] = config;
            }
            broadcastConfigChangedIntent(phoneId);
        } else {
            if (bindToConfigPackage(DEFAULT_CARRIER_CONFIG_PACKAGE,
                    phoneId, forFeatureConfig ? EVENT_CONNECTED_TO_DEFAULT_FOR_FEATURE_CONFIG
                            : EVENT_CONNECTED_TO_DEFAULT_FOR_NET_PRE_CONFIG)) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_BIND_DEFAULT_TIMEOUT, phoneId, -1),
                        BIND_TIMEOUT_MILLIS);
            } else {
                // Send bcast if bind fails
                broadcastConfigChangedIntent(phoneId);
            }
        }
    }

    private void connectToDefaultApp(Message msg) {
        mHandler.removeMessages(EVENT_BIND_DEFAULT_TIMEOUT);
        int phoneId = msg.arg1;
        CarrierServiceConnection conn = (CarrierServiceConnection) msg.obj;
        // If new service connection has been created, unbind.
        if (mServiceConnection[phoneId] != conn || conn.service == null) {
            mContext.unbindService(conn);
            return;
        }
        try {
            ICarrierService carrierService = ICarrierService.Stub
                    .asInterface(conn.service);
            CarrierIdentifier carrierId = null;
            PersistableBundle config = null;
            if (msg.what == EVENT_CONNECTED_TO_DEFAULT_FOR_FEATURE_CONFIG) {
                carrierId = new CarrierIdentifier(null, null, null, getFeatureConfigsInfo());
                config = carrierService.getCarrierConfig(carrierId);
                mFeatureConfigFromDefaultApp = config;
                saveConfigToXml(DEFAULT_CARRIER_CONFIG_PACKAGE, getFeatureConfigsInfo(), config);
            } else {
                carrierId = getNetPreCarrierIdForPhoneId(phoneId);
                config = carrierService.getCarrierConfig(carrierId);
                mNetPreferredConfigFromDefaultApp[phoneId] = config;
                saveConfigToXml(DEFAULT_CARRIER_CONFIG_PACKAGE, getNetworkNumeric(phoneId), config);
            }
            broadcastConfigChangedIntent(phoneId);
        } catch (RemoteException ex) {
            loge("Failed to get config[" + msg.what + "]: " + ex.toString());
        } finally {
            mContext.unbindService(mServiceConnection[phoneId]);
        }
    }
    /* @} */

    private CarrierIdentifier getNetPreCarrierIdForPhoneId(int phoneId) {
        String mcc = "";
        String mnc = "";
        String netNumeric = getNetworkNumeric(phoneId);
        if (netNumeric != null && netNumeric.length() >= 3) {
            mcc = netNumeric.substring(0, 3);
            mnc = netNumeric.substring(3);
        }
        return new CarrierIdentifier(mcc, mnc, "true", null);
    }

    private String getFeatureConfigsInfo() {
        return SystemProperties.get("persist.sys.boradv");
    }

    private String getNetworkNumeric(int phoneId) {
        return TelephonyManager.from(mContext).getNetworkOperatorForPhone(phoneId);
    }

    /**
     * SPRD: [bug475223] Update network preferred configs when service state changed.
     * Some configs such as ecc list are decided by registered network instead of sim card.
     */
    public void updateNetConfigForPhoneId(int phoneId) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.MODIFY_PHONE_STATE, null);
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            String numeric = getNetworkNumeric(phoneId);
            log("update network preferred config[" + phoneId + "]: "
                    + numeric + " mLastNetNumeric = " + mLastNetNumerics[phoneId]);
            if (!numeric.equals(mLastNetNumerics[phoneId])) {
                if (TextUtils.isEmpty(numeric)) {
                    mHandler.sendMessage(mHandler.obtainMessage(EVENT_CLEAR_NET_PRE_CONFIG, phoneId, -1));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(EVENT_FETCH_DEFAULT_FOR_NET_PRE_CONFIG, phoneId, -1));
                }
                mLastNetNumerics[phoneId] = numeric;
            }
        }
    }

    /**
     * SPRD: [bug475223] Update feature configs include area config, country config and operator config.
     */
    public void updateFeatureConfigs() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.MODIFY_PHONE_STATE, null);
        String featureConfig = getFeatureConfigsInfo();
        log("update feature configs : " + featureConfig);
        if (TextUtils.isEmpty(featureConfig)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_CLEAR_FEATURE_CONFIG, 0, -1));
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_FETCH_DEFAULT_FOR_FEATURE_CONFIG, 0 , -1));
        }
    }

    /**
     * SPRD: [bug475223] Get carrier configs by phoneId.
     */
    @Override
    @NonNull
    public PersistableBundle getConfigForPhoneId(int phoneId) {
        try {
            mContext.enforceCallingOrSelfPermission(READ_PRIVILEGED_PHONE_STATE, null);
            // SKIP checking run-time READ_PHONE_STATE since using PRIVILEGED
        } catch (SecurityException e) {
            mContext.enforceCallingOrSelfPermission(READ_PHONE_STATE, null);
        }
        PersistableBundle retConfig = CarrierConfigManager.getDefaultConfig();
        // SPRD: [bug529444] To avoid ArrayIndexOutOfBoundsException during switch Single-SIM-Mode to Dual-SIM-Mode.
        if (SubscriptionManager.isValidPhoneId(phoneId) && phoneId < mConfigFromDefaultApp.length) {
            PersistableBundle config = mConfigFromDefaultApp[phoneId];
            if (config != null)
                retConfig.putAll(config);
            config = mNetPreferredConfigFromDefaultApp[phoneId];
            if (config != null)
                retConfig.putAll(config);
            config = mFeatureConfigFromDefaultApp;
            if (config != null)
                retConfig.putAll(config);
            config = mConfigFromCarrierApp[phoneId];
            if (config != null) {
                retConfig.putAll(config);
            }
        }
        return retConfig;
    }
    /* @} */
}
