
package com.sprd.firewall;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.sprd.firewall.util.DateUtil;

public class ReceiverSmsState extends BroadcastReceiver {
    private static final String TAG = "ReceiverSmsState";

    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    private static final String WAP_PUSH_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";

    private static final boolean VERBOSE = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (VERBOSE) {
            Log.v(TAG, "onReceive : " + intent.getAction());
        }
    }

    private void handleSmsReceived(Context context, Intent intent) {
        StringBuilder body = new StringBuilder();
        StringBuilder number = new StringBuilder();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] _pdus = (Object[]) bundle.get("pdus");
            SmsMessage[] message = new SmsMessage[_pdus.length];
            for (int i = 0; i < _pdus.length; i++) {
                message[i] = SmsMessage.createFromPdu((byte[]) _pdus[i]);
            }
            for (SmsMessage currentMessage : message) {
                body.append(currentMessage.getDisplayMessageBody());
                if (number.length() == 0)
                    number.append(currentMessage.getDisplayOriginatingAddress());
            }
            if (VERBOSE) {
                Log.v(TAG, "number = " + number);
                Log.v(TAG, "body is " + body);
            }

            String smsNumber = number.toString();
            if (smsNumber.contains("+86")) {
                smsNumber = smsNumber.substring(3);
            } else if (smsNumber.startsWith("12530")) {
                smsNumber = smsNumber.substring(5);
            }

            if (PhoneUtils.CheckIsBlockNumber(context, smsNumber, true, false)) {
                try {
                    if (PhoneUtils.putToSmsBlockList(context, smsNumber, body.toString(),
                            (new Date()).getTime()) != null) {
                        this.abortBroadcast();
                    }
                } catch (SQLiteFullException e) {
                    Toast.makeText(context, R.string.sqlite_full, Toast.LENGTH_SHORT).show();
                } catch (SQLiteDiskIOException e) {
                    Toast.makeText(context, R.string.sqlite_full, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, R.string.sqlite_full, Toast.LENGTH_SHORT).show();
                }
                this.abortBroadcast();
            }
        }
    }
}
