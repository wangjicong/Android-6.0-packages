/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ParseException;
import android.net.WebAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;
import com.android.browser.util.Util;
import android.text.InputFilter;

public class AddSpeedDialItem extends Activity {

    private final static String LOGTAG = "SpeedDial";

    private EditText    mTitle;
    private EditText    mAddress;
    private TextView    mButton;
    private View        mCancelButton;
    private boolean     mEditingExisting;
    private Bundle      mMap;
    private String      mOriginalUrl;

    // Message IDs
    private static final int SAVE_SPEED_ITEM = 100;

    private Handler mHandler;

    private View.OnClickListener mSaveSpeedDial = new View.OnClickListener() {
        public void onClick(View v) {
            if (save()) {
                finish();
            }
        }
    };

    private View.OnClickListener mCancel = new View.OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.add_speed_dial);
        setTitle(R.string.add_favorite);

        String title = null;
        String url = null;
        mMap = getIntent().getExtras();
        if (mMap != null) {
            Bundle b = mMap.getBundle("item");
            if (b != null) {
                mMap = b;
                mEditingExisting = true;
                setTitle(R.string.edit_favorite);
            }
            title = mMap.getString("title");
            url = mOriginalUrl = mMap.getString("url");
        }

        mTitle = (EditText) findViewById(R.id.title);
        mTitle.setText(title);
        /*SPRD :373437 Fixbug,add url toask start@{*/
        InputFilter[] titleFilter = Util.getLengthFilter(this,50);
        mTitle.setFilters(titleFilter);
        /*SPRD :373437 Fixbug,add url toask end@}*/
        mAddress = (EditText) findViewById(R.id.address);
        mAddress.setText(url);
        /*SPRD :373437 Fixbug,add url toask start@{*/
        InputFilter[] addressFilter = Util.getLengthFilter(this,1024);
        mAddress.setFilters(addressFilter);
        /*SPRD :373437 Fixbug,add url toask end@}*/

        View.OnClickListener accept = mSaveSpeedDial;
        mButton = (TextView) findViewById(R.id.OK);
        mButton.setOnClickListener(accept);

        mCancelButton = findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(mCancel);

        if (!getWindow().getDecorView().isInTouchMode()) {
            mButton.requestFocus();
        }
    }

    /**
     * Runnable to save a bookmark, so it can be performed in its own thread.
     */
    private class SaveSpeedDialItemRunnable implements Runnable {
        private Message mMessage;
        public SaveSpeedDialItemRunnable(Message msg) {
            mMessage = msg;
        }
        public void run() {
            // Unbundle bookmark data.
            Bundle bundle = mMessage.getData();
            String title = bundle.getString("title");
            String url = bundle.getString("url");
            Log.i(LOGTAG,"SaveSpeedDialItemRunnable begin,url,title:"+url+", "+title);

            // Save to the bookmarks DB.
            try {
                final ContentResolver cr = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(SpeedDial.TITLE, title);
                values.put(SpeedDial.URL, url);
                cr.insert(SpeedDial.CONTENT_URI, values);
                Log.i(LOGTAG, "SaveSpeedDialItemRunnable begin");
//                DownloadFavicon.donwnloadFavicon(null, url, cr);
                mMessage.arg1 = 1;
            } catch (Exception e) {
                mMessage.arg1 = 0;
            }
            mMessage.sendToTarget();
            Log.i(LOGTAG,"SaveSpeedDialItemRunnable end");
        }
    }

    private void createHandler() {
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case SAVE_SPEED_ITEM:
                            if (1 == msg.arg1) {
                                Toast.makeText(AddSpeedDialItem.this, R.string.favorite_saved,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AddSpeedDialItem.this, R.string.favorite_not_saved,
                                        Toast.LENGTH_LONG).show();
                            }
                            break;
                    }
                }
            };
        }
    }

    /**
     * Parse the data entered in the dialog and post a message to update the bookmarks database.
     */
    boolean save() {
        createHandler();

        String title = mTitle.getText().toString().trim();
        String unfilteredUrl = UrlUtils.fixUrl(mAddress.getText().toString());
        boolean emptyTitle = title.length() == 0;
        boolean emptyUrl = unfilteredUrl.trim().length() == 0;
        Resources r = getResources();
        if (emptyTitle || emptyUrl) {
            if (emptyTitle) {
                mTitle.requestFocus();
                mTitle.setError(r.getText(R.string.favorite_needs_title));
            }
            if (emptyUrl) {
                mAddress.requestFocus();
                mAddress.setError(r.getText(R.string.favorite_needs_url));
            }
            return false;
        }
        String url = unfilteredUrl.trim();
        try {
            // We allow bookmarks with a javascript: scheme, but these will in most cases
            // fail URI parsing, so don't try it if that's the kind of bookmark we have.

            if (!url.toLowerCase().startsWith("javascript:")) {
                String scheme = null;
                try {
                    URI uriObj = new URI(url);
                    scheme = uriObj.getScheme();
                } catch (URISyntaxException e) {
                    if(e.getMessage().startsWith("Illegal character in scheme")){
                        Log.i(LOGTAG,"ignore invalid bookmark schema:" + e);
                    }else{
                        throw e;
                    }

                }
                if (!Bookmarks.urlHasAcceptableScheme(url)) {
                    // If the scheme was non-null, let the user know that we
                    // can't save their bookmark. If it was null, we'll assume
                    // they meant http when we parse it in the WebAddress class.
                    if (scheme != null) {
                        mAddress.setError(r.getText(R.string.favorite_cannot_save_url));
                        return false;
                    }
                    WebAddress address;
                    try {
                        address = new WebAddress(unfilteredUrl);
                    } catch (ParseException e) {
                        throw new URISyntaxException("", "");
                    }
                    if (address.getHost().length() == 0) {
                        throw new URISyntaxException("", "");
                    }
                    url = address.toString();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
            return false;
        }

        if (mEditingExisting) {
            mMap.putString("title", title);
            mMap.putString("url", url);
            try{
                String o = DownloadFavicon.getFaviconUrl(mOriginalUrl);
                String u = DownloadFavicon.getFaviconUrl(url);
                mMap.putBoolean("invalidateFavicon", !o.equals(u));
            }catch(ParseException e){
                mMap.putBoolean("invalidateFavicon", !url.equals(mOriginalUrl));
            }
            setResult(RESULT_OK, new Intent().putExtras(mMap));
        } else {
            // Post a message to write to the DB.
            Bundle bundle = new Bundle();
            bundle.putString("title", title);
            bundle.putString("url", url);
            Message msg = Message.obtain(mHandler, SAVE_SPEED_ITEM);
            msg.setData(bundle);
            // Start a new thread so as to not slow down the UI
            Thread t = new Thread(new SaveSpeedDialItemRunnable(msg));
            t.start();
            setResult(RESULT_OK);
        }
        return true;
    }

}
