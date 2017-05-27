package com.android.messaging.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.widget.BugleWidgetProvider;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.action.DeleteMessageAction;

public class WapPushDeleteReceiver extends BroadcastReceiver {
    private static final String TAG = "WapPushDeleteReceiver";
    private final static String WAP_PUSH_EXPRIRE_DELETE_ACTION = "com.android.mms.transaction.wappush_expire_delete";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "==========wap push===0==onReceive=====");
        if (PhoneUtils.getDefault().isSmsEnabled()) {
            if (WAP_PUSH_EXPRIRE_DELETE_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "======wap push====1======wap push delete=====");
                deleteExpiredWapPushMessage(intent);
            }
        }
    }

    private static void deleteExpiredWapPushMessage(Intent intent) {
        String uri = intent.getStringExtra("uri");
        if (TextUtils.isEmpty(uri)) {
            return;
        }
        final long messageId = ContentUris.parseId(Uri.parse(uri));
        final ContentResolver cr = Factory.get().getApplicationContext().getContentResolver();
        Log.d(TAG,"====wap push===deleteExpiredWapPushMessage=====currentTimeMillis: "
              + System.currentTimeMillis()+"    messageId: "+messageId+"   uri: "+uri);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = cr.query(Sms.CONTENT_URI,
                                             new String[] { "_id" }, " _id = '" + messageId
                                             + "'" + " and wap_push = '1'", null, null);
                    if (cursor == null || cursor.getCount() == 0)
                        return;
                    int sms_id = -1;
                    if (cursor.moveToFirst()) {
                        try {
                            do {
                                sms_id = cursor.getInt(0);
                            } while (cursor.moveToNext());
                        } finally {
                            cursor.close();
                            cursor = null;
                        }
                    }

                    String messageUri = "content://sms/" + sms_id;
                    Log.d(TAG,"========wap push====deleteExpiredWapPushMessage uri: "+ messageUri);
                    final DatabaseWrapper dbWrapper = DataModel.get().getDatabase();
                    cursor = dbWrapper.query(DatabaseHelper.MESSAGES_TABLE,
                                             new String[] { "_id" }, "sms_message_uri = ?",
                                             new String[] { messageUri }, null, null, null);

                    int message_id = -1;
                    if (cursor != null && cursor.moveToFirst()) {
                        try {
                            do {
                                message_id = cursor.getInt(0);
                            } while (cursor.moveToNext());
                        } finally {
                            cursor.close();
                            cursor = null;
                        }
                    }
                    DeleteMessageAction.deleteMessage(String.valueOf(message_id));
                    Log.d(TAG,"===wap push====deleteExpiredWapPushMessage messageId: "+ message_id);
                 } catch (Exception e) {
                     Log.e(TAG,"process deleteExpriedWapPushMessage happened exception!",e);
                 }
            }
        }).start();
    }
}
