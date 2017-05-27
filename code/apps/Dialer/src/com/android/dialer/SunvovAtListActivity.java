package com.android.dialer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;


/**
 * Created by Administrator on 2017/2/21 0021.
 */
public class SunvovAtListActivity extends Activity {
    private static final String TAG = SunvovAtListActivity.class.getName();
    private ScrollView mRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlist);
        mRoot = (ScrollView) findViewById(R.id.at_list);

        TextView view = new TextView(this);
        view.setGravity(Gravity.LEFT);
        view.setTextSize(20);
        view.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 2, 0, 2);
        view.setLayoutParams(layoutParams);
        String all_at = getAtList();
        view.setText(all_at);
        mRoot.addView(view);
    }

    /**
     * get at list
     * @return
     */
    private String getAtList() {
        String mmiVersion = getResources().getString(R.string.show_mmiVersion_hint) + getResources().getString(R.string.show_mmiVersion);
        String openFactory = getResources().getString(R.string.open_factoryTest_hint) + getResources().getString(R.string.open_factoryTest);
        String openEngineer = getResources().getString(R.string.open_engineerMode_hint) + getResources().getString(R.string.open_engineerMode);
        String openBootSelect = getResources().getString(R.string.open_bootselect_hint) + getResources().getString(R.string.open_bootselect);
        String openAgingTest = getResources().getString(R.string.open_AgingTest_hint) + getResources().getString(R.string.open_AgingTest);
        String openSetImei = getResources().getString(R.string.open_setimei_hint) + getResources().getString(R.string.open_setimei);
        String open_SalesTracker = getResources().getString(R.string.open_SalesTracker_hint) + getResources().getString(R.string.open_SalesTracker);
        String sensorsid = getResources().getString(R.string.open_sensorsid_hint) + getResources().getString(R.string.open_sensorsid);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(mmiVersion + "\n");
        stringBuffer.append(openFactory + "\n");
        stringBuffer.append(openEngineer + "\n");
        stringBuffer.append(openBootSelect + "\n");
        stringBuffer.append(openAgingTest + "\n");
        stringBuffer.append(openSetImei + "\n");
        stringBuffer.append(open_SalesTracker + "\n");
        stringBuffer.append(sensorsid+"\n");
        return stringBuffer.toString();

    }    
}
