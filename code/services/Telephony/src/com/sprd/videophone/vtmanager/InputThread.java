package com.sprd.videophone.vtmanager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import java.nio.ByteBuffer;

class InputThread extends HandlerThread {
    static final int DISPLAY_CLIENT = 0;
    static final int RECORD_CLIENT = 1;

    private static final String LOG_TAG = "InputThread";
    private static final boolean VDBG = (SystemProperties.getInt("debug.videophone", 0) == 1);

    private static final int BUFFER_SIZE = 2048;

    private static InputThread gInstance = null;
    private Thread mBgThread = null;
    private VideoSource mBgSource = null;
    private int mVideoType = VTManager.VIDEO_TYPE_H263;
    ByteBuffer backgroundBuff = ByteBuffer.allocate(VTManager.MAX_BUFFER_SIZE);
    private Object m_ThreadLock = new Object();

    private int mOffset = 0;
    private volatile boolean mStarted = false;
    private byte[] mBuffer = null;
    private String mDataSource = null;
    private List<VideoSource> mClients = new ArrayList<VideoSource>();
    Handler mHandler;
    private Object m_StopLock = new Object();
    private Object m_seqLock = new Object();

    private InputThread(String name) {
        super(name);
    }

    static synchronized InputThread getInstance() {
        if (gInstance == null) {
            gInstance = new InputThread(LOG_TAG);
        }
        return gInstance;
    }

    public void clear() {
        gInstance = null;
    }

    void stopWork() {
        log("stopWork e");
        if (mStarted) {
            mStarted = false;
        }
        log("stopWork X");
    }

    private void startBgThread()
    {
        Runnable BgRun = new Runnable() {
            public void run() {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (true) {
                    if (mBgSource.read(backgroundBuff) == 0) { // for Bug#226754
                        break;
                    }
                    backgroundBuff.position(0);
                }
                synchronized (m_ThreadLock) {
                    m_ThreadLock.notify();
                }
            }
        };
        mBgThread = new Thread(BgRun);
        mBgThread.start();
    }

    protected void onLooperPrepared() {
        mHandler = new Handler(InputThread.this.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VTManager.MSG_INPUT_STARTREAD:
                        log("handleMessage() MSG_INPUT_STARTREAD e");
                        boolean preStarted = false;
                        synchronized (m_StopLock) {
                            preStarted = mStarted;
                            mStarted = true;
                            m_StopLock.notify();
                        }
                        if (preStarted)
                        {
                            log("prestarted so return");
                            return;
                        }
                        if (mVideoType != VTManager.VIDEO_TYPE_H263)
                        {
                            mBgSource = new VideoSource(mDataSource, mVideoType);
                            mBgSource.start();
                            startBgThread();
                        }
                        internalStartRead();
                        break;
                }
            }
        };
        synchronized (m_StopLock) {
            m_StopLock.notify();
        }
    }

    int registerClient(VideoSource client, String dataSource, int VideoType) {
        log("registerClient() client: E" + client);
        synchronized (m_seqLock)
        {
            mVideoType = VideoType;
            if (mClients.size() == 0) {
                mDataSource = dataSource;
                if (mDataSource == null) {
                    log("first registrant should not have NULL dataSource");
                    return -1;
                }
                mClients.add(client);
                if (mHandler == null)
                {
                    synchronized (m_StopLock) {
                        start();
                        try {
                            m_StopLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                synchronized (m_StopLock) {
                    mHandler.sendEmptyMessage(VTManager.MSG_INPUT_STARTREAD);
                    try {
                        m_StopLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else
            {
                mClients.add(client);
            }
            log("registerClient() client: X" + client);
        }
        return 0;
    }

    void unregisterClient(VideoSource client) {
        log("unregisterClient() client:E" + client);
        synchronized (m_seqLock) {
            mClients.remove(client);
            if (mClients.size() == 0) {
                stopWork();
                log("InputThread.quit(): " + gInstance.quit());
                gInstance = null;
                log("InputThread.quit(): X");
            }
        }
        log("unregisterClient() client :X");
    }

    private void internalStartRead() {
        log("internalStartRead() E");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        mBuffer = new byte[BUFFER_SIZE];
        InputStream in = null;
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(mDataSource);
            in = new BufferedInputStream(fi/* "/data/vpin" */);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        for (;;) {
            if (!mStarted)
                break;
            int num = 0;
            try {
                num = in.read(mBuffer, 0, BUFFER_SIZE);
                vlog("internalStartRead(), read num: " + num);
                if (num > 0) {
                    mOffset += num;
                }
                else if (num == 0)
                {
                    log("internalStartRead(), data not available:");
                    try {
                        sleep(50, 0);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                }
                else {
                    log("internalStartRead() reach eof");
                    continue;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                continue;
            }
            synchronized (m_seqLock) {
                for (int i = 0; i < mClients.size(); i++) {
                    mClients.get(i).writeBuffer(mBuffer, num);
                }
            }
        }
        try {
            fi.close();
            in.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log("internalStartRead() X");

        synchronized (m_ThreadLock) {
            try {
                if (mBgSource != null) {
                    mBgSource.stop();
                    m_ThreadLock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized (m_StopLock) {
            m_StopLock.notify();
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void vlog(String msg) {
        if (VDBG)
            Log.d(LOG_TAG, msg);
    }
};
