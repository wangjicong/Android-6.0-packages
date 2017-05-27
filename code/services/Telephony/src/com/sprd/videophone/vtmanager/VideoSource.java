package com.sprd.videophone.vtmanager;

import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioCapbility;
import android.util.Log;

import com.sprd.videophone.joou.UByte;
import com.sprd.videophone.joou.UInteger;

import java.nio.ByteBuffer;

class VideoSource {
    private static final String LOG_TAG = "VideoSource";
    private static final boolean VDBG = (SystemProperties.getInt("debug.videophone", 0) == 1);

    private int m_id = 0;
    private String m_DataSource = null;
    private int mVideoType = VTManager.VIDEO_TYPE_H263;

    private volatile boolean m_bStarted = false;
    private volatile boolean m_bForeStop = false;
    private boolean m_bDataAvailable = false;
    private Object m_GetBuffer = new Object();

    private int m_nNum = 0;
    private int m_nDataStart = 0;
    private int m_nDataEnd = 0;
    private byte[] m_RingBuffer = new byte[VTManager.MAX_BUFFER_SIZE];
    private byte[] m_readBuffer = new byte[VTManager.MAX_FRAME_SIZE];

    private boolean m_bHasMpeg4Header = false;

    private boolean m_isReadHeader = false; // if write head ,if true , send head alone
    private int m_nRingBufferSize = VTManager.MAX_BUFFER_SIZE;

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void vlog(String msg) {
        if (VDBG)
            Log.d(LOG_TAG, msg);
    }

    VideoSource(String dataSource) {
        init(dataSource, VTManager.VIDEO_TYPE_H263);
    }

    VideoSource(String dataSource, int videoType) {
        init(dataSource, videoType);
    }

    private void init(String dataSource, int videoType) {
        log("[" + m_id + "]init(), " + dataSource + ", type: " + videoType);
        if (dataSource != null) {
            m_DataSource = dataSource;
            m_id = InputThread.DISPLAY_CLIENT;
        } else {
            m_id = InputThread.RECORD_CLIENT;
            if (m_DataSource == null)
            {
                int modemtype = SystemProperties.getInt("ril.radio.modemtype", -1);
                if (VTManager.MODEM_TYPE_WCDMA == modemtype) {
                    m_DataSource = new String(SystemProperties.get("ro.modem.w.tty"));
                }
                else if (VTManager.MODEM_TYPE_TDSCDMA == modemtype)
                {
                    m_DataSource = new String(SystemProperties.get("ro.modem.t.tty"));
                }
                else if (VTManager.MODEM_TYPE_LTE == modemtype)
                {
                    RadioCapbility radioCap = TelephonyManager.getRadioCapbility();
                    if (radioCap == RadioCapbility.TDD_SVLTE) {
                        m_DataSource = new String(SystemProperties.get("ro.modem.t.tty"));
                    } else if (radioCap == RadioCapbility.TDD_CSFB) {
                        m_DataSource = new String(SystemProperties.get("ro.modem.tl.tty"));
                    } else if (radioCap == RadioCapbility.FDD_CSFB) {
                        m_DataSource = new String(SystemProperties.get("ro.modem.lf.tty"));
                    } else {
                        m_DataSource = new String(SystemProperties.get("ro.modem.l.tty"));
                    }
                }
                else
                {
                    m_DataSource = "/dev/stty_td";
                }
                m_DataSource = m_DataSource + 12;
            }
        }
        mVideoType = videoType;

    }

    void start() {
        log("[" + m_id + "]start()");
        m_bStarted = true;
        m_bForeStop = false;
        InputThread.getInstance().registerClient(this, m_DataSource, mVideoType);
    }

    int getID()
    {
        log("[" + m_id + "]getID, id: " + m_id);
        return m_id;
    }

    void stop()
    {
        log("[" + m_id + "]stop, m_bForeStop: " + m_bForeStop + "m_bStarted:" + m_bStarted);

        if ((!m_bStarted) || m_bForeStop)
            return;

        m_bForeStop = true;
        synchronized (m_GetBuffer) {
            m_GetBuffer.notify();
        }
        InputThread.getInstance().unregisterClient(this);
    }

    int writeBuffer(byte[] data, int nLen) {
        if (m_RingBuffer == null || nLen <= 0)
            return 0;

        log("VT_TS Down writeBuffer(), nLen:" + nLen);
        // log("writeBuffer(): " + nLen + ", " + Integer.toHexString(data[0]&0xff)
        // + " " + Integer.toHexString(data[1]&0xff) + " " + Integer.toHexString(data[2]&0xff)
        // + " " + Integer.toHexString(data[3]&0xff) + " " + Integer.toHexString(data[4]&0xff)
        // + " " + Integer.toHexString(data[5]&0xff) + " " + Integer.toHexString(data[6]&0xff)
        // + " " + Integer.toHexString(data[7]&0xff) + " " + Integer.toHexString(data[8]&0xff)
        // + " " + Integer.toHexString(data[9]&0xff));

        boolean bChangeStart = false;

        synchronized (m_GetBuffer) {
            if ((m_nDataEnd < m_nDataStart && m_nDataEnd + nLen > m_nDataStart)
                    || (m_nDataEnd > m_nDataStart && m_nDataEnd + nLen > m_nDataStart
                            + m_nRingBufferSize))
            {
                log("buffer is overrun!!!");
                bChangeStart = true;
            }

            int nTemp = nLen;

            if (nLen > m_nRingBufferSize - m_nDataEnd)
                nTemp = m_nRingBufferSize - m_nDataEnd;

            System.arraycopy(data, 0, m_RingBuffer, m_nDataEnd, nTemp);
            // memcpy(m_RingBuffer+ m_nDataEnd,data,nTemp);
            // data += nTemp;
            m_nDataEnd += nTemp;

            int nSrcPos = nTemp;
            if ((nTemp = nLen - nTemp) > 0)
            {
                // memcpy(m_RingBuffer,data ,nTemp);
                System.arraycopy(data, nSrcPos, m_RingBuffer, 0, nTemp);
                m_nDataEnd = nTemp;
            }

            if (bChangeStart)
                m_nDataStart = m_nDataEnd;

            log("writeBuffer(), signal");
            m_bDataAvailable = true;
            /*
             * if (m_nDataEnd > m_nDataStart ) { log("VT_TS Down free size 1: " + (m_nRingBufferSize
             * - m_nDataEnd + m_nDataStart)); } else { log("VT_TS Down free size 2: " +
             * (m_nDataStart - m_nDataEnd)); }
             */
            m_GetBuffer.notify();
        }

        return nLen;
    }

    int read(ByteBuffer buffer) {
        m_isReadHeader = false; // add for Bug#202658
        int nSize = 0;
        log("[" + m_id + "]read START nNum: " + m_nNum);
        if (m_bForeStop)
        {
            return 0;
        }
        synchronized (m_GetBuffer) {
            nSize = readRingBuffer(m_readBuffer); // modify for Bug#202658
        }
        vlog("[" + m_id + "]read nSize: " + nSize);
        if (m_id == InputThread.DISPLAY_CLIENT) {
            do {
                video_srteam_t streambuf = new video_srteam_t();
                int ret = video_stream_init(streambuf, m_readBuffer, nSize);
                if (ret != 0)
                    break;
                ParamCls pcls = new ParamCls();
                pcls.bVop = false;
                ret = get_video_stream_info(streambuf, pcls);
                vlog("ret: " + ret + ", bVop: " + pcls.bVop);
                if ((ret == 0) && pcls.bVop) {
                    VTManager.getInstance().controlIFrame(true, false);
                    log("report i-frame");
                }
                break;
            } while (true);
        }
        if (nSize > 0) {
            try {
                vlog("buffer.put: " + nSize + ", " + Integer.toHexString(m_readBuffer[0] & 0xff)
                        + " " + Integer.toHexString(m_readBuffer[1] & 0xff) + " "
                        + Integer.toHexString(m_readBuffer[2] & 0xff)
                        + " " + Integer.toHexString(m_readBuffer[3] & 0xff) + " "
                        + Integer.toHexString(m_readBuffer[4] & 0xff)
                        + " " + Integer.toHexString(m_readBuffer[5] & 0xff) + " "
                        + Integer.toHexString(m_readBuffer[6] & 0xff)
                        + " " + Integer.toHexString(m_readBuffer[7] & 0xff) + " "
                        + Integer.toHexString(m_readBuffer[8] & 0xff)
                        + " " + Integer.toHexString(m_readBuffer[9] & 0xff));
                buffer.put(m_readBuffer, 0, nSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
            m_nNum++;
        }
        return nSize;
    }

    private int readRingBuffer(byte[] data) {
        vlog("[" + m_id + "]readRingBuffer E");
        int nSize = data.length;
        int offset = 0;
        if (m_RingBuffer == null)
            return 0;
        int nNext = m_nDataStart;
        int nLen = 0, nExtraLen = 0;
        boolean bStartRead = false;
        boolean bIsMpege4 = false;
        boolean bMpeg4Header = false;
        int nStart = m_nDataStart, nEnd = m_nDataStart;
        while (m_bStarted)
        {
            nEnd = nNext;
            nNext = (nNext + 1) % m_nRingBufferSize;
            while (true) {
                if ((nNext == m_nDataEnd) || (m_nDataStart == m_nDataEnd))
                {
                    try {
                        m_GetBuffer.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if ((!m_bStarted) || m_bForeStop)
                    return 0;
                if (!m_bDataAvailable) {
                    log("[" + m_id + "]readRingBuffer goto wait_again");
                    m_bDataAvailable = false;
                    continue;
                }
                break;
            }
            if (m_RingBuffer[nEnd] == 0x00 &&
                    m_RingBuffer[(nEnd + 1) % m_nRingBufferSize] == 0x00)
            {
                if ((m_RingBuffer[(nEnd + 2) % m_nRingBufferSize] & 0xFF) == 0x01 &&
                        (m_RingBuffer[(nEnd + 3) % m_nRingBufferSize] & 0xFF) == 0xb6)
                {
                    log("mpeg4 bStartRead: " + bStartRead);
                    if (!bStartRead)
                    {
                        log("[" + m_id + "]readRingBuffer START MEPGE4");
                        if (bMpeg4Header) {
                            bMpeg4Header = false;
                            VTManager.getInstance().m_iMpeg4Header_size = ((nEnd - nStart) + m_nRingBufferSize)
                                    % m_nRingBufferSize;
                            System.arraycopy(m_RingBuffer, nStart,
                                    VTManager.getInstance().m_Mpeg4Header, 0,
                                    VTManager.getInstance().m_iMpeg4Header_size);
                            m_isReadHeader = true; // first data is head, send head alone and set
                                                   // flag
                            bStartRead = true;
                            log("FrameHeader intermedially follow mpeg4 header, Header_size: "
                                    + VTManager.getInstance().m_iMpeg4Header_size);
                            log("write mpeg4 header,  "
                                    + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[0])
                                    + ", "
                                    + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[1])
                                    + ", "
                                    + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[2])
                                    + ", "
                                    + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[3])
                                    + ", "
                                    + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[4]));
                            break;
                        }
                        else {
                            nStart = nEnd;
                            bStartRead = true;
                            if (m_nNum == 0)
                            {
                                if (VTManager.getInstance().m_iMpeg4Header_size != 0)
                                {
                                    m_bHasMpeg4Header = true;
                                    break;
                                }
                                else
                                {
                                    bStartRead = false;
                                }
                            }
                        }
                    } else {
                        log("bStartRead is true , break");
                        break;
                    }
                } else if ((m_RingBuffer[(nEnd + 2) % m_nRingBufferSize] & 0xFC) == 0x80 &&
                        (m_RingBuffer[(nEnd + 3) % m_nRingBufferSize] & 0x03) == 0x02)
                {
                    if (!bStartRead)
                    {
                        vlog("[" + m_id + "]readRingBuffer START VOP");
                        nStart = nEnd;
                        bStartRead = true;
                    } else
                        break;
                } else if ((m_RingBuffer[(nEnd + 2) % m_nRingBufferSize] & 0xFF) == 0x01 &&
                        (m_RingBuffer[(nEnd + 3) % m_nRingBufferSize] & 0xFF) == 0xb0)
                {
                    log("readRingBuffer START MEPGE4 Header bStartRead: " + bStartRead);
                    if (!bStartRead)
                    {
                        log("[" + m_id + "]readRingBuffer START MEPGE4 Header");
                        nStart = nEnd;
                        bMpeg4Header = true;
                    } else {
                        log("readRingBuffer START MEPGE4 Header bStartRead is true , break");
                        break;
                    }
                }
            }
        }
        nLen = ((nEnd - nStart) + m_nRingBufferSize) % m_nRingBufferSize;
        log("VT_TS Down find frame size: " + nLen);
        if (nLen > nSize)
        {
            log("nLen " + nLen + " exceeds nSize " + nSize);
            return 0;
        }
        byte[] pOrginData = data;
        log("m_nNum " + m_nNum + ", m_bHasMpeg4Header " + m_bHasMpeg4Header);
        if ((m_nNum == 0) && (m_bHasMpeg4Header)) {
            if (mVideoType == VTManager.VIDEO_TYPE_MPEG4) {
                System.arraycopy(VTManager.getInstance().m_Mpeg4Header, 0, data, 0,
                        VTManager.getInstance().m_iMpeg4Header_size);
                m_isReadHeader = true;
                nExtraLen = VTManager.getInstance().m_iMpeg4Header_size;
                log("add mpeg4 header,  "
                        + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[0])
                        + ", " + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[1])
                        + ", " + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[2])
                        + ", " + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[3])
                        + ", " + Integer.toHexString(VTManager.getInstance().m_Mpeg4Header[4]));
            }
        }
        int nTemp = nLen;
        if (nTemp > m_nRingBufferSize - nStart)
            nTemp = m_nRingBufferSize - nStart;
        System.arraycopy(m_RingBuffer, nStart, data, nExtraLen, nTemp);
        offset += nTemp;
        if ((nTemp = nLen - nTemp) > 0) {
            System.arraycopy(m_RingBuffer, 0, data, offset + nExtraLen, nTemp);
        }
        m_nDataStart = nEnd;
        return (nLen + nExtraLen);
    }

    public boolean is_read_header() {
        return m_isReadHeader;
    }

    // functions for i-vop check
    private class video_srteam_t {
        byte[] data;
        int start;
        int curent;
        int current_byte;
        int current_bit;
        int length;
    };

    private class ParamCls {
        boolean bVop;
    };

    private static int video_stream_init(video_srteam_t pStream, byte[] data, int length) {
        if (length <= 0)
            return 1;
        pStream.data = data;
        pStream.start = 0;
        pStream.curent = 0;
        pStream.current_bit = 0;
        pStream.length = length;
        return 0;
    }

    private static UInteger show_video_bits(video_srteam_t pStream, int num)// num<=32
    {
        UInteger first32bits = UInteger.valueOf(0);
        UByte firstByte = UByte.valueOf(0);
        UByte secondByte = UByte.valueOf(0);
        UByte thirdByte = UByte.valueOf(0);
        UByte fourthByte = UByte.valueOf(0);
        UByte fifthByte = UByte.valueOf(0);
        if ((pStream.curent) < pStream.length)
            firstByte = UByte.valueOf(pStream.data[pStream.curent]);
        if ((pStream.curent + 1) < pStream.length)
            secondByte = UByte.valueOf(pStream.data[pStream.curent + 1]);
        if ((pStream.curent + 2) < pStream.length)
            thirdByte = UByte.valueOf(pStream.data[pStream.curent + 2]);
        if ((pStream.curent + 3) < pStream.length)
            fourthByte = UByte.valueOf(pStream.data[pStream.curent + 3]);
        if ((pStream.curent + 4) < pStream.length)
            fifthByte = UByte.valueOf(pStream.data[pStream.curent + 4]);
        // log("show_video_bits(), after " + firstByte+ ", " + secondByte + ", " + thirdByte + ", "
        // + fourthByte + ", " + fifthByte);
        first32bits = UInteger.valueOf((firstByte.intValue() << 24) | (secondByte.intValue() << 16)
                | (thirdByte.intValue() << 8) | (fourthByte.intValue()));
        if (pStream.current_bit != 0)
            first32bits = UInteger.valueOf((first32bits.intValue() << pStream.current_bit)
                    | (fifthByte.intValue() >> (8 - pStream.current_bit)));
        return UInteger.valueOf(first32bits.intValue() >>> (32 - num));
    }

    private static void flush_video_bits(video_srteam_t pStream, int num) {// num<=32
        pStream.curent += (pStream.current_bit + num) / 8;
        pStream.current_bit = (pStream.current_bit + num) % 8;
    }

    private static UInteger read_video_bits(video_srteam_t pStream, int num) {// num<=32
        UInteger tmp = show_video_bits(pStream, num);
        flush_video_bits(pStream, num);
        return tmp;
    }

    private static int decode_h263_header(video_srteam_t pStream, ParamCls pcls) {
        UInteger tmpVar = show_video_bits(pStream, 22);
        if (0x20 != tmpVar.intValue())
            return 1;
        flush_video_bits(pStream, 22);
        tmpVar = read_video_bits(pStream, 9);
        if ((tmpVar.intValue() & 0x1) == 0)
            return 1;
        tmpVar = read_video_bits(pStream, 7);
        if ((tmpVar.intValue() >>> 3) != 0)
            return 1;
        tmpVar = UInteger.valueOf(tmpVar.intValue() & 0x07);
        if (tmpVar.intValue() == 7) // do not support EXTENDED_PTYPE
            return 1;
        tmpVar = read_video_bits(pStream, 11);
        if ((tmpVar.intValue() >>> 10) == 0) {
            pcls.bVop = true;
        } else {
            pcls.bVop = false;
        }
        return 0;
    }

    private static int decode_mpeg4_header(video_srteam_t pStream, ParamCls pcls) {
        UInteger tmpVar = UInteger.valueOf(0);
        UInteger uStartCode = UInteger.valueOf(0);
        int loopNum = 0;
        int vopType;
        while (uStartCode.intValue() != 0x1B6) {
            uStartCode = show_video_bits(pStream, 32);
            if (0x1B6 == uStartCode.intValue()) {
                tmpVar = read_video_bits(pStream, 32);
                tmpVar = read_video_bits(pStream, 3);
                vopType = tmpVar.intValue() >>> 1;
                if (vopType == 0) {
                    pcls.bVop = true;
                } else {
                    pcls.bVop = false;
                }
            } else {
                read_video_bits(pStream, 8);
            }
            loopNum++;
            if (loopNum > 2048)
                return 1;
        }
        return 0;
    }

    private static int get_video_stream_info(video_srteam_t pStream, ParamCls pcls) {
        boolean is_h263 = true;
        int iret = show_video_bits(pStream, 21).intValue();
        is_h263 = (iret == 0x10);
        if (is_h263) {
            return decode_h263_header(pStream, pcls);
        } else {
            return decode_mpeg4_header(pStream, pcls);
        }
    }

}
