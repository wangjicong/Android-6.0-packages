/*
 * SPRD: bug498391/510827, request runtime permissions
 */

package com.android.calendar;

import com.sprd.calendar.vcalendar.OpenVcalendar;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Activity that shows permissions request dialogs and handles lack of critical permissions.
 */
public class PermissionsActivity extends Activity {

    private static final String TAG = "PermissionsActivity";
    private static final int CALENDAR_PERMISSIONS_REQUEST_CODE = 1;
    SharedPreferences mPreferences = null;
    SharedPreferences.Editor mEditor = null;
    private int mNumPermissionsToRequest = 0;
    private int mIndexReadCalendarPermission = 0;
    private int mIndexReadStoragePermission = 0;
    private int mIndexContactsPermission = 0;
    private int mIndexReadSMSPermission = 0;
    private boolean requestReadCalendarPermission = false;
    private boolean requestReadStoragePermission = false;
    private boolean requestContactsPermission = false;
    private boolean requestReadSMSPermission = false;
    private boolean mFlagHasReadCalendarPermission = false;
    private boolean mFlagHasReadStoragePermission = false;
    private boolean mFlagHasContactsPermission = false;
    private boolean mFlagHasReadSMSPermission = false;
    private int mActivityFlag = 0;
    public static final String ACTIVITY_FLAG = "Activity_Flag";
    private boolean mHasRequest = true;
    private boolean mFailureFlag = false;
    private AlertDialog mSecureCheckDialog;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.permissions);

        Intent intent = getIntent();
        mActivityFlag = intent.getIntExtra(ACTIVITY_FLAG, 0);

        mPreferences = getSharedPreferences("calendar", MODE_PRIVATE);
        mEditor = mPreferences.edit();

        mNumPermissionsToRequest = 0;
        checkPermissions();
    }

    public void dismissDialog() {
        if (mSecureCheckDialog != null) {
            mSecureCheckDialog.dismiss();
            mSecureCheckDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        dismissDialog();
    }

    private void checkPermissions() {

        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestReadCalendarPermission = true;
            mNumPermissionsToRequest++;
        } else {
            mFlagHasReadCalendarPermission = true;
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestReadStoragePermission = true;
            mNumPermissionsToRequest++;
        } else {
            mFlagHasReadStoragePermission = true;
        }

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestContactsPermission = true;
            mNumPermissionsToRequest++;
        } else {
            mFlagHasContactsPermission = true;
        }

        //SPRD: bug527686 Donot allow import vcs file
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestReadSMSPermission = true;
            mNumPermissionsToRequest++;
        } else {
            mFlagHasReadSMSPermission = true;
        }

        Log.d(TAG, "Calendar mHasRequest = " + mHasRequest);
        if (mNumPermissionsToRequest != 0) {
            if(mHasRequest) {
                mHasRequest = false;
                buildPermissionsRequest();
            }
        } else {
            handlePermissionsSuccess();
        }
    }

    private void buildPermissionsRequest() {
        String[] permissionsToRequest = new String[mNumPermissionsToRequest];
        int permissionsRequestIndex = 0;

        if (requestReadCalendarPermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_CALENDAR;
            mIndexReadCalendarPermission = permissionsRequestIndex;
            permissionsRequestIndex++;
        }

        if (requestReadStoragePermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_EXTERNAL_STORAGE;
            mIndexReadStoragePermission = permissionsRequestIndex;
            permissionsRequestIndex++;
        }

        if (requestContactsPermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_CONTACTS;
            mIndexContactsPermission = permissionsRequestIndex;
            permissionsRequestIndex++;
        }

        //SPRD: bug527686 Donot allow import vcs file
        if (requestReadSMSPermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_SMS;
            mIndexReadSMSPermission = permissionsRequestIndex;
        }

        Log.d(TAG, "requestPermissions count: " + permissionsToRequest.length);
        requestPermissions(permissionsToRequest, CALENDAR_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        Log.d(TAG, "Calendar onPermissionsResult counts: " + permissions.length + ":" + grantResults.length);

        mHasRequest = true;
        if (0 == permissions.length && 0 == grantResults.length) {
            //SPRD: bug522692, com.android.calendar happens ANR
            //handlePermissionsFailure();
            //return;
            startMainActivity();
            finish();
            return;
        }

        if (mNumPermissionsToRequest != permissions.length) {
            return;
        }

        if (requestReadCalendarPermission) {
            if (grantResults.length > 0 && grantResults[mIndexReadCalendarPermission] ==
                    PackageManager.PERMISSION_GRANTED) {
                mFlagHasReadCalendarPermission = true;
            } else {
                //handlePermissionsFailure();
            }
        }

        if (requestReadStoragePermission) {
            if (grantResults.length > 0 && grantResults[mIndexReadStoragePermission] ==
                    PackageManager.PERMISSION_GRANTED) {
                mFlagHasReadStoragePermission = true;
            } else {
                //handlePermissionsFailure();
            }
        }

        if (requestContactsPermission) {
            if (grantResults.length > 0 && grantResults[mIndexContactsPermission] ==
                    PackageManager.PERMISSION_GRANTED) {
                mFlagHasContactsPermission = true;
            } else {
                //handlePermissionsFailure();
            }
        }

        if (requestReadSMSPermission) {
            if (grantResults.length > 0 && grantResults[mIndexReadSMSPermission] ==
                    PackageManager.PERMISSION_GRANTED) {
                mFlagHasReadSMSPermission = true;
            } else {
                //handlePermissionsFailure();
            }
        }

        if (mFlagHasReadCalendarPermission && mFlagHasReadStoragePermission && mFlagHasContactsPermission && mFlagHasReadSMSPermission) {
            handlePermissionsSuccess();
        } else {
            if (!mFailureFlag) {
                handlePermissionsFailure();
            }
        }
    }

    private void handlePermissionsSuccess() {
        Log.d(TAG, "handlePermissionsSuccess");
        Intent intent = new Intent();
        if (1 == mActivityFlag) {
            /* SPRD: bug520815, check runtime permissions @} */
            Intent updateIntent = new Intent(Intent.ACTION_PROVIDER_CHANGED,
                    CalendarContract.CONTENT_URI);
            this.sendBroadcast(updateIntent, null);
            /* @} */
            intent.setClass(this, AllInOneActivity.class);
        } else if (2 == mActivityFlag) {
            intent.setClass(this, OpenVcalendar.class);
        } else {
            return;
        }
        startActivity(intent);
        finish();
    }

    private void handlePermissionsFailure() {
        Log.d(TAG, "handlePermissionsFailure");
        mFailureFlag = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.toast_calendar_internal_error))
                .setMessage(getResources().getString(R.string.error_permissions))
                .setCancelable(false)
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            mFailureFlag = false;
                            finish();
                        }
                        return true;
                    }
                })
                .setPositiveButton(getResources().getString(R.string.dialog_dismiss),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mFailureFlag = false;
                        finish();
                    }
                });
        mSecureCheckDialog = builder.show();
    }

    private void startMainActivity() {
        Intent intent = new Intent();
        intent.setClass(this, AllInOneActivity.class);
        startActivity(intent);
    }

    public boolean isPermissionResumed() {
        return isResumed();
    }

    /* SPRD 531630, com.android.calendar happens JavaCrash,log:java.lang.IllegalStateException. @{ */
    @Override
    public void onBackPressed() {
        if (isResumed()) {
            super.onBackPressed();
        }
    }
    /* @} */
}
