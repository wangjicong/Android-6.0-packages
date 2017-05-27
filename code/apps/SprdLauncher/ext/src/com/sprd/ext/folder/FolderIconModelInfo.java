package com.sprd.ext.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.android.sprdlauncher3.BubbleTextView;
import com.android.sprdlauncher3.FolderIcon;
import com.android.sprdlauncher3.ShortcutInfo;
import com.sprd.ext.dynamicIcon.DynamicIconUtils;

import java.util.ArrayList;

/**
 * Created by SPRD on 2017/1/19.
 */

abstract class FolderIconModelInfo {
    public Context mContext;
    private String mIconModel;
    int mMaxIconsNum;

    FolderIconModelInfo(Context context, String iconModel){
        mContext = context;
        mIconModel = iconModel;
    }

    String getIconModel(){
        return mIconModel;
    }

    int getMaxIconNum(){
        return mMaxIconsNum;
    }

    public abstract boolean drawFolderIconUI(Context context,Canvas canvas, FolderIcon folderIcon);

    /*
    *draw little icon on FolderIcon
    */
    void drawPreviewItem(Canvas canvas, float x, float y, int index, ArrayList<View> items, float iconScaleFactor, int iconSize){
        Drawable d;
        TextView v;
        canvas.save();
        canvas.scale(iconScaleFactor,iconScaleFactor);
        canvas.translate(x,y);
        v = (TextView) items.get(index);
        d = v.getCompoundDrawables()[1];
        if (d != null) {
            d.setBounds(0, 0, iconSize, iconSize);
            d.setFilterBitmap(true);
            d.setColorFilter(Color.argb(0, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            d.draw(canvas);
            d.clearColorFilter();
            d.setFilterBitmap(false);
        }
        canvas.restore();
        if (v instanceof BubbleTextView) {
            Object tag = v.getTag();
            if (tag instanceof ShortcutInfo) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) tag;
                if (shortcutInfo.dynamicIconDrawCallback != null) {
                    canvas.save();
                    canvas.scale(iconScaleFactor,iconScaleFactor);
                    int[] center = new int[2];
                    float offsetX = x + iconSize / 2;
                    float offsetY = y + iconSize / 2;
                    center[0] = Math.round(offsetX);
                    center[1] = Math.round(offsetY);
                    DynamicIconUtils.drawDynamicIconIfNeed(canvas, v, DynamicIconUtils.STABLE_SCALE, center);
                    canvas.restore();
                }
            }
        }
    }

}
