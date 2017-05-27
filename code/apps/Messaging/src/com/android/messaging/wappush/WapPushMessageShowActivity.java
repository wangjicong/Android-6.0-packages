package com.android.messaging.wappush;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.Telephony.Sms;



public class WapPushMessageShowActivity extends Activity {
    private static final String TAG = "WapPushMessageShowActivity";
    private static final int UNREAD = 0;
    private static final int READ = 1;
    private Context mContext;

    private Map<AlertDialog, Intent> dialogMap = new HashMap<AlertDialog, Intent>();

    private final OnClickListener mOKListener = new OnClickListener() {

        public void onClick(DialogInterface dialog, int whichButton) {
            closeDialog(dialog);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "===wap push======onCreate====");
        mContext = this;
        AlertDialog dialog = showConfirmDialog();
        synchronized (dialogMap) {
            dialogMap.put(dialog, this.getIntent());
        }
    }

    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "===wap push======onNewIntent====");
        AlertDialog dialog = showConfirmDialog();
        synchronized (dialogMap) {
            dialogMap.put(dialog, this.getIntent());
        }
    }

    private void closeDialog(DialogInterface dialog) {
        dialog.dismiss();
        synchronized (dialogMap) {
            dialogMap.remove(dialog);
            if (dialogMap.size() == 0)
                WapPushMessageShowActivity.this.finish();
        }
    }

    private AlertDialog showConfirmDialog() {
        String pushbody = "";
        if (this.getIntent().hasExtra("pushBody")) {
            pushbody = this.getIntent().getExtras().getString("pushBody");
        }
        String url = "";
        if (this.getIntent().hasExtra("href")) {
            url = this.getIntent().getExtras().getString("href");
        }
        final String href = url;
        return new AlertDialog.Builder(this)
                .setTitle(R.string.wap_push_message_dialog_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setMessage(pushbody)
                .setPositiveButton(R.string.open_website,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                try {
                                    cancelNotification(READ);
                                    Uri uri = Uri.parse(href);
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW, uri);
                                    intent.putExtra(
                                            Browser.EXTRA_APPLICATION_ID,
                                            WapPushMessageShowActivity.this
                                                    .getPackageName());
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                    callActivity(
                                            WapPushMessageShowActivity.this,
                                            intent);
                                } catch (NullPointerException ex) {
                                    Log.e(TAG, "href is null!!!");
                                    Toast.makeText(
                                            WapPushMessageShowActivity.this,
                                            "href is null!!!",
                                            Toast.LENGTH_SHORT).show();
                                } catch (ActivityNotFoundException ex) {
                                    Log.e(TAG,
                                            "send intent to browserApp happened exception !!:::"
                                                    + ex.toString(), ex);
                                    Toast.makeText(
                                            WapPushMessageShowActivity.this,
                                            R.string.activity_not_found,
                                            Toast.LENGTH_SHORT).show();
                                }
                                closeDialog(dialog);
                            }
                        })
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                cancelNotification(UNREAD);
                                closeDialog(dialog);
                            }
                        }).setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        closeDialog(dialog);
                    }
                }).show();
    }

    private void cancelNotification(final int read) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put("read", read);
                values.put("seen", 1);
                String uriString = getIntent().getExtras().getString(
                        "messageUri");
                if (!TextUtils.isEmpty(uriString)) {
                    int index = uriString.lastIndexOf("/");

                    Log.d(TAG,
                            "========wap push====cancelNotification messageUri: "
                                    + uriString + "     index: " + index);
                    int messageId = Integer.valueOf(uriString
                            .substring(index + 1));
                    Log.d(TAG,
                            "========wap push====cancelNotification messageUri: "
                                    + uriString + "   messageId: " + messageId
                                    + "     index: " + index);

                    mContext.getContentResolver().update(Sms.Inbox.CONTENT_URI,
                            values, "_id = ?",
                            new String[] { String.valueOf(messageId) });

                    final DatabaseWrapper dbWrapper = DataModel.get()
                            .getDatabase();
                    dbWrapper.update(DatabaseHelper.MESSAGES_TABLE, values,
                            "sms_message_uri = ?", new String[] { uriString });
                    mContext.getContentResolver().notifyChange(
                            MessagingContentProvider.CONVERSATIONS_URI, null);
                }
            }
        }, "wappush.cancelNotification").start();
    }

    private void callActivity(Context mContext, Intent intent) {
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext.getApplicationContext(),
                    "There is not such Application ", Toast.LENGTH_LONG).show();
            Log.e("StartActivityUtil", "No Activity found to handle Intent: "
                    + intent);
        }
    }
}
