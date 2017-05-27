package com.sprd.ext.folder;

import android.content.Context;
import android.graphics.Canvas;

import com.android.sprdlauncher3.FolderIcon;
import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;

import java.util.ArrayList;

/**
 * Created by SPRD on 2016/11/8.
 */
public class FolderIconController {
    private static final String TAG = "FolderIconController";
    private Context mContext;
    private String mFolderIconModel;

    public static final String FOLDER_MODEL_KEY = "pref_folder_model";

    private ArrayList<FolderIconModelInfo> mFolderModelInfo = new ArrayList<>();

    public FolderIconController(Context context) {
        mContext = context;
        init();
    }

    private void loadAllFolderIconModels(Context context){
        // the "Grid" and "Line"come from the array folder_model_values
        GridFolderIconModel gfModel = new GridFolderIconModel(context,"Grid");
        mFolderModelInfo.add(gfModel);

        LineFolderIconModel lfModel = new LineFolderIconModel(context,"Line");
        mFolderModelInfo.add(lfModel);
    }

    public boolean drawNativeFolderIconIfNeeded(Canvas canvas, FolderIcon folderIcon){
        boolean drawCustomIconSuccess = false;
        FolderIconModelInfo info = getCurrentFolderModelInfo();
        if(info != null) {
            drawCustomIconSuccess = info.drawFolderIconUI(mContext, canvas, folderIcon);
        }
        return !drawCustomIconSuccess;
    }

    private void init(){
        mFolderIconModel = UtilitiesExt.getLauncherSettingsString(mContext, FOLDER_MODEL_KEY,
                mContext.getResources().getString(R.string.default_folder_model));
        loadAllFolderIconModels(mContext);
        updateMaxFolderIconsNum();
    }

    public void onFolderIconModelChanged(String nweModel){
        mFolderIconModel = nweModel;
        updateMaxFolderIconsNum();
    }

    private FolderIconModelInfo getCurrentFolderModelInfo() {
        for(int i = 0; i < mFolderModelInfo.size(); i ++){
            FolderIconModelInfo info = mFolderModelInfo.get(i);
            if(mFolderIconModel.equals(info.getIconModel())){
                if(LogUtils.DEBUG){
                    LogUtils.d(TAG,"mFolderIconModel = "+ mFolderIconModel);
                }
                return info;
            }
        }
        return null;
    }

    private void updateMaxFolderIconsNum(){
        FolderIconModelInfo info = getCurrentFolderModelInfo();
        if(info != null) {
            FolderIcon.NUM_ITEMS_IN_PREVIEW = info.getMaxIconNum();
        }
    }
}
