/******************************************************************************
 *
 *  Copyright (C) 2009-2012 Broadcom Corporation
 *
 *  This program is the proprietary software of Broadcom Corporation and/or its
 *  licensors, and may only be used, duplicated, modified or distributed
 *  pursuant to the terms and conditions of a separate, written license
 *  agreement executed between you and Broadcom (an "Authorized License").
 *  Except as set forth in an Authorized License, Broadcom grants no license
 *  (express or implied), right to use, or waiver of any kind with respect to
 *  the Software, and Broadcom expressly reserves all rights in and to the
 *  Software and all intellectual property rights therein.
 *  IF YOU HAVE NO AUTHORIZED LICENSE, THEN YOU HAVE NO RIGHT TO USE THIS
 *  SOFTWARE IN ANY WAY, AND SHOULD IMMEDIATELY NOTIFY BROADCOM AND DISCONTINUE
 *  ALL USE OF THE SOFTWARE.
 *
 *  Except as expressly set forth in the Authorized License,
 *
 *  1.     This program, including its structure, sequence and organization,
 *         constitutes the valuable trade secrets of Broadcom, and you shall
 *         use all reasonable efforts to protect the confidentiality thereof,
 *         and to use this information only in connection with your use of
 *         Broadcom integrated circuit products.
 *
 *  2.     TO THE MAXIMUM EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED
 *         "AS IS" AND WITH ALL FAULTS AND BROADCOM MAKES NO PROMISES,
 *         REPRESENTATIONS OR WARRANTIES, EITHER EXPRESS, IMPLIED, STATUTORY,
 *         OR OTHERWISE, WITH RESPECT TO THE SOFTWARE.  BROADCOM SPECIFICALLY
 *         DISCLAIMS ANY AND ALL IMPLIED WARRANTIES OF TITLE, MERCHANTABILITY,
 *         NONINFRINGEMENT, FITNESS FOR A PARTICULAR PURPOSE, LACK OF VIRUSES,
 *         ACCURACY OR COMPLETENESS, QUIET ENJOYMENT, QUIET POSSESSION OR
 *         CORRESPONDENCE TO DESCRIPTION. YOU ASSUME THE ENTIRE RISK ARISING
 *         OUT OF USE OR PERFORMANCE OF THE SOFTWARE.
 *
 *  3.     TO THE MAXIMUM EXTENT PERMITTED BY LAW, IN NO EVENT SHALL BROADCOM
 *         OR ITS LICENSORS BE LIABLE FOR
 *         (i)   CONSEQUENTIAL, INCIDENTAL, SPECIAL, INDIRECT, OR EXEMPLARY
 *               DAMAGES WHATSOEVER ARISING OUT OF OR IN ANY WAY RELATING TO
 *               YOUR USE OF OR INABILITY TO USE THE SOFTWARE EVEN IF BROADCOM
 *               HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES; OR
 *         (ii)  ANY AMOUNT IN EXCESS OF THE AMOUNT ACTUALLY PAID FOR THE
 *               SOFTWARE ITSELF OR U.S. $1, WHICHEVER IS GREATER. THESE
 *               LIMITATIONS SHALL APPLY NOTWITHSTANDING ANY FAILURE OF
 *               ESSENTIAL PURPOSE OF ANY LIMITED REMEDY.
 *
 *****************************************************************************/


package com.broadcom.fm.fmreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;

import com.broadcom.fm.fmreceiver.IFmReceiverCallback;

import java.util.Iterator;
import java.util.LinkedList;
import java.text.DateFormat;
import java.util.Date;


/**
 * Provides a service for accessing and altering FM receiver hardware
 * settings. Requests should be made via proxy to this server and once the
 * requests have been processed, event(s) will be issued with the result of the
 * request execution. Also provides the callback event loop.
 * 
 * @hide
 */
public final class FmNativehandler  {
    private static final boolean V = FmReceiverServiceConfig.V;
    private static final String TAG = "FmNativehandler";


    /* The list of all registered client callbacks. */
    private final RemoteCallbackList<IFmReceiverCallback> mCallbacks =
            new RemoteCallbackList<IFmReceiverCallback>();

    /* The current active fm app client name */
    private String mClientName ;

    // ******************************************************
    // FM Queue mechanism - BEGIN
    private enum FMCommand {
        FM_CHIP_ON,
        FM_CHIP_OFF,
        FM_ON,
        FM_OFF,
        FM_TUNE_RADIO,
        FM_GET_STATUS,
        FM_MUTE_AUDIO,
        FM_SEEK_STATION,
        FM_SEEK_STATION_COMBO,
        FM_SEEK_RDS_STATION,
        FM_SEEK_STATION_ABORT,
        FM_SET_RDS_MODE,
        FM_SET_AUDIO_MODE,
        FM_SET_AUDIO_PATH,
        FM_SET_STEP_SIZE,
        FM_SET_WORLD_REGION,
        FM_ESTIMATE_NOISE_FLOOR_LEVEL,
        FM_SET_LIVE_AUDIO_POLLING,
        FM_SET_VOLUME
    }

    private class FM_Status_Params {
        private int mStFreq;
        private int mStRssi;
        private int mStSnr;
        private boolean mStRadioIsOn;
        private int mStRdsProgramType;
        private String mStRdsProgramService;
        private String mStRdsRadioText;
        private String mStRdsProgramTypeName;
        private boolean mStIsMute;

        public FM_Status_Params(int freq, int rssi, int snr, boolean radioIsOn,
            int rdsProgramType, String rdsProgramService, String rdsRadioText,
            String rdsProgramTypeName, boolean isMute)
        {
           mStFreq = freq;
           mStRssi = rssi;
           mStSnr = snr;
           mStRadioIsOn = radioIsOn;
           mStRdsProgramType = rdsProgramType;
           mStRdsProgramService = rdsProgramService;
           mStRdsRadioText = rdsRadioText;
           mStRdsProgramTypeName = rdsProgramTypeName;
           mStIsMute = isMute;
        }
    }

    private class FM_Search_Params {
        private int mStFreq;
        private int mStRssi;
        private int mStSnr;
        private boolean mStSeekSuccess;

        public FM_Search_Params(int freq, int rssi, int snr, boolean seekSuccess) {
           mStFreq = freq;
           mStRssi = rssi;
           mStSnr = snr;
           mStSeekSuccess = seekSuccess;
        }
    }

    static class FMJob {
        final FMCommand command;
        // 0 means this command was not been sent to the bt framework.
        long timeSent;
        boolean b_arg1;
        int arg1, arg2, arg3, arg4, arg5, arg6, arg7;

        // Job without arg
        public FMJob(FMCommand command) {
            this.command = command;
            this.timeSent = 0;
        }

        // Job with 1 arg
        public FMJob(FMCommand command, int arg1) {
            this.command = command;
            this.timeSent = 0;
            this.arg1 = arg1;
        }

        // Job with 2 args
        public FMJob(FMCommand command, int arg1, int arg2) {
            this.command = command;
            this.timeSent = 0;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        // Job with 3 args
        public FMJob(FMCommand command, int arg1, int arg2, int arg3) {
            this.command = command;
            this.timeSent = 0;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        // Job with 4 arg
        public FMJob(FMCommand command, int arg1, int arg2, int arg3, int arg4) {
            this.command = command;
            this.timeSent = 0;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }

        // Job with 1 boolean arg and 7 int arg, for the seekStationCombo function
        public FMJob(FMCommand command, boolean b_arg1, int arg1, int arg2, int arg3,
                     int arg4, int arg5, int arg6, int arg7) {
            this.command = command;
            this.timeSent = 0;
            this.b_arg1 = b_arg1;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
            this.arg7 = arg7;
        }

        // Job with 1 boolean arg
        public FMJob(FMCommand command, boolean b_arg1) {
            this.command = command;
            this.timeSent = 0;
            this.b_arg1 = b_arg1;
        }

        // Job with 2 arg, with arg1 is a boolean
        public FMJob(FMCommand command, boolean b_arg1, int arg2) {
            this.command = command;
            this.timeSent = 0;
            this.b_arg1 = b_arg1;
            this.arg2 = arg2;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(command.name());
            sb.append(" TimeSent:");
            if (timeSent == 0) {
                sb.append("not yet");
            } else {
                sb.append(DateFormat.getTimeInstance().format(new Date(timeSent)));
            }
            return sb.toString();
        }
    }

    private LinkedList<FMJob> FMQueue = new LinkedList<FMJob>();
    private int current_CMD = -1;


    private void cleanQueue(FMJob job) {
        long now = System.currentTimeMillis();
        Iterator<FMJob> it = FMQueue.iterator();
        if (job != null) {
            while (it.hasNext()) {
                FMJob existingJob = it.next();

                // Remove any pending job if we receive a FM_OFF
                if (job.command == FMCommand.FM_OFF) {
                    if (existingJob.timeSent == 0) {
                        Log.d(TAG, "Removed because of a FM off request. " + existingJob);
                        it.remove();
                        existingJob = null;
                        continue;
                    }
                } else
                if (job.command == FMCommand.FM_SEEK_STATION_ABORT) {
                    Log.d(TAG, "Removed because of an abort request. " + existingJob);
                    it.remove();
                    existingJob = null;
                    continue;
                }

                if (existingJob.timeSent != 0) {
                    Log.w(TAG, "****** ***** Sent. So remove Job:" + existingJob.toString() );
                    it.remove();
                    existingJob = null;
                    continue;
                }
                existingJob = null;
            }
        }
    }

    private void queueFMCommand(FMJob job) {
        Log.d(TAG, FMQueue.toString());

        synchronized (FMQueue) {
            // Add job to queue
            Log.d(TAG, "****** ****** Adding FM Job: " + job.toString());
            cleanQueue(job);
            FMQueue.add(job);

            // if there's nothing pending from before, send the command immediately.
            if (FMQueue.size() == 1) {
                processCommands();
            }
        }
    }

    private boolean processCommand(FMJob job) {
        int successful = FmProxy.STATUS_OK;
         // Reset the current CMD cache for abort check later
        current_CMD =  -1;

        if (job.timeSent == 0) {
            job.timeSent = System.currentTimeMillis();
            Log.d(TAG, "***** ***** processCommand:" + job.toString());
            switch (job.command) {
            case FM_CHIP_ON:
                successful = process_enableChip();
                break;
            case FM_ON:
                successful = process_turnOnRadio(job.arg1);
                break;
            case FM_OFF:
                successful = process_turnOffRadio();
                break;
            case FM_CHIP_OFF:
                successful = process_disableChip();
                break;
            case FM_TUNE_RADIO:
                successful = process_tuneRadio(job.arg1);
                break;
            case FM_GET_STATUS:
                successful = process_getStatus();
                break;
            case FM_MUTE_AUDIO:
                successful = process_muteAudio(job.b_arg1);
                break;
            case FM_SEEK_STATION:
            current_CMD =  FmReceiverServiceState.OPERATION_SEARCH;
                successful = process_seekStation(job.arg1, job.arg2);
                break;
            case FM_SEEK_STATION_COMBO:
            current_CMD =  FmReceiverServiceState.OPERATION_SEARCH;
                successful = process_seekStationCombo(job.b_arg1, job.arg1, job.arg2,
                    job.arg3, job.arg4, job.arg5, job.arg6, job.arg7);
                break;
            case FM_SEEK_RDS_STATION:
            current_CMD =  FmReceiverServiceState.OPERATION_SEARCH;
                successful = process_seekRdsStation(job.arg1, job.arg2, job.arg3, job.arg4);
                break;
                // Abort command will be processed directly, No queue needed
                // case FM_SEEK_STATION_ABORT:
                //     successful = process_seekStationAbort();
                //     break;
            case FM_SET_RDS_MODE:
                successful = process_setRdsMode(job.arg1, job.arg2, job.arg3, job.arg4);
                break;
            case FM_SET_AUDIO_MODE:
                successful = process_setAudioMode(job.arg1);
                break;
            case FM_SET_AUDIO_PATH:
                successful = process_setAudioPath(job.arg1);
                break;
            case FM_SET_STEP_SIZE:
                successful = process_setStepSize(job.arg1);
                break;
            case FM_SET_WORLD_REGION:
                successful = process_setWorldRegion(job.arg1, job.arg2);
                break;
            case FM_ESTIMATE_NOISE_FLOOR_LEVEL:
                try {
                    successful = process_estimateNoiseFloorLevel(job.arg1);
                } catch (RemoteException e) {
                    successful = FmProxy.STATUS_SERVER_FAIL;
                }
                break;
            case FM_SET_LIVE_AUDIO_POLLING:
                try {
                    successful = process_setLiveAudioPolling( job.b_arg1, job.arg2);
                } catch (RemoteException e) {
                    successful = FmProxy.STATUS_SERVER_FAIL;
                }
                break;
            case FM_SET_VOLUME:
                successful = process_setFMVolume(job.arg1);
                break;
            }
        }
        return (successful == FmProxy.STATUS_OK);
    }

    /* This method is called in 2 places:
     * 1) queueCommand() - when someone or something want to send the FM commands
     * 2) on receiving the notification from BTAPP
     */
    private void processCommands() {
        Log.d(TAG, "***** ***** processCommands:" + FMQueue.toString());

        Iterator<FMJob> it = FMQueue.iterator();
        while (it.hasNext()) {
            FMJob job = it.next();
            if (processCommand(job)) {
                // Sent to btld so done for now. Will remove this job
                // from queue when we get an event
                return;
            } else {
                /*
                 * If the command failed immediately, there will be no event
                 * callbacks. So delete the job immediately and move on to the
                 * next one
                 */
                it.remove();
            }
        }
    }

    private void fetchNextJob() {
        synchronized (FMQueue) {
            FMJob job = FMQueue.peek();
            if (job == null) {
                return;
            } else  {
                    Log.d(TAG, "******* ******* Processing job:" + job.toString());
                    cleanQueue(null);
                    FMQueue.poll();
            }
            processCommands();
        }
    }

    /** cleanAllQueue()
    * Method to clear up the FMQueue object and release it during application close
    * @ param none
    */
    public void clearAllQueue() {
        synchronized (FMQueue) {
            if(FMQueue != null) {
                Log.d(TAG, "******* Clearing the queue. Present size is "+ FMQueue.size());
                FMQueue.clear();
                FMQueue = null;
            }
        }
    }

    // FM Queue mechanism - END
    // ******************************************************


    // some gets and sets
    public boolean getRadioIsOn() {
        return FmReceiverServiceState.mRadioIsOn;
    }

    /**
     * Returns the FM Audio Mode state.
     * @param none
     * @return {@link FmProxy#AUDIO_MODE_AUTO},
     *         {@link FmProxy#AUDIO_MODE_STEREO},
     *         {@link FmProxy#AUDIO_MODE_MONO}, or
     *         {@link FmProxy#AUDIO_MODE_BLEND}.
     */
    public synchronized int getMonoStereoMode() {
        return FmReceiverServiceState.mAudioMode;
    }

    /**
     * Returns the present tuned FM Frequency
     * @param none
     * @return Tuned frequency
     */
    public synchronized int getTunedFrequency() {
        return FmReceiverServiceState.mFreq;
    }

    /**
     * Returns whether MUTE is turned ON or OFF
     * @param none
     * @return false if MUTE is OFF ; true otherwise
     */
    public synchronized  boolean getIsMute() {
        return FmReceiverServiceState.mIsMute;
    }

    public void registerCallback(IFmReceiverCallback cb) throws RemoteException {
        if (cb != null) {
            mCallbacks.register(cb);
        }
    }

    public synchronized void unregisterCallback(IFmReceiverCallback cb) throws RemoteException {
        if (cb != null) {
            mCallbacks.unregister(cb);
        }
    }

    static {
        classInitNative();
    }
    private native static void classInitNative();
    protected boolean mIsStarted = false;
    protected boolean mIsFinish = false;
    protected Context mContext;

    public FmNativehandler(Context context) {
        mContext = context;
    }

    protected void finalize() throws Throwable {
    }

    public synchronized void start() {
        if (V) Log.d(TAG, "start");
        if (mIsStarted) {
            Log.d(TAG,"Service already started...Skipping");
            return;
        }
        mIsStarted = true;

    }

    public synchronized void stop() {
        if (V) Log.d(TAG, "stop");
        if (!mIsStarted) {
            Log.d(TAG,"Service already stopped...Skipping");
            return;
        }

        unRegisterIntent();
        BluetoothAdapter btAdap = BluetoothAdapter.getDefaultAdapter();

        if (btAdap.isRadioEnabled()) {
            Log.e(TAG,"Disable radio if app failed to disable radio");
            setAudioPathNative(FmProxy.AUDIO_PATH_NONE);
            disableFmNative(false);
            btAdap.disableRadio();
            initializeStateMachine();
        }
        cleanupNative();
        mIsStarted =false;

    }

    private native void initializeNative();


    private native void cleanupNative();

    public void finish() {
        if(V) Log.d(TAG, "finish - cleanup Service here");
        if(operationHandler != null) {
            //Manually remove all messages to clean the MessageQueue of operationHandler
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_ENABLE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_DISABLE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SEARCH);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TUNE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_MUTE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SEEK_ABORT);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SET_RDS_MODE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SET_AUDIO_MODE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SET_AUDIO_PATH);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SET_STEP_SIZE);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SET_FM_VOLUME);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SET_WORLD_REGION);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_NFL);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_GENERIC);

            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_STATUS_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_SEARCH_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_RDS_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_RDS_DATA_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_AUDIO_MODE_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_AUDIO_PATH_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_REGION_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_NFE_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_LIVE_AUDIO_EVENT_CALLBACK);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_VOLUME_EVENT_CALLBACK);

            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT_STARTUP);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT_SHUTDOWN);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT_SEARCH);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT_NFL);
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);
            operationHandler.removeCallbacksAndMessages(null);
            operationHandler = null;
        }
        clearAllQueue();
        mCallbacks.kill(); //Ensure to clear off the callback loop
    }

    private void initializeStateMachine() {
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_OFF;
        FmReceiverServiceState.mFreq = 0;
        FmReceiverServiceState.mRssi = 127;
        FmReceiverServiceState.mSnr = 127;
        FmReceiverServiceState.mRadioIsOn = false;
        FmReceiverServiceState.mRdsProgramType = 0;
        FmReceiverServiceState.mRdsProgramService = "";
        FmReceiverServiceState.mRdsRadioText = "";
        FmReceiverServiceState.mRdsProgramTypeName = "";
        FmReceiverServiceState.mIsMute = false;
        FmReceiverServiceState.mSeekSuccess = false;
        FmReceiverServiceState.mRdsOn = false;
        FmReceiverServiceState.mAfOn = false;
        FmReceiverServiceState.mRdsType = 0; // RDS
        FmReceiverServiceState.mAlternateFreqHopThreshold = 0;
        FmReceiverServiceState.mAudioMode = FmProxy.AUDIO_MODE_AUTO;
        FmReceiverServiceState.mAudioPath = FmProxy.AUDIO_PATH_SPEAKER;
        FmReceiverServiceState.mWorldRegion = FmProxy.FUNC_REGION_DEFAULT;
        FmReceiverServiceState.mStepSize = FmProxy.FREQ_STEP_DEFAULT;
        FmReceiverServiceState.mLiveAudioQuality = FmProxy.LIVE_AUDIO_QUALITY_DEFAULT;
        FmReceiverServiceState.mEstimatedNoiseFloorLevel = FmProxy.NFL_DEFAULT;
        FmReceiverServiceState.mSignalPollInterval = FmProxy.SIGNAL_POLL_INTERVAL_DEFAULT;
        FmReceiverServiceState.mDeemphasisTime = FmProxy.DEEMPHASIS_TIME_DEFAULT;
        FmReceiverServiceState.radio_op_state = FmReceiverServiceState.RADIO_OP_STATE_NONE;
    }

    private void logTimeOut(int what) {
        switch(what) {
         case FmReceiverServiceState.OPERATION_ENABLE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_ENABLE"); break;
         case FmReceiverServiceState.OPERATION_DISABLE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_DISABLE"); break;
         case FmReceiverServiceState.OPERATION_SEARCH:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SEARCH"); break;
         case FmReceiverServiceState.OPERATION_TUNE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_TUNE"); break;
         case FmReceiverServiceState.OPERATION_MUTE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_MUTE"); break;
         case FmReceiverServiceState.OPERATION_SEEK_ABORT:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SEEK_ABORT"); break;
         case FmReceiverServiceState.OPERATION_SET_RDS_MODE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SET_RDS_MODE"); break;
         case FmReceiverServiceState.OPERATION_SET_AUDIO_MODE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SET_AUDIO_MODE"); break;
         case FmReceiverServiceState.OPERATION_SET_AUDIO_PATH:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SET_AUDIO_PATH"); break;
         case FmReceiverServiceState.OPERATION_SET_STEP_SIZE:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SET_STEP_SIZE"); break;
         case FmReceiverServiceState.OPERATION_SET_FM_VOLUME:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SET_FM_VOLUME"); break;
         case FmReceiverServiceState.OPERATION_SET_WORLD_REGION:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_SET_WORLD_REGION"); break;
         case FmReceiverServiceState.OPERATION_NFL:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_NFL"); break;
         case FmReceiverServiceState.OPERATION_GENERIC:
                 Log.w(TAG, "BTAPP TIMEOUT on OPERATION_GENERIC"); break;
         default:
                 Log.w(TAG, "BTAPP TIMEOUT (" + FmReceiverServiceState.OPERATION_TIMEOUT + ", " + what + ")"); break;
       }
    }

    /**
     * This class handles operation lock timeouts.
     */
    protected Handler operationHandler = new Handler() {
        public void handleMessage(Message msg) {
            /* Check if the current operation can be rescued. */
            switch (msg.what) {
                case FmReceiverServiceState.OPERATION_TIMEOUT:
                    /* Currently assume that any timeout is catastrophic. */
                    logTimeOut(msg.arg1);
                    /* Could not start radio. Reset state machine. */
                    if (msg.arg1 == FmReceiverServiceState.OPERATION_ENABLE)
                    {
                       initializeStateMachine();
                       FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_OFF;
                    } else if (msg.arg1 == FmReceiverServiceState.OPERATION_DISABLE)
                    {
                       initializeStateMachine();
                       try {
                            disableFmNative(true);
                       } catch (Exception e) { }
                       FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_OFF;
                    } else if (msg.arg1 == FmReceiverServiceState.OPERATION_SEARCH) {
                      /* Send search complet update to client. */
                      FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
                      FmReceiverServiceState.mSeekSuccess = true;
                      sendSeekCompleteEventCallback(FmReceiverServiceState.mFreq, FmReceiverServiceState.mRssi, FmReceiverServiceState.mSnr, FmReceiverServiceState.mSeekSuccess, 1);
                    } else {
                      /* Send status update to client. */
                      FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
                      sendStatusEventCallback(FmReceiverServiceState.mFreq,
                                              FmReceiverServiceState.mRssi,
                                              FmReceiverServiceState.mSnr,
                                              FmReceiverServiceState.mRadioIsOn,
                                              FmReceiverServiceState.mRdsProgramType,
                                              FmReceiverServiceState.mRdsProgramService,
                                              FmReceiverServiceState.mRdsRadioText,
                                              FmReceiverServiceState.mRdsProgramTypeName,
                                              FmReceiverServiceState.mIsMute, 1);
                    }
//                    fetchNextJob();
                    break;
                 case FmReceiverServiceState.OPERATION_STATUS_EVENT_CALLBACK:
                    {
                       Log.w(TAG, "Handling OPERATION_STATUS_EVENT_CALLBACK: calls sendStatusEventCallback");
                       FM_Status_Params st = (FM_Status_Params) msg.obj;
                       sendStatusEventCallback(st.mStFreq, st.mStRssi, st.mStSnr,
                                               st.mStRadioIsOn, st.mStRdsProgramType,
                                               st.mStRdsProgramService, st.mStRdsRadioText,
                                               st.mStRdsProgramTypeName, st.mStIsMute,
                                               msg.arg1);
                       break;
                    }
                 case FmReceiverServiceState.OPERATION_SEARCH_EVENT_CALLBACK:
                    {
                       Log.w(TAG, "Handling OPERATION_SEARCH_EVENT_CALLBACK: calls sendSeekCompleteEventCallback");
                       FM_Search_Params st = (FM_Search_Params) msg.obj;
                       sendSeekCompleteEventCallback(st.mStFreq, st.mStRssi, st.mStSnr, st.mStSeekSuccess, msg.arg1);
                       break;
                    }
                 case FmReceiverServiceState.OPERATION_RDS_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_RDS_EVENT_CALLBACK: calls sendRdsModeEventCallback");
                       sendRdsModeEventCallback(msg.arg1, msg.arg2);
                       break;

                 case FmReceiverServiceState.OPERATION_RDS_DATA_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_RDS_DATA_EVENT_CALLBACK: calls sendRdsDataEventCallback");
                       sendRdsDataEventCallback(msg.arg1, msg.arg2, (String) msg.obj);
                       break;

                 case FmReceiverServiceState.OPERATION_AUDIO_MODE_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_AUDIO_MODE_EVENT_CALLBACK: calls sendAudioModeEventCallback");
                       sendAudioModeEventCallback(msg.arg1);
                       break;

                 case FmReceiverServiceState.OPERATION_AUDIO_PATH_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_AUDIO_PATH_EVENT_CALLBACK: calls sendAudioPathEventCallback");
                       sendAudioPathEventCallback(msg.arg1);
                       break;

                 case FmReceiverServiceState.OPERATION_REGION_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_REGION_EVENT_CALLBACK: calls sendWorldRegionEventCallback");
                       sendWorldRegionEventCallback(msg.arg1);
                       break;

                 case FmReceiverServiceState.OPERATION_NFE_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_NFE_EVENT_CALLBACK: calls sendEstimateNflEventCallback");
                       sendEstimateNflEventCallback(msg.arg1);
                       break;

                 case FmReceiverServiceState.OPERATION_LIVE_AUDIO_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_LIVE_AUDIO_EVENT_CALLBACK: calls sendLiveAudioQualityEventCallback");
                       sendLiveAudioQualityEventCallback(msg.arg1, msg.arg2);
                       break;

                 case FmReceiverServiceState.OPERATION_VOLUME_EVENT_CALLBACK:
                       Log.w(TAG, "Handling OPERATION_VOLUME_EVENT_CALLBACK: calls sendVolumeEventCallback");
                       sendVolumeEventCallback(msg.arg1, msg.arg2);
                       break;

                /* Add command queue here if necessary. */
                default:
                     Log.w(TAG, "Handling UNKNOWN CALLBACK: " + msg.what);
                     break;
            }
        }
    };

    /**
     * Turns on the radio and plays audio using the specified functionality
     * mask.
     * <p>
     * After executing this function, the application should wait for a
     * confirmatory status event callback before calling further API functions.
     * Furthermore, applications should call the {@link #turnOffRadio()}
     * function before shutting down.
     *
     * @param functionalityMask
     *            is a bitmask comprised of one or more of the following fields:
     *            {@link FmProxy#FUNC_REGION_NA},
     *            {@link FmProxy#FUNC_REGION_JP},
     *            {@link FmProxy#FUNC_REGION_JP_II},
     *            {@link FmProxy#FUNC_REGION_EUR},
     *            {@link FmProxy#FUNC_RDS}, {@link FmProxy#FUNC_RDBS} and
     *            {@link FmProxy#FUNC_AF}
     *
     * @param clientPackagename
     *                 is the the client application package name , this is required for the
     *                 fm service to clean up it state when the client process gets killed
     *                 eg scenario: when client app dies without calling turnOffRadio()
     *
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */

    private static int instance = 0;
    int mFunctionalityMask;
    public synchronized int turnOnRadio(int functionalityMask, char[] clientPackagename) {
        mClientName = String.copyValueOf(clientPackagename);

        Log.d(TAG, "turnOnRadio: functionalityMask = " + functionalityMask +
              ", clientPackagename = " + clientPackagename);

        // register to get notified if client app process gets killed
        registerIntent();
        mFunctionalityMask = functionalityMask;

        instance++;
        Log.d(TAG,"instance"+instance);

        queueFMCommand(new FMJob(FMCommand.FM_CHIP_ON));


        return FmProxy.STATUS_OK;
    }

    /**
     * this registration for intents are to get notified when the client app process
     * gets killed
     */
    private void registerIntent() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        intentFilter.addAction(Intent.ACTION_QUERY_PACKAGE_RESTART);
        intentFilter.addDataScheme("package");
        mContext.registerReceiver(mIntentReceiver, intentFilter);


        IntentFilter intentFilter1 = new IntentFilter();
        intentFilter1.addAction(BluetoothAdapter.ACTION_RADIO_STATE_CHANGED);
        mContext.registerReceiver(mIntentRadioState, intentFilter1);
    }

    private void unRegisterIntent() {
        mContext.unregisterReceiver(mIntentReceiver);
        mClientName = null;
        mContext.unregisterReceiver(mIntentRadioState);
    }


    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED) ||
                action.equals(Intent.ACTION_PACKAGE_RESTARTED)) {

                Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }
                String pkgName = uri.getSchemeSpecificPart();
                if (pkgName == null) {
                    return;
                }
                Log.d(TAG,pkgName);

                // do the required clean up if it is our client killed
                if (pkgName.equals(mClientName)){
                    turnOffRadio();
                }
            }
        }
    };


    private BroadcastReceiver mIntentRadioState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            if (action.equals(BluetoothAdapter.ACTION_RADIO_STATE_CHANGED)) {
                 int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
                 if (V) Log.v(TAG,"***********state = " + state);
                 if (state == BluetoothAdapter.STATE_RADIO_ON) {
                    queueFMCommand(new FMJob(FMCommand.FM_ON, mFunctionalityMask));
                    } else if ((state == BluetoothAdapter.STATE_RADIO_OFF)){
                        /* Callback. */
                        //send the status
                    FmReceiverServiceState.mRadioIsOn = false;

                    /* This response indicates that system is alive and well. */
                    operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
                    
                    /* Update state machine. */
                    if (!FmReceiverServiceState.mRadioIsOn) {
                        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_OFF;
                    }

                    /* Callback. */
                    sendStatusEventCallbackFromLocalStore(true);
                    }

            }
        }
    };

    private int process_turnOnRadio(int functionalityMask) {
        Log.d(TAG, "turnOnRadio........()");

        initializeNative();
        int returnStatus = FmProxy.STATUS_OK;
        int requestedRegion = functionalityMask & FmReceiverServiceState.FUNC_REGION_BITMASK;
        int requestedRdsFeatures = functionalityMask & FmReceiverServiceState.FUNC_RDS_BITMASK;

        /* Sanity check of parameters. */
        if ((requestedRegion != FmProxy.FUNC_REGION_EUR)
                && (requestedRegion != FmProxy.FUNC_REGION_JP)
                && (requestedRegion != FmProxy.FUNC_REGION_JP_II)
                && (requestedRegion != FmProxy.FUNC_REGION_NA)) {
            if (V) Log.d(TAG, "Illegal parameter");
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if ((0 != (requestedRdsFeatures & FmProxy.FUNC_RDS))
                && (0 != (requestedRdsFeatures & FmProxy.FUNC_RBDS))) {
            if (V) Log.d(TAG, "Illegal parameter (2)");
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.mRadioIsOn) {
            /* Update local cache. */
            sendStatusEventCallbackFromLocalStore(true);
        } else if (FmReceiverServiceState.RADIO_STATE_INIT != FmReceiverServiceState.radio_state) {
            if (V) Log.d(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.mFuncMask = functionalityMask;
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_STARTING;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_ENABLE;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                FmReceiverServiceState.OPERATION_TIMEOUT_STARTUP);

            /* Trim excess data from parameters. */
            functionalityMask = functionalityMask & FmReceiverServiceState.FUNC_BITMASK;

            try {
                if (enableFmNative(functionalityMask)) {
                    returnStatus = FmProxy.STATUS_OK;
                } else {
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
                }
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "turnOnRadioNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                  operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean enableFmNative(int functionalityMask);

    /**
     * Turns off the radio.
     * <p>
     * After executing this function, the application should wait for a
     * confirmatory status event callback before shutting down.
     *
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int turnOffRadio() {
         queueFMCommand(new FMJob(FMCommand.FM_OFF));

         return FmProxy.STATUS_OK;
    }


    public synchronized int process_enableChip() {
        int returnStatus = FmProxy.STATUS_OK;
        if (V) Log.d(TAG, "processEnableChip()");

        if (FmReceiverServiceState.RADIO_STATE_OFF != FmReceiverServiceState.radio_state) {
             if (V) Log.d(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
             returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
         } else {
             FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_INIT;
             /* Set timer to check that this did not lock the state machine. */
             Message msg = Message.obtain();
             msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
             msg.arg1 = FmReceiverServiceState.OPERATION_ENABLE;
             operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
             operationHandler.sendMessageDelayed(msg,
                 FmReceiverServiceState.OPERATION_TIMEOUT_STARTUP);

             BluetoothAdapter btAdap = BluetoothAdapter.getDefaultAdapter();
             btAdap.enableRadio();
             if (returnStatus != FmProxy.STATUS_OK)
                   operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

         return returnStatus;
    }


    public synchronized int process_disableChip() {
        if (V) Log.d(TAG, "processDisableChip()");
        BluetoothAdapter btAdap = BluetoothAdapter.getDefaultAdapter();
        btAdap.disableRadio();
         return FmProxy.STATUS_OK;
    }

    private int process_turnOffRadio() {
        if (V) Log.d(TAG, "turnOffRadio()");

        int returnStatus = FmProxy.STATUS_OK;

        if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND != FmReceiverServiceState.radio_state)
            /* Should allow forcing the FM shutdown operation. */
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_STOPPING;
        /* Set timer to check that this did not lock the state machine. */
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
        msg.arg1 = FmReceiverServiceState.OPERATION_DISABLE;
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        operationHandler.sendMessageDelayed(msg,
             FmReceiverServiceState.OPERATION_TIMEOUT_SHUTDOWN);

        try {
            if (disableFmNative(false))
                returnStatus = FmProxy.STATUS_OK;
            else
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
        } catch (Exception e) {
            returnStatus = FmProxy.STATUS_SERVER_FAIL;
            Log.e(TAG, "turnOnRadioNative failed", e);
        }

        if (returnStatus != FmProxy.STATUS_OK)
        {
                  operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }
        /* restore the audio path */
        setAudioPath(FmProxy.AUDIO_PATH_NONE);
        /* State machine is reset immediately since there is risk
               of stack failure which would lock state machine in
               STOPPING state. */

        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_OFF;

        return returnStatus;
    }

    private native boolean disableFmNative(boolean bForcing);

    /**
     * Initiates forced clean-up of FMReceiverService from the application.
     *
     * @return STATUS_OK
     */
    public synchronized int cleanupFmService() {
        onRadioOffEvent(FmReceiverServiceState.BTA_FM_OK);
        return FmProxy.STATUS_OK;
    }

    /**
     * Tunes the radio to a specific frequency. If successful results in a
     * status event callback.
     *
     * @param freq
     *            the frequency to tune to.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int tuneRadio(int freq) {
         queueFMCommand(new FMJob(FMCommand.FM_TUNE_RADIO, freq));
         return FmProxy.STATUS_OK;
    }

    private int process_tuneRadio(int freq) {
        Log.v(TAG, "tuneRadio()");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((freq < FmReceiverServiceState.MIN_ALLOWED_FREQUENCY_CODE) ||
            (freq > FmReceiverServiceState.MAX_ALLOWED_FREQUENCY_CODE)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND != FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_TUNE;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,FmReceiverServiceState.OPERATION_TIMEOUT_SEARCH);

            try {
                if (tuneNative(freq))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "tuneNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean tuneNative(int freq);

    /**
     * Gets current radio status. This results in a status event callback.
     *
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int getStatus() {
         queueFMCommand(new FMJob(FMCommand.FM_GET_STATUS));
         return FmProxy.STATUS_OK;
    }

    private int process_getStatus() {
        Log.v(TAG, "getStatus()");

        int returnStatus = FmProxy.STATUS_OK;

        if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND != FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            /* Return latest known data. */
            sendStatusEventCallbackFromLocalStore(false);
        }

        return returnStatus;
    }

    /**
     * Mutes/unmutes radio audio. If muted the hardware will stop sending audio.
     * This results in a status event callback.
     *
     * @param mute
     *            TRUE to mute audio, FALSE to unmute audio.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int muteAudio(boolean mute) {
         queueFMCommand(new FMJob(FMCommand.FM_MUTE_AUDIO, mute));
         return FmProxy.STATUS_OK;
    }


    private int process_muteAudio(boolean mute) {
        Log.v(TAG, "muteAudio()");

        int returnStatus = FmProxy.STATUS_OK;

        if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND != FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_MUTE;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);

            try {
                if (muteNative(mute))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "muteAudio failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean muteNative(boolean mute);

    /**
     * Scans FM toward higher/lower frequency for next clear channel. Will
     * result in a seek complete event callback.
     * 
     * @param scanMode
     *            see {@link FmProxy#SCAN_MODE_NORMAL},
     *            {@link FmProxy#SCAN_MODE_DOWN},
     *            {@link FmProxy#SCAN_MODE_UP} and
     *            {@link FmProxy#SCAN_MODE_FULL}.
     * @param minSignalStrength
     *            minimum signal strength, default =
     *            {@link FmProxy#MIN_SIGNAL_STRENGTH_DEFAULT}
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int seekStation(int scanMode, int minSignalStrength) {
         queueFMCommand(new FMJob(FMCommand.FM_SEEK_STATION, scanMode, minSignalStrength));
         return FmProxy.STATUS_OK;
    }


    private int process_seekStation(int scanMode, int minSignalStrength) {
        Log.v(TAG, "seekStation():1");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((minSignalStrength < FmReceiverServiceState.MIN_ALLOWED_SIGNAL_STRENGTH_NUMBER) ||
            (minSignalStrength > FmReceiverServiceState.MAX_ALLOWED_SIGNAL_STRENGTH_NUMBER)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if ((scanMode != FmProxy.SCAN_MODE_DOWN) && (scanMode != FmProxy.SCAN_MODE_UP)  &&
            (scanMode != FmProxy.SCAN_MODE_FAST) && (scanMode != FmProxy.SCAN_MODE_FULL)) {
            /* CK - scanMode value, should take only 4 validated values = 0, 0x80, 0x81, 0x82 */
            Log.v(TAG, "seekStation failed, scanMode too high (0x" +
            Integer.toHexString(scanMode) + ")" );
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
        FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SEARCH;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                FmReceiverServiceState.OPERATION_TIMEOUT_SEARCH);

            /* Trim excess data from parameters. */
            scanMode = scanMode & FmReceiverServiceState.SCAN_MODE_BITMASK;

            try {
                if (searchNative(scanMode, minSignalStrength, FmProxy.RDS_COND_NONE, 0))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "searchNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    /**
     * Scans FM toward higher/lower frequency for next clear channel depending on the scanDirection.
     * will do wrap around when reached to mMaxFreq/mMinFreq,
     * when no wrap around is needed, use the low_bound or high_bound as endFrequency.
     * Will result in a seek complete event callback.
     *
     * @param startFrequency
     *            Starting frequency of search operation range.
     * @param endFrequency
     *            Ending frequency of search operation
     * @param minSignalStrength
     *            Minimum signal strength, default =
     *            {@link #MIN_SIGNAL_STRENGTH_DEFAULT}
     * @param scanDirection
     *            the direction to search in, it can only be either
     *            {@link #SCAN_MODE_UP} and {@link #SCAN_MODE_DOWN}.
     * @param scanMethod
     *            see {@link #SCAN_MODE_NORMAL}, {@link #SCAN_MODE_FAST},
     * @param multi_channel
     *            Is multiple channels are required, or only find next valid channel(seek).
     * @param rdsType
     *            the type of RDS condition to scan for.
     * @param rdsTypeValue
     *            the condition value to match.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int seekStationCombo(int startFreq, int endFreq,
        int minSignalStrength, int direction, int scanMethod,
        boolean multiChannel, int rdsType, int rdsTypeValue) {
        queueFMCommand(new FMJob(FMCommand.FM_SEEK_STATION_COMBO, multiChannel,
                                 startFreq, endFreq, minSignalStrength, direction,
                                 scanMethod, rdsType, rdsTypeValue));
        return FmProxy.STATUS_OK;
    }

    private int process_seekStationCombo(boolean multiChannel, int startFreq, int endFreq,
         int minSignalStrength, int direction, int scanMethod, int rdsType, int rdsTypeValue) {
        Log.v(TAG, "process_seekStationCombo()");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((minSignalStrength < FmReceiverServiceState.MIN_ALLOWED_SIGNAL_STRENGTH_NUMBER) ||
            (minSignalStrength > FmReceiverServiceState.MAX_ALLOWED_SIGNAL_STRENGTH_NUMBER)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                    FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = " + Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Log.v(TAG, "process_seekStationCombo(), OPERATION_TIMEOUT_SEARCH: " +
                FmReceiverServiceState.OPERATION_TIMEOUT_SEARCH);
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SEARCH;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg, FmReceiverServiceState.OPERATION_TIMEOUT_SEARCH);
            
            /* Trim excess data from parameters. */
            Log.v(TAG, "seekStationCombo: startFreq = " + startFreq +
                  ", endFeq = " + endFreq + ", minSignalStrength = " + minSignalStrength +
                  ", direction = " + direction + ", scanMethod = " + scanMethod +
                  ", multiChannel = " + multiChannel + ", rdsType = " + rdsType +
                  ", rdsTypeValue = " + rdsTypeValue);

            try {
                if (comboSearchNative(startFreq, endFreq, minSignalStrength, direction,
                    scanMethod, multiChannel, rdsType, rdsTypeValue))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "comboSearchNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean comboSearchNative(int startFreq, int endFreq, int rssiThreshold,
        int direction, int scanMethod, boolean multiChannel, int rdsType, int rdsTypeValue);

    /**
     * Scans FM toward higher/lower frequency for next clear channel. Will
     * result in a seek complete event callback. Will seek out a station that
     * matches the requested value for the desired RDS functionality support.
     * 
     * @param scanMode
     *            see {@link FmProxy#SCAN_MODE_NORMAL},
     *            {@link FmProxy#SCAN_MODE_DOWN},
     *            {@link FmProxy#SCAN_MODE_UP} and
     *            {@link FmProxy#SCAN_MODE_FULL}.
     * @param minSignalStrength
     *            Minimum signal strength, default =
     *            {@link FmProxy#MIN_SIGNAL_STRENGTH_DEFAULT}
     * @param rdsCondition
     *            the type of RDS condition to scan for.
     * @param rdsValue
     *            the condition value to match.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int seekRdsStation(int scanMode, int minSignalStrength, int rdsCondition,
            int rdsValue) {
         queueFMCommand(new FMJob(FMCommand.FM_SEEK_RDS_STATION, scanMode, minSignalStrength,
            rdsCondition, rdsValue));
         return FmProxy.STATUS_OK;
    }



    private int process_seekRdsStation(int scanMode, int minSignalStrength,
                int rdsCondition, int rdsValue) {
        Log.v(TAG, "seekRdsStation():1");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((minSignalStrength < FmReceiverServiceState.MIN_ALLOWED_SIGNAL_STRENGTH_NUMBER) ||
            (minSignalStrength > FmReceiverServiceState.MAX_ALLOWED_SIGNAL_STRENGTH_NUMBER)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if ((rdsValue < FmReceiverServiceState.MIN_ALLOWED_RDS_CONDITION_VALUE) ||
            (rdsValue > FmReceiverServiceState.MAX_ALLOWED_RDS_CONDITION_VALUE)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if ((rdsCondition != FmProxy.RDS_COND_NONE) &&
                (rdsCondition != FmProxy.RDS_COND_PTY) &&
                (rdsCondition != FmProxy.RDS_COND_TP)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SEARCH;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,FmReceiverServiceState.OPERATION_TIMEOUT_SEARCH);

            /* Trim excess data from parameters. */
            scanMode = scanMode & FmReceiverServiceState.SCAN_MODE_BITMASK;

            try {
                if (searchNative(scanMode, minSignalStrength, rdsCondition, rdsValue))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "searchNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean searchNative(int scanMode, int rssiThreshold, int condVal, int condType);

    /**
     * Aborts the current station seeking operation if any. Will result in a
     * seek complete event containing the last scanned frequency.
     * <p>
     * 
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int seekStationAbort() {
        Log.v(TAG, "seekStationAbort()");

        int returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;

        if (current_CMD != -1 && current_CMD == FmReceiverServiceState.OPERATION_SEARCH) {
            try {
                if (searchAbortNative())
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "searchAbortNative failed", e);
            }
        }
        return returnStatus;
    }

    private native boolean searchAbortNative();

    /**
     * Enables/disables RDS/RDBS feature and AF algorithm. Will result in an RDS
     * mode event callback.
     * <p>
     * 
     * @param rdsMode
     *            Turns on the RDS or RBDS. See {@link FmProxy#RDS_MODE_OFF},
     *            {@link FmProxy#RDS_MODE_DEFAULT_ON},
     *            {@link FmProxy#RDS_MODE_RDS_ON},
     *            {@link FmProxy#RDS_MODE_RDBS_ON}
     * @param rdsFields
     *            the mask specifying which types of RDS data to signal back.
     * @param afmode
     *            enables AF algorithm if True. Disables it if False
     * @param afThreshold
     *            the RSSI threshold when the AF should jump frequencies.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setRdsMode(int rdsMode, int rdsFeatures, int afMode, int afThreshold) {
         queueFMCommand(new FMJob(FMCommand.FM_SET_RDS_MODE, rdsMode,
            rdsFeatures, afMode, afThreshold));
         return FmProxy.STATUS_OK;
    }


    private int process_setRdsMode(int rdsMode, int rdsFeatures,
             int afMode, int afThreshold) {
        int returnStatus = FmProxy.STATUS_OK;

        Log.v(TAG, "setRdsMode()");

        /* Sanity check of parameters. */
        if ((afThreshold < FmReceiverServiceState.MIN_ALLOWED_AF_JUMP_THRESHOLD) ||
            (afThreshold > FmReceiverServiceState.MAX_ALLOWED_AF_JUMP_THRESHOLD)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            FmReceiverServiceState.radio_op_state = FmReceiverServiceState.RADIO_OP_STATE_NONE;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SET_RDS_MODE;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                    FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);

            /* Trim excess data from parameters and convert to native. */
            rdsMode = rdsMode & FmReceiverServiceState.RDS_MODE_BITMASK;
            afMode = afMode & FmReceiverServiceState.AF_MODE_BITMASK;
            rdsFeatures = rdsFeatures & FmReceiverServiceState.RDS_FEATURES_BITMASK;
            boolean rdsOnNative = (rdsMode != FmProxy.RDS_MODE_OFF);
            boolean afOnNative = (afMode != FmProxy.AF_MODE_OFF);

            /* Set RDS/RBDS from parameters. */
            int rdsModeNative = rdsMode & FmReceiverServiceState.RDS_RBDS_BITMASK;

            /* Execute commands. */
            try {
                if (setRdsModeNative(rdsOnNative, afOnNative, rdsModeNative)) {
                    returnStatus = FmProxy.STATUS_OK;
                } else {
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
                }
            } catch (Exception e) {
                Log.e(TAG, "setRdsNative failed", e);
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean setRdsModeNative(boolean rdsOn, boolean afOn, int rdsType);

    /**
     * Configures FM audio mode to be mono, stereo or blend. Will result in an
     * audio mode event callback.
     * 
     * @param audioMode
     *            the audio mode such as stereo or mono. The following values
     *            should be used {@link FmProxy#AUDIO_MODE_AUTO},
     *            {@link FmProxy#AUDIO_MODE_STEREO},
     *            {@link FmProxy#AUDIO_MODE_MONO} or
     *            {@link FmProxy#AUDIO_MODE_BLEND}.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setAudioMode(int audioMode) {
         queueFMCommand(new FMJob(FMCommand.FM_SET_AUDIO_MODE, audioMode));
         return FmProxy.STATUS_OK;
    }


    private int process_setAudioMode(int audioMode) {
        int returnStatus = FmProxy.STATUS_OK;

        Log.v(TAG, "setAudioMode()");

        if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                    FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SET_AUDIO_MODE;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                    FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);

            /* Trim excess data from parameters. */
            audioMode = audioMode & FmReceiverServiceState.AUDIO_MODE_BITMASK;

            try {
                if (setAudioModeNative(audioMode))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setAudioModeNative failed", e);
            }
             if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        }

        return returnStatus;
    }

    private native boolean setAudioModeNative(int audioMode);

    /**
     * Configures FM audio path to AUDIO_PATH_NONE, AUDIO_PATH_SPEAKER,
     * AUDIO_PATH_WIRED_HEADSET or AUDIO_PATH_DIGITAL. Will result in an audio
     * path event callback.
     * 
     * @param audioPath
     *            the audio path such as AUDIO_PATH_NONE, AUDIO_PATH_SPEAKER,
     *            AUDIO_PATH_WIRED_HEADSET or AUDIO_PATH_DIGITAL. The following
     *            values should be used {@link FmProxy#AUDIO_PATH_NONE},
     *            {@link FmProxy#AUDIO_PATH_SPEAKER},
     *            {@link FmProxy#AUDIO_PATH_WIRED_HEADSET} or
     *            {@link FmProxy#AUDIO_PATH_DIGITAL}.
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setAudioPath(int audioPath) {
         queueFMCommand(new FMJob(FMCommand.FM_SET_AUDIO_PATH, audioPath));
         return FmProxy.STATUS_OK;
    }


    private int process_setAudioPath(int audioPath) {
        int returnStatus = FmProxy.STATUS_OK;

        Log.v(TAG, "setAudioPath(" + Integer.toString(audioPath) + ")");

        if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND != 
                    FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SET_AUDIO_PATH;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                        FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);

            /* Trim excess data from parameters. */
            audioPath = audioPath & FmReceiverServiceState.AUDIO_PATH_BITMASK;

            try {
                if (setAudioPathNative(audioPath))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setAudioPathNative failed", e);
            }
        }
        if (returnStatus != FmProxy.STATUS_OK)
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

    if (FmReceiverServiceConfig.USE_FM_AUDIO_ROUTING)
    {
        if (audioPath == FmProxy.AUDIO_PATH_NONE)
            AudioSystem.setParameters("fm_route=disabled");
        else if (audioPath == FmProxy.AUDIO_PATH_SPEAKER)
            AudioSystem.setParameters("fm_route=fm_speaker");
        else if (audioPath == FmProxy.AUDIO_PATH_WIRE_HEADSET)
            AudioSystem.setParameters("fm_route=fm_headset");
    }

        return returnStatus;
    }

    private native boolean setAudioPathNative(int audioPath);

    /**
     * Sets the minimum frequency step size to use when scanning for stations.
     * This function does not result in a status callback and the calling
     * application should therefore keep track of this setting.
     * 
     * @param stepSize
     *            a frequency interval set to
     *            {@link FmProxy#FREQ_STEP_100KHZ} or
     *            {@link FmProxy#FREQ_STEP_50KHZ}.
     * 
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setStepSize(int stepSize) {
         queueFMCommand(new FMJob(FMCommand.FM_SET_STEP_SIZE, stepSize));
         return FmProxy.STATUS_OK;
    }



    private int process_setStepSize(int stepSize) {
        Log.v(TAG, "setStepSize()");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((stepSize != FmProxy.FREQ_STEP_50KHZ) &&
            (stepSize != FmProxy.FREQ_STEP_100KHZ)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                    FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SET_STEP_SIZE;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                    FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);

            try {
                if (setScanStepNative(stepSize))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setScanStepNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean setScanStepNative(int stepSize);

    /**
     * Sets the volume to use.
     * 
     * @param volume
     *            range from 0 to 0x100
     * 
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setFMVolume(int volume) {
         queueFMCommand(new FMJob(FMCommand.FM_SET_VOLUME, volume));
         return FmProxy.STATUS_OK;
    }

    private int process_setFMVolume(int volume) {
        Log.d(TAG, "setFMVolume()");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((volume < 0 ) || (volume > 256 ))
        {
            Log.d(TAG, "volume is illegal ="+volume);
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                    FmReceiverServiceState.radio_state) {
            Log.d(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SET_FM_VOLUME;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                        FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);
            Log.d(TAG, "setFMVolume ="+volume);
            try {
                if (setFMVolumeNative(volume))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setFMVolumeNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean setFMVolumeNative(int volume);

    /**
     * Sets a the world frequency region and deemphasis time. This results in a
     * world region event callback.
     * 
     * @param worldRegion
     *            the world region the FM receiver is located. Set to
     *            {@link FmProxy#FUNC_REGION_NA},
     *            {@link FmProxy#FUNC_REGION_EUR} or
     *            {@link FmProxy#FUNC_REGION_JP}.
     * @param deemphasisTime
     *            the deemphasis time that can be set to either
     *            {@link FmProxy#DEEMPHASIS_50U} or
     *            {@link FmProxy#DEEMPHASIS_75U}.
     * 
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setWorldRegion(int worldRegion, int deemphasisTime) {
        queueFMCommand(new FMJob(FMCommand.FM_SET_WORLD_REGION, worldRegion, deemphasisTime));
         return FmProxy.STATUS_OK;
    }


    private int process_setWorldRegion(int worldRegion, int deemphasisTime) {
        Log.v(TAG, "setWorldRegion()");

        int returnStatus = FmProxy.STATUS_OK;

        /* Sanity check of parameters. */
        if ((worldRegion != FmProxy.FUNC_REGION_NA) &&
            (worldRegion != FmProxy.FUNC_REGION_EUR) &&
            (worldRegion != FmProxy.FUNC_REGION_JP)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if ((deemphasisTime != FmProxy.DEEMPHASIS_50U) &&
            (deemphasisTime != FmProxy.DEEMPHASIS_75U)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                    FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
            /* Set timer to check that this did not lock the state machine. */
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;
            msg.arg1 = FmReceiverServiceState.OPERATION_SET_WORLD_REGION;
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            operationHandler.sendMessageDelayed(msg,
                        FmReceiverServiceState.OPERATION_TIMEOUT_GENERIC);

            try {
                if (setRegionNative(worldRegion) && configureDeemphasisNative(deemphasisTime))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setRdsNative failed", e);
            }
            if (returnStatus != FmProxy.STATUS_OK)
                operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean setRegionNative(int region);

    private native boolean configureDeemphasisNative(int time);

    /**
     * Estimates the current frequencies noise floor level. Generates an
     * Estimated NFL Event when complete. The returned NFL value can be used to
     * determine which minimum signal strength to use seeking stations.
     * 
     * @param estimatedNoiseFloorLevel
     *            Estimate noise floor to {@link FmProxy#NFL_LOW},
     *            {@link FmProxy#NFL_MED} or {@link FmRecei-vver#NFL_FINE}.
     * 
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int estimateNoiseFloorLevel(int nflLevel) {
         queueFMCommand(new FMJob(FMCommand.FM_ESTIMATE_NOISE_FLOOR_LEVEL, nflLevel));
         return FmProxy.STATUS_OK;
        }


    private int process_estimateNoiseFloorLevel(int nflLevel) throws RemoteException {
         Log.v(TAG, "estimateNoiseFloorLevel()");

        int returnStatus = FmProxy.STATUS_OK;

         /* Sanity check of parameters. */
         if ((nflLevel != FmProxy.NFL_FINE) &&
             (nflLevel != FmProxy.NFL_MED) &&
             (nflLevel != FmProxy.NFL_LOW)) {
             returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
         } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                    FmReceiverServiceState.radio_state) {
             Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
             returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
         } else {
             FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_BUSY;
             /* Set timer to check that this did not lock the state machine. */
             Message msg = Message.obtain();
             msg.what = FmReceiverServiceState.OPERATION_TIMEOUT;  
             msg.arg1 = FmReceiverServiceState.OPERATION_NFL;
             operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
             operationHandler.sendMessageDelayed(msg,FmReceiverServiceState.OPERATION_TIMEOUT_NFL);

             try {
                 if (estimateNoiseFloorNative(nflLevel))
                     returnStatus = FmProxy.STATUS_OK;
                 else
                     returnStatus = FmProxy.STATUS_SERVER_FAIL;
             } catch (Exception e) {
                 returnStatus = FmProxy.STATUS_SERVER_FAIL;
                 Log.e(TAG, "estimateNoiseFloorNative failed", e);
             }
             if (returnStatus != FmProxy.STATUS_OK)
                 operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        }

        return returnStatus;
    }

    private native boolean estimateNoiseFloorNative(int level);

    /**
     * Sets the live audio polling function that can provide RSSI data on the
     * currently tuned frequency at specified intervals.
     * 
     * @param liveAudioPolling
     *            enable/disable live audio data quality updating.
     * @param signalPollInterval
     *            time between RSSI signal polling in milliseconds.
     * 
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setLiveAudioPolling(boolean liveAudioPolling,
                int signalPollInterval) {
         queueFMCommand(new FMJob(FMCommand.FM_SET_LIVE_AUDIO_POLLING,
                    liveAudioPolling, signalPollInterval));
         return FmProxy.STATUS_OK;
    }


    private int process_setLiveAudioPolling(boolean liveAudioPolling,
            int signalPollInterval) throws RemoteException {
        int returnStatus = FmProxy.STATUS_OK;

        Log.v(TAG, "setLiveAudioPolling()");

        /* Sanity check of parameters. */
        if (liveAudioPolling &&
            ((signalPollInterval < FmReceiverServiceState.MIN_ALLOWED_SIGNAL_POLLING_TIME) ||
            (signalPollInterval > FmReceiverServiceState.MAX_ALLOWED_SIGNAL_POLLING_TIME))) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                FmReceiverServiceState.radio_state) {
            Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state =
                    FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

            try {
                if (getAudioQualityNative(liveAudioPolling) &&
                    configureSignalNotificationNative(signalPollInterval))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setLiveAudioPolling failed", e);
            }

            FmReceiverServiceState.radio_state =
                        FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        }

        Log.v(TAG, "STATE = "+Integer.toString(FmReceiverServiceState.radio_state));
    fetchNextJob    ();
        return returnStatus;
    }

    private native boolean getAudioQualityNative(boolean enabled);

    private native boolean configureSignalNotificationNative(int interval);

    /*
     * Callback generator section.
     */

    /**
     * Sends event data from the local cache to all
     * registered callbacks.
     */

    private void sendStatusEventCallbackFromLocalStore(boolean SendNextJob) {
            Message msg = Message.obtain();
            FM_Status_Params status = new FM_Status_Params(FmReceiverServiceState.mFreq, 
                            FmReceiverServiceState.mRssi,
                            FmReceiverServiceState.mSnr,
                            FmReceiverServiceState.mRadioIsOn,
                            FmReceiverServiceState.mRdsProgramType,
                            FmReceiverServiceState.mRdsProgramService,
                            FmReceiverServiceState.mRdsRadioText,
                            FmReceiverServiceState.mRdsProgramTypeName,
                            FmReceiverServiceState.mIsMute);
            msg.what = FmReceiverServiceState.OPERATION_STATUS_EVENT_CALLBACK;
            msg.arg1 = (SendNextJob == true) ? 1 : 0;
            msg.obj = (Object) status;
            operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents typically supplied by a Status Event
     * to all registered callbacks.
     *
     * @param freq the current listening frequency.
     * @param rssi the RSSI of the current frequency.
     * @param snr the SNR value at the current frequency.
     * @param radioIsOn TRUE if the radio is on and FALSE if off.
     * @param rdsProgramType integer representation of program type.
     * @param rdsProgramService name of the service.
     * @param rdsRadioText text of the current program/service.
     * @param rdsProgramTypeName string version of the rdsProgramType parameter.
     * @param isMute TRUE if muted by hardware and FALSE if not.
     */
    private void sendStatusEventCallback(int freq, int rssi, int snr,
            boolean radioIsOn, int rdsProgramType,
            String rdsProgramService, String rdsRadioText,
            String rdsProgramTypeName, boolean isMute, int iSendNextJob) {

        try {
           final int callbacks = mCallbacks.beginBroadcast();
           for (int i = 0; i < callbacks; i++) {
              try {
                   /* Send the callback to each registered receiver. */
                   mCallbacks.getBroadcastItem(i).onStatusEvent(freq,
                        rssi, snr, radioIsOn, rdsProgramType,
                        rdsProgramService, rdsRadioText,
                        rdsProgramTypeName, isMute);
               } catch (RemoteException e) {
                   e.printStackTrace();
               }
           }
           mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        if(FmReceiverServiceState.radio_state != FmReceiverServiceState.RADIO_STATE_STOPPING &&
            FmReceiverServiceState.radio_state != FmReceiverServiceState.RADIO_STATE_OFF)
            FmReceiverServiceState.radio_state =
                    FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

        if (iSendNextJob > 0)
            fetchNextJob();
    }

    /**
     * Sends event data from the local cache to all
     * registered callbacks.
     */
    private void sendSeekCompleteEventCallbackFromLocalStore(boolean SendNextJob) {
            Message msg = Message.obtain();
            msg.what = FmReceiverServiceState.OPERATION_SEARCH_EVENT_CALLBACK;
            FM_Search_Params search_st
                = new FM_Search_Params(FmReceiverServiceState.mFreq,
                                       FmReceiverServiceState.mRssi,
                                       FmReceiverServiceState.mSnr,
                                       FmReceiverServiceState.mSeekSuccess);
            msg.obj = (Object) search_st;
            msg.arg1 = (SendNextJob == true) ? 1 : 0;
            operationHandler.sendMessage(msg);

    }

    /**
     * Sends the contents of a seek complete event to all registered
     * callbacks.
     *
     * @param freq
     *            The station frequency if located or the last seek frequency if
     *            not successful.
     * @param rssi
     *            the RSSI at the current frequency.
     * @param snr
     *            the SNR at the current frequency.
     * @param seekSuccess
     *            TRUE if search was successful, false otherwise.
     */
    private void sendSeekCompleteEventCallback(int freq, int rssi,
                int snr, boolean seekSuccess, int iSendNextJob) {
        Log.v(TAG, "sendSeekCompleteEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_SEEK_CMPL);
            i.putExtra(FmProxy.EXTRA_FREQ, freq);
            i.putExtra(FmProxy.EXTRA_RSSI, rssi);
            i.putExtra(FmProxy.EXTRA_SNR, snr);
            i.putExtra(FmProxy.EXTRA_SUCCESS, seekSuccess);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);
        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
         try {

            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onSeekCompleteEvent(freq, rssi,
                                                            snr, seekSuccess);
                } catch (Throwable t) {
                    Log.e(TAG, "sendSeekCompleteEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
         } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
         }
       }
        /* Update state machine. */
        FmReceiverServiceState.radio_state =
                    FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

    if (iSendNextJob > 0)
        fetchNextJob();
    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendRdsModeEventCallbackFromLocalStore() {
        Log.v(TAG, "sendRdsModeEventCallbackFromLocalStore");
        int rds = 0;
        int af = FmReceiverServiceState.mAfOn ? 1 : 0;
        /* Adapt for RDS vs RBDS. */
        if (FmReceiverServiceState.mRdsOn) {
            rds = (FmReceiverServiceState.mRdsType == 0) ? 1 : 2;
        }
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_RDS_EVENT_CALLBACK;
        msg.arg1 = rds;
        msg.arg2 = af;
        operationHandler.sendMessage(msg);


    }

    /**
     * Sends the contents of an RDS mode event to all registered callbacks.
     *
     * @param rdsMode
     *            the current RDS mode
     * @param alternateFreqHopEnabled
     *            TRUE if AF is enabled, false otherwise.
     */
    private void sendRdsModeEventCallback(int rdsMode, int alternateFreqMode) {
        Log.d(TAG, "sendRdsModeEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_RDS_MODE);
            i.putExtra(FmProxy.EXTRA_RDS_MODE, rdsMode);
            i.putExtra(FmProxy.EXTRA_ALT_FREQ_MODE, alternateFreqMode);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);

        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onRdsModeEvent(rdsMode, alternateFreqMode);
                } catch (Throwable t) {
                    Log.e(TAG, "sendRdsModeEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
       }
        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        fetchNextJob();

    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendRdsDataEventCallbackFromLocalStore(int rdsDataType,
                    int rdsIndex, String rdsText) {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_RDS_DATA_EVENT_CALLBACK;
        msg.arg1 = rdsDataType;
        msg.arg2 = rdsIndex;
        msg.obj = new String(rdsText);
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of an RDS mode event to all registered callbacks.
     *
     * @param rdsMode
     *            the current RDS mode
     * @param alternateFreqHopEnabled
     *            TRUE if AF is enabled, false otherwise.
     */
    private void sendRdsDataEventCallback(int rdsDataType, int rdsIndex, String rdsText) {
        Log.v(TAG, "sendRdsDataEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_RDS_DATA);
            i.putExtra(FmProxy.EXTRA_RDS_DATA_TYPE, rdsDataType);
            i.putExtra(FmProxy.EXTRA_RDS_IDX, rdsIndex);
            i.putExtra(FmProxy.EXTRA_RDS_TXT, rdsIndex);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);
        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
         try {
           final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onRdsDataEvent(rdsDataType, rdsIndex, rdsText);
                } catch (Throwable t) {
                    Log.e(TAG, "sendRdsModeEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendAudioModeEventCallbackFromLocalStore() {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_AUDIO_MODE_EVENT_CALLBACK;
        msg.arg1 = FmReceiverServiceState.mAudioMode;
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of an audio mode event to all registered callbacks.
     *
     * @param audioMode
     *            the current audio mode in use.
     */
    private void sendAudioModeEventCallback(int audioMode) {
        Log.v(TAG, "sendAudioModeEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_AUDIO_MODE);
            i.putExtra(FmProxy.EXTRA_AUDIO_MODE, audioMode);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);
        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onAudioModeEvent(audioMode);
                } catch (Throwable t) {
                    Log.e(TAG, "sendAudioModeEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
        /* Update state machine. */
        if(FmReceiverServiceState.radio_state != FmReceiverServiceState.RADIO_STATE_STOPPING &&
            FmReceiverServiceState.radio_state != FmReceiverServiceState.RADIO_STATE_OFF)
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

        fetchNextJob();
    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendAudioPathEventCallbackFromLocalStore() {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_AUDIO_PATH_EVENT_CALLBACK;
        msg.arg1 = FmReceiverServiceState.mAudioPath;
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of an audio path event to all registered callbacks.
     *
     * @param audioPath
     *            the current audio mode in use.
     */
    private void sendAudioPathEventCallback(int audioPath) {
        Log.v(TAG, "sendAudioPathEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_AUDIO_PATH);
            i.putExtra(FmProxy.EXTRA_AUDIO_PATH, audioPath);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);
        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onAudioPathEvent(audioPath);
                } catch (Throwable t) {
                    Log.e(TAG, "sendAudioPathEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
        /* Update state machine. */
        if(FmReceiverServiceState.radio_state != FmReceiverServiceState.RADIO_STATE_STOPPING &&
            FmReceiverServiceState.radio_state != FmReceiverServiceState.RADIO_STATE_OFF)
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

        fetchNextJob();

    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendWorldRegionEventCallbackFromLocalStore() {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_REGION_EVENT_CALLBACK;
        msg.arg1 = FmReceiverServiceState.mWorldRegion;
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of a world region event to all registered callbacks.
     *
     * @param worldRegion
     *            the current frequency band region in use.
     */
    private void sendWorldRegionEventCallback(int worldRegion) {
        Log.v(TAG, "sendWorldRegionEventCallback");
        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_WRLD_RGN);
            i.putExtra(FmProxy.EXTRA_WRLD_RGN, worldRegion);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);

        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onWorldRegionEvent(worldRegion);
                } catch (Throwable t) {
                    Log.e(TAG, "sendWorldRegionEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        fetchNextJob();
    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendEstimateNflEventCallbackFromLocalStore() {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_NFE_EVENT_CALLBACK;
        msg.arg1 = FmReceiverServiceState.mEstimatedNoiseFloorLevel;
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of a world region event to all registered callbacks.
     *
     * @param worldRegion
     *            the current frequency band region in use.
     */
    private void sendEstimateNflEventCallback(int nfl) {
        Log.v(TAG, "sendEstimateNflEventCallback");
        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_EST_NFL);
            i.putExtra(FmProxy.EXTRA_NFL, nfl);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);

        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onEstimateNflEvent(nfl);
                } catch (Throwable t) {
                    Log.e(TAG, "sendEstimateNflEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

        fetchNextJob();

    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    private void sendLiveAudioQualityEventCallbackFromLocalStore(int rssi, int snr) {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_LIVE_AUDIO_EVENT_CALLBACK;
        msg.arg1 = rssi;
        msg.arg2 = snr;
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of a world region event to all registered callbacks.
     *
     * @param worldRegion
     *            the current frequency band region in use.
     */
    private void sendLiveAudioQualityEventCallback(int rssi, int snr) {
        Log.v(TAG, "sendLiveAudioQualityEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_AUDIO_QUAL);
            i.putExtra(FmProxy.EXTRA_RSSI, rssi);
            i.putExtra(FmProxy.EXTRA_SNR, snr);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);

        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onLiveAudioQualityEvent(rssi, snr);
                } catch (Throwable t) {
                    Log.e(TAG, "sendLiveAudioQualityEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
    }

    /**
     * Sends volume event from the local cache to all
     * registered callbacks.
     */
    private void sendVolumeEventCallbackFromLocalStore(int status, int volume) {
        Message msg = Message.obtain();
        msg.what = FmReceiverServiceState.OPERATION_VOLUME_EVENT_CALLBACK;
        msg.arg1 = status;
        msg.arg2 = volume;
        operationHandler.sendMessage(msg);
    }

    /**
     * Sends the contents of a FM volume event to all registered callbacks.
     *
     * @param status
     *            equal to 0 if successful. Otherwise returns a non-zero error
     *            code.
     * @param volume
     *            range from 0 to 0x100
     */
    private void sendVolumeEventCallback(int status, int volume) {
        Log.v(TAG, "sendVolumeEventCallback");

        if (FmReceiverServiceConfig.USE_BROADCAST_INTENTS == true) {
            Intent i = new Intent(FmProxy.ACTION_ON_VOL);
            i.putExtra(FmProxy.EXTRA_STATUS, status);
            i.putExtra(FmProxy.EXTRA_VOL, volume);
            mContext.sendOrderedBroadcast(i, FmProxy.FM_RECEIVER_PERM);

        } else if (FmReceiverServiceConfig.USE_CALLBACKS == true) {
        try {
            final int callbacks = mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacks; i++) {
                try {
                    /* Send the callback to each registered receiver. */
                    mCallbacks.getBroadcastItem(i).onVolumeEvent(status, volume);
                } catch (Throwable t) {
                    Log.e(TAG, "sendVolumeEventCallback", t);
                }
            }
            mCallbacks.finishBroadcast();
        } catch (IllegalStateException e_i) {
            e_i.printStackTrace();
        }
        }
        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        fetchNextJob();

    }

    /**
     * Sends event data from the local cache to all registered callbacks.
     */
    public void checkForPendingResponses() {
        if (V) Log.d(TAG, "checkForPendingResponses");
        sendLiveAudioQualityEventCallbackFromLocalStore(FmReceiverServiceState.mRssi,
                            FmReceiverServiceState.mSnr);
    }

    /**
     * Sets the SNR threshold for the subsequent FM frequency tuning.
     * This value will be used by BTA stack internally.
     *
     * @param signalPollInterval
     *           SNR Threshold value (0 ~ 31 (BTA_FM_SNR_MAX) )
     *
     * @return STATUS_OK = 0 if successful. Otherwise returns a non-zero error
     *         code.
     */
    public synchronized int setSnrThreshold(int snrThreshold)
        /* throws RemoteException */ {
        int returnStatus = FmProxy.STATUS_OK;

        if (V) Log.d(TAG, "setSnrThreshold() - "+snrThreshold);

        /* Sanity check of parameters. */
        if ((snrThreshold < FmReceiverServiceState.MIN_ALLOWED_SNR_THRESHOLD) ||
            (snrThreshold > FmReceiverServiceState.MAX_ALLOWED_SNR_THRESHOLD)) {
            returnStatus = FmProxy.STATUS_ILLEGAL_PARAMETERS;
        } else if (FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND !=
                        FmReceiverServiceState.radio_state) {
            if (V) Log.d(TAG, "STATE = " + Integer.toString(FmReceiverServiceState.radio_state));
            returnStatus = FmProxy.STATUS_ILLEGAL_COMMAND;
        } else {
            FmReceiverServiceState.radio_state =
                            FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

            try {
                if (setSnrThresholdNative(snrThreshold))
                    returnStatus = FmProxy.STATUS_OK;
                else
                    returnStatus = FmProxy.STATUS_SERVER_FAIL;
            } catch (Exception e) {
                returnStatus = FmProxy.STATUS_SERVER_FAIL;
                Log.e(TAG, "setSnrThreshold failed", e);
            }

            FmReceiverServiceState.radio_state =
                        FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        }

        if (V) Log.d(TAG, "STATE = " + Integer.toString(FmReceiverServiceState.radio_state));
        return returnStatus;
    }

    private native boolean setSnrThresholdNative(int snrThreshold);

    /* JNI BTA Event callback functions. */
    public void onRadioOnEvent(int status) {
        if (V) Log.d(TAG, "onRadioOnEvent()");
        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mRadioIsOn = true;
        }

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        /* Update state machine. */
        if (!FmReceiverServiceState.mRadioIsOn) {
            FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_OFF;
        }

        /* Update local cache. */
        sendStatusEventCallbackFromLocalStore(true);
    }

    public void onRadioOffEvent(int status) {
        if (V) Log.d(TAG, "onRadioOffEvent()");

        queueFMCommand(new FMJob(FMCommand.FM_CHIP_OFF));
    }

    public void onRadioMuteEvent(int status, boolean muted) {
        if (V) Log.d(TAG, "onRadioMuteEvent()");

        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mIsMute = muted;
        }

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);


        /* Callback. */
       sendStatusEventCallbackFromLocalStore(true);

    }

    public void onRadioTuneEvent(int status, int rssi, int snr, int freq) {
        if (V) Log.d(TAG, "onRadioTuneEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);


        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mRssi = rssi;
            FmReceiverServiceState.mSnr = snr;
            FmReceiverServiceState.mFreq = freq;
        }
        sendStatusEventCallbackFromLocalStore(true);

    }

    public void onRadioSearchEvent(int rssi, int snr, int freq) {
        if (V) Log.d(TAG, "onRadioSearchEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        /* Update local cache. */
        FmReceiverServiceState.mRssi = rssi;
        FmReceiverServiceState.mFreq = freq;
        FmReceiverServiceState.mSnr = snr;
        FmReceiverServiceState.mSeekSuccess = true;
        sendSeekCompleteEventCallbackFromLocalStore(false);
    }

    public void onRadioSearchCompleteEvent(int status, int rssi, int snr, int freq) {
        if (V) Log.d(TAG, "onRadioSearchCompleteEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        /* Update local cache. */
        FmReceiverServiceState.mRssi = rssi;
        FmReceiverServiceState.mSnr = snr;
        FmReceiverServiceState.mFreq = freq;
        FmReceiverServiceState.mSeekSuccess = (status == FmProxy.STATUS_OK);
        sendSeekCompleteEventCallbackFromLocalStore(true);

    }

    public void onRadioAfJumpEvent(int status, int rssi, int freq) {
        Log.v(TAG, "onRadioAfJumpEvent: status = " + status + ", rssi = " + rssi +
              ", freq = " + freq + ")");
        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        FmReceiverServiceState.mRssi = rssi;
        FmReceiverServiceState.mFreq = freq;
        FmReceiverServiceState.mSeekSuccess = true;
        sendSeekCompleteEventCallbackFromLocalStore(true);

        /* Is this of interest internally without knowing the new frequency? */
    }

    public void onRadioAudioModeEvent(int status, int mode) {
        if (V) Log.d(TAG, "onRadioAudioModeEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);


        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mAudioMode = mode;
        }
        sendAudioModeEventCallbackFromLocalStore();

    }

    public void onRadioAudioPathEvent(int status, int path) {
        if (V) Log.d(TAG, "onRadioAudioPathEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);


        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mAudioPath = path;
        }
        sendAudioPathEventCallbackFromLocalStore();

    }

    public void onRadioAudioDataEvent(int status, int rssi, int snr, int mode) {
        Log.v(TAG, "onRadioAudioDataEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mRssi = rssi;
            FmReceiverServiceState.mSnr = snr;
            FmReceiverServiceState.mAudioMode = mode;
        }
        sendLiveAudioQualityEventCallbackFromLocalStore(rssi, snr);
    }

    public void onRadioRdsModeEvent(int status, boolean rdsOn, boolean afOn, int rdsType) {
        if (V) Log.d(TAG, "onRadioRdsModeEvent()");

        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mRdsOn = rdsOn;
            FmReceiverServiceState.mAfOn = afOn;
            FmReceiverServiceState.mRdsType = rdsType;
            if (V) Log.d(TAG, "onRadioRdsModeEvent( rdsOn " + Boolean.toString(rdsOn) + ", afOn"
                    + Boolean.toString(afOn) + ","
                    + Integer.toString(FmReceiverServiceState.mRdsType) + ")");
        }

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
        /* Transmit event upwards to app. */
        sendRdsModeEventCallbackFromLocalStore();
        FmReceiverServiceState.radio_op_state = FmReceiverServiceState.RADIO_OP_STATE_NONE;
        /* Indicate that this command in the sequence is finished. */


    }

    // Should not be needed anymore
    public void onRadioRdsTypeEvent(int status, int rdsType) {
        if (V) Log.d(TAG, "onRadioRdsTypeEvent()");

        /* Update local cache. */
        if (FmReceiverServiceState.BTA_FM_OK == status) {
            FmReceiverServiceState.mRdsType = rdsType;
        }

        if (FmReceiverServiceState.RADIO_OP_STATE_STAGE_1 == FmReceiverServiceState.radio_op_state) {

            /* This response indicates that system is alive and well. */
            operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);
            /* Transmit event upwards to app. */
            sendRdsModeEventCallbackFromLocalStore();
            FmReceiverServiceState.radio_op_state = FmReceiverServiceState.RADIO_OP_STATE_NONE;
        } else {
            /* Indicate that this command in the sequence is finished. */
            FmReceiverServiceState.radio_op_state = FmReceiverServiceState.RADIO_OP_STATE_STAGE_2;
        }
    }

    public void onRadioRdsUpdateEvent(int status, int data, int index, String text) {
        if (V) Log.d(TAG, "onRadioRdsUpdateEvent(" + Integer.toString(status) + ","
                + Integer.toString(data) + "," + Integer.toString(index) + "," + text + ")");

        if (FmReceiverServiceState.BTA_FM_OK == status) {
            /* Update local cache. (For retrieval in status commands.) */
            switch (data) {
                case FmReceiverServiceState.RDS_ID_PTY_EVT:
                    FmReceiverServiceState.mRdsProgramType = index;
                    break;
                case FmReceiverServiceState.RDS_ID_PTYN_EVT:
                    FmReceiverServiceState.mRdsProgramTypeName = text;
                    break;
                case FmReceiverServiceState.RDS_ID_RT_EVT:
                    FmReceiverServiceState.mRdsRadioText = text;
                    break;
                case FmReceiverServiceState.RDS_ID_PS_EVT:
                    FmReceiverServiceState.mRdsProgramService = text;
                    break;

                default:
                    break;
            }

            /* Transmit individual message to app. */
            sendRdsDataEventCallbackFromLocalStore(data, index, text);
        }
    }

    public void onRadioDeemphEvent(int status, int time) {
        if (V) Log.d(TAG, "onRadioDeemphEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        fetchNextJob();
    }

    public void onRadioScanStepEvent(int stepSize) {
        if (V) Log.d(TAG, "onRadioScanStepEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;
        fetchNextJob();
    }

    public void onRadioRegionEvent(int status, int region) {
        if (V) Log.d(TAG, "onRadioRegionEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        FmReceiverServiceState.mWorldRegion = region;
        sendWorldRegionEventCallbackFromLocalStore();

    }

    public void onRadioNflEstimationEvent(int level) {
        if (V) Log.d(TAG, "onRadioNflEstimationEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        /* Update state machine. */
        FmReceiverServiceState.radio_state = FmReceiverServiceState.RADIO_STATE_READY_FOR_COMMAND;

        /* Update local cache. */
        FmReceiverServiceState.mEstimatedNoiseFloorLevel = level;
        sendEstimateNflEventCallbackFromLocalStore();

    }

    public void onRadioVolumeEvent(int status, int volume) {
        if (V) Log.d(TAG, "onRadioVolumeEvent()");

        /* This response indicates that system is alive and well. */
        operationHandler.removeMessages(FmReceiverServiceState.OPERATION_TIMEOUT);

        sendVolumeEventCallbackFromLocalStore(status, volume);
    }
    

}
