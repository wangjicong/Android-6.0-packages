/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import android.graphics.drawable.Drawable;

public class AppInfo {

    private boolean checked = false;

    private Drawable icon = null;

    private String name = null;

    private String packageName = null;

    private String packagePath = null;

    private int versionCode = -1;

    private String versionName = null;

    private String sourceDir = null;

    private long apkSize = 0;

    public String getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public void setApkSize(long size) {
        this.apkSize = size;
    }

    public long getApkSize() {
        return apkSize;
    }
}
/* @} */