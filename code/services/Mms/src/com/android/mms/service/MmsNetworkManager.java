/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.SystemClock;
/* Add for bug 542996 {@ */
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
/* @} */
import com.android.mms.service.exception.MmsNetworkException;

/**
 * Manages the MMS network connectivity
 */
public class MmsNetworkManager {
    // Timeout used to call ConnectivityManager.requestNetwork
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 60 * 1000;
    // Wait timeout for this class, a little bit longer than the above timeout
    // to make sure we don't bail prematurely
    private static final int NETWORK_ACQUIRE_TIMEOUT_MILLIS =
            NETWORK_REQUEST_TIMEOUT_MILLIS + (5 * 1000);

    private final Context mContext;

    // The requested MMS {@link android.net.Network} we are holding
    // We need this when we unbind from it. This is also used to indicate if the
    // MMS network is available.
    private Network mNetwork;
    // The current count of MMS requests that require the MMS network
    // If mMmsRequestCount is 0, we should release the MMS network.
    private int mMmsRequestCount;
    // This is really just for using the capability
    private final NetworkRequest mNetworkRequest;
    // The callback to register when we request MMS network
    // Modify by SPRD for bug 542996
    private ArrayList<ConnectivityManager.NetworkCallback> mNetworkCallbacks =
            new ArrayList<ConnectivityManager.NetworkCallback>();

    private volatile ConnectivityManager mConnectivityManager;

    // The MMS HTTP client for this network
    private MmsHttpClient mMmsHttpClient;

    // The SIM ID which we use to connect
    private final int mSubId;

    /* Add by SPRD for bug 542996 Start */
    private static AtomicReference<MmsNetworkManager> mPendingReleaseMmsNetworkManager = new AtomicReference<MmsNetworkManager>();
    private static final int EVENT_NETWORK_REQUEST = 1;
    private static final int EVENT_NETWORK_RELEASE = 2;
    private static final int EVENT_NETWORK_RELEASE_DELAY = 3;
    private static final int EVENT_NETWORK_HANDLER_QUIT = 4;
    private static final int NETWORK_RELEASE_PROCESS_DELAY = 3 * 1000;
    private static final int NETWORK_RELEASE_PROCESS_WAIT = 4 * 1000;

    private static NetworkRelease mReleaseNwHandler = null;

    private final class NetworkRelease extends Handler{
        public NetworkRelease() {
            super();
        }

        public NetworkRelease(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg){
            synchronized (this) {
                switch (msg.what) {
                    case EVENT_NETWORK_RELEASE: // Delay to release the network
                    case EVENT_NETWORK_RELEASE_DELAY:
                        if (mPendingReleaseMmsNetworkManager.get() != null) {
                            LogUtil.d("MmsNetworkManager: handle EVENT_NETWORK_RELEASE message.");
                            ArrayList<ConnectivityManager.NetworkCallback> networkCallbacks =
                                    mPendingReleaseMmsNetworkManager.get().mNetworkCallbacks;
                            synchronized (networkCallbacks) {
                                Iterator<ConnectivityManager.NetworkCallback> iterator = networkCallbacks.iterator();
                                while (iterator.hasNext()) {
                                    ConnectivityManager.NetworkCallback callback = iterator.next();
                                    iterator.remove();
                                    mPendingReleaseMmsNetworkManager.get().releaseRequestLocked(callback);
                                }
                            }
                        } else {
                            return;
                        }
                        try {
                            Thread.sleep(NETWORK_RELEASE_PROCESS_WAIT);
                        } catch (InterruptedException e) {
                            LogUtil.d("MmsNetworkManager: something wrong with Thread.sleep, return.");
                            e.printStackTrace();
                            return;
                        }
                        break;

                  case EVENT_NETWORK_HANDLER_QUIT:
                      getLooper().quit();
                      break;
                }
            }
        }
    }
    /* Add by SPRD for bug 542996 End */

    /**
     * Network callback for our network request
     */
    private class NetworkRequestCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            LogUtil.d("NetworkCallbackListener.onAvailable: network=" + network +
                                  ", mSubId=" + mSubId);
            synchronized (MmsNetworkManager.this) {
                if (mNetworkCallbacks.size() != 0) {
                    mNetwork = network;
                }
                MmsNetworkManager.this.notifyAll();
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            LogUtil.d("NetworkCallbackListener.onLost: network=" + network +
                                  ", mSubId=" + mSubId);
            synchronized (MmsNetworkManager.this) {
                releaseRequestLocked(this);
                synchronized (mNetworkCallbacks) {
                    mNetworkCallbacks.remove(this);
                }
                resetLocked();
                MmsNetworkManager.this.notifyAll();
            }
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            LogUtil.d("NetworkCallbackListener.onUnavailable" + ", mSubId=" + mSubId);
            synchronized (MmsNetworkManager.this) {
                releaseRequestLocked(this);
                synchronized (mNetworkCallbacks) {
                    mNetworkCallbacks.remove(this);
                }
                resetLocked();
                MmsNetworkManager.this.notifyAll();
            }
        }
    }

    public MmsNetworkManager(Context context, int subId) {
        mContext = context;
        mNetwork = null;
        mMmsRequestCount = 0;
        mConnectivityManager = null;
        mMmsHttpClient = null;
        mSubId = subId;
        mNetworkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                .setNetworkSpecifier(Integer.toString(mSubId))
                .build();
    }

    /**
     * Acquire the MMS network
     *
     * @param requestId request ID for logging
     * @throws com.android.mms.service.exception.MmsNetworkException if we fail to acquire it
     */
    public void acquireNetwork(final String requestId) throws MmsNetworkException {
        synchronized (this) {
            mMmsRequestCount += 1;
            /* Delete by SPRD for bug 542996 Start */
            //if (mNetwork != null) {
                // Already available
            //    LogUtil.d(requestId, "MmsNetworkManager: already available");
            //    return;
            //}
            // Not available, so start a new request if not done yet
            //if (mNetworkCallback == null) {
            //    LogUtil.d(requestId, "MmsNetworkManager: start new network request");
            //    startNewNetworkRequestLocked();
            //}
            /* Delete by SPRD for bug 542996 End */
            /* Add by SPRD for bug 542996 Start */
            if (mReleaseNwHandler == null) {
                HandlerThread thread = new HandlerThread("MmsNetworkManager");
                thread.start();
                mReleaseNwHandler = new NetworkRelease(thread.getLooper());
            }

            if ((mPendingReleaseMmsNetworkManager.get() != null)
                    && (mPendingReleaseMmsNetworkManager.get().mSubId != mSubId)) {
                LogUtil.d(requestId, "MmsNetworkManager: different SIM pdp request, currentSubId=" + mSubId +
                        ", pendingSubId=" + mPendingReleaseMmsNetworkManager.get().mSubId);
                if (mReleaseNwHandler.hasMessages(EVENT_NETWORK_RELEASE_DELAY)) {
                    LogUtil.d("MmsNetworkManager: remove EVENT_NETWORK_RELEASE_DELAY messages");
                    mReleaseNwHandler.removeMessages(EVENT_NETWORK_RELEASE_DELAY);
                    mReleaseNwHandler.sendMessage(mReleaseNwHandler.obtainMessage(EVENT_NETWORK_RELEASE));
                }
            } else {
                LogUtil.d("MmsNetworkManager: first request or same sim pdp request, currentSubId ="
                                + mSubId
                                + ", pendingSubId = "
                                + ((mPendingReleaseMmsNetworkManager.get() !=
                                  null ? mPendingReleaseMmsNetworkManager.get().mSubId : null)));
            }

            if (mReleaseNwHandler.hasMessages(EVENT_NETWORK_RELEASE_DELAY)) {
                LogUtil.d("MmsNetworkManager: remove EVENT_NETWORK_RELEASE_DELAY messages");
                mReleaseNwHandler.removeMessages(EVENT_NETWORK_RELEASE_DELAY);
            }
            final ConnectivityManager.NetworkCallback current = getNetworkCallback();
            mReleaseNwHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (MmsNetworkManager.this) {
                        if (mNetwork != null) {
                            // Already available
                            LogUtil.d(requestId, "MmsNetworkManager: already available, subId=" + mSubId);
                            MmsNetworkManager.this.notifyAll();
                            return;
                        }
                        LogUtil.d(requestId, "MmsNetworkManager: start new network request, subId=" + mSubId);
                        // Not available, so start a new request
                        synchronized (mNetworkCallbacks) {
                            mNetworkCallbacks.add(current);
                        }
                        startNewNetworkRequestLocked(current);
                    }
                }
            });
            /* Add by SPRD for bug 542996 End */

            final long shouldEnd = SystemClock.elapsedRealtime() + NETWORK_ACQUIRE_TIMEOUT_MILLIS;
            long waitTime = NETWORK_ACQUIRE_TIMEOUT_MILLIS;
            while (waitTime > 0) {
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    LogUtil.w(requestId, "MmsNetworkManager: acquire network wait interrupted");
                }
                if (mNetwork != null) {
                    // Success
                    return;
                }
                // Calculate remaining waiting time to make sure we wait the full timeout period
                waitTime = shouldEnd - SystemClock.elapsedRealtime();
            }
            // Timed out, so release the request and fail
            LogUtil.d(requestId, "MmsNetworkManager: timed out");
            /* Add by SPRD for bug 542996 Start */
            synchronized (mNetworkCallbacks) {
                mNetworkCallbacks.remove(current);
            }
            mMmsRequestCount -= 1;
            releaseRequestLocked(current);
            /* Add by SPRD for bug 542996 End */
            // Delete by SPRD for bug 542996
            //releaseRequestLocked(mNetworkCallback);
            throw new MmsNetworkException("Acquiring network timed out");
        }
    }

    /**
     * Release the MMS network when nobody is holding on to it.
     *
     * @param requestId request ID for logging
     */
    public void releaseNetwork(final String requestId) {
        synchronized (this) {
            if (mMmsRequestCount > 0) {
                mMmsRequestCount -= 1;
                LogUtil.d(requestId, "MmsNetworkManager: release, count=" + mMmsRequestCount);
                if (mMmsRequestCount < 1) {
                    /* Add by SPRD for bug 542996 Start */
                    mPendingReleaseMmsNetworkManager.set(this);
                    mReleaseNwHandler.sendMessageDelayed(
                            mReleaseNwHandler.obtainMessage(EVENT_NETWORK_RELEASE_DELAY),
                            NETWORK_RELEASE_PROCESS_DELAY);
                    /* Add by SPRD for bug 542996 End */
                    // Delete by SPRD for bug 542996
                    //releaseRequestLocked(mNetworkCallback);
                }
            }
        }
    }

    /**
     * Start a new {@link android.net.NetworkRequest} for MMS
     */
    /* Modify by SPRD for bug 542996 Start */
    private void startNewNetworkRequestLocked(ConnectivityManager.NetworkCallback networkCallback) {
        final ConnectivityManager connectivityManager = getConnectivityManager();
        connectivityManager.requestNetwork(
                mNetworkRequest, networkCallback, NETWORK_REQUEST_TIMEOUT_MILLIS);
    }
    /* Modify by SPRD for bug 542996 End */

    /**
     * Release the current {@link android.net.NetworkRequest} for MMS
     *
     * @param callback the {@link android.net.ConnectivityManager.NetworkCallback} to unregister
     */
    private void releaseRequestLocked(ConnectivityManager.NetworkCallback callback) {
        if (callback != null) {
            final ConnectivityManager connectivityManager = getConnectivityManager();
            connectivityManager.unregisterNetworkCallback(callback);
        }
        /* Modify by SPRD for bug 542996 Start */
        LogUtil.d("MmsNetworkManager: releaseRequestLocked() mNetworkCallbacks.size() "+mNetworkCallbacks.size());
        if (mNetworkCallbacks.size() == 0){
            resetLocked();
        }
        /* Modify by SPRD for bug 542996 End */
    }

    /**
     * Reset the state
     */
    private void resetLocked() {
        // Delete by SPRD for bug 542996
        //mNetworkCallback = null;
        mNetwork = null;
        mMmsRequestCount = 0;
        mMmsHttpClient = null;
        // Add by SPRD for bug 542996
        mPendingReleaseMmsNetworkManager.set(null);
    }

    private ConnectivityManager getConnectivityManager() {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        }
        return mConnectivityManager;
    }

    /* Add by SPRD for bug 542996 Start */
    private ConnectivityManager.NetworkCallback getNetworkCallback() {
        return new NetworkRequestCallback();
    }
    /* Add by SPRD for bug 542996 End */

    /**
     * Get an MmsHttpClient for the current network
     *
     * @return The MmsHttpClient instance
     */
    public MmsHttpClient getOrCreateHttpClient() {
        synchronized (this) {
            if (mMmsHttpClient == null) {
                if (mNetwork != null) {
                    // Create new MmsHttpClient for the current Network
                    mMmsHttpClient = new MmsHttpClient(mContext, mNetwork);
                }
            }
            return mMmsHttpClient;
        }
    }

    /**
     * Get the APN name for the active network
     *
     * @return The APN name if available, otherwise null
     */
    public String getApnName() {
        Network network = null;
        synchronized (this) {
            if (mNetwork == null) {
                return null;
            }
            network = mNetwork;
        }
        String apnName = null;
        final ConnectivityManager connectivityManager = getConnectivityManager();
        final NetworkInfo mmsNetworkInfo = connectivityManager.getNetworkInfo(network);
        if (mmsNetworkInfo != null) {
            apnName = mmsNetworkInfo.getExtraInfo();
        }
        return apnName;
    }
}
