
package com.sprd.engineermode.telephony;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View.OnClickListener; 
import android.os.Bundle;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.sprd.engineermode.R;
import com.sprd.engineermode.utils.IATUtils;
import android.os.SystemProperties;
import android.os.PowerManager;

public class SetIMEI extends Activity implements OnClickListener {
    private static final String LOG_TAG = "SetIMEI";

    private EditText mIMEIEdit, mIMEIEdit2;

    private String mATline = null;
    private String mIMEIInput = null, mIMEIInput2 = null;

    private static final int IMEI_LENGTH = 15;
    private static final int ENG_AT_REQUEST_IMEI = 1;
    private int phoneCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setimei);
        mIMEIEdit = (EditText) findViewById(R.id.imei_edit);
        mIMEIEdit.setText("");
        mIMEIEdit2 = (EditText) findViewById(R.id.imei_edit2);
        mIMEIEdit2.setText("");
        phoneCount = SystemProperties.getInt("persist.msms.phone_count", 1);

        mReadIMEI.start();
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0 && mIMEIInput != null) {
                mIMEIEdit.setText(mIMEIInput);
            } else if (msg.what == 1 && mIMEIInput2 != null) {
                mIMEIEdit2.setText(mIMEIInput2);
            } else if (msg.what == 2) {
                hideSim2();
            }
        }
    };

    Thread mReadIMEI = new Thread() {

        @Override
        public void run() {
            mIMEIInput = readIMEI(0);
            mHandler.sendEmptyMessage(0);
            if (phoneCount > 1) {
                mIMEIInput2 = readIMEI(1);
                mHandler.sendEmptyMessage(1);
            } else {
                mHandler.sendEmptyMessage(2);
            }
        }

    };

    private void hideSim2() {
        mIMEIEdit2.setVisibility(View.GONE);
        findViewById(R.id.wbutton2).setVisibility(View.GONE);
        findViewById(R.id.rbutton2).setVisibility(View.GONE);
        findViewById(R.id.sim2).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.wbutton) {
            if (checkInvalid(0)) {
                String imei = mIMEIEdit.getText().toString();
                if (writeIMEI(imei, 0)) {
                    showToast(R.string.reboot_message_toast);
                    //showRebootDialog();
                } else {
                    showToast("failed!");
                }
            }
        } else if (v.getId() == R.id.rbutton) {
            String imei = readIMEI(0);
            if (imei != null) {
                mIMEIEdit.setText(imei);
            } else {
                showToast("read IMEI1 failed!");
            }
        }
        if (v.getId() == R.id.wbutton2) {
            if (checkInvalid(1)) {
                String imei = mIMEIEdit2.getText().toString();
                if (writeIMEI(imei, 1)) {
                    showToast(R.string.reboot_message_toast);
                    //showRebootDialog();
                } else {
                    showToast("failed!");
                }
            }
        } else if (v.getId() == R.id.rbutton2) {
            String imei = readIMEI(1);
            if (imei != null) {
                mIMEIEdit2.setText(imei);
            } else {
                showToast("read IMEI2 failed!");
            }
        }
    }

    private String readIMEI(int i) {
        String mATResponse;
        Log.d(LOG_TAG, "Engmode socket open, id:" + i);
        mATline = "AT+SPIMEI?";
        mATResponse = IATUtils.sendATCmd(mATline, "atchannel" + i);

        Log.d(LOG_TAG, "Read IMEI: " + i + mATResponse);
        if (!mATResponse.equals("Error")) {
            return mATResponse.substring(0, IMEI_LENGTH);
        } else
            return null;
    }

    private boolean checkInvalid(int i) {
        String imei = null;
        if (i == 0) {
            imei = mIMEIEdit.getText().toString();
        } else if (i == 1) {
            imei = mIMEIEdit2.getText().toString();
        }
        if (imei == null || imei.equals("")) {
            showToast("empty input!");
            return false;
        }
        if (imei.trim().length() != IMEI_LENGTH) {
            showToast("must be 15 digits!");
            return false;
        }
        return true;
    }

    private boolean writeIMEI(String imei, int id) {
        Log.e(LOG_TAG, "Engmode socket open, id:" + id);
        String mATResponse;
        mATline = "AT+SPIMEI=\"" + imei + "\"";
        mATResponse = IATUtils.sendATCmd(mATline, "atchannel" + id);

        Log.d(LOG_TAG, "Write IMEI: " + id + mATResponse);
        if (mATResponse.equals("Error")) {
            return false;
        } else
            return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private void showToast(int id) {
        Toast.makeText(this, this.getResources().getString(id), Toast.LENGTH_SHORT).show();
    }
    private void showRebootDialog() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String message = this.getResources().getString(R.string.reboot_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getResources().getString(R.string.confirm));
        builder.setMessage(message);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "setNegativeButton, Cancel");
            }
        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "setPositiveButton, reboot system");
                pm.reboot(null);
            }
        });
        builder.show();
    }

}
