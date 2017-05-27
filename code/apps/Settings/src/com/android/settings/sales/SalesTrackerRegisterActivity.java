package com.android.settings.sales;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import com.android.settings.R;


public class SalesTrackerRegisterActivity extends Activity implements OnClickListener {
	private TextView Reg_text;
	private Button Reg_button;
	private boolean Is_before = false;
	private boolean Is_after = false;
	private boolean Is_test = false;
	private String test_num = null;

    private static final String TAG = "SalesTracker";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        
        Intent intent = this.getIntent();
        Is_before = intent.getBooleanExtra("is_before", false);
        Is_test = intent.getBooleanExtra("is_test", false);
        Is_after = intent.getBooleanExtra("is_after", false);
        
        if(Is_test)
        {
        	test_num = intent.getStringExtra("test_num");
        }
        
        Reg_text=(TextView)findViewById(R.id.Reg_text);
        
        if(Is_before||Is_test)
        {        	
        	Reg_text.setText(SalesTrackerInfo.Reg_string);
        }
        else if(Is_after)
        {
        	Reg_text.setText(SalesTrackerInfo.Reg_success_string);
        }
        
        Reg_button=(Button)findViewById(R.id.Reg_button);
        Reg_button.setText("OK");
        Reg_button.setOnClickListener(this);
        
        enableHomeKeyDispatched(true);//used to get HOME Key

    }
    
    private void enableHomeKeyDispatched(boolean enable) {  
        final Window window = getWindow();  
        final WindowManager.LayoutParams lp = window.getAttributes();  
        /*if (enable) {  
             lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;  
        } else {  
             lp.flags &= ~WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;  
        }  */
        window.setAttributes(lp);  
    }  
    
	public void onClick(View v) {
        switch (v.getId()) {
        case R.id.Reg_button:
		finish();            
		break;
        }
	}	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "SalesTrackerRegisterActivity------onKeyDown");
		finish();
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		if(SalesTrackerInfo.REGISTER_FUNCTION)
		{
			if(Is_test)
			{
				Intent testintent = new Intent("com.android.sales.sendtestsms");
				testintent.putExtra("test_num", test_num);
				testintent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				sendBroadcast(testintent);
			}
			else if(Is_before)
			{
				Intent sms_intent = new Intent("com.android.sales.sendsms");
				sms_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);				
				sendBroadcast(sms_intent);
			}
		}
		Log.d(TAG, "SalesTrackerRegisterActivity------onDestroy");
		// TODO Auto-generated method stub
		super.onDestroy();
	}

}
