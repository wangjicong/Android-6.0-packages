/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.UserManager;
import android.util.Log;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

/**
 * This state machine handles Bluetooth Adapter State.
 * States:
 *      {@link OnState} : Bluetooth is on at this state
 *      {@link OffState}: Bluetooth is off at this state. This is the initial
 *      state.
 *      {@link PendingCommandState} : An enable / disable operation is pending.
 * TODO(BT): Add per process on state.
 */

final class AdapterState extends StateMachine {
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private static final String TAG = "BluetoothAdapterState";

    static final int BLE_TURN_ON = 0;
    static final int USER_TURN_ON = 1;
    static final int BREDR_STARTED=2;
    static final int ENABLED_READY = 3;
    static final int BLE_STARTED=4;

    static final int USER_TURN_OFF = 20;
    static final int BEGIN_DISABLE = 21;
    static final int ALL_DEVICES_DISCONNECTED = 22;
    static final int BLE_TURN_OFF = 23;

    static final int DISABLED = 24;
    static final int BLE_STOPPED=25;
    static final int BREDR_STOPPED = 26;

    static final int BREDR_START_TIMEOUT = 100;
    static final int ENABLE_TIMEOUT = 101;
    static final int DISABLE_TIMEOUT = 103;
    static final int BLE_STOP_TIMEOUT = 104;
    static final int SET_SCAN_MODE_TIMEOUT = 105;
    static final int BLE_START_TIMEOUT = 106;
    static final int BREDR_STOP_TIMEOUT = 107;

    static final int USER_TURN_OFF_DELAY_MS=500;

    static final int ENABLED_RADIO = 200;
    static final int BEGIN_ENABLE_RADIO = 201;
    // For other radio can be handled in similar to FM or same as FM
    static final int USER_TURN_ON_RADIO = 202;
    static final int USER_TURN_OFF_RADIO = 203;
    static final int DISABLED_RADIO = 204;
    static final int BEGIN_DISABLE_RADIO = 205;

    //TODO: tune me
    private static final int ENABLE_TIMEOUT_DELAY = 12000;
    private static final int DISABLE_TIMEOUT_DELAY = 8000;
    private static final int BREDR_START_TIMEOUT_DELAY = 4000;
    //BLE_START_TIMEOUT can happen quickly as it just a start gattservice
    private static final int BLE_START_TIMEOUT_DELAY = 2000; //To start GattService
    private static final int BLE_STOP_TIMEOUT_DELAY = 2000;
    //BREDR_STOP_TIMEOUT can < STOP_TIMEOUT
    private static final int BREDR_STOP_TIMEOUT_DELAY = 4000;
    private static final int PROPERTY_OP_DELAY =2000;
    private AdapterService mAdapterService;
    private AdapterProperties mAdapterProperties;
    private PendingCommandState mPendingCommandState = new PendingCommandState();
    private OnState mOnState = new OnState();
    private OffState mOffState = new OffState();
    private BleOnState mBleOnState = new BleOnState();
    
	// Irrespective of the Bluetooth State  isRadioOn is used to maintain the Fm state
    private boolean isRadioOn = false;

    public boolean isRadioOn() {
        return isRadioOn;
    }

    public boolean isTurningOn() {
        boolean isTurningOn=  mPendingCommandState.isTurningOn();
        verboseLog("isTurningOn()=" + isTurningOn);
        return isTurningOn;
    }

    public boolean isBleTurningOn() {
        boolean isBleTurningOn=  mPendingCommandState.isBleTurningOn();
        verboseLog("isBleTurningOn()=" + isBleTurningOn);
        return isBleTurningOn;
    }

    public boolean isBleTurningOff() {
        boolean isBleTurningOff =  mPendingCommandState.isBleTurningOff();
        verboseLog("isBleTurningOff()=" + isBleTurningOff);
        return isBleTurningOff;
    }

    public boolean isTurningOff() {
        boolean isTurningOff= mPendingCommandState.isTurningOff();
        verboseLog("isTurningOff()=" + isTurningOff);
        return isTurningOff;
    }

    private AdapterState(AdapterService service, AdapterProperties adapterProperties) {
        super("BluetoothAdapterState:");
        addState(mOnState);
        addState(mBleOnState);
        addState(mOffState);
        addState(mPendingCommandState);
        mAdapterService = service;
        mAdapterProperties = adapterProperties;
        setInitialState(mOffState);
    }

    public static AdapterState make(AdapterService service, AdapterProperties adapterProperties) {
        Log.d(TAG, "make() - Creating AdapterState");
        AdapterState as = new AdapterState(service, adapterProperties);
        as.start();
        return as;
    }

    public void doQuit() {
        quitNow();
    }

    public void cleanup() {
        if(mAdapterProperties != null)
            mAdapterProperties = null;
        if(mAdapterService != null)
            mAdapterService = null;
    }

    private class OffState extends State {
        @Override
        public void enter() {
            infoLog("Entering OffState");
        }

        @Override
        public boolean processMessage(Message msg) {
            AdapterService adapterService = mAdapterService;
            if (adapterService == null) {
                errorLog("Received message in OffState after cleanup: " + msg.what);
                return false;
            }

            debugLog("Current state: OFF, message: " + msg.what);

            switch(msg.what) {
               case BLE_TURN_ON:
                   notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_TURNING_ON);
                   mPendingCommandState.setBleTurningOn(true);
                   transitionTo(mPendingCommandState);
                   sendMessageDelayed(BLE_START_TIMEOUT, BLE_START_TIMEOUT_DELAY);
                   adapterService.BleOnProcessStart();
                   break;
			   case USER_TURN_ON_RADIO:
				   if (DBG) Log.d(TAG,"CURRENT_STATE=OFF, MESSAGE = USER_TURN_ON_RADIO");
				   if (!isRadioOn) {
					   //Enable
						mPendingCommandState.setTurningOnRadio(true);
						transitionTo(mPendingCommandState);
						sendMessage(BEGIN_ENABLE_RADIO);
					} else {
						Log.d(TAG,"Radio already turned ON");
					}
				   
				   break;
               case USER_TURN_OFF_RADIO://Michael
                   if (DBG) Log.d(TAG,"CURRENT_STATE=OFF, MESSAGE = USER_TURN_OFF_RADIO, requestId= " + msg.arg1);
                   mPendingCommandState.setTurningOffRadio(true);
                   transitionTo(mPendingCommandState);
                   Message m = obtainMessage(BEGIN_DISABLE_RADIO);
                   m.arg1 = msg.arg1;
                   sendMessage(m);
				   break;

               case USER_TURN_OFF:
                   //TODO: Handle case of service started and stopped without enable
                   break;

               default:
                   return false;
            }
            return true;
        }
    }

    private class BleOnState extends State {
        @Override
        public void enter() {
            infoLog("Entering BleOnState");
        }

        @Override
        public boolean processMessage(Message msg) {

            AdapterService adapterService = mAdapterService;
            AdapterProperties adapterProperties = mAdapterProperties;
            if ((adapterService == null) || (adapterProperties == null)) {
                errorLog("Received message in BleOnState after cleanup: " + msg.what);
                return false;
            }

            debugLog("Current state: BLE ON, message: " + msg.what);

            switch(msg.what) {
               case USER_TURN_ON:
                   notifyAdapterStateChange(BluetoothAdapter.STATE_TURNING_ON);
                   mPendingCommandState.setTurningOn(true);
                   transitionTo(mPendingCommandState);
                   sendMessageDelayed(BREDR_START_TIMEOUT, BREDR_START_TIMEOUT_DELAY);
                   adapterService.startCoreServices();
                   break;

               case USER_TURN_ON_RADIO:
                   if (DBG) Log.d(TAG,"CURRENT_STATE=OFF, MESSAGE = USER_TURN_ON_RADIO");
                   if (!isRadioOn) {
                       //Enable
                        mPendingCommandState.setTurningOnRadio(true);
                        transitionTo(mPendingCommandState);
                        sendMessage(BEGIN_ENABLE_RADIO);
                    } else {
                        Log.d(TAG,"Radio already turned ON");
                    }
               
                   break;
				   
               case USER_TURN_OFF:
                   notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_TURNING_OFF);
                   mPendingCommandState.setBleTurningOff(true);
                   adapterProperties.onBleDisable();
                   transitionTo(mPendingCommandState);
                   sendMessageDelayed(DISABLE_TIMEOUT, DISABLE_TIMEOUT_DELAY);
                   boolean ret = adapterService.disableNative();
                   if (!ret) {
                        removeMessages(DISABLE_TIMEOUT);
                        errorLog("Error while calling disableNative");
                        //FIXME: what about post enable services
                        mPendingCommandState.setBleTurningOff(false);
                        notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_ON);
                   }
                   break;

                case USER_TURN_OFF_RADIO:
                   if (DBG) Log.d(TAG,"CURRENT_STATE=OFF, MESSAGE = USER_TURN_OFF_RADIO, requestId= " + msg.arg1);
               
                   mPendingCommandState.setTurningOffRadio(true);
                   transitionTo(mPendingCommandState);
               
                   Message m = obtainMessage(BEGIN_DISABLE_RADIO);
                   m.arg1 = msg.arg1;
                   sendMessage(m);
               
                   break;
               default:
                   if (DBG) Log.d(TAG,"ERROR: UNEXPECTED MESSAGE: CURRENT_STATE=OFF, MESSAGE = " + msg.what );
                   return false;
            }
            return true;
        }
    }

    private class OnState extends State {
        @Override
        public void enter() {
            infoLog("Entering OnState");

            AdapterService adapterService = mAdapterService;
            if (adapterService == null) {
                errorLog("Entered OnState after cleanup");
                return;
            }
			// If OnState entry is because of turning ON radio the auto connect
            // should not be started as it is BT ON specific behaviour.
            if (mPendingCommandState.isTurningOnRadio())
                mPendingCommandState.setTurningOnRadio(false);
            else 
            	adapterService.autoConnect();
        }

        @Override
        public boolean processMessage(Message msg) {
            AdapterProperties adapterProperties = mAdapterProperties;
            if (adapterProperties == null) {
                errorLog("Received message in OnState after cleanup: " + msg.what);
                return false;
            }

            debugLog("Current state: ON, message: " + msg.what);

            switch(msg.what) {
               case BLE_TURN_OFF:
                   notifyAdapterStateChange(BluetoothAdapter.STATE_TURNING_OFF);
                   mPendingCommandState.setTurningOff(true);
                   transitionTo(mPendingCommandState);

                   // Invoke onBluetoothDisable which shall trigger a
                   // setScanMode to SCAN_MODE_NONE
                   Message m = obtainMessage(SET_SCAN_MODE_TIMEOUT);
                   sendMessageDelayed(m, PROPERTY_OP_DELAY);
                   adapterProperties.onBluetoothDisable();
                   break;
               case USER_TURN_OFF_RADIO:
                   if (DBG) Log.d(TAG,"CURRENT_STATE=ON, MESSAGE = USER_TURN_OFF_RADIO");
                   if (isRadioOn) {
                       mPendingCommandState.setTurningOffRadio(true);
                       transitionTo(mPendingCommandState);
               
                       Message m1 = obtainMessage(BEGIN_DISABLE_RADIO);
                       m1.arg1 = msg.arg1;
                       sendMessage(m1);
                    } else {
                        Log.d(TAG,"Radio already turned OFF");
                    }
                   break;

               case USER_TURN_ON:
                   if (DBG) Log.d(TAG,"CURRENT_STATE=ON, MESSAGE = USER_TURN_ON");
                   Log.i(TAG,"Bluetooth already ON, ignoring USER_TURN_ON");
                   break;
               case USER_TURN_ON_RADIO:
                   if (DBG) Log.d(TAG,"CURRENT_STATE=ON, MESSAGE = USER_TURN_ON_RADIO");
                   if (!isRadioOn) {
                       //Enable
                        mPendingCommandState.setTurningOnRadio(true);
                        transitionTo(mPendingCommandState);
                        sendMessage(BEGIN_ENABLE_RADIO);
                    } else {
                        Log.d(TAG,"Radio already turned ON");
                    }
               
                   break;

               default:
                   if (DBG) Log.d(TAG,"ERROR: UNEXPECTED MESSAGE: CURRENT_STATE=ON, MESSAGE = " + msg.what );
                   return false;
            }
            return true;
        }
    }

    private class PendingCommandState extends State {
        private boolean mIsTurningOn;
        private boolean mIsTurningOff;
        private boolean mIsBleTurningOn;
        private boolean mIsBleTurningOff;
        private boolean mIsTurningOnRadio;
        private boolean mIsTurningOffRadio;
		
        public void enter() {
            infoLog("Entering PendingCommandState");
        }

        public void setTurningOn(boolean isTurningOn) {
            mIsTurningOn = isTurningOn;
        }

        public boolean isTurningOn() {
            return mIsTurningOn;
        }

        public void setTurningOff(boolean isTurningOff) {
            mIsTurningOff = isTurningOff;
        }

        public boolean isTurningOff() {
            return mIsTurningOff;
        }

        public void setBleTurningOn(boolean isBleTurningOn) {
            mIsBleTurningOn = isBleTurningOn;
        }
		public void setTurningOnRadio(boolean isTurningOnRadio) {
            mIsTurningOnRadio = isTurningOnRadio;
        }
        public boolean isTurningOnRadio() {
            return mIsTurningOnRadio;
        }
        public void setTurningOffRadio(boolean isTurningOffRadio) {
            mIsTurningOffRadio = isTurningOffRadio;
        }

        public boolean isTurningOffRadio() {
            return mIsTurningOffRadio;
        }

        public boolean isBleTurningOn() {
            return mIsBleTurningOn;
        }

        public void setBleTurningOff(boolean isBleTurningOff) {
            mIsBleTurningOff = isBleTurningOff;
        }

        public boolean isBleTurningOff() {
            return mIsBleTurningOff;
        }

        @Override
        public boolean processMessage(Message msg) {

            boolean isTurningOn= isTurningOn();
            boolean isTurningOff = isTurningOff();
            boolean isBleTurningOn = isBleTurningOn();
            boolean isBleTurningOff = isBleTurningOff();
            boolean isTurningOnRadio= isTurningOnRadio();
            boolean isTurningOffRadio = isTurningOffRadio();

            AdapterService adapterService = mAdapterService;
            AdapterProperties adapterProperties = mAdapterProperties;
            if ((adapterService == null) || (adapterProperties == null)) {
                errorLog("Received message in PendingCommandState after cleanup: " + msg.what);
                return false;
            }

            debugLog("Current state: PENDING_COMMAND, message: " + msg.what);

            switch (msg.what) {
                case USER_TURN_ON:
                    if (isBleTurningOff || isTurningOff) { //TODO:do we need to send it after ble turn off also??
                        infoLog("Deferring USER_TURN_ON request...");
                        deferMessage(msg);
                    }
                    break;

                case USER_TURN_OFF:
                    if (isTurningOn || isBleTurningOn) {
                        infoLog("Deferring USER_TURN_OFF request...");
                        deferMessage(msg);
                    }
                    break;

                case BLE_TURN_ON:
                    if (isTurningOff || isBleTurningOff) {
                        infoLog("Deferring BLE_TURN_ON request...");
                        deferMessage(msg);
                    }
                    break;
                case USER_TURN_ON_RADIO:
                    if (DBG) Log.d(TAG,"CURRENT_STATE=PENDING, MESSAGE = USER_TURN_ON_RADIO"
                            + ", isTurningOn=" + isTurningOnRadio + ", isTurningOff=" + isTurningOffRadio);
                    if (isTurningOnRadio) {
                        Log.i(TAG,"CURRENT_STATE=PENDING: Alreadying turning on Radio... Ignoring USER_TURN_ON_RADIO...");
                    } else {
                        Log.i(TAG,"CURRENT_STATE=PENDING: Deferring request USER_TURN_ON_RADIO");
                        deferMessage(msg);
                    }
                    break;
                case USER_TURN_OFF_RADIO:
                    if (DBG) Log.d(TAG,"CURRENT_STATE=PENDING, MESSAGE = USER_TURN_OFF_RADIO"
                            + ", isTurningOn=" + isTurningOnRadio + ", isTurningOff=" + isTurningOffRadio);
                    if (isTurningOffRadio) {
                        Log.i(TAG,"CURRENT_STATE=PENDING: Alreadying turning off Radio... Ignoring USER_TURN_OFF_RADIO...");
                    } else {
                        Log.i(TAG,"CURRENT_STATE=PENDING: Deferring request USER_TURN_OFF_RADIO");
                        deferMessage(msg);
                    }
                    break;
                case BEGIN_ENABLE_RADIO:
                    if (DBG) Log.d(TAG,"CURRENT_STATE=PENDING, MESSAGE = BEGIN_ENABLE_RADIO, isTurningOnRadio="
                        + isTurningOnRadio + ", isTurningOffRadio=" + isTurningOffRadio);
                
                    //Enable
                    boolean ret = mAdapterService.enableRadioNative();
                    if (!ret) {
                        Log.e(TAG, "Error while turning Radio On");
                        if (BluetoothAdapter.STATE_OFF == mAdapterProperties.getState())
                            transitionTo(mOffState);
                        else
                            transitionTo(mOnState);
                        mPendingCommandState.setTurningOnRadio(false);
                    } else {
                        sendMessageDelayed(ENABLE_TIMEOUT, ENABLE_TIMEOUT_DELAY);
                    }
                    break;
				case BEGIN_DISABLE_RADIO:
                    if (DBG) Log.d(TAG,"CURRENT_STATE=PENDING, MESSAGE = BEGIN_DISABLE_RADIO" +
                        isTurningOnRadio + ", isTurningOffRadio=" + isTurningOffRadio);
                    sendMessageDelayed(DISABLE_TIMEOUT, DISABLE_TIMEOUT_DELAY);
                    ret = mAdapterService.disableRadioNative();
                    if (!ret) {
                        removeMessages(DISABLE_TIMEOUT);
                        Log.e(TAG, "Error while turning Radio Off");
                        mPendingCommandState.setTurningOffRadio(false);
                    }
                    break;
				case ENABLED_RADIO:
                    if (DBG) Log.d(TAG,
                        "CURRENT_STATE=PENDING, MESSAGE = ENABLED_RADIO, isTurningOnRadio="
                        + isTurningOnRadio + ", isTurningOffRadio=" + isTurningOffRadio);
                    removeMessages(ENABLE_TIMEOUT);
                    isRadioOn = true;
                    if (BluetoothAdapter.STATE_OFF == mAdapterProperties.getState()){
                        mPendingCommandState.setTurningOnRadio(false);
                        transitionTo(mOffState);
                        notifyAdapterRadioStateChange(BluetoothAdapter.STATE_RADIO_ON);
                    }
                    else {
                        transitionTo(mOnState);
                        // Retain TurningOnRadio info till new state enter happens
                        // for skipping auto connect for radio turn ON
                        notifyAdapterRadioStateChange(BluetoothAdapter.STATE_RADIO_ON);
                    }
                    break;
				 case DISABLED_RADIO:
                    if (DBG) Log.d(TAG,"CURRENT_STATE=PENDING, MESSAGE = DISABLED_RADIO, isTurningOnRadio="
                        + isTurningOnRadio + ", isTurningOffRadio=" + isTurningOffRadio());
                    if (isTurningOnRadio) {
                        removeMessages(ENABLE_TIMEOUT);
                        errorLog("Error enabling radio - hardware init failed");
                        mPendingCommandState.setTurningOnRadio(false);
                    }
                    removeMessages(DISABLE_TIMEOUT);
                    setTurningOffRadio(false);
                    isRadioOn = false;
                    if (BluetoothAdapter.STATE_ON == mAdapterProperties.getState()) {
                        transitionTo(mOnState);
                        notifyAdapterRadioStateChange(BluetoothAdapter.STATE_RADIO_OFF);
                    } else {
                        transitionTo(mOffState);
                        notifyAdapterRadioStateChange(BluetoothAdapter.STATE_RADIO_OFF);
                        //mAdapterService.startShutdown(requestId1);
                    }
                
                    break;
                case BLE_TURN_OFF:
                    if (isTurningOn || isBleTurningOn) {
                        infoLog("Deferring BLE_TURN_OFF request...");
                        deferMessage(msg);
                    }
                    break;

                case BLE_STARTED:
                    //Remove start timeout
                    removeMessages(BLE_START_TIMEOUT);

                    //Enable
                    boolean isGuest = UserManager.get(mAdapterService).isGuestUser();
                    if (!adapterService.enableNative(isGuest)) {
                        errorLog("Error while turning Bluetooth on");
                        notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                        transitionTo(mOffState);
                    } else {
                        sendMessageDelayed(ENABLE_TIMEOUT, ENABLE_TIMEOUT_DELAY);
                    }
                    break;

                case BREDR_STARTED:
                    //Remove start timeout
                    removeMessages(BREDR_START_TIMEOUT);
                    adapterProperties.onBluetoothReady();
                    mPendingCommandState.setTurningOn(false);
                    transitionTo(mOnState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_ON);
                    break;

                case ENABLED_READY:
                    removeMessages(ENABLE_TIMEOUT);
                    mPendingCommandState.setBleTurningOn(false);
                    transitionTo(mBleOnState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_ON);
                    /* SPRD: because of weakly support for Classic Only device. workaround FIX */
                    if (!adapterService.isLESupport()) {
                        infoLog("Classic only");
                        adapterService.onLeServiceUp();
                    }
                    break;

                case SET_SCAN_MODE_TIMEOUT:
                     warningLog("Timeout while setting scan mode. Continuing with disable...");
                     //Fall through
                case BEGIN_DISABLE:
                    removeMessages(SET_SCAN_MODE_TIMEOUT);
                    sendMessageDelayed(BREDR_STOP_TIMEOUT, BREDR_STOP_TIMEOUT_DELAY);
                    adapterService.stopProfileServices();
                    break;

                case DISABLED:
                    if (isTurningOn) {
                        removeMessages(ENABLE_TIMEOUT);
                        errorLog("Error enabling Bluetooth - hardware init failed?");
                        mPendingCommandState.setTurningOn(false);
                        transitionTo(mOffState);
                        adapterService.stopProfileServices();
                        notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                        break;
                    }
                    removeMessages(DISABLE_TIMEOUT);
                    sendMessageDelayed(BLE_STOP_TIMEOUT, BLE_STOP_TIMEOUT_DELAY);
                    if (adapterService.stopGattProfileService()) {
                        debugLog("Stopping Gatt profile services that were post enabled");
                        break;
                    }
                    //Fall through if no services or services already stopped
                case BLE_STOPPED:
                    removeMessages(BLE_STOP_TIMEOUT);
                    setBleTurningOff(false);
                    transitionTo(mOffState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                    break;

                case BREDR_STOPPED:
                    removeMessages(BREDR_STOP_TIMEOUT);
                    setTurningOff(false);
                    transitionTo(mBleOnState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_ON);
                    break;

                case BLE_START_TIMEOUT:
                    errorLog("Error enabling Bluetooth (BLE start timeout)");
                    mPendingCommandState.setBleTurningOn(false);
                    transitionTo(mOffState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                    break;

                case BREDR_START_TIMEOUT:
                    errorLog("Error enabling Bluetooth (start timeout)");
                    mPendingCommandState.setTurningOn(false);
                    transitionTo(mBleOnState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_ON);
                    break;

                case ENABLE_TIMEOUT:
                    errorLog("Error enabling Bluetooth (enable timeout)");
                    mPendingCommandState.setBleTurningOn(false);
					mPendingCommandState.setTurningOnRadio(false);
                    isRadioOn = false;
                    transitionTo(mOffState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                    break;

                case BREDR_STOP_TIMEOUT:
                    errorLog("Error stopping Bluetooth profiles (stop timeout)");
                    mPendingCommandState.setTurningOff(false);
                    transitionTo(mBleOnState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);

                    notifyAdapterStateChange(BluetoothAdapter.STATE_BLE_ON);
                    break;

                case BLE_STOP_TIMEOUT:
                    errorLog("Error stopping Bluetooth profiles (BLE stop timeout)");
                    mPendingCommandState.setTurningOff(false);
                    transitionTo(mOffState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                    break;

                case DISABLE_TIMEOUT:
                    errorLog("Error disabling Bluetooth (disable timeout)");
                    if (isTurningOn)
                        mPendingCommandState.setTurningOn(false);
                    adapterService.stopProfileServices();
                    adapterService.stopGattProfileService();
                    mPendingCommandState.setTurningOff(false);
                    setBleTurningOff(false);
                    mPendingCommandState.setTurningOffRadio(false);
                    transitionTo(mOffState);
                    notifyAdapterStateChange(BluetoothAdapter.STATE_OFF);
                    break;

                default:
                    return false;
            }
            return true;
        }
    }

    private void notifyAdapterStateChange(int newState) {
        AdapterService adapterService = mAdapterService;
        AdapterProperties adapterProperties = mAdapterProperties;
        if ((adapterService == null) || (adapterProperties == null)) {
            errorLog("notifyAdapterStateChange after cleanup:" + newState);
            return;
        }

        int oldState = adapterProperties.getState();
        adapterProperties.setState(newState);
        infoLog("Bluetooth adapter state changed: " + oldState + "-> " + newState);
        adapterService.updateAdapterState(oldState, newState);
    }
    private void notifyAdapterRadioStateChange(int newState) {
	    AdapterService adapterService = mAdapterService;
        infoLog("Bluetooth adapter radio state changed: " + newState);
        // Use the already defined callback prototype to send the bt_state and new_fm_state
        // TBD to use seperate callback while integration wtih FM app
        adapterService.updateAdapterState(mAdapterProperties.getState(), newState);
    }

    void stateChangeCallback(int status) {
        if (status == AbstractionLayer.BT_STATE_OFF) {
            sendMessage(DISABLED);

        } else if (status == AbstractionLayer.BT_STATE_ON) {
            // We should have got the property change for adapter and remote devices.
            sendMessage(ENABLED_READY);
        } else if (status == AbstractionLayer.BT_RADIO_OFF) {
            sendMessage(DISABLED_RADIO);
        } else if (status == AbstractionLayer.BT_RADIO_ON) {
            sendMessage(ENABLED_RADIO);
        } else {
            errorLog("Incorrect status in stateChangeCallback");
        }
    }

    private void infoLog(String msg) {
        if (DBG) Log.i(TAG, msg);
    }

    private void debugLog(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void warningLog(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void verboseLog(String msg) {
        if (VDBG) Log.v(TAG, msg);
    }

    private void errorLog(String msg) {
        Log.e(TAG, msg);
    }

}
