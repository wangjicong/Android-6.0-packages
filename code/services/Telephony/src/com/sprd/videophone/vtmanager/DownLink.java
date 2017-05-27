package com.sprd.videophone.vtmanager;

import java.nio.ByteBuffer;
import java.io.IOException;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Surface;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioCapbility;

class DownLink extends HandlerThread {
    private static final String LOG_TAG = "DownLink";
    private static final boolean VDBG = (SystemProperties.getInt("debug.videophone", 0) == 1);

    private static final long DEQUEQUE_TIMEOUT = 1000 * 30; // modify 1000 -> 1000*30 for dec
                                                            // latency;
    private int m_nRingBufferSize = VTManager.MAX_BUFFER_SIZE;

    int mVideoType = VTManager.VIDEO_TYPE_H263;
    private MediaCodec mCodec;
    Handler mHandler;
    private Surface mSurface;
    private Object m_ThreadLock = new Object();
    private VideoSource mVideoSource = null;
    private String vpatch = null;
    private Object m_seqLock = new Object();

    private volatile boolean m_bStarted = false;

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void vlog(String msg) {
        if (VDBG)
            Log.d(LOG_TAG, msg);
    }

    DownLink(String name) {
        super(name);
    }

    protected void onLooperPrepared() {
        mHandler = new Handler(DownLink.this.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VTManager.MSG_DOWNLINK_START:
                        boolean prestarted = false;
                        synchronized (m_ThreadLock) {
                            prestarted = m_bStarted;
                            m_bStarted = true;
                            m_ThreadLock.notify();
                        }
                        if (prestarted)
                        {
                            log("prestarted so return");
                            return;
                        }
                        int modemtype = SystemProperties.getInt("ril.radio.modemtype", -1);
                        if (VTManager.MODEM_TYPE_WCDMA == modemtype) {
                            vpatch = new String(SystemProperties.get("ro.modem.w.tty"));
                        }
                        else if (VTManager.MODEM_TYPE_TDSCDMA == modemtype)
                        {
                            vpatch = new String(SystemProperties.get("ro.modem.t.tty"));
                        }
                        else if (VTManager.MODEM_TYPE_LTE == modemtype)
                        {

                            RadioCapbility radioCap = TelephonyManager.getRadioCapbility();
                            if (radioCap == RadioCapbility.TDD_SVLTE) {
                                vpatch = new String(SystemProperties.get("ro.modem.t.tty"));
                            } else if (radioCap == RadioCapbility.TDD_CSFB) {
                                vpatch = new String(SystemProperties.get("ro.modem.tl.tty"));
                            } else if (radioCap == RadioCapbility.FDD_CSFB) {
                                vpatch = new String(SystemProperties.get("ro.modem.lf.tty"));
                            } else {
                                vpatch = new String(SystemProperties.get("ro.modem.l.tty"));
                            }
                        }
                        else
                        {
                            vpatch = "/dev/stty_td";
                        }
                        log("MSG_DOWNLINK_START m_bStarted: " + m_bStarted + ":vpatch: " + vpatch
                                + ":ModemType: "
                                + modemtype);
                        mVideoSource = new VideoSource(vpatch + 12, mVideoType);
                        mVideoSource.start();
                        try {
                            internalStartDecode();
                        } catch (MediaCodec.CodecException e) {
                            log("internalStartDecode error:" + e);
                            /* SPRD: add for bug410365 @{ */
                            synchronized (m_ThreadLock) {
                                m_ThreadLock.notify();
                            }
                            /* @} */
                            releaseCodec();
                            /* SPRD: modify for bug410365 @{ */
                            // stopWork();
                            stopWorkWhenCodecError();
                            /* @} */
                        }
                        break;
                }
            }
        };
        synchronized (m_ThreadLock) {
            m_ThreadLock.notify();
        }
    }

    /* SPRD: add for bug410365 @{ */
    void stopWorkWhenCodecError() {
        log("stopWork() E");
        synchronized (m_seqLock) {
            if (m_bStarted) {
                synchronized (m_ThreadLock) {
                    m_bStarted = false;
                    if (mVideoSource != null)
                        mVideoSource.stop();
                }
            }
        }
        log("stopWork() X");
    }

    /* @} */
    void startWork() {
        synchronized (m_seqLock)
        {
            if (mHandler == null)
            {
                log("mHandler == null waite HandlerThread to start ");
                synchronized (m_ThreadLock) {
                    start();
                    try {
                        m_ThreadLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (m_ThreadLock) {
                mHandler.sendEmptyMessage(VTManager.MSG_DOWNLINK_START);
                try {
                    m_ThreadLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void stopWork() {
        log("stopWork() E");
        synchronized (m_seqLock) {
            if (m_bStarted) {
                synchronized (m_ThreadLock) {
                    m_bStarted = false;
                    if (mVideoSource != null)
                        mVideoSource.stop();
                    try {
                        m_ThreadLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        log("stopWork() X");
    }

    void setSurface(Surface sf) {
        log("setSurface() E surface=" + sf);
        mSurface = sf;
        /*
         * if (m_bStarted && (null != sf)) { stopCB();
         * mHandler.sendEmptyMessage(VTManager.MSG_DOWNLINK_START); }
         */
        log("setSurface() X");
    }

    void setDecodeType(int type) {
        log("setDecodeType() type: " + type);
        mVideoType = type;
    }

    private void internalStartDecode() {
        log("internalStartDecode() EE");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        String typeStr = "video/3gpp";
        if (mVideoType == VTManager.VIDEO_TYPE_MPEG4) {
            typeStr = "video/mp4v-es";
        }
        log("internalStartDecode() create type: " + typeStr);
        try {
            mCodec = MediaCodec.createDecoderByType(typeStr);
        } catch (IOException e) {
            log("createDecoderByType error:" + e);
        }
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, typeStr);
        format.setInteger(MediaFormat.KEY_WIDTH, 176);
        format.setInteger(MediaFormat.KEY_HEIGHT, 144);
        log("internalStartDecode() configure" + mSurface);
        try {
            mCodec.configure(format, mSurface, null, 0);
        } catch (IllegalArgumentException f) {
            mCodec.configure(format, null, null, 0);
            log("configure() surface null ");
        }
        log("internalStartDecode() start");
        mCodec.start();
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
        int inputBufferIndex = -1;
        for (;;) {
            if (!m_bStarted)
            {
                log("internalStartDecode exit from stop  ");
                break;
            }
            try {
                inputBufferIndex = mCodec.dequeueInputBuffer(DEQUEQUE_TIMEOUT);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                if (!m_bStarted)
                {
                    log("internalStartDecode exit from stop ");
                    break;
                }
                log("to check mediaCodec is ok");
                try {
                    format = mCodec.getOutputFormat();
                } catch (IllegalStateException f) {
                    log(" checked mediaCodec is bad so restart downlink");
                    mVideoSource.stop();
                    mVideoSource = null;
                    releaseCodec();
                    log("restart downlink start mVideoSource");
                    mVideoSource = new VideoSource(vpatch + 12, mVideoType);
                    mVideoSource.start();
                    try {
                        mCodec = MediaCodec.createDecoderByType(typeStr);
                    } catch (IOException ex) {
                        log("createDecoderByType error:" + ex);
                    }
                    format = new MediaFormat();
                    format.setString(MediaFormat.KEY_MIME, typeStr);
                    format.setInteger(MediaFormat.KEY_WIDTH, 176);
                    format.setInteger(MediaFormat.KEY_HEIGHT, 144);
                    mCodec.configure(format, mSurface, null, 0);
                    log("restart downlink start mCodec ");
                    mCodec.start();
                    inputBuffers = mCodec.getInputBuffers();
                    outputBuffers = mCodec.getOutputBuffers();
                    inputBufferIndex = -1;
                }
                continue;
            }
            vlog("internalStartDecode(), dequeueInputBuffer: " + inputBufferIndex);
            if (inputBufferIndex >= 0) {
                int nSize = mVideoSource.read(inputBuffers[inputBufferIndex]); // modify for
                                                                               // Bug#202658
                vlog("internalStartDecode(), nSize: " + nSize);
                if (nSize > 0) {
                    try {
                        inputBuffers[inputBufferIndex].position(0);
                        log("(mVideoSource.is_read_header()" + mVideoSource.is_read_header());
                        if (mVideoSource.is_read_header()) {
                            mCodec.queueInputBuffer(inputBufferIndex, 0, nSize, 0, 2);
                        } else {
                            mCodec.queueInputBuffer(inputBufferIndex, 0, nSize, 0, 0);
                        }
                    } catch (MediaCodec.CryptoException e) {
                        e.printStackTrace();
                        log("internalStartDecode() e.getErrorCode(): " + e.getErrorCode());
                        if (e.getErrorCode() == -2998) { // decode error, need i-frame
                            VTManager.getInstance().controlIFrame(false, true);
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                } else {
                    // break;
                    continue;
                }
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputBufferIndex = -1;
            try {
                outputBufferIndex = mCodec.dequeueOutputBuffer(info, DEQUEQUE_TIMEOUT);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                continue;
            }
            vlog("internalStartDecode(), dequeueOutputBuffer: " + outputBufferIndex);
            if (outputBufferIndex >= 0) {// outputBuffer is ready to be processed or rendered.
                vlog("VT_TS Down internalStartDecode(), releaseOutputBuffer");
                mCodec.releaseOutputBuffer(outputBufferIndex, true);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Subsequent data will conform to new format.
                format = mCodec.getOutputFormat();
            }
        }
        log("internalStartDecode() X");
        releaseCodec();
        synchronized (m_ThreadLock) {
            m_ThreadLock.notify();
        }
        log("internalStartDecode() XX");
    }

    private void releaseCodec() {
        if (mCodec != null) {
            try {
                mCodec.stop();
                mCodec.release();
                mCodec = null;
            } catch (IllegalStateException e) {
                log("releaseCodec()->error:" + e);
            }
        }
    }

    public boolean isStared() {
        return m_bStarted;
    }
};
