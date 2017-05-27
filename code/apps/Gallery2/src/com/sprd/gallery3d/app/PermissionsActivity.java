package com.sprd.gallery3d.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.R;
/**
 * SPRD : Add PermissionsActivity for check gallery permission
 */

public class PermissionsActivity extends Activity{
    private static final String TAG = "PermissionsActivity";

    private static int PERMISSION_REQUEST_CODE = 1;
    private int mIndexPermissionRequestStorage;
    private boolean mShouldRequestStoragePermission;
    /** SPRD:Bug474639 check phone permission @{ */
    private boolean mShouldRequestPhonePermission;
    private int mIndexPermissionRequestPhone;
    private static String PERMISSION_READ_PHONE = "android.permission.READ_PHONE_STATE";
    /**@}*/
    private static String PERMISSION_READ_EXTERNAL_STORAGE = "Manifest.permission.READ_EXTERNAL_STORAGE";
    private int mNumPermissionsToRequest;
    private boolean mFlagHasStoragePermission;
    private boolean mFlagHasPhonePermission;
    private boolean mDialogShow;

    /**SPRD:Bug510007  check storage permission  @{*/
    public static final String UI_START_BY="permission-activity-start-by";
    /**@}*/
    private int mStartFrom;
    // 0:gallery; 1: video; -1;other app
    public static final int START_FROM_GALLERY = 0;
    public static final int START_FROM_VIDEO = 1;
    public static final int START_FROM_MOVIE = 2;
    public static final int START_FROM_OTHER = -1;
    //for result
    public static final int RESULT_FOR_MOVIE = 1;

    private Bundle mSavedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.permissions_layout);
        mDialogShow =  false;
        mSavedInstanceState = getIntent().getExtras();
        mStartFrom = getIntent().getIntExtra(UI_START_BY, START_FROM_OTHER);
        checkPermissions();
        mNumPermissionsToRequest = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void checkPermissions() {
        // check read_external_storage permission first
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            mNumPermissionsToRequest++;
            mShouldRequestStoragePermission = true;
        } else {
            mFlagHasStoragePermission = true;
        }

        if (START_FROM_MOVIE == mStartFrom || START_FROM_VIDEO == mStartFrom) {
            /** SPRD:Bug474639 check phone permission @{ */
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                mNumPermissionsToRequest++;
                mShouldRequestPhonePermission = true;
            } else {
                mFlagHasPhonePermission = true;
            }
            /**@}*/
        }

        if (mNumPermissionsToRequest != 0) {
            if (!mDialogShow) {
                buildPermissionsRequest();
            } else {
                handlePermissionsFailure();
            }
        } else {
            handlePermissionsSuccess();
        }
    }

    private void buildPermissionsRequest() {
        String[] permissionsToRequest = new String[mNumPermissionsToRequest];
        int permissionsRequestIndex = 0;

        if (mShouldRequestStoragePermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_EXTERNAL_STORAGE;
            mIndexPermissionRequestStorage = permissionsRequestIndex;
            permissionsRequestIndex++;
        }

        /**SPRD:Bug474639  check phone permission  @{*/
        if (mShouldRequestPhonePermission) {
            permissionsToRequest[permissionsRequestIndex] = PERMISSION_READ_PHONE;
            mIndexPermissionRequestPhone = permissionsRequestIndex;
            permissionsRequestIndex++;
        }
        /**@}*/
        requestPermissions(permissionsToRequest, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mDialogShow = true;
        if (mShouldRequestStoragePermission) {
            if (grantResults.length > 0 && grantResults[mIndexPermissionRequestStorage] ==
                    PackageManager.PERMISSION_GRANTED) {
                mFlagHasStoragePermission = true;
            } else {
                handlePermissionsFailure();
            }
        }
        /**SPRD:Bug474639  check phone permission  @{*/
        if(mShouldRequestPhonePermission){
            if (grantResults.length > 0 && grantResults[mIndexPermissionRequestPhone] ==
                    PackageManager.PERMISSION_GRANTED) {
                mFlagHasPhonePermission = true;
            } else {
                handlePermissionsFailure();
            }
        }
        /**@}*/
        switch (mStartFrom) {
            case START_FROM_GALLERY:
                if (mFlagHasStoragePermission) {
                    handlePermissionsSuccess();
                }
                break;
            case START_FROM_VIDEO:
            case START_FROM_MOVIE:
                if (mFlagHasStoragePermission && mFlagHasPhonePermission) {
                    handlePermissionsSuccess();
                }
                break;
        }
    }

    private void handlePermissionsSuccess() {
        /** SPRD:Bug510007 check storage permission @{ */
        Intent intent = null;
        switch (mStartFrom) {
            case START_FROM_GALLERY:
                intent = new Intent(this, GalleryActivity.class);
                break;
            case START_FROM_VIDEO:
                intent = new Intent(this, VideoActivity.class);
                break;
            case START_FROM_MOVIE:
                intent = new Intent();
                if (mSavedInstanceState != null) {
                    intent.putExtras(mSavedInstanceState);
                }
                setResult(RESULT_OK, intent);
            default:
                finish();
                return;
        }
        /** @}*/
        /* SPRD: bug 517885 , lose of the read or write uri permissions*/
        if (Intent.isAccessUriMode(getIntent().getFlags())) {
            intent.setFlags(getIntent().getFlags());
        }
        /* @} */
        if(getIntent().getAction() != null){
            intent.setAction(getIntent().getAction());
        }
        if (getIntent().getType() != null) {
            intent.setType(getIntent().getType());
        }
        if (getIntent().getData() != null) {
            intent.setData(getIntent().getData());
        }
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        if(Intent.ACTION_GET_CONTENT.equalsIgnoreCase(getIntent().getAction())){
            Toast.makeText(this, R.string.gallery_premission_change, Toast.LENGTH_SHORT).show();
        }else{
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void handlePermissionsFailure() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.error_permissions))
                .setCancelable(false)
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            finish();
                        }
                        return true;
                    }
                })
                .setPositiveButton(getResources().getString(R.string.dialog_dismiss),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        switch (mStartFrom) {
            case START_FROM_GALLERY:
                alertBuilder.setTitle(R.string.gallery_error_title);
                alertBuilder.show();
                break;
            case START_FROM_VIDEO:
            case START_FROM_MOVIE:
                alertBuilder.setTitle(R.string.videoplayer_error_title);
                alertBuilder.show();
                break;
        }
        mDialogShow = true;
    }

    /*SPRD: bug 532248,Can not perform this action after onSaveInstanceState @{ */
    @Override
    public void onBackPressed() {
        if (isResumed()) {
            super.onBackPressed();
        }
    };
    /* @} */

    @Override
    protected void onDestroy() {
        mSavedInstanceState = null;
        super.onDestroy();
    }
}
