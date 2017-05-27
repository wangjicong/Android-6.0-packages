/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.htmlviewer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.provider.Browser;
import android.content.ActivityNotFoundException;

import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/* SPRD: 494238 import @{ */
import android.Manifest;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.view.KeyEvent;
/* SPRD: 537156 import @{ */
import java.io.BufferedInputStream;
import java.io.FileInputStream;
/* @} */
import java.util.Arrays;
/* @} */

/**
 * Simple activity that shows the requested HTML page. This utility is
 * purposefully very limited in what it supports, including no network or
 * JavaScript.
 */
public class HTMLViewerActivity extends Activity {
    private static final String TAG = "HTMLViewer";

    private WebView mWebView;
    private View mLoading;

    /* SPRD: 494238 add request permission code @{ */
    private final static boolean DBG = true;
    private final static int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 0;
    private final static int REQUIRE_STORAGE_PERMISSION_DIALOG = 1;
    private String mUrl = "";
    private AlertDialog mAlertDialog = null;
    /* @} */
    /* SPRD: 537156 the Chinese text shows with messy when
     * this text is opened with HTMLViewer.@{ */
    private final String MIMETYPE_TEXTPLAIN = "text/plain";
    /* @} */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mWebView = (WebView) findViewById(R.id.webview);
        mLoading = findViewById(R.id.loading);

        mWebView.setWebChromeClient(new ChromeClient());
        mWebView.setWebViewClient(new ViewClient());

        WebSettings s = mWebView.getSettings();
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSavePassword(false);
        s.setSaveFormData(false);
        s.setBlockNetworkLoads(true);

        // Javascript is purposely disabled, so that nothing can be
        // automatically run.
        s.setJavaScriptEnabled(false);
        s.setDefaultTextEncodingName("utf-8");

        final Intent intent = getIntent();
        if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(Intent.EXTRA_TITLE));
        }

        /**
         * SPRD: 494238 check storage permission
         * @orig mWebView.loadUrl(String.valueOf(intent.getData()));
         * @{
         */
        Uri uri = intent.getData();
        if (uri != null) {
            /* SPRD: 537156 the Chinese text shows with messy when
             * this text is opened with HTMLViewer.@{ */
            if (MIMETYPE_TEXTPLAIN.equals(intent.getType())) {
                String encodingType = null;
                try {
                    encodingType = codeString(uri.getPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!encodingType.equals("")) {
                    mWebView.getSettings().setDefaultTextEncodingName(encodingType);
                }
            }
            /* @} */
            mUrl = String.valueOf(uri);
            loadUrlForWebView();
        } else {
            finish();
        }
        /* @} */
    }

    /* SPRD: 537156 the Chinese text shows with messy when
     * this text is opened with HTMLViewer.@{ */
    private String codeString(String fileName) throws Exception {
        BufferedInputStream bin = null;
        int p = 0;
        try {
            bin = new BufferedInputStream(new FileInputStream(fileName));
            p = (bin.read() << 8) + bin.read();
        } catch (Exception e) {
            Log.e(TAG, "IOException when get encode type", e);
        } finally {
            if(bin != null) {
                bin.close();
            }
        }
        String code = null;
        switch (p) {
        case 0xefbb:
            code = "UTF-8";
            break;
        case 0xfffe:
            code = "Unicode";
            break;
        case 0xfeff:
            code = "UTF-16BE";
            break;
        case 0x5c75:
            code = "ANSI|ASCII";
            break;
        default:
            code = "GBK";
        }
        return code;
    }
    /* @} */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
    }

    private class ChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (!getIntent().hasExtra(Intent.EXTRA_TITLE)) {
                HTMLViewerActivity.this.setTitle(title);
            }
        }
    }

    private class ViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            mLoading.setVisibility(View.GONE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                WebResourceRequest request) {
            final Uri uri = request.getUrl();
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())
                    && uri.getPath().endsWith(".gz")) {
                Log.d(TAG, "Trying to decompress " + uri + " on the fly");
                try {
                    final InputStream in = new GZIPInputStream(
                            getContentResolver().openInputStream(uri));
                    final WebResourceResponse resp = new WebResourceResponse(
                            getIntent().getType(), "utf-8", in);
                    resp.setStatusCodeAndReasonPhrase(200, "OK");
                    return resp;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to decompress; falling back", e);
                }
            }
            return null;
        }

        /* SPRD: 497162 add shouldOverrideUrlLoading @{ */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent;
            // Perform generic parsing of the URI to turn it into an Intent.
            Log.d(TAG, "start url is " + url);

            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ex) {
                Log.w(TAG, "Bad URI " + url + ": " + ex.getMessage());
                return false;
            }
            // Sanitize the Intent, ensuring web pages can not bypass browser
            // security (only access to BROWSABLE activities).
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            Intent selector = intent.getSelector();
            if (selector != null) {
                selector.addCategory(Intent.CATEGORY_BROWSABLE);
                selector.setComponent(null);
            }
            // Pass the package name as application ID so that the intent from
            // the same application can be opened in the same tab.
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, view.getContext().getPackageName());

            try {
                view.getContext().startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Log.w(TAG, "No application can handle " + url);
                return false;
            }
            return true;
        }
        /* @} */
    }

    /* SPRD: 494238 check storage permission  @{ */
    private void loadUrlForWebView() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            mWebView.loadUrl(mUrl);
        } else {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private void showAlertDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HTMLViewerActivity.this);
        switch (id) {
            case REQUIRE_STORAGE_PERMISSION_DIALOG:
                builder.setTitle(R.string.app_label)
                    .setMessage(R.string.storage_permission_missed_hint)
                    .setNegativeButton(R.string.dialog_dismiss,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                    .setOnKeyListener(new Dialog.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                finish();
                            }
                            return true;
                        }
                     })
                    .setCancelable(false);
                break;
            default:
                break;
        }

        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_EXTERNAL_STORAGE:
                if (DBG) Log.d(TAG, "onRequestPermissionsResult(): permissions are "
                    + Arrays.toString(permissions) + ", result is " + Arrays.toString(grantResults));
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mWebView.loadUrl(mUrl);
                } else {
                    showAlertDialog(REQUIRE_STORAGE_PERMISSION_DIALOG);
                }
                break;
            default:
                break;
        }
    }
    /* @} */
}
