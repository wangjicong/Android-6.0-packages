package com.android.launcher3;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.util.Log;

import com.android.launcher3.compat.UserHandleCompat;

// SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info.
public class SprdUnreadInfoManager extends ContentObserver {

    private static final String TAG = "UnreadInfoManager";
    private static final boolean DEBUG = false;

    public static final String EXCEED_TEXT = "99+";
    public static final int MAX_UNREAD_COUNT = 99;

    // Apps that show in Launcher workspace or in all apps, currently we only
    // use Mms and Phone, we will show unread info num on their icon.
    private static final ComponentName sMmsComponentName = new ComponentName("com.android.mms",
            "com.android.mms.ui.ConversationList");
    private static final ComponentName sDialerComponentName = new ComponentName("com.android.dialer",
            "com.android.dialer.DialtactsActivity");

    private static final  int MATCH_CALL = 1;
    private static final int MATCH_MMSSMS = 2;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI("call_log", "calls", MATCH_CALL);
        sURIMatcher.addURI("mms-sms", null, MATCH_MMSSMS);
    }

    private static final Uri MMSSMS_CONTENT_URI = Uri.parse("content://mms-sms");
    private static final Uri CALLS_CONTENT_URI = CallLog.Calls.CONTENT_URI;
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");

    private static final String MISSED_CALLS_SELECTION =
            CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.NEW + " = 1";

    private Context mContext;
    private WeakReference<Launcher> mLauncherRef;
    private LauncherAppState mAppState;
    private HashMap<ComponentName, UnreadInfo> mUnreadInfoCache = new HashMap<ComponentName, UnreadInfo>();
    private HashMap<ComponentName, Boolean> mUnreadInfoChangedCache = new HashMap<ComponentName, Boolean>();

    // assume the unit is dp
    private float mLargeTextSize = 14;
    private float mMiddleTextSize = 12;
    private float mSmallTextSize = 10;
    private Drawable mBackground;

    private boolean mUpdateUnreadInfoTheFirstTime;

    public SprdUnreadInfoManager(LauncherAppState appState) {
        super(null);

        mAppState = appState;
        mContext = appState.getContext();

        mLargeTextSize = mContext.getResources().getDimension(R.dimen.unread_info_large_text_size);
        mMiddleTextSize = mContext.getResources().getDimension(R.dimen.unread_info_middle_text_size);
        mSmallTextSize = mContext.getResources().getDimension(R.dimen.unread_info_small_text_size);
        mBackground = mContext.getResources().getDrawable(R.drawable.unread_info_background);

        // TODO: the following code should be remove to prepareUnreadInfo() if it block Main thread.
        IconCache iconCache = appState.getIconCache();
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        Intent intent = new Intent();
        intent.setComponent(sMmsComponentName);
        Bitmap originIcon = iconCache.getIcon(intent, user);
        mUnreadInfoCache.put(sMmsComponentName, new UnreadInfo(0, originIcon, originIcon));

        intent.setComponent(sDialerComponentName);
        originIcon = iconCache.getIcon(intent, user);
        mUnreadInfoCache.put(sDialerComponentName, new UnreadInfo(0, originIcon, originIcon));

        // register content observer only once for the LauncherApplication
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(CALLS_CONTENT_URI, true, this);
        resolver.registerContentObserver(MMSSMS_CONTENT_URI, true, this);

        mUpdateUnreadInfoTheFirstTime = true;
    }

    public void bindLauncher(Launcher launcher) {
        mLauncherRef = new WeakReference<Launcher>(launcher);
    }

    public void terminate() {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(this);
    }

    /**
     * Note: this method be called in Launcher.onResume to check if there has
     * any unread info need to be updated, then update these unread info all at
     * once.
     */
    public void updateUnreadInfoIfNeeded() {
        if (mUnreadInfoChangedCache.size() > 0
                && mLauncherRef != null && mLauncherRef.get() != null) {
            mLauncherRef.get().updateUnreadInfo();
        }
    }

    /**
     * Note: this method is called by Working thread in LauncherModel before load
     * and bind any icon into Workspace or AppsCustomizePagedView.
     * 
     * Prepare the unread data and bitmap and then apply these bitmap when
     * new BubbleTextView need to be created.
     * 
     * This method should not be called in UI thread for it manipulate database.
     */
    public void prepareUnreadInfo() {
        if (mUpdateUnreadInfoTheFirstTime) {
            mUpdateUnreadInfoTheFirstTime = false;
            for(ComponentName cn : mUnreadInfoCache.keySet()) {
                int unreadNum = loadUnreadInfoCount(cn);
                updateUnreadInfoCache(cn, unreadNum);
            }
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (DEBUG) {
            Log.d(TAG, String.format("onChange: uri=%s selfChange=%b", uri.toString(), selfChange));
        }
        int match = sURIMatcher.match(uri);
        switch (match) {
            case MATCH_CALL:
                asyncGetUnreadInfoAndTriggerUpdate(sDialerComponentName);
                break;
            case MATCH_MMSSMS:
                asyncGetUnreadInfoAndTriggerUpdate(sMmsComponentName);
                break;
        }
    }

    public Bitmap getBitmapWithUnreadInfo(ShortcutInfo info, Bitmap originBmp) {
        ComponentName cn = info.getIntent().getComponent();
        if (originBmp == null) {
            originBmp = info.getIcon(mAppState.getIconCache());
        }
        return getBitmapWithUnreadInfoInternal(cn, originBmp);
    }

    public Bitmap getBitmapWithUnreadInfo(AppInfo info, Bitmap originBmp) {
        ComponentName cn = info.getIntent().getComponent();
        if (originBmp == null) {
            originBmp = info.iconBitmap;
        }
        return getBitmapWithUnreadInfoInternal(cn, originBmp);
    }

    public boolean updateBubbleTextViewUnreadInfo(BubbleTextView bubble) {
        if (bubble != null) {
            Object tag = bubble.getTag();
            ComponentName cn = null;
            if (tag instanceof ShortcutInfo) {
                ShortcutInfo info = (ShortcutInfo) tag;
                Bitmap b = getBitmapWithUnreadInfo(info, null);
                FastBitmapDrawable iconDrawable = Utilities.createIconDrawable(b);
                iconDrawable.setGhostModeEnabled(info.isDisabled != 0);
                bubble.setCompoundDrawables(null, iconDrawable, null, null);
                cn = info.getIntent().getComponent();
            } else if (tag instanceof AppInfo) {
                AppInfo info = (AppInfo) tag;
                DeviceProfile grid = mAppState.getDynamicGrid().getDeviceProfile();
                Bitmap b = getBitmapWithUnreadInfo(info, null);
                Drawable topDrawable = Utilities.createIconDrawable(b);
                topDrawable.setBounds(0, 0, grid.allAppsIconSizePx, grid.allAppsIconSizePx);
                bubble.setCompoundDrawables(null, topDrawable, null, null);
                cn = info.getIntent().getComponent();
            }
            return hasComponentUnreadInfoChanged(cn);
        }
        return false;
    }

    private Bitmap getBitmapWithUnreadInfoInternal(ComponentName cn, Bitmap origin) {
        if (mUnreadInfoCache.containsKey(cn)) {
            UnreadInfo unreadInfo = mUnreadInfoCache.get(cn);
            if (unreadInfo != null && unreadInfo.mIconWithNum != null) {
                return unreadInfo.mIconWithNum;
            }
        }
        return origin;
    }

    private boolean hasComponentUnreadInfoChanged(ComponentName cn) {
        if (mUnreadInfoChangedCache.containsKey(cn)) {
            return mUnreadInfoChangedCache.get(cn);
        }
        return false;
    }

    private void asyncGetUnreadInfoAndTriggerUpdate(final ComponentName cn) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return loadUnreadInfoCount(cn);
            }

            @Override
            protected void onPostExecute(Integer unreadNum) {
                if (mUnreadInfoCache.containsKey(cn)
                        && unreadNum != mUnreadInfoCache.get(cn).mUnreadInfoNum) {
                    boolean updated = updateUnreadInfoCache(cn, unreadNum);
                    if (updated && mLauncherRef != null && mLauncherRef.get() != null) {
                        mUnreadInfoChangedCache.put(cn, true);
                        mLauncherRef.get().updateUnreadInfo();
                    }
                }
            }
        }.execute();
    }

    private int loadUnreadInfoCount(ComponentName component) {
        if (component.equals(sMmsComponentName)) {
            return getUnreadMessageCount();
        } else if (component.equals(sDialerComponentName)) {
            return getMissedCallCount();
        } else {
            return 0;
        }
    }


    private int getUnreadMessageCount() {
        int unreadSms = 0;
        int unreadMms = 0;

        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();

        // get unread sms count
        cursor = resolver.query(SMS_CONTENT_URI, new String[] { BaseColumns._ID },
                "type = 1 AND read = 0", null, null);
        if (cursor != null) {
            unreadSms = cursor.getCount();
            cursor.close();
            if (DEBUG) {
                Log.i(TAG, String.format("getUnreadMessageCount unreadSms=%d", unreadSms));
            }
        }

        /* SPRD: bug379279 2014-12-11 Bugfix unread mms logic wrong. @{ */
        // if the following values redefined in module telephony-common, then we
        // need to redefined them here.

        // MESSAGE_TYPE_NOTIFICATION_IND = 0x82
        // MESSAGE_TYPE_RETRIEVE_CONF = 0x84

        // get unread mms count
        cursor = resolver.query(MMS_CONTENT_URI, new String[] { BaseColumns._ID },
                "msg_box = 1 AND read = 0 AND ( m_type = 130 OR m_type = 132 ) AND thread_id > 0",
                null, null);
        /* SPRD: bug379279 2014-12-11 Bugfix unread mms logic wrong. @} */

        if (cursor != null) {
            unreadMms = cursor.getCount();
            cursor.close();
            if (DEBUG) {
                Log.i(TAG, String.format("getUnreadMessageCount unreadMms=%d", unreadMms));
            }
        }

        return (unreadSms + unreadMms);
    }


    private int getMissedCallCount() {
        int missedCalls = 0;

        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();

        cursor = resolver.query(CALLS_CONTENT_URI, new String[] { BaseColumns._ID },
                MISSED_CALLS_SELECTION, null, null);
        if (cursor != null) {
            missedCalls = cursor.getCount();
            cursor.close();
            if (DEBUG) {
                Log.i(TAG, String.format("getMissedCallCount missedCalls=%d", missedCalls));
            }
        }

        return missedCalls;
    }


    private boolean updateUnreadInfoCache(ComponentName cn, int unreadNum) {
        if (mUnreadInfoCache.containsKey(cn)) {
            UnreadInfo info = mUnreadInfoCache.get(cn);
            if (info.mUnreadInfoNum != unreadNum
                    && !(info.mUnreadInfoNum > MAX_UNREAD_COUNT && unreadNum > MAX_UNREAD_COUNT)) {
                if (unreadNum == 0) {
                    // restore origin icon
                    info.mIconWithNum = info.mOriginIcon;
                } else {
                    if (info.mOriginIcon != null) {
                        info.mIconWithNum = createBitmapWithUnreadInfo(info.mOriginIcon, unreadNum);
                    }
                }
                info.mUnreadInfoNum = unreadNum;
                return true;
            }
        }
        return false;
    }

    private Bitmap createBitmapWithUnreadInfo(Bitmap origin, int unreadNum) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        String finalText;
        if (unreadNum <= MAX_UNREAD_COUNT) {
            finalText = String.valueOf(unreadNum);
        } else {
            finalText = EXCEED_TEXT;
        }
        switch (finalText.length()) {
        case 1:
            paint.setTextSize(mLargeTextSize);
            break;
        case 2:
            paint.setTextSize(mMiddleTextSize);
            break;
        default:
            paint.setTextSize(mSmallTextSize);
            break;
        }

        int bgWidth = mBackground.getIntrinsicWidth();
        int bgHeight = mBackground.getIntrinsicHeight();

        Rect textBounds = new Rect();
        paint.getTextBounds(finalText, 0, finalText.length(), textBounds);

        int textHeight = textBounds.height();

        // Why we not use textBounds.width() as textWidth?
        // After test on devices, use the measured width is more precise to center
        // the number in the background circle. May be it is the red circle not in
        // then center of the background resource 'R.drawable.unread_info_background'
        // cause this problem.
        int textWidth = (int) paint.measureText(finalText, 0, finalText.length());

        // TODO: if textWidth >= bgWidth or textHeight >= bgHeight,
        // if textWidth >= circleWidth or textHeight >= circleHeight,
        // we must reduce the font size until fit the previous condition.

        // Why we multiply bgHeight by 0.71
        // Because the red circle's height in the background bitmap occupied 71%.
        // If the background resource changed, the percentage here also need to
        // be change.
        int circleHeight = (int) (bgHeight * 0.71);

        Canvas canvas = new Canvas();
        Bitmap compoundBmp = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(compoundBmp);
        mBackground.setBounds(0, 0, bgWidth, bgHeight);
        mBackground.draw(canvas); // draw background

        int x = (bgWidth - textWidth) / 2;

        // Why we add circleHeight by 1 pixel?
        // As we can see from the background bitmap resource file, there has 1 pixel
        // between the top of the red circle and the top of the background bitmap.
        // With xhdpi resource bitmap, it is 2 pixel, but we can not detect which
        // resource be used easily, so we always plus 1 pixel when calculate the
        // y position to make the number be drawn in the center of the red circle.
        //
        // It is the background bitmap cause this problem for the red circle not
        // in the center of the background. The guy who design the background resource
        // bitmap is to be blame.
        int y = (circleHeight + 1 ) - (circleHeight - textHeight) / 2;

        canvas.drawText(finalText, x, y, paint);

        Bitmap finalBitmap = origin.copy(Bitmap.Config.ARGB_8888, true);
        canvas.setBitmap(finalBitmap);
        canvas.drawBitmap(compoundBmp, 0, 0, null);

        compoundBmp.recycle();
        return finalBitmap;
    }

    public void resetComponentsUnreadInfoChangedValue() {
        mUnreadInfoChangedCache.clear();
    }

    private class UnreadInfo {
        int mUnreadInfoNum;
        Bitmap mOriginIcon;
        Bitmap mIconWithNum;

        public UnreadInfo(int num, Bitmap origin, Bitmap withNum) {
            mUnreadInfoNum = num;
            mOriginIcon = origin;
            mIconWithNum = withNum;
        }
    }
}
