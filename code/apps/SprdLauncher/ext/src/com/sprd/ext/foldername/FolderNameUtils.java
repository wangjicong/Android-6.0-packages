package com.sprd.ext.foldername;

import android.content.Context;

import com.android.sprdlauncher3.FolderEditText;
import com.android.sprdlauncher3.FolderInfo;
import com.android.sprdlauncher3.R;
import com.sprd.ext.FeatureOption;

/**
 * Created by SPRD on 2017/02/27.
 */
public class FolderNameUtils {
    private static final String TAG = "FolderNameUtils";

    public static void init(Context context, String defaultfolderName,String hintName){
        if(FeatureOption.SPRD_DEFAULT_FOLDER_NAME_SUPPORT) {
            defaultfolderName = hintName = context.getString(R.string.folder_default_name);
        }
    }

    public static void updateFolderTitle(Context context, FolderInfo info){
        if(FeatureOption.SPRD_DEFAULT_FOLDER_NAME_SUPPORT) {
            CharSequence title = info.getTitle();
            if(title != null && title.toString().isEmpty()){
                info.setTitle(context.getText(R.string.folder_default_name));
            }
        }
    }

    public static boolean isFolderNameChanged(String newStr, FolderInfo info, FolderEditText folderName){
        boolean changed = false;
        if(FeatureOption.SPRD_DEFAULT_FOLDER_NAME_SUPPORT){
            CharSequence title = info.getTitle();
            changed = !newStr.trim().isEmpty() && !newStr.equals(title.toString());
            if(!changed){
                folderName.setText(title);
            }
        } else {
           changed = true;
        }
        return changed;
    }

    public static boolean isDefaultFolderName(Context context, FolderInfo info){
        boolean isDefaut = false;
        if(FeatureOption.SPRD_DEFAULT_FOLDER_NAME_SUPPORT) {
            CharSequence title = info.getTitle();
            isDefaut = title != null && title.toString().equals(context.getString(R.string.folder_default_name));
        }
        return isDefaut;
    }
}
