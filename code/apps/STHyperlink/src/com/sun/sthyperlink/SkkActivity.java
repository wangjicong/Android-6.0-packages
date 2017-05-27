package com.sun.sthyperlink;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public class SkkActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		final String sHomepage = getResources().getString(R.string.home_page);
		try{
	        Intent intent= new Intent();        
	        intent.setAction("android.intent.action.VIEW");    
	        Uri content_url = Uri.parse(sHomepage);
	        intent.setData(content_url);           
	        intent.setClassName("com.android.browser","com.android.browser.BrowserActivity");    
	        startActivityForResult(intent,1);
		}catch (Exception e1) {
            // TODO Auto-generated catch block
            try{
	            Intent intent= new Intent();        
		        intent.setAction("android.intent.action.VIEW");    
		        Uri content_url = Uri.parse(sHomepage);   
		        intent.setData(content_url);           
		        intent.setClassName("com.android.chrome","com.google.android.apps.chrome.Main");
		        startActivityForResult(intent,1);
			}catch (Exception e2) {
				 // TODO Auto-generated catch block
			}
        }
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.d("Kalyy","requestCode="+requestCode);
        if (requestCode == 1) {
			Log.d("Kalyy","finish!!!");
        	finish();
         }
    }
}
