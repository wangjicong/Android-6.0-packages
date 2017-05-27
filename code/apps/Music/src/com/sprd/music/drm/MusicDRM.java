package com.sprd.music.drm;

import java.io.File;

import android.app.Activity;
import android.app.AddonManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.net.Uri;
import android.util.Log;

import com.android.music.R;
import com.android.music.TrackBrowserActivity.TrackListAdapter.ViewHolder;

public class MusicDRM {
    static MusicDRM sInstance;

    public static MusicDRM getInstance() {
        if (sInstance != null)
            return sInstance;
        sInstance = (MusicDRM) AddonManager.getDefault().getAddon(R.string.music_drm,
                MusicDRM.class);
        Log.d("DRM", "sInstance :" + sInstance);
        return sInstance;
    }

    public MusicDRM() {

    }

    public void initDRM(Context context) {

    }

    public void destroyDRM() {

    }

    public void onCreateDRMTrackBrowserContextMenu(ContextMenu menu, Cursor mTrackCursor) {

    }

    public void onContextDRMTrackBrowserItemSelected(MenuItem item, Context context) {

    }

    public void onListItemClickDRM(Context context, Cursor cursor, int position) {

    }

    public boolean isDRM(Cursor cursor, int position) {
        return false;
    }

    public ViewHolder bindViewDrm(Cursor cursor, int mDataIdx, ViewHolder vh) {
        return vh;
    }

    public void onListDRMQueryBrowserItemClick(Context context, long[] list, Cursor cursor) {

    }

    public void playDRMQueryBrowser(Context context, long[] list, Uri uri) {

    }

    public boolean isDRM(Uri uri, Context context) {
        return false;
    }

    public void onPrepareDRMMediaplaybackOptionsMenu(Menu menu) {

    }

    public void onDRMMediaplaybackOptionsMenuSelected(Context context, MenuItem item) {

    }

    public boolean openIsInvaildDrm(Cursor mCursor) {
        return false;
    }

    public String getAudioData(Cursor mCursor) {
        return null;
    }

    public boolean getAudioIsDRM(Cursor mCursor) {
        return false;
    }

    public boolean isDRM(Cursor mCursor) {
        return false;
    }

    public boolean isDRM() {
        return false;
    }
    public boolean isSupportDRMType(File file) {
        return false;
    }
}
