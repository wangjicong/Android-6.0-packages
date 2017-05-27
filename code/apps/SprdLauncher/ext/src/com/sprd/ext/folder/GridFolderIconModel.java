package com.sprd.ext.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.sprdlauncher3.FolderIcon;
import com.android.sprdlauncher3.R;
import com.sprd.ext.FeatureOption;

import java.util.ArrayList;

/**
 * Created by SPRD on 2017/1/19.
 */

class GridFolderIconModel extends FolderIconModelInfo {
    private static final int LINE_NUM_MAX = 4;

    private int mLineNum;
    private int mIconMagin;
    private int mIconMaginPadding;

    GridFolderIconModel(Context context, String iconModel){
        super(context,iconModel);
        mLineNum = Math.min(context.getResources().getInteger(R.integer.folder_line_num),LINE_NUM_MAX);
        mIconMagin = context.getResources().getDimensionPixelSize(R.dimen.folder_icon_margin);
        mIconMaginPadding = context.getResources().getDimensionPixelSize(R.dimen.folder_icon_margin_padding);
        mMaxIconsNum = mLineNum * mLineNum;
    }

    @Override
    public boolean drawFolderIconUI(Context context, Canvas canvas, FolderIcon folderIcon){
        if(!FeatureOption.SPRD_FOLDER_MODEL_SUPPORT){
            return false;
        }

        ImageView previewBackground = folderIcon.getmPreviewBackground();
        folderIcon.getmPreviewBackground().setImageResource(R.drawable.foldericon_grid_bg);

        ArrayList<View> items = folderIcon.getmFolder().getItemsInReadingOrder();
        TextView v = (TextView) items.get(0);
        Drawable d = v.getCompoundDrawables()[1];
        folderIcon.computePreviewDrawingParams(d);


        int left = previewBackground.getLeft();
        int top = previewBackground.getTop();
        int width = previewBackground.getWidth();
        int height = previewBackground.getWidth();

        int intrinsicIconSize = folderIcon.getmIntrinsicIconSize();
        float iconScaleFactor = ( intrinsicIconSize * 1f - 2 * mIconMaginPadding - (mLineNum - 1) * mIconMagin ) / ( mLineNum *  intrinsicIconSize);
        int max = mLineNum * mLineNum;
        if(items.size() < max){
            max = items.size();
        }

        for(int i = 0; i < max; i++){
            int basex = i % mLineNum;
            int basey = i / mLineNum;
            int x = left + (width - intrinsicIconSize) / 2 + mIconMaginPadding + basex * (intrinsicIconSize + mIconMagin - 2 * mIconMaginPadding) / mLineNum;
            int y = top + (height - intrinsicIconSize) / 2 + mIconMaginPadding + basey * (intrinsicIconSize + mIconMagin - 2 * mIconMaginPadding) / mLineNum;
            x= (int) (x / iconScaleFactor);
            y= (int) (y / iconScaleFactor);
            drawPreviewItem(canvas,x,y,i,items,iconScaleFactor,intrinsicIconSize);
        }
        return true;
    }
}
