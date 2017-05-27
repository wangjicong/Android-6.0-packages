package com.sprd.music.filemanager;

import com.android.music.R;

public class FileInfo {
    public String Name;
    public String mPath;
    public long Size;
    public boolean mIsDirectory = false;
    public int FileCount = 0;
    public int FolderCount = 0;

    public int getIconResourceId() {
        if (mIsDirectory) {
            return R.drawable.folder_icon;
        } else if (FileUtil.isMusicType(Name)) {
            return R.drawable.music_icon;
        }
        return R.drawable.music_icon;
    }
}
