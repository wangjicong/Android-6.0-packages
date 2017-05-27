
package com.spreadtrum.sgps;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientSocket {

    private static final int MASK_8_BIT = 0xFF;
    // private static final int SOCKET_TIME_OUT_TIME = 2000;
    private static final int LINE_OUT_SIZE = 10;
    private static final int BUFFER_SIZE = 1024;
    private static final String TAG = "SGPS/ClientSocket";
    private static final int SERVER_PORT = 7000;
    private Socket mClientSocket = null;
    private DataInputStream mDataInput = null;
    private DataOutputStream mDataOutput = null;
    private String mCommand = null;
    private String mResponse = null;
    private BlockingQueue<String> mCommandQueue = null;
    private SgpsActivity mCallBack = null;
    private Thread mSendThread = null;
    private Thread mReceiveThread = null;
    private byte[] mDataBuffer = null;
    private boolean receiveThreadRunning = false;

    public ClientSocket(SgpsActivity callBack) {
        Log.v(TAG, "ClientSocket constructor");
        this.mCallBack = callBack;
        mCommandQueue = new LinkedBlockingQueue<String>();
        mDataBuffer = new byte[BUFFER_SIZE];
        mReceiveThread = new Thread(new Runnable() {

            public void run() {
                Log.v(TAG, "mReceiveThread run !");
                openClient();
                while (receiveThreadRunning) {
                    if (null != mDataInput) {
                        try {
                            String result = null;
                            int line = 0;
                            int count = -1;
                            while ((count = mDataInput.read(mDataBuffer)) != -1) {
                                line++;
                                result = new String(mDataBuffer, 0, count);
                                Log.v(TAG, "line: " + line + " sentence: "
                                        + result);
                                if (result.contains("PSPRD")) {
                                    mResponse = result;
                                    Log.v(TAG, "Get response from MNL: "
                                            + result);
                                    break;
                                }
                                if (line > LINE_OUT_SIZE) {
                                    mResponse = "TIMEOUT";
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            Log.w(TAG,
                                    "sendCommand IOException: "
                                            + e.getMessage());
                            mResponse = "ERROR";
                        }
                    } else {
                        Log.d(TAG, "out is null");
                        mResponse = "ERROR";
                    }
                    if (null != ClientSocket.this.mCallBack) {
                        ClientSocket.this.mCallBack.onResponse(mResponse);
                    }
                }
            }
        });
        mReceiveThread.start();
        receiveThreadRunning = true;

        mSendThread = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    try {
                        mCommand = mCommandQueue.take();
                        Log.v(TAG, "Queue take command:" + mCommand);
                    } catch (InterruptedException ie) {
                        Log.w(TAG,
                                "Take command interrupted:" + ie.getMessage());
                        return;
                    }
                    if (null != mDataOutput) {
                        try {
                            mDataOutput.writeBytes(mCommand);
                            mDataOutput.write('\r');
                            mDataOutput.write('\n');
                            mDataOutput.flush();
                        } catch (IOException e) {
                            Log.w(TAG,
                                    "sendCommand IOException: "
                                            + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "out is null");
                    }
                    mCommand = null;
                }
            }
        });
        mSendThread.start();
    }

    public void endClient() {
        Log.v(TAG, "enter endClient");
        receiveThreadRunning = false;
        mReceiveThread.interrupt();
        mSendThread.interrupt();
        Log.v(TAG, "Queue remaining:" + mCommandQueue.size());
        closeClient();
        mCallBack = null;
    }

    public void sendCommand(String command) {
        Log.v(TAG, "enter sendCommand");
        String sendComm = "$" + command + "*" + calcCS(command);
        Log.v(TAG, "Send command: " + sendComm);
        // if (!mSendThread.isAlive()) {
        // Log.v(TAG, "sendThread is not alive");
        // mSendThread.start();
        // }
        if (command.equals(sendComm) || mCommandQueue.contains(sendComm)) {
            Log.v(TAG, "send command return because of hasn't handle the same");
            return;
        }
        try {
            mCommandQueue.put(sendComm);
        } catch (InterruptedException ie) {
            Log.w(TAG, "send command interrupted:" + ie.getMessage());
        }
    }

    private String calcCS(String command) {
        if (null == command || "".equals(command)) {
            return "";
        }
        byte[] ba = command.toUpperCase().getBytes();
        int temp = 0;
        for (byte b : ba) {
            temp ^= b;
        }
        return String.format("%1$02x", temp & MASK_8_BIT).toUpperCase();
    }

    private void openClient() {
        Log.v(TAG, "enter startClient");
        if (null != mClientSocket && mClientSocket.isConnected()) {
            Log.d(TAG, "localSocket has started, return");
            return;
        }
        try {
            mClientSocket = new Socket("127.0.0.1", SERVER_PORT);
            // mClientSocket.setSoTimeout(SOCKET_TIME_OUT_TIME);
            mDataOutput = new DataOutputStream(mClientSocket.getOutputStream());
            mDataInput = new DataInputStream(mClientSocket.getInputStream());
        } catch (UnknownHostException e) {
            receiveThreadRunning = false;
            Log.w(TAG, e.getMessage());
        } catch (IOException e) {
            receiveThreadRunning = false;
            Log.w(TAG, e.getMessage());
        }
    }

    private void closeClient() {
        Log.v(TAG, "enter closeClient");
        try {
            if (null != mDataInput) {
                mDataInput.close();
            }
            if (null != mDataOutput) {
                mDataOutput.close();
            }
            if (null != mClientSocket) {
                mClientSocket.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "closeClient IOException: " + e.getMessage());
        } finally {
            mClientSocket = null;
            mDataInput = null;
            mDataOutput = null;
        }
    }

}
