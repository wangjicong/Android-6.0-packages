/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import java.net.URI;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.ucamera.ucomm.sns.ShareItem.ItemsFilter;
import com.ucamera.ucomm.sns.integration.ShareUtils;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareFile;
import com.ucamera.ucomm.sns.tencent.QQVatar;
import com.ucamera.ucomm.sns.tencent.SetWeixin;
import com.ucamera.ugallery.ImageGallery;
import com.ucamera.ugallery.ViewImage;

public class ShareActivity extends Activity {
    private static final String TAG = "ShareActivity";

    private Button mButtonNetPrint;
    private Button mButtonShare;
    private Button mButtonShareMore;
    private Button mButtonShareLine;
    private Button mButtonPrivacy;
    private Button mButtonAccount;
    private ImageView mImgPound;
    private CheckBox mCheckLocation;

    private EditText mEditMessage;
    private ImageView mImageViewThumbnail;
    private ShareItemView[] mShareItems;

    private Uri mUriData;
    private String mStrLocation;
    private String mLatitude;
    private String mLongitude;
    private Button mPhotoPrint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sns_share);
        attachShareItems();
        loadBitmapFromUri(getIntent().getData());
        mEditMessage = (EditText) findViewById(R.id.edit_message);
        mEditMessage.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(v.getId() == R.id.edit_message) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch (event.getAction()&MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                    }
                }
                return false;
            }
        });
//        loadEventMessage();

        mButtonShare = (Button) findViewById(R.id.btn_share);
        if(mShowTencentShare) {
            findViewById(R.id.direct_share_line).setVisibility(View.VISIBLE);
            findViewById(R.id.tencent_share).setVisibility(View.VISIBLE);
            TextView avaTextView = (TextView) findViewById(R.id.set_ava);
            Button btn_back = (Button) findViewById(R.id.sns_btn_back);
            btn_back.setVisibility(View.VISIBLE);
            btn_back.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ShareActivity.this.finish();
                }
            });
            avaTextView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!Util.checkNetworkShowAlert(ShareActivity.this)){
                        return;
                    }
                    QQVatar.getInstance(ShareActivity.this).share(mUriData, ShareActivity.this);
                }
            });
            TextView weixin_button = (TextView) findViewById(R.id.set_weixin);
            weixin_button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!Util.checkNetworkShowAlert(ShareActivity.this)){
                        return;
                    }
                    SetWeixin.getInstance(ShareActivity.this).sendToFriend(getRealPathFromURI(mUriData));
                }
            });
            TextView weixin_quan = (TextView) findViewById(R.id.set_weixin_quan);
            weixin_quan.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!Util.checkNetworkShowAlert(ShareActivity.this)){
                        return;
                    }
                    SetWeixin.getInstance(ShareActivity.this).sendToFriendQuan(getRealPathFromURI(mUriData));
                }
            });
        } else {
            mButtonShare.setBackgroundResource(R.drawable.sns_bg_button_other);
            mEditMessage.setBackgroundResource(R.drawable.sns_bg_input_other);
        }
        mButtonShare.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!Util.checkNetworkShowAlert(ShareActivity.this))
                    return;

                /*
                 * BUG FIX: 658
                 * BUG CAUSE: in some case, the mUriData is null
                 * FIX COMMENT: if it is null, alert the user
                 * DATE: 2012-05-18
                 */
                if (mUriData == null) {
                    Util.showAlert(ShareActivity.this, getString(R.string.sns_title_share),
                            getString(R.string.sns_msg_missing_share_pic));
                    return;
                }

                ArrayList<ShareItemView> targets = new ArrayList<ShareItemView>();
                for (ShareItemView item : mShareItems) {
                    if (item.isChecked()) {
                        targets.add(item);
                    }
                }

                if (targets.isEmpty()) {
                    Util.showAlert(ShareActivity.this, getString(R.string.sns_title_share),
                            getString(R.string.sns_msg_please_choose_share_account));
                    return;
                }

                String message = mEditMessage.getText().toString();
                ShareContent content = new ShareContent(getString(R.string.sns_ucam_link), message);
                content.setUCamShare(getString(R.string.sns_ucam_share));
                if (mCheckLocation.isChecked()
                        && !TextUtils.isEmpty(mLatitude)
                        && !TextUtils.isEmpty(mLongitude)) {
                    content.setLocation(mLatitude, mLongitude);
                }
                new ShareTask(ShareActivity.this, targets, content, new ShareFile(
                        ShareActivity.this, mUriData)).execute();
            }
        });
        if(mShowLineShare) {
            mButtonShareLine = (Button) findViewById(R.id.btn_line_share);
            mButtonShareLine.setVisibility(View.VISIBLE);
            mButtonShareLine.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!Util.checkNetworkShowAlert(ShareActivity.this))
                        return;
                    if (mUriData == null) {
                        Util.showAlert(ShareActivity.this, getString(R.string.sns_title_share),
                                getString(R.string.sns_msg_missing_share_pic));
                        return;
                    }
                    try {
                        Intent intent = new Intent();
                        intent.setData(Uri.parse("line://msg/image"+getRealPathFromURI(mUriData)));
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "not install line app");
                        Util.showAlert(ShareActivity.this, getString(R.string.sns_title_share),
                                getString(R.string.sns_msg_no_install_line_app));
                    }
                }
            });
        }

        /*
         *BUG FIX: 4007
         *BUG COMMENT: TOLOT&Shimauma Print Integration
         *FIX DATE: 2013-05-27
         */
        if(sShowNetPrintShare) {
            mButtonNetPrint = (Button) findViewById(R.id.btn_net_print);
            mButtonNetPrint.setVisibility(View.VISIBLE);
            mButtonNetPrint.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    NetLayoutDialog dialog = new NetLayoutDialog(ShareActivity.this, mUriData == null? null: getRealPathFromURI(mUriData) ,mEditMessage.getText().toString());
                    dialog.showDialog();
                }
            });
            if(getIntent().getBooleanExtra("isIDPhotoModule", false)) {
                mPhotoPrint = (Button) findViewById(R.id.btn_photo_print);
                mPhotoPrint.setVisibility(View.VISIBLE);
                mPhotoPrint.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        new IDphotoPrintDialog(ShareActivity.this).show();
                    }
                });
                ImageButton mBackCamera = (ImageButton) findViewById(R.id.sns_btn_back_idphoto);
                mBackCamera.setVisibility(View.VISIBLE);
                mBackCamera.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        com.ucamera.ucomm.emptyimpl.smartcut.SmartCutEngineUtil.destroyInstance();
                        Intent intent  = new Intent();
                        intent.setClassName(getApplicationContext(), "com.ucamera.ucam.CameraActivity");
                        intent.setAction("android.intent.action.MAIN");
                        intent.addCategory("android.intent.category.LAUNCHER");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                });
            }
        }

        mButtonShareMore = (Button) findViewById(R.id.btn_more_share);
        mButtonShareMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareUtils.otherShare(ShareActivity.this, mUriData, null, Bitmap.CompressFormat.JPEG);
            }
        });
        mImageViewThumbnail = (ImageView) findViewById(R.id.img_thumb);
        mImageViewThumbnail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        mButtonPrivacy = (Button) findViewById(R.id.btn_privacy);
        mButtonPrivacy.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(ShareActivity.this)
                        .setTitle(R.string.sns_title_privacy)
                        .setMessage(R.string.sns_msg_privacy)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        mButtonAccount = (Button) findViewById(R.id.btn_account_settings);
        mButtonAccount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ShareActivity.this, AccountsActivity.class));
            }
        });

        mImgPound = (ImageView) findViewById(R.id.img_pound);
        mImgPound.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int pos = mEditMessage.getSelectionStart();
                if (pos == -1) {
                    pos = mEditMessage.length();
                }
                mEditMessage.getText().insert(pos, "##");
                if(pos <  mEditMessage.length()){
                    mEditMessage.setSelection(pos + 1);
                }
            }
        });

        mCheckLocation = (CheckBox) findViewById(R.id.chk_location);
        /*
         * FIX BUG: 3659
         * FIX DATE: 2013-04-26
         */
        if(sHideLocation) {
            mCheckLocation.setVisibility(View.INVISIBLE);
        }
        mCheckLocation.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    DialogInterface.OnClickListener callback = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                updateLoactionUI();
                            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                mCheckLocation.setChecked(false);
                            }
                        }
                    };

                    if (mPermissionNotice == null || !mPermissionNotice.showDialogIfNeeded(ShareActivity.this, callback)){
                        updateLoactionUI();
                    }
                } else{
                    mCheckLocation.setText(R.string.sns_label_hide_location);
                }
            }
        });
        //if sdk version is 3.0 or higher, upload the image by thread will throws android.os.NetworkOnMainThreadException
        try {
            if(android.os.Build.VERSION.SDK_INT > 8) {
                //sdk version more than 8, there is StrictMode class.
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .permitDiskReads()
                    .permitDiskWrites()
                    .detectNetwork()
                    .penaltyLog().build());
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            }
        } catch (Exception e) {
            Log.d(TAG, "error on strickmode settings.");
        }
        if(QQVatar.getInstance(this) != null) {
            QQVatar.getInstance(this).initActivity(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // back from account setting activity
        for (ShareItemView item : mShareItems) {
            if (!item.getShareService().isAuthorized()) {
                item.setChecked(false);
            }
        }
    }

    private void updateLoactionUI() {
        if (TextUtils.isEmpty(mStrLocation)){
            mCheckLocation.setText(R.string.sns_msg_getting_location);
            getLocation();
        } else{
            mCheckLocation.setText(mStrLocation);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*
     * Do NOT remove, used for Stat
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
//        StatApi.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if(mAdapter != null) {
//            mAdapter.stopImageLoader();
//        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*
         * buf fix: 1983
         * fix comment: we consume the MENU key event
         * date: 2012-11-21
         */
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void attachShareItems() {

        ShareItem[] items = ShareItem.sortedValues();
        mShareItems = new ShareItemView[items.length];

        TableLayout shareItems = (TableLayout)findViewById(R.id.sns_share_items);
        final int COLS = 4;
        final int ROWS = items.length / COLS + (items.length % COLS == 0 ? 0 : 1);
        LayoutInflater lf = getLayoutInflater();
        for (int r = 0; r < ROWS; r++) {
            TableRow tablerow = new TableRow(ShareActivity.this);
            tablerow.setLayoutParams(new TableLayout.LayoutParams());
            shareItems.addView(tablerow);
            for (int c = 0; c < COLS; c++) {
                int index = r*COLS + c;
                ShareItemView view;
                if (index < items.length) {
                    view = ShareItemView.create(ShareActivity.this, items[index], tablerow);
                    mShareItems[index] = view;
                } else {
                    view = ShareItemView.create(ShareActivity.this, null, tablerow);
                }
                if (c < COLS -1  ) {
                    lf.inflate(R.layout.sns_v_line, tablerow);
                }
            }
            lf.inflate(R.layout.sns_h_line, shareItems);
        }
    }

    private void getLocation(){
        LocationHelper locationHelper = new LocationHelper(this);
        LocationHelper.TextLocationCallBack listener = new LocationHelper.TextLocationCallBack(){
            public void obtainLocation(String textString, String latitude, String longitude){
                mStrLocation = textString;
                mLatitude = latitude;
                mLongitude = longitude;
                Log.d(TAG, "lat:" + latitude + ", long:" + longitude);
                if ((mCheckLocation != null)&&(mCheckLocation.isChecked())){
                    mCheckLocation.setText(mStrLocation);
                }
            }
            public void opError(int errCode){
                Toast.makeText(ShareActivity.this, R.string.sns_toast_location_error, Toast.LENGTH_LONG).show();
                mCheckLocation.setChecked(false);
            }
        };
        locationHelper.getTextLocation(mUriData, listener);
    }

    private void loadBitmapFromUri(final Uri uri) {
        if (uri == null) {
            Log.w(TAG, "No image uri provided!");
            return;
        }
        Log.d(TAG,"Load " + uri);
        mUriData = uri;
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return loadBitmap(uri);
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null) {
                    mImageViewThumbnail.setImageBitmap(result);
                } else {
                    mImageViewThumbnail.setImageResource(R.drawable.sns_thumbnail);
                    Toast.makeText(ShareActivity.this, getString(R.string.text_image_share_damage), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }.execute();
    }

    private Bitmap loadBitmap(Uri uri) {
        if (uri == null) return null;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapUtil.create(this).createBitmap(uri);
            if(bitmap != null) {
                int w = getResources().getDimensionPixelSize(R.dimen.sns_thumbnail_width);
                int h = getResources().getDimensionPixelSize(R.dimen.sns_thumbnail_height);
                return ThumbnailUtils.extractThumbnail(bitmap, w, h);
            }
        } catch (Exception e) {
            Log.w(TAG, "Fail load bitmap from uri:" + uri);
        } catch (OutOfMemoryError e) {
            bitmap = null;
            Log.w(TAG, "java.lang.OutOfMemoryError:" + uri);
        }
        return null;
    }

    private static final int REQUEST_CODE_PICK = 0xABCDE;
    private void pickImage() {
        ImageGallery.showImagePicker(this, REQUEST_CODE_PICK, 1, ViewImage.IMAGE_ENTRY_UPHOTO_VALUE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_PICK:
                Parcelable[] obj = data.getParcelableArrayExtra(ImageGallery.INTENT_EXTRA_IMAGES);
                if (obj != null) {
                    Uri[] images = new Uri[obj.length];
                    System.arraycopy(obj, 0, images, 0, obj.length);
                    loadBitmapFromUri(images[0]);
                }
                break;
        }
    }

    public static PermissionNotice mPermissionNotice;
    public static final void setPermissionNotice(PermissionNotice dialog) {
        mPermissionNotice = dialog;
    }

    private static boolean sShowNetPrintShare = false;
    public static final void setShowNetPrintShare(boolean isKDDI) {
        sShowNetPrintShare = isKDDI;
    }
    public static boolean showNetPrint() {
        return sShowNetPrintShare;
    }
    private static boolean sShowGalleryNetPrint = false;
    public static final void setGalleryShowNetPrint(boolean isShow) {
        sShowGalleryNetPrint = isShow;
    }
    public static boolean showGalleryNetPrint() {
        return sShowGalleryNetPrint;
    }
    private static boolean sHideLocation = false;
    public static final void setHideLocation(boolean isLajiao) {
        ShareActivity.sHideLocation = isLajiao;
    }

    private static boolean mShowLineShare = false;
    public static final void showLineShare(boolean isSourceNext) {
        ShareActivity.mShowLineShare = isSourceNext;
    }

    protected static boolean mShowTencentShare = true;

    public static final void setShowTencentShare(boolean isShow) {
        mShowTencentShare = isShow;
    }

    protected static boolean mShowTurkeyShare = false;
    public static final void setShowTurkeyShare(boolean showTurkeyShare) {
        mShowTurkeyShare = showTurkeyShare;
    }

    public interface PermissionNotice {
        public boolean showDialogIfNeeded(Activity activity, DialogInterface.OnClickListener callback);
    }

    public String getRealPathFromURI(Uri contentUri) {
        // can post image
        String path = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, // Which columns to return
                null, // WHERE clause; which rows to return (all rows)
                null, // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        if (cursor != null) {
            try {
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    path = cursor.getString(column_index);
                }
            } finally {
                cursor.close();
            }
        }
        return path;
    }
}
