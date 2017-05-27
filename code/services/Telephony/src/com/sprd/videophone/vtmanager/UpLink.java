package com.sprd.videophone.vtmanager;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.NoSuchElementException;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioCapbility;

class UpLink extends HandlerThread implements Camera.PreviewCallback {
    private static final String LOG_TAG = "UpLink";
    private static final boolean VDBG = (SystemProperties.getInt("debug.videophone", 0) == 1);

    private static final int DEQUEQUE_TIMEOUT = 30 * 1000; // modify 1000 to 30*1000 according enc
                                                           // lentancy
    private static final int BUFFER_SIZE = 38016;// 176*144*1.5;
    private static final int MAX_FRAME_COUNT = 3;

    private static final boolean DUMP_CAMERA = false;

    Handler mHandler;
    private Camera mCamera;
    private MediaCodec mCodec;

    private volatile boolean m_bStarted = false;
    private volatile  boolean mStartWaitData = false;
    private Object m_GetBuffer = new Object();
    private Object m_StopLock = new Object();
    private Object m_CameraLock = new Object();
    private Object m_StartLock = new Object();//SPRD:add for bug524107
    // Object m_pipeLock = new Object();
    private Thread mWorkThread = null;

    private int m_iframe = 0;
    private int m_nNum = 0;
    private int m_nDataStart = 0;
    private int m_nDataEnd = 0;
    private byte[] m_readBuffer = new byte[VTManager.MAX_FRAME_SIZE];
    private Queue<ByteBuffer> m_RingBuffer = new ConcurrentLinkedQueue<ByteBuffer>();
    private ByteBuffer mFakeCameraOutBuffer = null;

    private int m_nRingBufferSize = VTManager.MAX_BUFFER_SIZE;
    private int mVideoType = VTManager.VIDEO_TYPE_H263;

    private long m_presentationTimeUs = 0;
    private Object m_seqLock = new Object();
    private Bitmap mBitmapSrc = null;
    // native fields
    private int mRecorder;

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void vlog(String msg) {
        Log.d(LOG_TAG, msg);
    }

    UpLink(String name) {
        super(name);
        log("mRecorder: " + mRecorder);
    }

    void setCamera(Camera cam) {
        log("setCamera() E, " + cam);
        synchronized (m_CameraLock) {
            mCamera = cam;
        }
        log("setCamera() X");
    }

    void enablePreviewCallback(boolean enable) {
        log("enablePreviewCallback, " + enable);
        synchronized (m_CameraLock) {
            if (mCamera == null)
                return;
            if (enable)
            {
                mCamera.setPreviewCallback(UpLink.this);
            }
            else
            {
                mCamera.setPreviewCallback(null);
            }
        }
        log("enablePreviewCallback");
    }

    void startWork() {
        synchronized (m_seqLock) {
            if (mHandler == null)
            {
                log("mHandler == null wait HandlerThread start");
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
                mHandler.sendEmptyMessage(VTManager.MSG_UPLINK_START);
                try {
                    m_StopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void enableSubstitutePic(String fn, boolean enable)
    {
        synchronized (m_seqLock) {
            if (mHandler == null)
            {
                log("mHandler == null wait HandlerThread start");
                synchronized (m_StopLock) {
                    start();
                    try {
                        m_StopLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!m_bStarted)
            {
                synchronized (m_StopLock) {
                    mHandler.sendEmptyMessage(VTManager.MSG_UPLINK_START);
                    try {
                        m_StopLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            int iFlag = 0;
            if (enable)
                iFlag = 1;
            mHandler.sendMessage(mHandler.obtainMessage(VTManager.MSG_UPLINK_ENABLE_PIC, iFlag, 0,
                    fn));
        }
    }

    void stopWork() {
        log("stopWork() E");
        /* SPRD:bug494915 @{ */
        if (!mStartWaitData) {
            stopWorkWhenCodecError();
        }
        /* @} */
        synchronized (m_seqLock) {
            if (m_bStarted) {
                synchronized (m_StopLock) {
                    m_bStarted = false;
                    synchronized (m_CameraLock) {
                        if (mCamera != null) {
                            mCamera.setPreviewCallback(null);
                        }
                    }
                    synchronized (m_GetBuffer) {
                        m_GetBuffer.notify();
                    }
                    /*
                     * synchronized(m_pipeLock) { m_pipeLock.notify(); }
                     */
                    try {
                        m_StopLock.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        log("stopWork() X");
    }

    void setEncodeType(int type) {
        log("setEncodeType() type: " + type);
        mVideoType = type;
    }

    protected void onLooperPrepared() {
        mHandler = new Handler(UpLink.this.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VTManager.MSG_UPLINK_START:
                        log("handleMessage() MSG_UPLINK_START, UpLink.this: " + UpLink.this);
                        boolean preStarted = false;
                        synchronized (m_StopLock) {
                            preStarted = m_bStarted;
                            m_bStarted = true;
                            m_StopLock.notify();
                        }
                        if (preStarted)
                        {
                            log("prestarted so return");
                            return;
                        }
                        synchronized (m_CameraLock) {
                            if (mCamera != null) {
                                mCamera.setPreviewCallback(UpLink.this);
                            }
                        }
                        try {
                            internalStartEncode();
                        } catch (MediaCodec.CodecException e) {
                            log("internalStartDecode error:" + e);
                            /* SPRD: add for bug410365 @{ */
                            synchronized (m_StopLock) {
                                m_StopLock.notify();
                            }
                            /* @} */
                            releaseCodec();
                            /* SPRD: modify for bug410365 @{ */
                            // stopWork();
                            stopWorkWhenCodecError();
                            /* @} */
                        }
                        break;
                    case VTManager.MSG_UPLINK_ENABLE_PIC:
                        log("handleMessage() MSG_UPLINK_ENABLE_PIC, enable: " + msg.arg1 + ", fn: "
                                + msg.obj);
                        if (msg.arg1 == 0) {
                            if (mBitmapSrc != null)
                                mBitmapSrc.recycle();
                            mBitmapSrc = null;
                            mHandler.removeMessages(VTManager.MSG_UPLINK_ENABLE_PIC);
                        } else {
                            decodePic((String) msg.obj);
                            mHandler.sendMessageDelayed(Message.obtain(msg), 100);
                        }
                        break;
                }
            }
        };
        synchronized (m_StopLock) {
            m_StopLock.notify();
        }
    }

    /* SPRD: add for bug410365 @{ */
    void stopWorkWhenCodecError() {
        log("stopWork() E");
        synchronized (m_seqLock) {
            if (m_bStarted) {
                synchronized (m_StopLock) {
                    m_bStarted = false;
                    synchronized (m_CameraLock) {
                        if (mCamera != null) {
                            mCamera.setPreviewCallback(null);
                        }
                    }
                    synchronized (m_GetBuffer) {
                        m_GetBuffer.notify();
                    }
                }
            }
        }
        log("stopWork() X");
    }

    /* @} */
    private void decodePic(String fn) {
        log("decodePic() E fn:" + fn);
        int size = 0;
        if (mBitmapSrc == null) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            mBitmapSrc = BitmapFactory.decodeFile(fn, options);
            ;// createThumb(fn);
            if (mBitmapSrc == null)
            {
                log(" decodePic fail so return ");
                return;
            }
            size = mBitmapSrc.getByteCount();
            log("decodePic() size: " + size);
            ByteBuffer inBuffer = null;
            inBuffer = ByteBuffer.allocate(size);
            mFakeCameraOutBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            mBitmapSrc.copyPixelsToBuffer(inBuffer);
            VTManager.getInstance().native_RGB565toYUV420(inBuffer.array(), size,
                    mFakeCameraOutBuffer.array(), 144, 176);
            synchronized (m_GetBuffer) {
                m_RingBuffer.clear();
            }
        }
        synchronized (m_GetBuffer) {
            m_RingBuffer.add(mFakeCameraOutBuffer);
            m_GetBuffer.notify();
        }
    }

    private void internalStartEncode() {

        Runnable mainRun = new Runnable() {
            public void run() {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                log("internalStartEncode() E");
                String typeStr = "video/3gpp";
                if (mVideoType == VTManager.VIDEO_TYPE_MPEG4) {
                    typeStr = "video/mp4v-es";
                }
                /* SPRD: add for bug524107 @{ */
                MediaFormat format = new MediaFormat();
                ByteBuffer[] inputBuffers;
                ByteBuffer[] outputBuffers;
                synchronized (m_StartLock) {
                try {
                    mCodec = MediaCodec.createEncoderByType(typeStr);
                } catch (IOException e) {
                    log("createDecoderByType error:" + e);
                    return;
                }
                format.setString(MediaFormat.KEY_MIME, typeStr);
                format.setInteger(MediaFormat.KEY_WIDTH, 176);
                format.setInteger(MediaFormat.KEY_HEIGHT, 144);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 48000);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, 0x7FD00001);// OMX_SPRD_COLOR_FormatYVU420SemiPlanar
                format.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                log("internalStartEncode() configure");
                mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                log("internalStartEncode() start");
                mCodec.start();
                log("internalStartEncode() getInputBuffers");
                inputBuffers = mCodec.getInputBuffers();
                outputBuffers = mCodec.getOutputBuffers();
                }
                /* @} */
                // format = mCodec.getOutputFormat();
                OutputStream out = null;
                FileOutputStream fo = null;
                String vpatch;
                int modemtype = SystemProperties.getInt("ril.radio.modemtype", -1);
                ;
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
                log("MSG_UPLINK_START m_bStarted: " + m_bStarted + ":vpatch: " + vpatch
                        + ":ModemType: "
                        + modemtype);
                // OutputStream outFn = null;
                try {
                    fo = new FileOutputStream(vpatch + 12);
                    out = new BufferedOutputStream(fo);
                    // outFn = new BufferedOutputStream(new FileOutputStream("/data/vpout"));
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // for flow-control of vt uplink
                byte[] rData = new byte[20];
                int inputBufferIndex = -1;
                int outputBufferIndex = -1;
                /* SPRD: add for bug524107 @{ */
                synchronized (m_StartLock) {
                for (;;) {
                    if (!m_bStarted) {
                        break;
                    }
                    try {
                        inputBufferIndex = mCodec.dequeueInputBuffer(DEQUEQUE_TIMEOUT);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        if (!m_bStarted) {
                            break;
                        }
                        format = mCodec.getOutputFormat();
                        continue;
                    }
                    vlog("internalStartEncode(), dequeueInputBuffer: " + inputBufferIndex);
                    if (inputBufferIndex >= 0) {
                        int nSize = 0;
                        nSize = m_RingBuffer.size();
                        vlog("internalStartEncode(), m_RingBuffer.size(): " + nSize);
                        if (nSize <= 0) {
                            synchronized (m_GetBuffer) {
                                try {
                                    m_GetBuffer.wait();
                                    mStartWaitData = true;// SPRD:bug494915
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                        synchronized (m_GetBuffer) {
                            nSize = m_RingBuffer.size();
                            vlog("internalStartEncode(), m_RingBuffer.size() before encode: "
                                    + nSize);
                            if (nSize > MAX_FRAME_COUNT) {
                                for (int delSize = nSize - MAX_FRAME_COUNT; delSize > 0; delSize--) {
                                    m_RingBuffer.remove();
                                }
                            }
                        }
                        ByteBuffer srcBuffer = null;
                        try {
                            synchronized (m_GetBuffer) {
                                srcBuffer = m_RingBuffer.remove();
                            }
                        }
                        catch (NoSuchElementException e)
                        {
                            e.printStackTrace();
                            continue;
                        }
                        vlog("VT_TS Up internalStartEncode(), srcBuffer.size(): "
                                + srcBuffer.array().length);
                        inputBuffers[inputBufferIndex].position(0);
                        // log("fhy, position after: " + inputBuffers[inputBufferIndex].position());
                        inputBuffers[inputBufferIndex].put(srcBuffer.array(), 0,
                                srcBuffer.array().length);
                        inputBuffers[inputBufferIndex].position(0);
                        try {
                            mCodec.queueInputBuffer(inputBufferIndex, 0, srcBuffer.array().length,
                                    m_presentationTimeUs, 0);
                            m_presentationTimeUs += 100 * 1000;
                        } catch (MediaCodec.CryptoException e) {
                            e.printStackTrace();
                        }
                        srcBuffer = null;
                    }
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    try {
                        outputBufferIndex = mCodec.dequeueOutputBuffer(info, DEQUEQUE_TIMEOUT);
                    }
                    catch (IllegalStateException e) {
                        e.printStackTrace();
                        continue;
                    }
                    vlog("internalStartEncode(), dequeueOutputBuffer, index: " + outputBufferIndex
                            + ", offset: " + info.offset + ", size: " + info.size + ", flag: "
                            + info.flags);
                    if (outputBufferIndex >= 0) {// outputBuffer is ready to be processed or
                                                 // rendered.
                        byte[] tempBuffer = new byte[info.size];
                        outputBuffers[outputBufferIndex].get(tempBuffer);
                        outputBuffers[outputBufferIndex].position(0);
                        VTManager.getInstance().waitRequestForAT();
                        try {
                            if (out != null) {
                                out.write(tempBuffer, 0, tempBuffer.length);
                                out.flush();
                                vlog("writeBuffertomodem: " + tempBuffer.length + ", "
                                        + Integer.toHexString(tempBuffer[0] & 0xff)
                                        + " " + Integer.toHexString(tempBuffer[1] & 0xff) + " "
                                        + Integer.toHexString(tempBuffer[2] & 0xff)
                                        + " " + Integer.toHexString(tempBuffer[3] & 0xff) + " "
                                        + Integer.toHexString(tempBuffer[4] & 0xff)
                                        + " " + Integer.toHexString(tempBuffer[5] & 0xff) + " "
                                        + Integer.toHexString(tempBuffer[6] & 0xff)
                                        + " " + Integer.toHexString(tempBuffer[7] & 0xff) + " "
                                        + Integer.toHexString(tempBuffer[8] & 0xff)
                                        + " " + Integer.toHexString(tempBuffer[9] & 0xff));
                            }
                            // outFn.write(tempBuffer, 0, tempBuffer.length);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        mCodec.releaseOutputBuffer(outputBufferIndex, false);
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        log("internalStartEncode(), INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = mCodec.getOutputBuffers();
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        log("internalStartEncode(), INFO_OUTPUT_FORMAT_CHANGED");
                        // Subsequent data will conform to new format.
                        format = mCodec.getOutputFormat();
                    }
                }
                mStartWaitData = false;//SPRD:bug494915
                log("internalStartEncode() X");
                try {
                    if (out != null) {
                        out.close();
                        fo.close();
                    }
                    // outFn = new BufferedOutputStream(new FileOutputStream("/data/vpout"));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                releaseCodec();
                synchronized (m_GetBuffer) {
                    m_RingBuffer.clear();
                }
                if (mBitmapSrc != null)
                    mBitmapSrc.recycle();
                mBitmapSrc = null;
                mHandler.removeMessages(VTManager.MSG_UPLINK_ENABLE_PIC);
                synchronized (m_StopLock) {
                    m_StopLock.notify();
                }
            }
            }
            /* @} */
        };
        mWorkThread = new Thread(mainRun);
        mWorkThread.start();
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        vlog("VT_TS Up onPreviewFrame() E");
        if (m_bStarted && (data != null) && (data.length > 0) && (camera != null)) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);// data.length);
            buffer.put(data, 0, BUFFER_SIZE);// data.length);
            /*
             * log("fhy onPreviewFrame() m_iframe: " + m_iframe + ", data.length: " +
             * BUFFER_SIZE);//data.length); log("fhy onPreviewFrame() , first bytes: " + data[0] +
             * " " + data[1] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5] + " " +
             * data[6] + " " + data[7] + " " + data[8] + " " + data[9]);
             * log("fhy onPreviewFrame(), last bytes: " + data[BUFFER_SIZE - 10] + " " +
             * data[BUFFER_SIZE - 9] + " " + data[BUFFER_SIZE - 8] + " " + data[BUFFER_SIZE - 7] +
             * " " + data[BUFFER_SIZE - 6] + " " + data[BUFFER_SIZE - 5] + " " + data[BUFFER_SIZE -
             * 4] + " " + data[BUFFER_SIZE - 3] + " " + data[BUFFER_SIZE - 2] + " " +
             * data[BUFFER_SIZE - 1]);
             */
            if (DUMP_CAMERA)
                dump("/data/camera_preview" + m_iframe, data, BUFFER_SIZE);
            synchronized (m_GetBuffer) {
                m_RingBuffer.add(buffer);
                m_iframe++;
                m_GetBuffer.notify();
            }
        }
    }

    private void dump(String fn, byte[] data, int length) {
        OutputStream outFn = null;
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(fn);
            if (fo == null)
            {
                return;
            }
            outFn = new BufferedOutputStream(fo);
            if (outFn == null)
            {
                fo.close();
                return;
            }
            outFn.write(data, 0, length);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            outFn.close();
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseCodec() {
        if (mCodec != null) {
            try {
                /* SPRD: add for bug524107 @{ */
                synchronized (m_StartLock) {
                mCodec.stop();
                mCodec.release();
                mCodec = null;
                }
                /* @} */
            } catch (IllegalStateException e) {
                log("releaseCodec()->error:" + e);
            }
        }
    }

    public boolean isStared() {
        return m_bStarted;
    }
}
