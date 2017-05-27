
package com.sprd.engineermode.connectivity.wifi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.text.TextUtils;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import android.widget.Toast;

public class WifiEUTHelper {

    private static final String TAG = "WifiEUTHelper";
    private static final String SOCKET_NAME = "wcnd_eng";
    private static final String CMD_SUCCESS = "OK";
    private static final String CMD_FAIL = "Fail";

    // iwnpi wlan0 ifaceup is sended by iwnpi, so equal to ifconfig wlan0 up,
    // iwnpi wlan0 ifacedown is sended by iwnpi, so equal to ifconfig wlan0 down
    private static final String SET_EUT_UP = "iwnpi wlan0 ifaceup";
    private static final String SET_EUT_DOWN = "iwnpi wlan0 ifacedown";
    private static final String SET_EUT_START = "iwnpi wlan0 start";
    private static final String SET_EUT_STOP = "iwnpi wlan0 stop";
    private static final String SET_EUT_SET_CHANNEL = "iwnpi wlan0 set_channel ";
    private static final String SET_EUT_SET_RATE = "iwnpi wlan0 set_rate ";

    // for tx
    private static final String SET_EUT_TX_START = "iwnpi wlan0 tx_start";
    private static final String SET_EUT_TX_STOP = "iwnpi wlan0 tx_stop";
    private static final String SET_EUT_CW_START = "iwnpi wlan0 sin_wave";
    private static final String SET_EUT_SET_POWER = "iwnpi wlan0 set_tx_power ";
    private static final String SET_EUT_SET_LENGTH = "iwnpi wlan0 set_pkt_length ";
    private static final String SET_EUT_SET_COUNT = "iwnpi wlan0 set_tx_count ";
    private static final String SET_EUT_SET_PREAMBLE = "iwnpi wlan0 set_preamble ";
    private static final String SET_EUT_BANDWIDTH = "iwnpi wlan0 set_bandwidth ";
    private static final String SET_EUT_GUARDINTERVAL = "iwnpi wlan0 set_guard_interval ";

    // for rx
    private static final String SET_EUT_RX_START = "iwnpi wlan0 rx_start";
    private static final String SET_EUT_RX_STOP = "iwnpi wlan0 rx_stop";
    private static final String SET_EUT_GET_RXOK = "iwnpi wlan0 get_rx_ok";

    // for reg_wr
    private static final String SET_EUT_READ = "iwnpi wlan0 get_reg ";
    private static final String SET_EUT_WRITE = "iwnpi wlan0 set_reg ";

    private static final String CMD_ENABLED_POWER_SAVE = "iwnpi wlan0 lna_on";
    private static final String CMD_DISABLED_POWER_SAVE = "iwnpi wlan0 lna_off";
    private static final String CMD_GET_POWER_SAVE_STATUS = "iwnpi wlan0 lna_status";

    private static final int WIFI_UP = 0;
    private static final int WIFI_START = 1;
    private static final int WIFI_STOP = 2;
    private static final int WIFI_DOWN = 3;
    private static final int WIFI_SET_CHANNEL = 4;
    private static final int WIFI_SET_RATE = 5;
    private static final int WIFI_TX_START = 6;
    private static final int WIFI_TX_STOP = 7;
    private static final int WIFI_TX_POWER = 8;
    private static final int WIFI_TX_COUNT = 9;
    private static final int WIFI_TX_LENGTH = 10;
    private static final int WIFI_TX_PREAMBLE = 11;
    private static final int WIFI_TX_SINWAVE = 12;
    private static final int WIFI_TX_BANDWIDTH = 13;
    private static final int WIFI_TX_GUARDINTERVAL = 14;
    private static final int WIFI_RX_START = 15;
    private static final int WIFI_RX_STOP = 16;
    private static final int WIFI_RX_OK = 17;
    private static final int WIFI_REG_READ = 18;
    private static final int WIFI_REG_WRITE = 19;
    private static final int WIFI_GET_STATUS = 20;
    private static final int WIFI_SET_STATUS_ON = 21;
    private static final int WIFI_SET_STATUS_OFF = 22;

    private static LocalSocket mSocketClient;
    private static LocalSocketAddress mSocketAddress;
    private static OutputStream mOutputStream;
    private static InputStream mInputStream;

    private static LocalSocket mInsmodeSocketClient;
    private static LocalSocketAddress mInsmodeSocketAddress;
    private static OutputStream mInsmodeOutputStream;
    private static InputStream mInsmodeInputStream;

    static class WifiTX {
        public String channel;
        public String pktlength;
        public String pktcnt;
        public String powerlevel;
        public String rate;
        public String mode;
        public String preamble;
        public String bandwidth;
        public String guardinterval;
    }

    static class WifiRX {
        public String channel;
        public String rxtestnum;
    }

    static class WifiREG {
        public String type;
        public String addr;
        public String length;
        public String value;
    }

    /**
     * creat wcnd socket and send iwnpi wlan0 ifaceup because this cmd is the
     * first cmd, so creat socket here but the socket is not closed here, and
     * close it when wifiDown(), cause the "iwnpi wlan0 ifacedown" cmd is the
     * last cmd sended by the wcnd socket
     *
     * @return true if success
     */
    public boolean wifiUp() {
        byte[] buf = new byte[255];
        String result = null;
        try {
            mSocketClient = new LocalSocket();
            mSocketAddress = new LocalSocketAddress(SOCKET_NAME,
                    LocalSocketAddress.Namespace.ABSTRACT);
            mSocketClient.connect(mSocketAddress);
            Log.d(TAG, "mSocketClient connect is " + mSocketClient.isConnected());
            mOutputStream = mSocketClient.getOutputStream();
            if (mOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder("eng " + SET_EUT_UP)
                        .append('\0');
                final String cmd = cmdBuilder.toString();
                mOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                mOutputStream.flush();
            }
            mInputStream = mSocketClient.getInputStream();
            int count = mInputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(TAG, "wifiUp count = " + count + ", wifiUp result is " + result);
        } catch (IOException e) {
            Log.e(TAG, "wifiUp Failed get output stream: " + e);
        }
        return analysisResult(WIFI_UP, result);
    }

    /**
     * send iwnpi wlan0 start
     *
     * @return the cmd result, true if success
     */
    public static boolean wifiStart() {
        byte[] buf = new byte[255];
        String result = null;
        try {
            mSocketClient = new LocalSocket();
            mSocketAddress = new LocalSocketAddress(SOCKET_NAME,
                    LocalSocketAddress.Namespace.ABSTRACT);
            mSocketClient.connect(mSocketAddress);
            Log.d(TAG, "mSocketClient connect is " + mSocketClient.isConnected());
            mOutputStream = mSocketClient.getOutputStream();
            if (mOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder("eng " + SET_EUT_START)
                        .append('\0');
                final String cmd = cmdBuilder.toString();
                mOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                mOutputStream.flush();
            }
            mInputStream = mSocketClient.getInputStream();
            int count = mInputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(TAG, "wifiStart count = " + count + ", wifiStart result is " + result);
        } catch (IOException e) {
            Log.e(TAG, "wifiStart Failed get output stream: " + e);
        }
        return analysisResult(WIFI_START, result);
    }

    /**
     * send cmd(iwnpi wlan0 ifacedown) and close wcnd socket
     *
     * @return true if success
     */
    public boolean wifiDown() {
        byte[] buf = new byte[255];
        String result = null;
        try {
            mOutputStream = mSocketClient.getOutputStream();
            if (mOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder("eng " + SET_EUT_STOP)
                        .append('\0');
                final String cmd = cmdBuilder.toString();
                mOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                mOutputStream.flush();
            }
            mInputStream = mSocketClient.getInputStream();
            int count = mInputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(TAG, "wifiStop count = " + count + ", wifiStop result is " + result);
        } catch (IOException e) {
            Log.e(TAG, "wifiStop Failed get output stream: " + e);
        } finally {
            try {
                buf = null;
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
                if (mInputStream != null) {
                    mInputStream.close();
                }
                if (mSocketClient.isConnected()) {
                    mSocketClient.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "catch exception is " + e);
            }
        }
        return analysisResult(WIFI_DOWN, result);
    }

    /**
     * send iwnpi wlan0 stop
     *
     * @return true if success
     */
    public static boolean wifiStop() {
        byte[] buf = new byte[255];
        String result = null;
        try {
            mOutputStream = mSocketClient.getOutputStream();
            if (mOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder("eng " + SET_EUT_STOP)
                        .append('\0');
                final String cmd = cmdBuilder.toString();
                mOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                mOutputStream.flush();
            }
            mInputStream = mSocketClient.getInputStream();
            int count = mInputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(TAG, "wifiStop count = " + count + ", wifiStop result is " + result);
        } catch (IOException e) {
            Log.e(TAG, "wifiStop Failed get output stream: " + e);
        } finally {
            try {
                buf = null;
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
                if (mInputStream != null) {
                    mInputStream.close();
                }
                if (mSocketClient.isConnected()) {
                    mSocketClient.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "catch exception is " + e);
            }
        }
        return analysisResult(WIFI_STOP, result);
    }

    /**
     * This is for TX EX: mode 802.11 pkt iwnpi wlan0 set_channel xx iwnpi wlan0
     * set_pkt_length xx iwnpi wlan0 set_tx_count xx iwnpi wlan0 set_tx_power xx
     * iwnpi wlan0 set_rate xx iwnpi wlan0 set_preamble xx iwnpi wlan0
     * set_bandwidth xx iwnpi wlan0 set_guard_interval iwnpi wlan0 tx_start mode
     * Sin Wave iwnpi wlan0 set_channel xx iwnpi wlan0 set_pkt_length xx iwnpi
     * wlan0 set_tx_count xx iwnpi wlan0 set_tx_power xx iwnpi wlan0 set_rate xx
     * iwnpi wlan0 set_preamble xx iwnpi wlan0 set_bandwidth xx iwnpi wlan0
     * set_guard_interval iwnpi wlan0 sin_wave
     *
     * @param tx
     * @return
     */

    public static boolean wifiTXGo(WifiTX tx) {
        int temp_power = 0;
        int temp_rate = 0;
        if(TextUtils.isEmpty(tx.powerlevel)){
            tx.powerlevel = "0";
        }
        if (tx.mode.equals("802.11 pkt")) {
            temp_rate = Integer.parseInt(tx.rate, 10);
            if (temp_rate == 7 || temp_rate == 58 || temp_rate == 65) {
                temp_power = Integer.parseInt(tx.powerlevel, 10) + 1000;
            } else {
                temp_power = Integer.parseInt(tx.powerlevel, 10);
            }
            Log.d(TAG, "2351 eut iwnpi temp_power =" + temp_power);
            if (sendCmd(SET_EUT_SET_CHANNEL + tx.channel, WIFI_SET_CHANNEL)
                    && sendCmd(SET_EUT_SET_LENGTH + tx.pktlength,
                            WIFI_TX_LENGTH)
                    && sendCmd(SET_EUT_SET_COUNT + tx.pktcnt, WIFI_TX_COUNT)
                    && sendCmd(SET_EUT_SET_POWER + String.valueOf(temp_power), WIFI_TX_POWER)
                    && sendCmd(SET_EUT_SET_RATE + tx.rate, WIFI_SET_RATE)
                    && sendCmd(SET_EUT_SET_PREAMBLE + tx.preamble,
                            WIFI_TX_PREAMBLE)
                    && sendCmd(SET_EUT_BANDWIDTH + tx.bandwidth,
                            WIFI_TX_BANDWIDTH)
                    && sendCmd(SET_EUT_GUARDINTERVAL + tx.guardinterval,
                            WIFI_TX_GUARDINTERVAL)
                    && sendCmd(SET_EUT_TX_START, WIFI_TX_START)) {
                return true;
            }
        } else if (tx.mode.equals("Sin Wave")) {
            if (sendCmd(SET_EUT_SET_CHANNEL + tx.channel, WIFI_SET_CHANNEL)
                    && sendCmd(SET_EUT_SET_LENGTH + tx.pktlength,
                            WIFI_TX_LENGTH)
                    && sendCmd(SET_EUT_SET_COUNT + tx.pktcnt, WIFI_TX_COUNT)
                    && sendCmd(SET_EUT_SET_POWER + tx.powerlevel, WIFI_TX_POWER)
                    && sendCmd(SET_EUT_SET_RATE + tx.rate, WIFI_SET_RATE)
                    && sendCmd(SET_EUT_SET_PREAMBLE + tx.preamble,
                            WIFI_TX_PREAMBLE)
                    && sendCmd(SET_EUT_BANDWIDTH + tx.bandwidth,
                            WIFI_TX_BANDWIDTH)
                    && sendCmd(SET_EUT_GUARDINTERVAL + tx.guardinterval,
                            WIFI_TX_GUARDINTERVAL)
                    && sendCmd(SET_EUT_CW_START, WIFI_TX_SINWAVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is for TX EX: iwnpi wlan0 tx_stop
     *
     * @return
     */
    public static boolean wifiTXStop() {
        if (sendCmd(SET_EUT_TX_STOP, WIFI_TX_STOP)) {
            return true;
        }
        return false;
    }

    /**
     * This is for TX EX: iwnpi wlan0 set_channel xx iwnpi wlan0 tx_start
     *
     * @param tx
     * @return
     */
    public static boolean wifiTXCw(WifiTX tx) {
        if (sendCmd(SET_EUT_SET_CHANNEL + tx.channel, WIFI_SET_CHANNEL)
                && sendCmd(SET_EUT_TX_START, WIFI_TX_START)) {
            return true;
        }
        return false;
    }

    /**
     * This is for RX EX: iwnpi wlan0 set_channel xx iwnpi wlan0 rx_start
     *
     * @return
     */

    public static boolean wifiRXStart(WifiRX rx) {
        if (sendCmd(SET_EUT_SET_CHANNEL + rx.channel, WIFI_SET_CHANNEL)
                && sendCmd(SET_EUT_RX_START, WIFI_RX_START)) {
            return true;
        }
        return false;
    }

    /**
     * This is for RX EX: iwnpi wlan0 get_rx_ok
     *
     * @return
     */
    public static String wifiRXResult() {
        return sendCmdStr(SET_EUT_GET_RXOK, WIFI_RX_OK);
    }

    /**
     * This is for RX EX: iwnpi wlan0 rx_stop
     *
     * @return
     */
    public static boolean wifiRXStop() {
        if (sendCmd(SET_EUT_RX_STOP, WIFI_RX_STOP)) {
            return true;
        }
        return false;
    }

    /**
     * This is for REG_R EX: iwnpi wlan0 get_reg %s(type) %x(Addr) %x(Length)
     *
     * @param reg
     * @return
     */
    public static String wifiREGR(WifiREG reg) {
        return sendCmdStr(SET_EUT_READ + reg.type + " " + reg.addr + " " + reg.length,
                WIFI_REG_READ);
    }

    /**
     * This is for REG_W EX: iwnpi wlan0 set_reg %s(type) %x(Addr) %x(Vlaue)
     *
     * @param reg
     * @return
     */
    public static String wifiREGW(WifiREG reg) {
        return sendCmdStr(SET_EUT_WRITE + reg.type + " " + reg.addr + " " + reg.value,
                WIFI_REG_WRITE);
    }

    public static String wifiGetStatus() {
        return sendCmdStr(CMD_GET_POWER_SAVE_STATUS, WIFI_GET_STATUS);
    }

    public static String wifiSetStatusOn() {
        return sendCmdStr(CMD_ENABLED_POWER_SAVE, WIFI_SET_STATUS_ON);
    }

    public static String wifiSetStatusOff() {
        return sendCmdStr(CMD_DISABLED_POWER_SAVE, WIFI_SET_STATUS_OFF);
    }

    /**
     * analysis the socket result
     *
     * @param cmd send cmd
     * @param res the socket return result
     * @return EM analysis Result
     */
    public static boolean analysisResult(int cmd, String result) {
        Log.d(TAG, "analysisResult cmd is " + cmd + ", socket return result is " + result);
        switch (cmd) {
            case WIFI_UP:
            case WIFI_START:
            case WIFI_STOP:
            case WIFI_DOWN:
            case WIFI_SET_CHANNEL:
            case WIFI_SET_RATE:
            case WIFI_TX_START:
            case WIFI_TX_STOP:
            case WIFI_TX_POWER:
            case WIFI_TX_COUNT:
            case WIFI_TX_LENGTH:
            case WIFI_TX_PREAMBLE:
            case WIFI_TX_SINWAVE:
            case WIFI_TX_BANDWIDTH:
            case WIFI_TX_GUARDINTERVAL:
            case WIFI_RX_START:
            case WIFI_RX_STOP:
                if (result != null && result.startsWith(CMD_SUCCESS)) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    private static boolean sendCmd(String cmd, int anaycmd) {
        byte[] buf = new byte[255];
        String result = null;
        try {
            if (mSocketClient == null) {
                mSocketClient = new LocalSocket();
                mSocketAddress = new LocalSocketAddress(SOCKET_NAME,
                        LocalSocketAddress.Namespace.ABSTRACT);
                mSocketClient.connect(mSocketAddress);
                Log.d(TAG, "mSocketClient connect is " + mSocketClient.isConnected());
            }
            mOutputStream = mSocketClient.getOutputStream();
            if (mOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder("eng " + cmd).append('\0');
                final String cmdc = cmdBuilder.toString();
                mOutputStream.write(cmdc.getBytes(StandardCharsets.UTF_8));
                mOutputStream.flush();
            }
            mInputStream = mSocketClient.getInputStream();
            int count = mInputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(TAG, "doCmd " + cmd + " count" + count + ", result is " + result);
        } catch (IOException e) {
            Log.e(TAG, "doCmd " + cmd + ", Failed get output stream: " + e);
        }
        return analysisResult(anaycmd, result);
    }

    private static String sendCmdStr(String cmd, int anaycmd) {
        byte[] buf = new byte[255];
        String result = null;
        try {
            if (mSocketClient == null) {
                mSocketClient = new LocalSocket();
                mSocketAddress = new LocalSocketAddress(SOCKET_NAME,
                        LocalSocketAddress.Namespace.ABSTRACT);
                mSocketClient.connect(mSocketAddress);
                Log.d(TAG, "mSocketClient connect is " + mSocketClient.isConnected());
            }
            mOutputStream = mSocketClient.getOutputStream();
            if (mOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder("eng " + cmd).append('\0');
                final String cmdc = cmdBuilder.toString();
                mOutputStream.write(cmdc.getBytes(StandardCharsets.UTF_8));
                mOutputStream.flush();
            }
            mInputStream = mSocketClient.getInputStream();
            int count = mInputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(TAG, "doCmd " + cmd + " count" + count + ", result is " + result);
        } catch (IOException e) {
            Log.e(TAG, "doCmd " + cmd + ", Failed get output stream: " + e);
        }
        return result;
    }

    /**
     * we should load wifi driver and start wifi before testing TX/RX
     *
     * @return true if loading driver success, false if fail
     */
    public static boolean insmodeWifi() {

        // connect socket and send cmd (insmod /system/lib/modules/sprdwl.ko)
        SystemProperties.set("persist.sys.cmdservice.enable", "enable");
        String status = SystemProperties.get("persist.sys.cmdservice.enable", "");
        Log.d(TAG, "cmd_service pro is " + status);

        // sleep 100ms to make sure cmd_service start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            mInsmodeSocketClient = new LocalSocket();
            mInsmodeSocketAddress = new LocalSocketAddress("cmd_skt",
                    LocalSocketAddress.Namespace.ABSTRACT);
            mInsmodeSocketClient.connect(mInsmodeSocketAddress);
            Log.d(TAG, "mInsmodeSocketClient connect is " + mInsmodeSocketClient.isConnected());
            // insmod driver
            try {
                byte[] buffer = new byte[1024];
                mInsmodeOutputStream = mInsmodeSocketClient.getOutputStream();
                if (mInsmodeOutputStream != null) {
                    final StringBuilder cmdBuilder = new StringBuilder(
                            "insmod /system/lib/modules/sprdwl.ko").append('\0');
                    final String cmmand = cmdBuilder.toString();
                    mInsmodeOutputStream.write(cmmand.getBytes(StandardCharsets.UTF_8));
                    mInsmodeOutputStream.flush();
                }
                mInsmodeInputStream = mInsmodeSocketClient.getInputStream();
                int count = mInsmodeInputStream.read(buffer, 0, 1024);
                String insmodResult = new String(buffer, "utf-8");
                Log.d(TAG, "insmodeResult is " + insmodResult);
                String[] str = insmodResult.split("\n");
                if ("Result".equals(str[0].trim()) || insmodResult.contains("File exists")) {
                    Log.d(TAG, "insmod /system/lib/modules/sprdwl.ko is success");
                    return true;
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed get outputStream: " + e);
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "mInsmodeSocketClient connect is false");
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * we shoule reload the wifi driver when finish
     * WifiTXActivity/WifiRXActivity
     *
     * @return true if reload success, false if fail
     */
    public static boolean remodeWifi() {
        if (!mInsmodeSocketClient.isConnected()) {
            try {
                mInsmodeSocketClient.connect(mInsmodeSocketAddress);
            } catch (Exception e) {
                Log.d(TAG, "remodeWifi mInsmodeSocketClient is not connected");
            }
        }
        try {
            byte[] buffer = new byte[1024];
            mInsmodeOutputStream = mInsmodeSocketClient.getOutputStream();
            if (mInsmodeOutputStream != null) {
                final StringBuilder cmdBuilder = new StringBuilder(
                        "rmmod /system/lib/modules/sprdwl.ko").append('\0');
                final String cmmand = cmdBuilder.toString();
                mInsmodeOutputStream.write(cmmand.getBytes(StandardCharsets.UTF_8));
                mInsmodeOutputStream.flush();
            }
            mInsmodeInputStream = mInsmodeSocketClient.getInputStream();
            int count = mInsmodeInputStream.read(buffer, 0, 1024);
            String rmmodResult = new String(buffer, "utf-8");
            Log.d(TAG, "count is " + count + ",rmmodResult is " + rmmodResult);
            String[] str = rmmodResult.split("\n");
            if (!"Result".equals(str[0].trim())) {
                Log.d(TAG, "rmmod /system/lib/modules/sprdwl.ko is fail");
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed get outputStream: " + e);
            e.printStackTrace();
            return false;
        } finally {
            // close socket
            try {
                if (mInsmodeOutputStream != null) {
                    mInsmodeOutputStream.close();
                }
                if (mInsmodeInputStream != null) {
                    mInsmodeInputStream.close();
                }
                if (mInsmodeSocketClient != null) {
                    mInsmodeSocketClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        SystemProperties.set("persist.sys.cmdservice.enable", "disable");
        String status = SystemProperties.get("persist.sys.cmdservice.enable", "");
        Log.d(TAG, "status:" + status);
        return true;
    }
}
