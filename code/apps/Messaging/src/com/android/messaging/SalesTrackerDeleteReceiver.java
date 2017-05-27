package com.android.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms;  
import android.os.AsyncTask;
import android.database.ContentObserver;
import android.util.Log;
import com.sprd.android.config.OptConfig;

/* Sunvov:jiazhenl 20150609 add start @{ */
public class SalesTrackerDeleteReceiver extends BroadcastReceiver {
    private static Context mContext;
    private String comparestring = null;
    private static final Uri SMS_QUERY_URI = Uri.parse("content://sms");
    private static final String[] SMS_STATUS_PROJECTION = new String[] { Sms.BODY, Sms._ID };
    private static final String NEW_SENT_SM_CONSTRAINT = Sms.TYPE + " = " + Sms.MESSAGE_TYPE_SENT;


	public void onReceive(Context context, Intent intent) {
		if(mContext == null)
		{
			mContext = context;
		}
		
        if (intent.getAction().equals("com.android.sales.deletesms")) {
			comparestring = intent.getStringExtra("msg_body");
			if(comparestring!= null)
			{
				DeleteSentSMS();
			}
        }
    }
    
    public void DeleteSentSMS() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {

                Cursor cursor = mContext.getContentResolver()
                        .query(SMS_QUERY_URI, SMS_STATUS_PROJECTION,null, null, null);

				Log.d("shaocheng", "lupei DeleteSentSMS start");
                if (cursor != null) {
                    try {
							while (cursor.moveToNext()) 
							{ 
								String body = cursor.getString(cursor.getColumnIndex("body")).trim();
			
								Log.d("shaocheng", "lupei body =" + body);
								
								if (body.equals(comparestring)) 
								{ 
									int id = cursor.getInt(cursor.getColumnIndex("_id")); 
									mContext.getContentResolver().delete(Uri.parse("content://sms/"+id), null, null); 
							    }
								
							} 
                    } finally {
                        cursor.close();
                    }
                }
                return null;
            }

            @Override
            public void onPostExecute(Void result) {
            }
        }.execute(null, null, null);
    }
}
/* Sunvov:jiazhenl 20150609 add end @} */
