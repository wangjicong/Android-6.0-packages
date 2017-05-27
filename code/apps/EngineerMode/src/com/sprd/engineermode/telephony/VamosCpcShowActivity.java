
package com.sprd.engineermode.telephony;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import java.lang.String;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.LinearLayout;
import com.sprd.engineermode.engconstents;
import com.sprd.engineermode.utils.IATUtils;
import com.sprd.engineermode.R;
import android.widget.ArrayAdapter;
import com.sprd.engineermode.engconstents;

public class VamosCpcShowActivity extends Activity {

    private static final String TAG = "VamosCpcShowActivity";

    private static final int GET_VAMOS_CPC_STATUS = 0;
    private ListView listView;
    private String SaveName = "atchannel0";

    private VamosCpcHandler mVamosCpcHandler;
    private Handler mUiThread = new Handler();
    private String[] values = {
            "VAMOS: ", "CPC: "
    };
    private String[][] Description_name = {
            {
                    "Not Support", "VAMOS1", "VAMOS2"
            }, {
                    "Not Support", "Support"
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vamos_cpc_layout);
        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mVamosCpcHandler = new VamosCpcHandler(ht.getLooper());
        listView = (ListView) findViewById(R.id.vamos_cpc_listview);

    }

    @Override
    protected void onResume() {
        if (listView != null) {
            Message getVamosCpcStatus = mVamosCpcHandler.obtainMessage(GET_VAMOS_CPC_STATUS);
            mVamosCpcHandler.sendMessage(getVamosCpcStatus);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mVamosCpcHandler != null) {
            mVamosCpcHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    class VamosCpcHandler extends Handler {
        public VamosCpcHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String result = null;
            String atCmd;
            switch (msg.what) {
                case GET_VAMOS_CPC_STATUS:
                    atCmd = engconstents.ENG_CET_VAMOS_CPC + "0,0,7";
                    result = IATUtils.sendATCmd(atCmd, SaveName);
                    Log.d(TAG, atCmd + ": " + result);
                    if (result != null && result.contains(IATUtils.AT_OK)) {
                        result = result.replaceAll("--", "-+");
                        String[] str1 = result.split("\n");
                        String[] str2 = str1[0].split("-");
                        /*
                         * SPRD: FixBug451913 ,EngineerMode crash beacause
                         * java.lang.ArrayIndexOutOfBoundsException {@
                         */
                        int vamosValue = -1;
                        if (str2.length > 7) {
                            vamosValue = Integer.valueOf(str2[7].replace("+", "-").trim());
                        }
                        int cpcValue = -1;
                        if (str2.length > 14) {
                            cpcValue = Integer.valueOf(str2[14].replace("+", "-").trim());
                        }
                        /* @} */
                        if ((vamosValue >= 0) && (vamosValue <= 2)) {
                            values[0] = values[0] + Description_name[0][vamosValue];
                        } else {
                            values[0] = values[0] + "NA";
                        }
                        if ((cpcValue >= 0) && (cpcValue <= 1)) {
                            values[1] = values[1] + Description_name[1][cpcValue];
                        } else {
                            values[1] = values[1] + "NA";
                        }
                    } else {
                        values[0] = values[0] + "NA";
                        values[1] = values[1] + "NA";
                    }
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                    VamosCpcShowActivity.this, R.layout.array_item, values);
                            listView.setAdapter(adapter);
                        }
                    });
                    break;
            }
        }
    }

}
