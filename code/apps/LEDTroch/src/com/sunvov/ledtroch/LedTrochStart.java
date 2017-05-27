package com.sunvov.ledtroch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.Window;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class LedTrochStart extends Activity {

    private boolean isopent = false;
    private ImageButton switch_button;
    private RelativeLayout mBackground;

	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what)
			{
			case 1:
				openLED();
				break;
			case 2:
				colseLED();
				break;
			default:
				break;
			}
		}
		
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		mBackground = (RelativeLayout)findViewById(R.id.troch_background);
		switch_button = (ImageButton) findViewById(R.id.switch_led);
		switch_button.setOnClickListener(SwitchListener);
	}
	 final OnClickListener SwitchListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
            if (!isopent) {
				openLED();
            } else {
				colseLED();         
            }
        }
	 };
	public void LedSendMessage(String message){
		Intent intent = new Intent(message);
		sendBroadcast(intent);
	}

	public void openLED(){
		LedSendMessage("LedTroch_OpenLight");
        isopent = true;
		mBackground.setBackgroundResource(R.drawable.backgroundstrong);            	
        switch_button.setImageResource(R.drawable.on);
	}
	
	public void colseLED(){
		LedSendMessage("LedTroch_CloseLight");
  		isopent = false;
		mBackground.setBackgroundResource(R.drawable.backgroundoff);			
        switch_button.setImageResource(R.drawable.off);
	}
	protected void onStop() {
		super.onStop();
		if(isopent){
			switch_button.setImageResource(R.drawable.off);
			mBackground.setBackgroundResource(R.drawable.backgroundoff);
			colseLED();
		}	 
	}

	@Override
	 protected void onDestroy() {
		 super.onDestroy();
		if(isopent){
			colseLED();
		}	 
	 }	 

 	@Override
    protected void onResume() {
    	super.onResume();
		mHandler.sendEmptyMessageDelayed(1, 100);
 	}
}
