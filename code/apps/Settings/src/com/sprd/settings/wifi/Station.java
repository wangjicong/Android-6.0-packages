/** Created by Spreadst */

package com.sprd.settings.wifi;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;
import com.android.settings.R;
import android.content.DialogInterface;
import android.os.SystemProperties;

class Station extends Preference {

    private WifiManager mWifiManager;
    private Context mContext;

    private String stationName;
    private String stationMac;
    private String stationIP;
    private boolean isConnected;
    private boolean isWhitelist;
    private boolean supportWhitelist = SystemProperties.get("ro.softap.whitelist", "true").equals("true");
    private OnClickListener mButtonClick;

    public Station(Context context, String name, String mac, String ip, boolean connected, boolean whitelist) {
        super(context);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mContext = context;
        stationName = name;
        stationMac = mac;
        stationIP = ip;
        isConnected = connected;
        isWhitelist = whitelist;
        if (stationName != null) {
            stationName = WifiSsid.createFromAsciiEncoded(stationName).toString();
            setTitle(stationName);
            if (isConnected) {
                setSummary("IP: "+stationIP+"\nMAC: "+stationMac);
            } else {
                setSummary("Mac: "+stationMac);
            }
        } else {
            setTitle(stationMac);
        }
    }

    @Override
    protected void onClick() {
        askToAddWhiteList();
    }
    private void askToAddWhiteList() {
        // TODO Auto-generated method stub
        String stationNameTemp = stationName;
        if (stationNameTemp == null) {
            stationNameTemp = "";
        }
        if (isWhitelist) {
            new AlertDialog.Builder(mContext).setTitle(stationNameTemp)
            .setMessage("MAC: "+stationMac)
            .setPositiveButton(R.string.hotspot_offwhite, removeWhiteListListener)
            .setNegativeButton(R.string.hotspot_whitelist_cancel, null).show();
        } else if (isConnected) {
            if (mWifiManager.softApIsWhiteListEnabled()) {
                new AlertDialog.Builder(mContext).setTitle(stationNameTemp)
                .setMessage("IP: "+stationIP+"\nMAC: "+stationMac)
                .setNegativeButton(R.string.hotspot_whitelist_cancel, null).show();
            } else if (supportWhitelist){
                new AlertDialog.Builder(mContext).setTitle(stationNameTemp)
                .setMessage("IP: "+stationIP+"\nMAC: "+stationMac)
                .setPositiveButton(R.string.hotspot_whitelist_add, addWhiteListListener)
                .setNegativeButton(R.string.block, controlBlockListener)
                .setNeutralButton(R.string.hotspot_whitelist_cancel, null).show();
            } else {
                new AlertDialog.Builder(mContext).setTitle(stationNameTemp)
                .setMessage("IP: "+stationIP+"\nMAC: "+stationMac)
                .setNegativeButton(R.string.block, controlBlockListener)
                .setNeutralButton(R.string.hotspot_whitelist_cancel, null).show();
            }
        } else {
            if (supportWhitelist) {
                new AlertDialog.Builder(mContext).setTitle(stationNameTemp)
                .setMessage("MAC: "+stationMac)
                .setPositiveButton(R.string.hotspot_whitelist_add, addWhiteListListener)
                .setNegativeButton(R.string.unblock, controlBlockListener)
                .setNeutralButton(R.string.hotspot_whitelist_cancel, null).show();
            } else {
                new AlertDialog.Builder(mContext).setTitle(stationNameTemp)
                .setMessage("MAC: "+stationMac)
                .setNegativeButton(R.string.unblock, controlBlockListener)
                .setNeutralButton(R.string.hotspot_whitelist_cancel, null).show();
            }
        }
    }

    android.content.DialogInterface.OnClickListener addWhiteListListener  = new android.content.DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            if (which == DialogInterface.BUTTON_POSITIVE) {
                List<String> mWhitelistStations = mWifiManager.softApGetClientWhiteList();
                if (mWhitelistStations.size() < 8) {
                    mWifiManager.softApAddClientToWhiteList(stationMac,stationName);
                    mContext.sendBroadcast(new Intent(HotspotSettings.STATIONS_STATE_CHANGED_ACTION));
                } else {
                    Toast.makeText(mContext, R.string.wifi_add_whitelist_limit,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    android.content.DialogInterface.OnClickListener removeWhiteListListener  = new android.content.DialogInterface.OnClickListener()
{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            if (which == DialogInterface.BUTTON_POSITIVE) {
                offWhiteButton();
            }
        }
    };

    android.content.DialogInterface.OnClickListener controlBlockListener  = new android.content.DialogInterface.OnClickListener
()
{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                setBlockButton();
            }
        }
    };

    void offWhiteButton() {
       mWifiManager.softApDelClientFromWhiteList(stationMac,stationName);
       mContext.sendBroadcast(new Intent(HotspotSettings.STATIONS_STATE_CHANGED_ACTION));
    }
    void setBlockButton() {
        if (isConnected) {
            List<String> mBlockedStationsDetail = mWifiManager.softApGetBlockedStationsDetail();
            if (mBlockedStationsDetail.size() >= 8) {
                Toast.makeText(mContext, R.string.hotspot_add_blockedstations_limit, Toast.LENGTH_SHORT).show();
                return;
            }
            mWifiManager.softApBlockStation(stationMac);
        } else {
            mWifiManager.softApUnblockStation(stationMac);
        }
        mContext.sendBroadcast(new Intent(HotspotSettings.STATIONS_STATE_CHANGED_ACTION));
    }

}
