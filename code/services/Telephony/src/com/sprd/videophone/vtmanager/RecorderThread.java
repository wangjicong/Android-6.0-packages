package com.sprd.videophone.vtmanager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.NoSuchElementException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

class RecorderThread extends HandlerThread
        implements AudioRecord.OnRecordPositionUpdateListener {
    private static final String LOG_TAG = "RecorderThread";
    private static final boolean VDBG = (SystemProperties.getInt("debug.videophone", 0) == 1);

    private static final int DEQUEQUE_TIMEOUT = 1000;
    private static final int RECORDER_BOTH = 0;
    private static final int RECORDER_AUDIO = 1;
    private static final int RECORDER_VIDEO = 2;

    Handler mHandler;
    private VideoSource mVideoSource = null;
    private AudioRecord mAudioSource = null;
    private static final int mSampleRate = 8000;
    int mFrameSize = 2;
    int mAudioTotalSize = AudioRecord.getMinBufferSize(mSampleRate,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT) * 2;
    private Queue<ByteBuffer> m_RingBuffer = new ConcurrentLinkedQueue<ByteBuffer>();
    private Object m_GetBuffer = new Object();
    private Object m_StopEncLock = new Object();
    private Object m_StopCapLock = new Object();
    private Object m_StopVideocLock = new Object();
    private Object m_ThreadLock = new Object();
    private Thread mWorkThread = null;
    private Thread mCaptureThread = null;
    private Thread mVideoThread = null;

    private long mPrevSampleTimeUs = 0;

    private int mVideoType = VTManager.VIDEO_TYPE_H263;

    private volatile boolean m_bVideoStarted = false;
    private volatile boolean m_bEncAudio = false;
    private volatile boolean m_bCaptureAudio = false;

    // native fields
    private Object mRecorder;

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void vlog(String msg) {
        if (VDBG)
            Log.d(LOG_TAG, msg);
    }

    RecorderThread(String name, int videoType) {
        super(name);
        mVideoType = videoType;
        log("mRecorder: " + mRecorder + ", videoType: " + videoType);
    }

    void stopWork() {
        log("stopWork() E");
        if (m_bVideoStarted) {
            synchronized (m_StopVideocLock) {
                m_bVideoStarted = false;
                mVideoSource.stop();
                try {
                    m_StopVideocLock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (m_bEncAudio) {
            synchronized (m_StopEncLock) {
                m_bEncAudio = false;
                synchronized (m_GetBuffer) {
                    m_GetBuffer.notify();
                }
                try {
                    m_StopEncLock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (m_bCaptureAudio)
        {
            synchronized (m_StopCapLock) {
                m_bCaptureAudio = false;
                try {
                    m_StopCapLock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        log("stopWork() X");
    }

    protected void onLooperPrepared() {
        mHandler = new Handler(RecorderThread.this.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VTManager.MSG_RECORDER_ENABLE:
                        int type = msg.arg1;
                        String fn = (String) msg.obj;
                        if (m_bEncAudio || m_bVideoStarted)
                        {
                            log("already recording so return");
                            return;
                        }
                        VTManager.getInstance().native_enableRecord(true, type, fn, mVideoType);
                        if ((type == RECORDER_BOTH) || (type == RECORDER_AUDIO)) {
                            m_bEncAudio = true;
                            m_bCaptureAudio = true;
                            mAudioSource = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                    8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    mAudioTotalSize);
                            mAudioSource.setRecordPositionUpdateListener(RecorderThread.this);
                            mAudioSource.startRecording();
                            startVoiceCapture();
                            startVoiceEncode();
                        }
                        if ((type == RECORDER_BOTH) || (type == RECORDER_VIDEO)) {
                            m_bVideoStarted = true;
                            mVideoSource = new VideoSource(null, mVideoType);
                            mVideoSource.start();
                            // request i-frame for recording
                            VTManager.getInstance().controlIFrame(false, true);
                            Runnable videoRun = new Runnable() {
                                public void run() {
                                    android.os.Process
                                            .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                                    log("VideoEncode() E");
                                    ByteBuffer videoInputBuffer = ByteBuffer
                                            .allocate(VTManager.MAX_FRAME_SIZE);
                                    for (;;) {
                                        videoInputBuffer.position(0);
                                        int nSize = mVideoSource.read(videoInputBuffer);
                                        vlog("read video size: " + nSize);
                                        if (nSize > 0)
                                            VTManager.getInstance().native_writeVideo(
                                                    videoInputBuffer.array(), nSize);
                                        if (!m_bVideoStarted)
                                        {
                                            synchronized (m_StopVideocLock) {
                                                m_StopVideocLock.notify();
                                            }
                                            break;
                                        }
                                    }
                                }
                            };
                            mVideoThread = new Thread(videoRun);
                            mVideoThread.start();
                        }
                        break;
                    case VTManager.MSG_RECORDER_DISABLE:
                        if (m_bEncAudio || m_bVideoStarted)
                        {
                            log("not recording so return");
                            type = msg.arg1;
                            stopWork();
                            VTManager.getInstance().native_enableRecord(false, type, null,
                                    mVideoType);
                        }
                        synchronized (m_ThreadLock) {
                            m_ThreadLock.notify();
                        }
                        break;
                }
            }
        };
        synchronized (m_ThreadLock) {
            m_ThreadLock.notify();
        }
    }

    void enableRecord(boolean bEnable, int type, String fn) {
        log("enableRecord() , bEnable: " + bEnable + ", type:" + type + ", fn: " + fn);
        synchronized (m_ThreadLock) {
            try {
                if (mHandler == null && bEnable)
                {
                    start();
                    m_ThreadLock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (bEnable) {
                mHandler.removeMessages(VTManager.MSG_RECORDER_ENABLE);
                mHandler.sendMessage(mHandler.obtainMessage(VTManager.MSG_RECORDER_ENABLE, type, 0,
                        fn));
            } else {
                if (mHandler != null)
                {
                    mHandler.removeMessages(VTManager.MSG_RECORDER_DISABLE);
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(VTManager.MSG_RECORDER_DISABLE, type, 0, null),
                            500);
                }
            }
        }
        log("enableRecord() X");
    }

    void ExitRecord(int type)
    {
        log("ExitRecord E mHandler " + mHandler);
        synchronized (m_ThreadLock) {
            if (mHandler != null)
            {
                mHandler.removeMessages(VTManager.MSG_RECORDER_DISABLE);
                mHandler.sendMessage(mHandler.obtainMessage(VTManager.MSG_RECORDER_DISABLE, type,
                        0, null));
                try {
                    m_ThreadLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        log("ExitRecord X ");
    }

    /**
     * Called on the listener to notify it that the previously set marker has been reached by the
     * recording head.
     */
    public void onMarkerReached(AudioRecord recorder) {
        log("onMarkerReached() E");
    }

    /**
     * Called on the listener to periodically notify it that the record head has reached a multiple
     * of the notification period.
     */
    public void onPeriodicNotification(AudioRecord recorder) {
        log("onPeriodicNotification() E");
    }

    private void startVoiceCapture() {

        Runnable mainRun = new Runnable() {
            public void run() {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                for (;;) {
                    if (!m_bCaptureAudio) {
                        synchronized (m_StopCapLock) {
                            m_StopCapLock.notify();
                        }
                        break;
                    }
                    ByteBuffer srcBuffer = ByteBuffer.allocateDirect(mAudioTotalSize);
                    srcBuffer.position(0);
                    mAudioSource.read(srcBuffer, mAudioTotalSize);
                    synchronized (m_GetBuffer) {
                        m_RingBuffer.add(srcBuffer);
                        m_GetBuffer.notify();
                    }
                }
                mAudioSource.setRecordPositionUpdateListener(null);
                mAudioSource.stop();
            }
        };
        mCaptureThread = new Thread(mainRun);
        mCaptureThread.start();
    }

    private void startVoiceEncode() {

        Runnable mainRun = new Runnable() {
            public void run() {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                log("startVoiceEncode() E");
                MediaCodec codec;
                mPrevSampleTimeUs = 0;
                String typeStr = "audio/3gpp";
                try {
                    codec = MediaCodec.createEncoderByType(typeStr);
                } catch (IOException e) {
                    log("createDecoderByType error:" + e);
                    return;
                }
                MediaFormat format = new MediaFormat();
                format.setString(MediaFormat.KEY_MIME, typeStr);
                format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 12200);
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8 * 1024);
                log("startVoiceEncode() configure");
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                log("startVoiceEncode() start");
                codec.start();
                log("startVoiceEncode() getInputBuffers");
                ByteBuffer[] inputBuffers = codec.getInputBuffers();
                ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                for (;;) {
                    if (!m_bEncAudio) {
                        break;
                    }
                    int inputBufferIndex = codec.dequeueInputBuffer(DEQUEQUE_TIMEOUT);
                    vlog("startVoiceEncode(), dequeueInputBuffer: " + inputBufferIndex);
                    if (inputBufferIndex >= 0) {
                        synchronized (m_GetBuffer) {
                            int nSize = m_RingBuffer.size();
                            vlog("startVoiceEncode(), m_RingBuffer.size(): " + nSize);
                            if (nSize <= 0) {
                                try {
                                    m_GetBuffer.wait();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
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
                        int bufferSize = srcBuffer.remaining();
                        vlog("startVoiceEncode(), srcBuffer.size(): " + bufferSize
                                + ", inputBuffers.size: "
                                + inputBuffers[inputBufferIndex].remaining());
                        inputBuffers[inputBufferIndex].position(0);
                        // log("fhy, position after: " + inputBuffers[inputBufferIndex].position());
                        inputBuffers[inputBufferIndex].put(srcBuffer);
                        inputBuffers[inputBufferIndex].position(0);
                        long timestampUs = mPrevSampleTimeUs
                                + ((1000000 * (long) bufferSize / mFrameSize) + (mSampleRate >> 1))
                                / mSampleRate;
                        try {
                            vlog("startVoiceEncode(), queueInputBuffer, info.mPrevSampleTimeUs: "
                                    + mPrevSampleTimeUs + ",bufferSize:" + bufferSize);
                            codec.queueInputBuffer(inputBufferIndex, 0, bufferSize,
                                    mPrevSampleTimeUs, 0);
                            mPrevSampleTimeUs = timestampUs;
                        } catch (MediaCodec.CryptoException e) {
                            e.printStackTrace();
                        }
                    }
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    int outputBufferIndex = codec.dequeueOutputBuffer(info, DEQUEQUE_TIMEOUT);
                    vlog("startVoiceEncode(), dequeueOutputBuffer, index: " + outputBufferIndex
                            + ", offset: " + info.offset + ", size: " + info.size + ", flag: "
                            + info.flags);
                    if (outputBufferIndex >= 0) {// outputBuffer is ready to be processed or
                                                 // rendered.
                        outputBuffers[outputBufferIndex].position(0);
                        vlog("startVoiceEncode(), dequeueOutputBuffer, info.presentationTimeUs: "
                                + info.presentationTimeUs);
                        VTManager.getInstance().native_writeAudio(outputBuffers[outputBufferIndex],
                                info.size, info.presentationTimeUs);
                        codec.releaseOutputBuffer(outputBufferIndex, false);
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        log("startVoiceEncode(), INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = codec.getOutputBuffers();
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        log("startVoiceEncode(), INFO_OUTPUT_FORMAT_CHANGED");
                        // Subsequent data will conform to new format.
                        format = codec.getOutputFormat();
                    }
                }
                log("startVoiceEncode() X");
                codec.stop();
                codec.release();
                codec = null;
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                synchronized (m_StopEncLock) {
                    m_StopEncLock.notify();
                }
            }
        };

        mWorkThread = new Thread(mainRun);
        mWorkThread.start();
    }
}