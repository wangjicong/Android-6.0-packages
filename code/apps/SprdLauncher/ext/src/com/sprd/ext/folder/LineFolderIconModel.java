package com.sprd.ext.folder;

import android.content.Context;
import android.graphics.Canvas;

import com.android.sprdlauncher3.FolderIcon;
import com.android.sprdlauncher3.R;

/**
 * Created by SPRD on 2017/1/19.
 */

class LineFolderIconModel extends FolderIconModelInfo {
    private static final int NUM_ITEMS_IN_PREVIEW = 3;

    LineFolderIconModel(Context context, String iconMode){
        super(context, iconMode);
        mMaxIconsNum = NUM_ITEMS_IN_PREVIEW;
    }

    @Override
    public boolean drawFolderIconUI(Context context,Canvas canvas, FolderIcon folderIcon){
        folderIcon.getmPreviewBackground().setImageResource(R.drawable.portal_ring_inner_holo);
        return false;
    }
}
