
package com.sprd.settings.networkrate;

import java.lang.ref.WeakReference;
import com.android.settings.DataUsageSummary.DataUsageViewChangedReceive;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;

public class NetworkRateManager implements DataUsageViewChangedReceive {

    private static final String TAG = "NetworkRateManager";
    private static final String TAB_3G = "3g";
    private static final String TAB_4G = "4g";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";
    private static final String TAB_ETHERNET = "ethernet";
    private static final String NOLINK = "0Kbps";
    private static final int UNITCHANGE = 900;

    private Context mContext;
    private Resources mResources;
    private TextView mUpLinkRate = null;
    private TextView mDownLinkRate = null;
    private TextView mTotalRate = null;
    private String mNetWorkType = null;
    private int mCurrentSubId = -1;
    private RateHandler mRateHandler = null;

    private static final long UPDATE_UI_DELAY = 1000;
    private static final int MSG_UPDATE_RATE_HIDE = 0;
    private static final int MSG_UPDATE_RATE_SHOW = 1;
    private static final int MSG_UPDATE_RATE_INIT = 2;

    class RateData {
        public String upLinkTheoryPeak;
        public String downLinkTheoryPeak;

        public long upLinkRate;
        public long downLinkRate;

        public long totalSend;
        public long totalReceive;
    }

    private Handler mUpdateUIHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_RATE_SHOW: {

                    RateData rateData = (msg.obj != null && msg.obj instanceof RateData ? (RateData) msg.obj
                            : null);
                    if (rateData != null) {
                        if(SubscriptionManager.getDefaultDataSubId() == mCurrentSubId || "wifi".equalsIgnoreCase(mNetWorkType)){
                            showNetworkRateViews(true);
                        }
                        // update UI
                        updateNetworkRate(rateData.upLinkTheoryPeak, rateData.downLinkTheoryPeak,
                                Math.max(0, rateData.upLinkRate),
                                Math.max(0, rateData.downLinkRate));
                        updateNetworkTotal(rateData.totalSend, rateData.totalReceive);
                    } else {
                        showNetworkRateViews(false);
                    }
                }
                    break;
                case MSG_UPDATE_RATE_HIDE: {
                    // updateNetworkRateUnknown();
                    showNetworkRateViews(false);
                }
                    break;
                case MSG_UPDATE_RATE_INIT: {
                    DataNetworkRate.NetworkTrafficCurrentRate networkTrafficCurrentRate = (msg.obj != null
                            && msg.obj instanceof DataNetworkRate.NetworkTrafficCurrentRate ? (DataNetworkRate.NetworkTrafficCurrentRate) msg.obj
                            : null);
                    if (networkTrafficCurrentRate != null) {
                        updateNetworkTotal(networkTrafficCurrentRate.upLinkRate,
                                networkTrafficCurrentRate.downLinkRate);
                    }
                }
                    break;
            }
        };
    };

    class RateHandler extends Handler {

        // create rate information
        public final static int MSG_NETWORK_NETWORK_RATE_INIT = 0;
        // update rate information
        public final static int MSG_NETWORK_NETWORK_RATE_UPDATE = 1;

        private final WeakReference<Context> mContext;

        public RateHandler(Context context, Looper looper) {
            super(looper);
            mContext = new WeakReference<Context>(context);
            mDataNetworkRate = new DataNetworkRate(context);
        }

        private boolean mIsMobileDataUsage = false;

        private DataNetworkRate mDataNetworkRate = null;

        private DataNetworkRate.NetworkTrafficTheoryPeak mNetworkTrafficTheoryPeak = null;
        private DataNetworkRate.NetworkTrafficCurrentRate mNetworkTrafficCurrentRate = null;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_NETWORK_NETWORK_RATE_INIT: {
                    mIsMobileDataUsage = isMobileDataUsageView();
                    mNetworkTrafficTheoryPeak = mDataNetworkRate
                            .getCurrentNetworkTrafficTheoryPeak(mContext != null ? mContext.get()
                                    : null, mIsMobileDataUsage);
                    mNetworkTrafficCurrentRate = mDataNetworkRate.getCurrentNetworkTrafficRate(
                            mContext != null ? mContext.get() : null, mIsMobileDataUsage);
                    mUpdateUIHandler.sendMessage(Message.obtain(mUpdateUIHandler,
                            MSG_UPDATE_RATE_INIT,
                            mDataNetworkRate.getCurrentNetworkTrafficTotal(mIsMobileDataUsage)));
                    networkRateUpdate(false);
                }
                    break;
                case MSG_NETWORK_NETWORK_RATE_UPDATE: {
                    DataNetworkRate.NetworkTrafficCurrentRate netWorkTrafficStatus = mDataNetworkRate
                            .getCurrentNetworkTrafficRate(mContext != null ? mContext.get() : null,
                                    mIsMobileDataUsage);
                    // read current network
                    if (mNetworkTrafficCurrentRate != null && netWorkTrafficStatus != null
                            && mNetworkTrafficTheoryPeak != null) {
                        RateData rateData = new RateData();
                        rateData.upLinkTheoryPeak = mNetworkTrafficTheoryPeak.upLinkTheoryPeak;
                        rateData.downLinkTheoryPeak = mNetworkTrafficTheoryPeak.downLinkTheoryPeak;
                        rateData.upLinkRate = netWorkTrafficStatus.upLinkRate
                                - mNetworkTrafficCurrentRate.upLinkRate;
                        rateData.downLinkRate = netWorkTrafficStatus.downLinkRate
                                - mNetworkTrafficCurrentRate.downLinkRate;
                        rateData.totalSend = netWorkTrafficStatus.upLinkRate;
                        rateData.totalReceive = netWorkTrafficStatus.downLinkRate;
                        // update ui
                        mUpdateUIHandler.sendMessage(Message.obtain(mUpdateUIHandler,
                                MSG_UPDATE_RATE_SHOW, rateData));
                    } else {
                        mUpdateUIHandler.sendEmptyMessage(MSG_UPDATE_RATE_HIDE);
                    }
                    mNetworkTrafficCurrentRate = netWorkTrafficStatus;
                    // update current network
                    networkRateUpdate(true);
                }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public NetworkRateManager(Context context) {
        this.mContext = context;
        this.mResources = this.mContext.getResources();
    }

    // bind views
    public void initializeRateViews(View view) {
        if (view != null) {
            // uplink_spread
            mUpLinkRate = (TextView) view.findViewById(R.id.uplink_spread);
            // downlink_spread
            mDownLinkRate = (TextView) view.findViewById(R.id.downlink_spread);
            // total
            mTotalRate = (TextView) view.findViewById(R.id.total_traffic);
        }
    }

    // hide or show views
    private void showNetworkRateViews(boolean bShow) {
        if (mUpLinkRate == null)
            return;
        if (bShow) {
            if (!mUpLinkRate.isShown()) {
                mUpLinkRate.setVisibility(View.VISIBLE);
                mDownLinkRate.setVisibility(View.VISIBLE);
                mTotalRate.setVisibility(View.VISIBLE);
            }
        } else {
            if (mUpLinkRate.isShown()) {
                mUpLinkRate.setVisibility(View.GONE);
                mDownLinkRate.setVisibility(View.GONE);
                mTotalRate.setVisibility(View.GONE);
            }
        }
    }

    private String formatFileSize(Context context, long number, boolean shorter) {
        if (context == null) {
            return "";
        }

        float result = number;
        int suffix = R.string.byteShort;
        if (result > UNITCHANGE) {
            suffix = R.string.kilobyteShort;
            result = result / 1024;
        }
        if (result > UNITCHANGE) {
            suffix = R.string.megabyteShort;
            result = result / 1024;
        }
        if (result > UNITCHANGE) {
            suffix = R.string.gigabyteShort;
            result = result / 1024;
        }
        if (result > UNITCHANGE) {
            suffix = R.string.terabyteShort;
            result = result / 1024;
        }
        if (result > UNITCHANGE) {
            suffix = R.string.petabyteShort;
            result = result / 1024;
        }
        String value;
        if (result < 1) {
            value = String.format("%.2f", result);
        } else if (result < 10) {
            if (shorter) {
                value = String.format("%.1f", result);
            } else {
                value = String.format("%.2f", result);
            }
        } else if (result < 100) {
            if (shorter) {
                value = String.format("%.0f", result);
            } else {
                value = String.format("%.2f", result);
            }
        } else {
            value = String.format("%.0f", result);
        }
        return context.getResources().getString(R.string.fileSizeSuffix,
                value, context.getString(suffix));
    }

    // update network rate
    private void updateNetworkRate(String uplinkMax, String downlinkMax, long uplinkRate,
            long downlinkRate) {
        String uplinkMaxStr = (uplinkMax == null ? NOLINK : uplinkMax);
        String downlinkMaxStr = (downlinkMax == null ? NOLINK : downlinkMax);
        String uplinkStr = formatFileSize(mContext, Math.max(0, uplinkRate), false);
        String downlinkStr = formatFileSize(mContext, Math.max(0, downlinkRate), false);
        mUpLinkRate.setText(String.format(mResources.getString(R.string.uplink_rate), uplinkMaxStr,
                uplinkStr));
        mDownLinkRate.setText(String.format(mResources.getString(R.string.downlink_rate),
                downlinkMaxStr, downlinkStr));
    }

    //Send and Receive Total
    private void updateNetworkTotal(long sendTotal, long recTotal) {
        String sendStr = formatFileSize(mContext, Math.max(0, sendTotal), false);
        String recStr = formatFileSize(mContext, Math.max(0, recTotal), false);
        mTotalRate
                .setText(String.format(mResources.getString(R.string.total_rate), sendStr, recStr));
    }

    // NetWorkRateManager
    @Override
    public void onDataUsageViewChanged(String networkType) {
        mNetWorkType = networkType;
        mUpdateUIHandler.sendEmptyMessage(MSG_UPDATE_RATE_HIDE);
        networkRateInit();
    }

    private boolean isMobileDataUsageView() {
        if (mNetWorkType == null) {
            return false;
        }
        Log.d(TAG,"mNetWorkType = " + mNetWorkType);
        if (mNetWorkType.contains("mobile")) {
            return true;
        } else {
            return false;
        }
    }

    private void networkRateInit() {
        if (mRateHandler != null) {
            if (mRateHandler.hasMessages(RateHandler.MSG_NETWORK_NETWORK_RATE_INIT)) {
                mRateHandler.removeMessages(RateHandler.MSG_NETWORK_NETWORK_RATE_INIT);
            }
            mRateHandler.sendEmptyMessage(RateHandler.MSG_NETWORK_NETWORK_RATE_INIT);
        }
    }

    private void networkRateUpdate(boolean bDelay) {
        if (mRateHandler != null) {
            if (mRateHandler.hasMessages(RateHandler.MSG_NETWORK_NETWORK_RATE_UPDATE)) {
                mRateHandler.removeMessages(RateHandler.MSG_NETWORK_NETWORK_RATE_UPDATE);
            }
            if (bDelay) {
                // sleep one second , and update ui
                mRateHandler.sendEmptyMessageDelayed(RateHandler.MSG_NETWORK_NETWORK_RATE_UPDATE,
                        UPDATE_UI_DELAY);
            } else {
                mRateHandler.sendEmptyMessage(RateHandler.MSG_NETWORK_NETWORK_RATE_UPDATE);
            }
        }
    }

    public void resume() {
        clean();
        HandlerThread handlerThread = new HandlerThread("measure_network_rate");
        handlerThread.setPriority(Thread.MIN_PRIORITY);
        handlerThread.start();
        mRateHandler = new RateHandler(mContext, handlerThread.getLooper());
    }

    public void clean() {
        if (mRateHandler != null) {
            mRateHandler.getLooper().quit();
            mRateHandler = null;
        }
    }

    public void setSubId(int id){
            mCurrentSubId = id;
    }

}
