package com.sprd.ext.defaultpage;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.sprdlauncher3.CellLayout;
import com.android.sprdlauncher3.DeviceProfile;
import com.android.sprdlauncher3.Launcher;
import com.android.sprdlauncher3.LauncherAppState;
import com.android.sprdlauncher3.Workspace;
import com.sprd.ext.LogUtils;

/**
 * Created by sprd on 10/20/16.
 */

public class DefaultPageController {

    public static final String DEFAULT_PAGE_INDEX = "defaultPageIndex";
    private static final String TAG = "DefaultPageController";
    public static boolean H_LAYOUT_OF_LEFT = false;
    public static boolean mIsHorizontalMode = false;

    private Launcher mLauncher;

    public DefaultPageController(Context context) {
        mLauncher = (Launcher) context;

        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        mIsHorizontalMode = grid.isLandscape();
    }

    public boolean isTouched(View view, MotionEvent ev) {
        boolean isTouched = false;
        if (view instanceof CellLayout) {
            HomeImageView homeImageView = ((CellLayout) view).getHomeImageView();
            if (isHomeImageVisible(homeImageView)) {
                Rect homeRect = new Rect();
                homeImageView.getHitRect(homeRect);
                if (homeRect.contains((int) ev.getX(), (int) ev.getY())) {
                    isTouched = true;
                }
            }
        }

        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "isTouched: isTouched = " + isTouched);
        }
        return isTouched;
    }

    public int getCalculateCellWidth(int childWidthSize, int countX, HomeImageView homeImageView) {
        int cw = 0;
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        if (isHomeImageVisible(homeImageView)) {
            if (H_LAYOUT_OF_LEFT && mIsHorizontalMode) {
                cw = grid.calculateCellWidth(childWidthSize * 2, countX * 2 + 1);
            } else {
                cw = grid.calculateCellWidth(childWidthSize, countX);
            }
        } else {
            cw = grid.calculateCellWidth(childWidthSize, countX);
        }

        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "getCalculateCellWidth: cw = " + cw);
        }
        return cw;
    }

    public int getWidthOffsetDueToHome(HomeImageView homeImageView) {
        int offset = 0;
        if (isHomeImageVisible(homeImageView)) {
            if (mIsHorizontalMode && H_LAYOUT_OF_LEFT) {
                offset = homeImageView.getMeasuredWidth();
            }
        }

        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "getWidthOffsetDueToHome: offset = " + offset);
        }
        return offset;
    }

    public int getHeightOffsetDueToHome(HomeImageView homeImageView) {
        int offset = 0;
        if (isHomeImageVisible(homeImageView)) {
            if (!(H_LAYOUT_OF_LEFT && mIsHorizontalMode)) {
                offset = homeImageView.getMeasuredHeight();
                if (mIsHorizontalMode) {
                    setCellLayoutMoveDown(homeImageView);
                }
            }
        }

        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "getHeightOffsetDueToHome: offset = " + offset);
        }
        return offset;
    }

    private boolean isHomeImageVisible(HomeImageView homeImageView) {
        boolean visible = homeImageView != null && homeImageView.isVisible();

        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "isHomeImageVisible: visible = " + visible);
        }
        return visible;
    }

    private void setCellLayoutMoveDown(HomeImageView homeImageView) {
        if (homeImageView == null) {
            LogUtils.d(TAG, "setCellLayoutMoveDown: homeImageView is a null object");
            return;
        }

        int homeImageHeight = homeImageView.getMeasuredHeight();
        if(LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG,  "setLayout Horizontal mode mParentCellLayout move down");
        }

        CellLayout cl = (CellLayout) homeImageView.getParent();
        cl.setTop(cl.getTop() +  homeImageHeight / 2);
        cl.setBottom(cl.getBottom() + homeImageHeight /2);
        LinearLayout overviewPanel = (LinearLayout) mLauncher.getOverviewPanel();
        if (overviewPanel != null && mIsHorizontalMode && !DefaultPageController.H_LAYOUT_OF_LEFT) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) overviewPanel.getLayoutParams();
            if(lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                if(LogUtils.DEBUG_DEFAULT_PAGE){
                    LogUtils.d(TAG,  "setLayout Horizontal mode overviewPanel move down");
                }
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.topMargin = 0;
                overviewPanel.setLayoutParams(lp);
                overviewPanel.setGravity(Gravity.BOTTOM);
            }
        }
    }

    public void updateHomeVisibility(HomeImageView homeImage, boolean visible) {
        if (homeImage != null) {
            homeImage.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public boolean isDefaultPage(CellLayout cl) {
        boolean result = false;
        if (cl != null) {
            HomeImageView homeImage = cl.getHomeImageView();
            if (homeImage != null) {
                result = homeImage.isDefaultPage();
            }
        }

        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "isDefaultPage: cl = " + cl + ", result  = " + result);
        }
        return result;
    }

    public void updateDefaultPage(int newDefaultPage, boolean needAddTint) {
        updateDefaultPage(newDefaultPage, needAddTint, false, null);
    }

    /**
     * This is called by HomeImageView.onClick to update the default home page index and tint.
     * @param newDefaultPage the page index of the parent view that the homeImage belongs to.
     * @param needAddTint whether add tint to the new default page or not.
     * @param changeOldDefault whether the old default page state should be changed or not.
     * @param homeImage the homeImage in the new default page.
     */
    public void updateDefaultPage(int newDefaultPage, boolean needAddTint, boolean changeOldDefault, HomeImageView homeImage) {
        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "updateDefaultPage: newDefaultPage = " + newDefaultPage
                    + ", needAddTint = " + needAddTint + ", changeOldDefault = " + changeOldDefault
                    + ", homeImage = " + homeImage);
        }

        if (changeOldDefault) {
            changeOldDefaultPageToNormal();
        }

        if (needAddTint) {
            if (homeImage != null) {
                homeImage.updateHomeImageTint(true);
                homeImage.setDefaultPage(true);
            } else {
                Workspace workspace = mLauncher.getWorkspace();
                if (workspace != null) {
                    CellLayout cl = (CellLayout) workspace.getChildAt(newDefaultPage);
                    if (cl != null) {
                        HomeImageView home = cl.getHomeImageView();
                        if (home != null && !home.isDefaultPage()) {
                            // when the removed page is the old default page
                            home.updateHomeImageTint(true);
                            home.setDefaultPage(true);
                        }
                    }
                }
            }
        }

        updateDefaultPageIndex(newDefaultPage);
    }

    private void updateDefaultPageIndex(int newDefaultPage) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace != null) {
            workspace.setDefaultPage(newDefaultPage);
            saveSharedDefaultPage(newDefaultPage);
        }
    }

    private void changeOldDefaultPageToNormal() {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace != null) {
            int oldDefaultPage = workspace.getDefaultPage();
            View oldHomeCellLayout = workspace.getChildAt(oldDefaultPage);
            if(oldHomeCellLayout instanceof CellLayout) {
                HomeImageView oldHomeImage = ((CellLayout) oldHomeCellLayout)
                        .getHomeImageView();
                if(oldHomeImage != null){
                    oldHomeImage.updateHomeImageTint(false);
                    oldHomeImage.setDefaultPage(false);
                    if (LogUtils.DEBUG_DEFAULT_PAGE) {
                        LogUtils.d(TAG, "changeOldDefaultPageToNormal: oldDefaultPage = " + oldDefaultPage);
                    }
                }
            }
        }
    }

    public void saveSharedDefaultPage(int defaultIndex) {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences.Editor editor =
                mLauncher.getSharedPreferences(spKey, Context.MODE_PRIVATE)
                        .edit();
        editor.putInt(DEFAULT_PAGE_INDEX, defaultIndex);
        editor.commit();
    }

    public int getSharedDefaultPage(int defaultIndex) {
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = mLauncher.getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        return sp.getInt(DEFAULT_PAGE_INDEX, defaultIndex);
    }
}
