/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import java.util.HashSet;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Browser;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.browser.util.Util;
import android.database.sqlite.SQLiteException;

public class DefaultHomeView extends FrameLayout implements AdapterView.OnItemClickListener {

    private Controller mController;
    private Activity mActivity;
    private TitleBar mTitleBar;
    private ChangeObserver mChangeObserver;
    private LayoutInflater mFactory;
    private GridView mGridView;
    private final GridAdapter mGAdapter;
    private Cursor mCursor = null;
    private Bitmap mCapture;
    private boolean mInForeground;
    private int mPadding;
    private int mColumnWidth;
    private int mSpacing;
    private int mCaptureWidth;
    private int mCaptureHeight;
    private static final String[] PROJECTION = new String[]{SpeedDial._ID, SpeedDial.TITLE,
        SpeedDial.URL, SpeedDial.FAVICON
    };

    private HashSet<String> mSet = new HashSet<String>();

    private static String LOGTAG = "SpeedDial";

    private static final int ID_INDEX = 0;
    private static final int TITLE_INDEX = 1;
    private static final int URL_INDEX = 2;
    private static final int FAVICON_INDEX = 3;

    private static Paint sAlphaPaint = new Paint();
    static {
        sAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        sAlphaPaint.setColor(Color.TRANSPARENT);
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(LOGTAG,"onchange");
            if(mCursor == null || mCursor.isClosed()){
                return;
            }
            mGAdapter.refreshData();
        }
    }

    public DefaultHomeView(Context context, Controller controller) {
        super(context);
        Log.d(LOGTAG,"DefaultHomeView create");
        mController = controller;
        mActivity = mController.getActivity();
        mPadding = (int)context.getResources().getDimension(R.dimen.homepage_padding);
        mColumnWidth = (int)context.getResources().getDimension(R.dimen.homepage_column_width);
        mSpacing = (int)context.getResources().getDimension(R.dimen.homepage_spacing);
        //Context mContext = context;
        mCaptureWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.tab_thumbnail_width);
        mCaptureHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.tab_thumbnail_height);
        setBackgroundColor(0xFFDCDCDC);
        mChangeObserver = new ChangeObserver();
        mFactory = LayoutInflater.from(context);

        try {
            mCursor = mContext.getContentResolver().query(SpeedDial.CONTENT_URI,
                    PROJECTION, null, null, null);
        } catch (SQLiteException e) {
            Log.e(LOGTAG, "DefaultHomeView:", e);
        }
        mGAdapter = new GridAdapter(mCursor);
        mFactory.inflate(R.layout.home_page, this);
        mGridView = (GridView)findViewById(R.id.grid);
        setupGrid(mFactory);
        mInForeground = false;
        mGridView.setFocusable(true);
        mGridView.setFocusableInTouchMode(true);
    }

    void setupGrid(LayoutInflater inflater) {
        mGridView.setColumnWidth(mColumnWidth);
        mGridView.setOnItemClickListener(this);
        mGridView.setAdapter(mGAdapter);
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setVerticalSpacing(mSpacing);
        mGridView.setHorizontalSpacing(mSpacing);
        mGridView.setStretchMode(GridView.STRETCH_SPACING);
        mGridView.setDrawSelectorOnTop(true);
        mGridView.setOnItemClickListener(this);
        mGridView.setDrawingCacheEnabled(true);
        mGridView.setFocusable(true);
        mGridView.setFocusableInTouchMode(true);
        mGridView.setPadding(mPadding, mPadding, mPadding, 0);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(position >= (mGAdapter.getCount()-1) && mGAdapter.getExtraOffset() == 1){
            //add a new item
            Intent intent = new Intent(mContext, AddSpeedDialItem.class);
            mContext.startActivity(intent);
        }else{
            openSpeedItem(position);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {//SPRD:Bug 327833,make sure this view get focus.
        // TODO Auto-generated method stub
        requestFocus();
        return super.dispatchTouchEvent(ev);
    }

    void openSpeedItem(int position){
       String url = mGAdapter.getString(position, URL_INDEX);
       //mController.openUrlFromHome(url, null);
       final Tab tab = mController.getCurrentTab();
       if(tab instanceof DefaultHomeTab){
           tab.loadUrl(url, null);
       }
    }

    void editSpeedItem(int position){
        Intent intent = new Intent(mContext, AddSpeedDialItem.class);
        intent.putExtra("item", mGAdapter.getRow(position));
        mActivity.startActivityForResult(intent, Controller.EDIT_SPEEDDIAL);
    }

    void deleteSpeedItem(int positon){
        mGAdapter.deleteRow(positon);
    }

    void updateRow(Bundle extras){
        mGAdapter.updateRow(extras);
    }

    int getCount(){
        return mGAdapter.getCount();
    }

    public int getExtraOffset(){
        return mGAdapter.getExtraOffset();
    }

    synchronized public Bitmap getScreenshot(){
        if(mCapture == null){
            return capture();
        }
        return mCapture;
    }

    public void destroy(){
        Log.d(LOGTAG, "detroy home view");
        setEmbeddedTitleBar(null);
        if(mCursor != null ){
            if(mInForeground){
                mCursor.unregisterContentObserver(mChangeObserver);
            }
            mCursor.close();
            mCursor = null;
        }
        setContentMenuLister(null);
    }

    synchronized public Bitmap capture() {
        if (mCapture == null) {
            mCapture = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.RGB_565);
        }
        if (mGridView == null || mCapture == null)
            return mCapture;
        mCapture.eraseColor(0xFFDCDCDC);
        Canvas c = new Canvas(mCapture);
        final int left = mGridView.getScrollX();
        int padding = 0;
        if(mTitleBar != null){
            padding = mGridView.getPaddingTop() - mPadding;
        }
        final int top = mGridView.getScrollY() + padding;
        int state = c.save();
        c.translate(-left, -top);
        float scale = mCaptureWidth / (float) mGridView.getWidth();
        //add scale of height for horizontal screen
        float scaleH = mCaptureHeight/ (float) mGridView.getHeight();
        c.scale(scale, scaleH, left, top);
        mGridView.draw(c);
        c.restoreToCount(state);
        // manually anti-alias the edges for the tilt
        c.drawRect(0, 0, 1, mCapture.getHeight(), sAlphaPaint);
        c.drawRect(mCapture.getWidth() - 1, 0, mCapture.getWidth(), mCapture.getHeight(),
                sAlphaPaint);
        c.drawRect(0, 0, mCapture.getWidth(), 1, sAlphaPaint);
        c.drawRect(0, mCapture.getHeight() - 1, mCapture.getWidth(), mCapture.getHeight(),
                sAlphaPaint);
        //Fix Bug178550 on 20130627
        //c.setBitmap(null);
        return mCapture;
    }

    public void putInForeground() {
        if(mInForeground){
            return;
        }
        mSet.clear();
        mInForeground = true;
        setContentMenuLister(mActivity);
        if (mCursor != null) {
            mCursor.registerContentObserver(mChangeObserver);
        }
    }

    public void putInBackground(){
        if(!mInForeground){
            return;
        }
        mInForeground = false;
        setContentMenuLister(null);
        if (mCursor != null) {
            mCursor.unregisterContentObserver(mChangeObserver);
        }
    }

    public void clear(){
        mSet.clear();
    }

    public void setContentMenuLister(OnCreateContextMenuListener l){
        if(mGridView != null){
            mGridView.setOnCreateContextMenuListener(l);
        }
    }

    public void setEmbeddedTitleBar(TitleBar v){
        if (mTitleBar == v) return;
        if (mTitleBar != null) {
            if(v == null){
                mGridView.setPadding(mPadding, mPadding, mPadding, 0);
            }
//            removeView(mTitleBar);
        }
        if (null != v) {
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            int height = v.getMeasuredHeight();
//            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
//                    LayoutParams.WRAP_CONTENT);
            mGridView.setPadding(mPadding, height + mPadding, mPadding, 0);
//            addView(v, lp);
        }
        mTitleBar = v;
    }

    public class GridAdapter extends BaseAdapter {
        private int mCount;
        private int mExtraOffset;

        private Cursor cursor = null;

        public GridAdapter(Cursor cs) {
            cursor = cs;
            int count = (cursor == null) ? 0 : cursor.getCount();
            if(count >= SpeedDial.MAX_COUNT){
                mCount = count;
                mExtraOffset = 0;
            }else{
                mCount = count +1;
                mExtraOffset = 1;
            }
        }

        public int getExtraOffset(){
            return mExtraOffset;
        }

       public int getCount() {
            return mCount;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || !(convertView instanceof SpeedDialItem)) {
                convertView = (SpeedDialItem)mFactory.inflate(R.layout.speed_dial_item, null);
            }
            if (cursor == null || cursor.isClosed()) {
                return convertView;
            }
            ImageView favicon = (ImageView)convertView.findViewById(R.id.favicon);
            TextView title = (TextView)convertView.findViewById(R.id.title);
            ImageView add = (ImageView)convertView.findViewById(R.id.add_new_item);
            LinearLayout ll = (LinearLayout)convertView.findViewById(R.id.normal_item);

            if (position == cursor.getCount()) {
                add.setVisibility(View.VISIBLE);
                ll.setVisibility(View.GONE);
            }else{
                add.setVisibility(View.GONE);
                ll.setVisibility(View.VISIBLE);
                cursor.moveToPosition(position);
                byte[] data = cursor.getBlob(FAVICON_INDEX);
                Bitmap bp = null;
                if (data != null && (bp = BitmapFactory.decodeByteArray(data, 0, data.length)) != null) {
                    favicon.setImageBitmap(bp);
                } else {
                    favicon.setImageResource(R.drawable.app_web_browser_sm_white);
                    String url = position + cursor.getString(URL_INDEX);
                    if(!mSet.contains(url)){
                        Log.d(LOGTAG,"getview downloadfavicon: "+url);
                        DownloadFavicon.donwnloadFavicon(null, cursor.getString(URL_INDEX),
                                mContext, mContext.getContentResolver());
                        mSet.add(url);
                    }
                }
                title.setText(cursor.getString(TITLE_INDEX));
            }
            return convertView;
        }

        void refreshData() {
            if (cursor == null || cursor.isClosed()) {
                return;
            }
            cursor.requery();
            int count = cursor.getCount();
            if(count >= SpeedDial.MAX_COUNT){
                mCount = count;
                mExtraOffset = 0;
            }else{
                mCount = count +1;
                mExtraOffset = 1;
            }
            notifyDataSetChanged();
        }

       public String getString(int position, int index){
           if (cursor == null || cursor.isClosed()) {
               return "";
           }
            if (position < 0 || position >= mCount - mExtraOffset) {
                return "";
            }
            cursor.moveToPosition(position);
            return cursor.getString(index);
        }

        public Bundle getRow(int position) {
            Bundle map = new Bundle();
            if (cursor == null || cursor.isClosed()) {
                return map;
            }
            if (position < 0 || position >= mCount - mExtraOffset) {
                return map;
            }
            cursor.moveToPosition(position);
            map.putString("url", cursor.getString(URL_INDEX));
            map.putString("title",cursor.getString(TITLE_INDEX));
            map.putInt("id", cursor.getInt(ID_INDEX));
            return map;
        }

        public void deleteRow(int position) {
            if (cursor == null || cursor.isClosed()) {
                return ;
            }
            if (position < 0 || position >= mCount - mExtraOffset) {
                return;
            }
            cursor.moveToPosition(position);
            int id = cursor.getInt(ID_INDEX);
            try {
                mContext.getContentResolver().delete(SpeedDial.CONTENT_URI, "_id = " + id, null);
            } catch (SQLiteException e) {
                Log.e(LOGTAG, "deleteRow:", e);
            }
            refreshData();
        }

        public void updateRow(Bundle map){
            if (cursor == null || cursor.isClosed()) {
                return ;
            }
            // Find the record
            int id = map.getInt("id");
            int position = -1;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (cursor.getInt(ID_INDEX) == id) {
                    position = cursor.getPosition();
                    break;
                }
            }
            if (position < 0) {
                return;
            }

            cursor.moveToPosition(position);
            ContentValues values = new ContentValues();
            String title = map.getString(Browser.BookmarkColumns.TITLE);
            if (!title.equals(cursor
                    .getString(TITLE_INDEX))) {
                values.put(Browser.BookmarkColumns.TITLE, title);
            }
            String url = map.getString(Browser.BookmarkColumns.URL);
            if (!url.equals(cursor.
                    getString(URL_INDEX))) {
                values.put(Browser.BookmarkColumns.URL, url);
            }

            boolean invalidateFavicon = map.getBoolean("invalidateFavicon");
            if (invalidateFavicon) {
                values.put("favicon", new byte[0]);
            }
            try {
                if (values.size() > 0
                        && mContext.getContentResolver().update(SpeedDial.CONTENT_URI,
                                values, "_id = " + id, null) > 0) {
                    refreshData();
                }
            } catch (SQLiteException e) {
                Log.e(LOGTAG, "updateRow:", e);
            }
        }
    }
}
