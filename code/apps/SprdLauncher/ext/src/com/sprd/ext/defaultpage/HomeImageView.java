package com.sprd.ext.defaultpage;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.sprdlauncher3.CellLayout;
import com.android.sprdlauncher3.Launcher;
import com.android.sprdlauncher3.R;
import com.android.sprdlauncher3.Workspace;
import com.sprd.ext.LogUtils;

/**
 * Created by sprd on 10/20/16.
 */

public class HomeImageView extends ImageView implements View.OnClickListener {
    private String TAG = "HomeImageView";
    private Launcher mLauncher;
    private CellLayout mParentCellLayout;
    private DefaultPageController mDefaultPageController;
    private int mDefaultColor;
    private boolean mIsDefaultPage;

    public HomeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HomeImageView(Context context, boolean isDefaultPage, CellLayout cellLayout) {
        super(context);
        mLauncher = (Launcher) context;
        mIsDefaultPage = isDefaultPage;
        mParentCellLayout = cellLayout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDefaultColor = context.getColor(R.color.default_home_page_color);
        } else {
            mDefaultColor = getResources().getColor(R.color.default_home_page_color);
        }
        setImageResource(R.drawable.home_icon);
        updateHomeImageTint(mIsDefaultPage);
        setVisibility(View.GONE);
        setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        boolean isDefaultPage = isDefaultPage();
        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "onClick: isDefaultPage = " + isDefaultPage);
        }
        if (!isDefaultPage) {
            Workspace workspace = mLauncher.getWorkspace();
            if (workspace != null) {
                int currentPage = workspace.indexOfChild(mParentCellLayout);
                if (mDefaultPageController != null) {
                    mDefaultPageController.updateDefaultPage(currentPage, true, true, this);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int cw = mParentCellLayout.getCellWidth();
        int ch = mParentCellLayout.getCellHeight();
        int width = View.MeasureSpec.makeMeasureSpec(cw / 2, View.MeasureSpec.EXACTLY);
        int height = View.MeasureSpec.makeMeasureSpec(ch / 2, View.MeasureSpec.EXACTLY);
        setMeasuredDimension(width, height);
    }

    public DefaultPageController getDefaultPageController() {
        return mDefaultPageController;
    }

    public void setDefaultPageController(DefaultPageController controller) {
        mDefaultPageController = controller;
    }

    public void setDefaultPage(boolean defaultPage) {
        if (defaultPage != mIsDefaultPage) {
            mIsDefaultPage = defaultPage;
        }
    }

    public boolean isDefaultPage() {
        return mIsDefaultPage;
    }

    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void updateHomeImageTint(boolean tint) {
        if (LogUtils.DEBUG_DEFAULT_PAGE) {
            LogUtils.d(TAG, "updateHomeImageTint: tint = " + tint);
        }
        if (tint) {
            setImageTintList(ColorStateList.valueOf(mDefaultColor));
        } else {
            setImageTintList(null);
        }
    }
}
