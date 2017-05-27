package com.sprd.validationtools.itemstest;

import android.os.Handler;
import android.os.Message;
import com.sprd.validationtools.BaseActivity;
import com.sprd.validationtools.R;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;

import android.util.Log;
import com.sprd.android.config.OptConfig;//SUN:jicong.wang add for rgb 

public class LedTest extends BaseActivity {

    private static final String TAG = "LedTest";
    private TextView mTextView;
                            
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_led_info);
        mTextView = (TextView)findViewById(R.id.note_led_info);        
    }

    public void onResume() {
        super.onResume();
        /*SUN:jicong.wang add for rgb start {@*/
        if (OptConfig.SUN_RGB_SINGLE_FOR_LEDS){
            mTextView.setText(getResources().getString(R.string.Note_Led_Info_RGB));
        }
        /*SUN:jicong.wang add for rgb end @}*/        
        Log.d("","pfbz1116 LedTest onResume");
        Intent ledTestStartIntent = new Intent("com.android.intent.action.validate.ledtest.start");
		sendBroadcast(ledTestStartIntent);
    }

    public void onPause() {
        super.onPause();
        Log.d("","pfbz1116 LedTest onPause");
        Intent ledTestEndIntent = new Intent("com.android.intent.action.validate.ledtest.end");
		sendBroadcast(ledTestEndIntent);
    }
}
