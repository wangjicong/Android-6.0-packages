// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Package path can be changed, but should match <manifest package="..."> in AndroidManifest.xml.
package com.android.partnerbrowsercustomizations.example;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.SystemProperties;

// Class name can be changed, but should match <provider android:name="..."> in AndroidManifest.xml.
public class PartnerHomepageProviderExample extends ContentProvider {
    // "http://www.android.com/" is just an example. Please replace this to actual homepage.
    // Other strings in this class must remain as it is.
    private static String HOMEPAGE_URI = "https://www.google.com";
    private static final int URI_MATCH_HOMEPAGE = 0;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI("com.android.partnerbrowsercustomizations", "homepage",
                URI_MATCH_HOMEPAGE);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // In fact, Chrome does not call this.
        // Just a recommaned ContentProvider practice in general.
        switch (URI_MATCHER.match(uri)) {
            case URI_MATCH_HOMEPAGE:
                return "vnd.android.cursor.item/partnerhomepage";
            default:
                return null;
        }
    }

    private String getHomepage(){
        //String operator = SystemProperties.get("ro.operator");
        //android.util.Log.d("mgh","****oper:"+operator);
        //if (operator.equalsIgnoreCase("za")){
        //    HOMEPAGE_URI ="http://live.vodafone.com" ; 
        //}else if (operator.equalsIgnoreCase("uk")){
        //    HOMEPAGE_URI ="http://myweb.vodafone.co.uk" ; 
        //}else if (operator.equalsIgnoreCase("gr")){
        //    HOMEPAGE_URI ="http://myweb.vodafone.gr" ; 
        //}else if (operator.equalsIgnoreCase("nz")){
        //    HOMEPAGE_URI ="www.vodafone.co.nz" ; 
        //}else if (operator.equalsIgnoreCase("ro")){
        //    HOMEPAGE_URI ="http://live.vodafone.com" ;  
        //}else if (operator.equalsIgnoreCase("al")){
        //    HOMEPAGE_URI ="http://www.vodafone.al" ;
        //}
        return getContext().getString(R.string.homepage_uri);//HOMEPAGE_URI; //yanghua modify
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        switch (URI_MATCHER.match(uri)) {
            case URI_MATCH_HOMEPAGE:
                MatrixCursor cursor = new MatrixCursor(new String[] { "homepage" }, 1);
                //cursor.addRow(new Object[] { HOMEPAGE_URI });
                cursor.addRow(new Object[] { getHomepage() });
                return cursor;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
