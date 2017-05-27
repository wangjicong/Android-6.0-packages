/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.phone;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import com.android.internal.telephony.OperatorInfo;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import android.util.Log;

import java.util.ArrayList;

/**
 * Service code used to assist in querying the network for service
 * availability.
 */
public class NetworkQueryService extends Service {
    // debug data
    private static final String LOG_TAG = "NetworkQuery";
    private static final boolean DBG = true;

    // static events
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100;
    /* SPRD: add for manual network query @{ */
    private static final int EVENT_ALL_DATA_DISCONNECTED = 200;
    private static final int EVENT_ALL_DATA_DETACHED = 201;
    private static final int EVENT_ABORT_QUERY_NETWORK = 202;
    /* @} */

    // static states indicating the query status of the service
    private static final int QUERY_READY = -1;
    private static final int QUERY_IS_RUNNING = -2;

    // error statuses that will be retured in the callback.
    public static final int QUERY_OK = 0;
    public static final int QUERY_EXCEPTION = 1;
    
    /* SPRD: add for manual network query @{ */
    private static final String[] ABORT_QUERY_NETWORK_CMDS = new String[] { "AT+SAC" };
    private static final String[] FORCE_DETACH_CMDS = new String[] { "AT+CLSSPDT=1" };
    /* @} */

    /** state of the query service */
    private int mState;

    /* SPRD: add for manual network query @{ */
    private TelephonyManager mTelephonyManager;
    private ProxyController mProxyController;
    private int mPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
    private int mDisconnectPendingCount;
    private int mConnectedPhoneId; //current data activate  phone id
    /* @} */
    // SPRD: add for manual network query
    private PowerManager.WakeLock mWakeLock = null;
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        INetworkQueryService getService() {
            return mBinder;
        }
    }
    private final IBinder mLocalBinder = new LocalBinder();

    /**
     * Local handler to receive the network query compete callback
     * from the RIL.
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // if the scan is complete, broadcast the results.
                // to all registerd callbacks.
                case EVENT_NETWORK_SCAN_COMPLETED:
                    if (DBG) log("scan completed, broadcasting results");
                    /* SPRD: add for manual network query @{ */
                    enableData();
                    mPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
                    /* @} */
                    broadcastQueryResults((AsyncResult) msg.obj);
                    // SPRD: set screen on when query network
                    releaseWakeLock();
                    break;

                /* SPRD: add for manual network query @{ */
                case EVENT_ALL_DATA_DISCONNECTED:
                    if (DBG) log("EVENT_ALL_DATA_DISCONNECTED id=" + msg.arg1);
                    if (mDisconnectPendingCount > 0) {
                        mDisconnectPendingCount--;
                    }
                    if (mDisconnectPendingCount == 0) {
                        if (DBG) log("Data has been disabled on all phones");
                        if (hasClients()) {
                            if (needForceDetach(mConnectedPhoneId)) {
                                //do data detach on data card when query network
                                Phone dataPhone = PhoneFactory.getPhone(mConnectedPhoneId);
                                if (dataPhone != null) {
                                    forceDataConnectionDetach(dataPhone,
                                            mHandler.obtainMessage(EVENT_ALL_DATA_DETACHED));
                                }
                            } else {
                                if (DBG) log("not need detach, do query network");
                                getAvailableNetworks();
                            }
                        } else {
                            log("Query has been aborted");
                        }
                    }
                    break;

                case EVENT_ABORT_QUERY_NETWORK:
                    if (DBG) log("EVENT_ABORT_QUERY_NETWORK");
                    enableData();
                    // SPRD: set screen on when query network
                    releaseWakeLock();
                    mPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
                    break;

                case EVENT_ALL_DATA_DETACHED:
                    if (DBG) log("EVENT_ALL_DATA_DETACHED");
                    if (hasClients()) {
                        getAvailableNetworks();
                    } else {
                        log("Query has been aborted");
                    }
                    break;
                /* @} */
            }
        }
    };
    
    /** 
     * List of callback objects, also used to synchronize access to 
     * itself and to changes in state.
     */
    final RemoteCallbackList<INetworkQueryServiceCallback> mCallbacks =
        new RemoteCallbackList<INetworkQueryServiceCallback> ();
    
    /**
     * Implementation of the INetworkQueryService interface.
     */
    private final INetworkQueryService.Stub mBinder = new INetworkQueryService.Stub() {
        
        /**
         * Starts a query with a INetworkQueryServiceCallback object if
         * one has not been started yet.  Ignore the new query request
         * if the query has been started already.  Either way, place the
         * callback object in the queue to be notified upon request 
         * completion.
         */
        public void startNetworkQuery(INetworkQueryServiceCallback cb, int phoneId) {
            // SPRD: add for manual network query
            mPhoneId = phoneId;
            if (cb != null) {
                // register the callback to the list of callbacks.
                synchronized (mCallbacks) {
                    mCallbacks.register(cb);
                    if (DBG) log("registering callback " + cb.getClass().toString());
                    
                    switch (mState) {
                        case QUERY_READY:
                            // TODO: we may want to install a timeout here in case we
                            // do not get a timely response from the RIL.
                            /* SPRD: add for manual network query @{ */
                            deactivateData();
                            /* @} */
                            break;
                            
                        // do nothing if we're currently busy.
                        case QUERY_IS_RUNNING:
                            if (DBG) log("query already in progress");
                            break;
                        default:
                    }
                }
            }
        }
        
        /**
         * Stops a query with a INetworkQueryServiceCallback object as
         * a token.
         */
        public void stopNetworkQuery(INetworkQueryServiceCallback cb) {
            // currently we just unregister the callback, since there is 
            // no way to tell the RIL to terminate the query request.  
            // This means that the RIL may still be busy after the stop 
            // request was made, but the state tracking logic ensures
            // that the delay will only last for 1 request even with
            // repeated button presses in the NetworkSetting activity.
            /* SPRD: add for manual network query @{ */
            log("stopNetworkQuery state=" + mState);
            if (mState == QUERY_IS_RUNNING) {
                Phone phone = PhoneFactory.getPhone(mPhoneId);
                if (phone != null) {
                    abortNetworkQuery(phone,
                            mHandler.obtainMessage(EVENT_ABORT_QUERY_NETWORK));
                }
            } else {
                // not running, just notify aborted
                mHandler.sendEmptyMessage(EVENT_ABORT_QUERY_NETWORK);
            }
            /* @} */
            unregisterCallback(cb);
        }

        /**
         * Unregisters the callback without impacting an underlying query.
         */
        public void unregisterCallback(INetworkQueryServiceCallback cb) {
            if (cb != null) {
                synchronized (mCallbacks) {
                    if (DBG) log("unregistering callback " + cb.getClass().toString());
                    mCallbacks.unregister(cb);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        mState = QUERY_READY;        
        /* SPRD: add for manual network query @{ */
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mProxyController = ProxyController.getInstance();
        /* @} */
    }

    /**
     * Required for service implementation.
     */
    @Override
    public void onStart(Intent intent, int startId) {
    }
    
    /**
     * Handle the bind request.
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Currently, return only the LocalBinder instance.  If we
        // end up requiring support for a remote binder, we will need to 
        // return mBinder as well, depending upon the intent.
        if (DBG) log("binding service implementation");
        return mLocalBinder;
    }

    /**
     * Broadcast the results from the query to all registered callback
     * objects. 
     */
    private void broadcastQueryResults (AsyncResult ar) {
        // reset the state.
        synchronized (mCallbacks) {
            mState = QUERY_READY;
            
            // see if we need to do any work.
            if (ar == null) {
                if (DBG) log("AsyncResult is null.");
                return;
            }
    
            // TODO: we may need greater accuracy here, but for now, just a
            // simple status integer will suffice.
            int exception = (ar.exception == null) ? QUERY_OK : QUERY_EXCEPTION;
            if (DBG) log("AsyncResult has exception " + exception);
            
            // Make the calls to all the registered callbacks.
            for (int i = (mCallbacks.beginBroadcast() - 1); i >= 0; i--) {
                INetworkQueryServiceCallback cb = mCallbacks.getBroadcastItem(i); 
                if (DBG) log("broadcasting results to " + cb.getClass().toString());
                try {
                    cb.onQueryComplete((ArrayList<OperatorInfo>) ar.result, exception);
                } catch (RemoteException e) {
                }
                /* SPRD: add for manual network query @{ */
                //unregister after complete
                mCallbacks.unregister(cb);
                /* @} */
            }
            
            // finish up.
            mCallbacks.finishBroadcast();
        }
    }

    /* SPRD: add for manual network query @{ */
    private void deactivateData() {
        mDisconnectPendingCount = mTelephonyManager.getPhoneCount();
        mConnectedPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        // Disable data connection on all phones
        for (int id = 0; id < mTelephonyManager.getPhoneCount(); id++) {
            /* SPRD: Bug 527251 Data connected phone Id is incorrect @{ */
            if (mConnectedPhoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
                int[] subId = SubscriptionManager.getSubId(id);
                if (subId != null && subId.length > 0) {
                    if (!mProxyController.isDataDisconnected(subId[0])) {
                        mConnectedPhoneId = id; //current data phone id
                    }
                }
            }
            /* @} */

            mProxyController.disableDataConnectivity(id,
                    mHandler.obtainMessage(EVENT_ALL_DATA_DISCONNECTED, id, 0));
        }
        log("Exit deactivateData(): mConnectedPhoneId=" + mConnectedPhoneId);
    }

    private void enableData() {
        for (int id = 0; id < mTelephonyManager.getPhoneCount(); id++) {
            mProxyController.enableDataConnectivity(id);
        }
    }

    private void getAvailableNetworks() {
        if (mState == QUERY_IS_RUNNING) {
            if (DBG) log("query already in progress");
        } else {
            Phone phone = PhoneFactory.getPhone(mPhoneId);
            if (phone != null) {
                // SPRD: set screen on when query network
                acquireWakeLock();
                phone.getAvailableNetworks(
                        mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED));
                mState = QUERY_IS_RUNNING;
                if (DBG) log("starting new query");
            } else {
                if (DBG) {
                    log("phone is null");
                }
            }
        }
    }

    private boolean needForceDetach(int phoneId) {
       if (!mTelephonyManager.isDeviceSupportLte() ||
           !SubscriptionManager.isValidPhoneId(phoneId)) {
           return false;
       } else {
           return true;
       }
    }

    private void abortNetworkQuery(Phone phone, Message onCompleted) {
        phone.invokeOemRilRequestStrings(ABORT_QUERY_NETWORK_CMDS, onCompleted);
    }

    private void forceDataConnectionDetach(Phone phone, Message onCompleted) {
        phone.invokeOemRilRequestStrings(FORCE_DETACH_CMDS, onCompleted);
    }

    private boolean hasClients() {
        synchronized(mCallbacks) {
            return mCallbacks.getRegisteredCallbackCount() != 0;
        }
    }
    /* @} */
    /* SPRD: set screen on when query network @{ */
    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, LOG_TAG);
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
    /* @} */
    
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }    
}
