/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.content.pm.PackageParser;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;

public class Utils {

    private static final String TAG = "Utils";
    private static final boolean DEBUG = true;//Debug.isDebug();

    public static final int SUCCESS = 0;

    public static final String SU = "/system/xbin/spsu";

    public static final int FAIL = -1;

    public static final String ROOT = "root";

    public static final String SYSTEM = "system";

    public static final String APP_ = "app_";

    public static String[] getUserChown(int uid) {
        String owner = ROOT;
        String group = ROOT;

        if (uid == 0) {
            owner = ROOT;
            group = ROOT;
        } else if (uid == 1000) {
            owner = SYSTEM;
            group = SYSTEM;
        } else if (uid > 10000) {
            owner = APP_ + (uid - 10000);
            group = APP_ + (uid - 10000);
        }

        return new String[] {
                owner, group
        };
    }

    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        if (file != null && file.exists()) {
            return true;
        }
        return false;
    }

    public static void checkAndMakeFolder(String filePath) {
        File file = new File(filePath);
        if (file != null && !file.exists()) {
            file.mkdir();
        }
    }

    public static boolean unPackageTar(String source, String target) {
        String command = null;
        int status = 0;
        if (source != null && target != null) {
            command = "busybox tar -xf " + source + " " + "-C" + " " + "/";
            if (DEBUG) {
                Log.d(TAG, "command = " + command);
            }
        }
        if ((status = execute(command, null)) == SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean packageTar(String source, String target) {
        String command = null;
        int status = 0;
        if (source != null && target != null) {
            command = "busybox tar -cf " + target + " " + source;
            if (DEBUG) {
                Log.d(TAG, "command = " + command);
            }
        }
        if ((status = execute(command, null)) == SUCCESS) {
            return true;
        } else {
            return false;
        }

    }

    public static int chown(String user, String group, String file) {

        String command = "chown";
        int status = 0;
        String mFile = file.trim();
        String mOwnUser = user.trim();
        String mOwnGroup = group.trim();
        String tmp = "";

        if (null != mOwnUser && !"".equals(mOwnUser)) {
            tmp = mOwnUser;
        }

        if (null != mOwnGroup && !"".equals(mOwnGroup)) {
            tmp += "." + mOwnGroup;
        }

        if (null != tmp && !"".equals(tmp) && null != mFile && !"".equals(mFile)) {
            command = command + " " + tmp + " " + "/" + mFile;
        }
        if (DEBUG) {
            Log.d(TAG, "command = " + command);
        }
        if ((status = execute(command, null)) == SUCCESS) {
            return SUCCESS;
        } else {
            return FAIL;
        }
    }

    public static int tarListFile(String tar, String[] result) {
        String command = null;
        int status = 0;
        if (tar != null) {
            command = "busybox tar -tf " + tar;
            if (DEBUG) {
                Log.d(TAG, "command = " + command);
            }
        }
        if ((status = execute(command, result)) == SUCCESS) {
            return SUCCESS;
        } else {
            return FAIL;
        }
    }

    public static int execute(String cmd, String[] result) {
        int status = SUCCESS;
        Process process = null;
        DataOutputStream os = null;
        DataInputStream is = null;

        if (cmd == null || "".equals(cmd.trim())) {
            return FAIL;
        }

        try {
            process = Runtime.getRuntime().exec(SU);
            os = new DataOutputStream(process.getOutputStream());
            is = new DataInputStream(process.getInputStream());
            os.writeBytes(cmd + " \n");
            os.flush();

            os.writeBytes(" exit \n");
            os.flush();
            process.waitFor();

            if (null != result) {
                int count = is.available();
                byte[] retBytes = new byte[count + 1];
                int retCount = 0;
                if (count != 0) {
                    retCount = is.read(retBytes);
                }
                retBytes[retCount] = 0;
                result[0] = new String(retBytes);
                is.close();

                is = new DataInputStream(process.getErrorStream());
                count = is.available();
                retBytes = new byte[count + 1];
                retCount = 0;
                if (count != 0) {
                    retCount = is.read(retBytes);
                }
                retBytes[retCount] = 0;
                result[1] = new String(retBytes);
            }

            if ((process.exitValue() == 0)) {
                status = SUCCESS;
            } else {
                status = FAIL;
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error - Here is what I know: " + e.getMessage());
            status = FAIL;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return status;
    }

    public static void deletePath(File path) {
        if (path == null || !path.exists()) {
            return;
        }
        if (path.isFile()) {
            path.delete();
        } else {
            File[] list = path.listFiles();
            if (list == null) {
                return;
            }
            for (File f : list) {
                if (!f.delete()) {
                    deletePath(f);
                }
            }
            path.delete();
        }
    }

    public static boolean copyFileToDir(String appPath, String dirPath, String appName) {
        boolean ret = false;
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        if (DEBUG) {
            Log.i(TAG, "appPath : " + appPath);
        }
        File appFile = new File(appPath);
        if (DEBUG) {
            Log.i(TAG, "appFile.length() : " + appFile.length());
        }
        long time = appFile.lastModified();
        File backupAppFile = new File(dirPath + "/" + appName);
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(appFile);
            out = new FileOutputStream(backupAppFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            backupAppFile.setLastModified(time);
            ret = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ret = false;
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return ret;
    }

    public static boolean isSDCardMounted() {
        return (Environment.MEDIA_MOUNTED).equals( Environment.getExternalStoragePathState());
    }

    public static long internalSdcardSurplusSize() {
        File path = Environment.getInternalStoragePath();
        StatFs statfs = new StatFs(path.getPath());
        long blockSize = statfs.getBlockSize();
        long availableBlocks = statfs.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public static long sdcardSurplusSize() {
        File path =  Environment.getExternalStoragePath();
        StatFs statfs = new StatFs(path.getPath());
        long blockSize = statfs.getBlockSize();
        long availableBlocks = statfs.getAvailableBlocks();
        return availableBlocks * blockSize;
    }


    public static String drawableToString(Drawable drawable) {
        String ret = null;
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable
                .getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        ret = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Drawable stringToDrawable(String string) {
        byte[] bs;
        bs = Base64.decode(string, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bs, 0, bs.length);
        return new BitmapDrawable(bitmap);
    }

    public static boolean writePropertiesToDir(String dstPath, String dirPath, AppInfo info) {
        Properties properties = new Properties();
        InputStream in = null;
        OutputStream out = null;
        String apkname = info.getPackagePath();
        if (apkname.startsWith("/data")) {
            apkname = apkname.substring(10, apkname.length());
        } else if (apkname.startsWith("/mnt")) {
            apkname = apkname.substring(10, apkname.length());
            apkname = apkname.substring(0, apkname.lastIndexOf("/")) + ".apk";
        }
        if (DEBUG) {
            Log.d(TAG, " apkname= " + apkname);
        }
        boolean ret = false;
        try {
            File file = new File(dstPath + "/" + dirPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            in = new FileInputStream(file);
            properties.load(in);
            out = new FileOutputStream(file);
            printAppinfo(info);

            String name = (info.getName() == null) ? "" : info.getName();
            properties.setProperty("name", name);

            String packageName = info.getPackageName() == null ? "" : info.getPackageName();
            properties.setProperty("packageName", packageName);

            String packagePath = dstPath + "/" + apkname;
            properties.setProperty("packagePath", packagePath);

            properties.setProperty("versionCode", String.valueOf(info.getVersionCode()));

            String versionName = info.getVersionName() == null ? "" : info.getVersionName();
            properties.setProperty("versionName", versionName);

            String icon = info.getIcon() == null ? "" : drawableToString(info.getIcon());
            properties.setProperty("icon", icon);

            properties.store(out, null);
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private static void printAppinfo(AppInfo info) {
        if (DEBUG) {
            Log.d(TAG, "name : " + info.getName());
            Log.d(TAG, "packageName : " + info.getPackageName());
            Log.d(TAG, "packagePath : " + info.getPackagePath());
            Log.d(TAG, "versionCode : " + info.getVersionCode());
            Log.d(TAG, "versionName : " + info.getVersionName());
            Log.d(TAG, "icon : " + info.getIcon());
        }
    }

    public static AppInfo getAppInfo(File file) {
        AppInfo info = new AppInfo();
        Properties properties = new Properties();

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            properties.load(in);
            // in.close();
            if (properties.getProperty("name") != null) {
                info.setName(properties.getProperty("name"));
            } else {
                return null;
            }
            if (properties.getProperty("packageName") != null) {
                info.setPackageName(properties.getProperty("packageName"));
            } else {
                return null;
            }
            if (properties.getProperty("packagePath") != null) {
                info.setPackagePath(properties.getProperty("packagePath"));
            } else {
                return null;
            }
            if (properties.getProperty("versionCode") != null) {
                info.setVersionCode(Integer.parseInt(properties.getProperty("versionCode")));
            } else {
                return null;
            }
            if (properties.getProperty("versionName") != null) {
                info.setVersionName(properties.getProperty("versionName"));
            } else {
                return null;
            }
            if (properties.getProperty("icon") != null) {
                info.setIcon(stringToDrawable(properties.getProperty("icon")));
            } else {
                return null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return info;
    }

    public static AppInfo packageInfoToAppInfo(PackageManager pm, PackageInfo info,
            Drawable drawable) {
        AppInfo appInfo = new AppInfo();
        if (info != null) {
            appInfo.setPackagePath(info.applicationInfo.sourceDir);
            appInfo.setPackageName(info.applicationInfo.packageName);
            appInfo.setVersionCode(info.versionCode);
            appInfo.setVersionName(info.versionName);
            appInfo.setName(pm.getApplicationLabel(info.applicationInfo).toString());
            appInfo.setApkSize(new File(info.applicationInfo.sourceDir).length());
            if (drawable != null) {
                appInfo.setIcon(drawable);
            } else {
                appInfo.setIcon(pm.getApplicationIcon(info.applicationInfo));
            }
        }
        return appInfo;
    }

    public static Drawable getUninstalledAPKIcon(Context context, String apkPath) {
      /*  PackageParser parser = new PackageParser(apkPath);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pac = parser.parsePackage(new File(apkPath), apkPath, metrics, 0);*/
        PackageParser parser = new PackageParser();
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pac = null;
        try {
            pac = parser.parsePackage(new File(apkPath), 0);
            if (pac == null) {
                return null;
            }
        } catch (PackageParser.PackageParserException e) {
            return null;
        }
        ApplicationInfo info = pac.applicationInfo;
        AssetManager assetMag = new AssetManager();
        assetMag.addAssetPath(apkPath);
        Resources res = context.getResources();
        res = new Resources(assetMag, metrics, res.getConfiguration());
        if (info.icon != 0) {
            Drawable icon = res.getDrawable(info.icon);
            return icon;
        }
        return null;
    }

    public static int getBackupItemCount(String field, File file) {
        int count = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (field.equals(line)) {
                    count++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return count;
    }

    public static String getCurrentStorageState() {
        if (FileConstants.IS_NAND) {
            return Environment.getExternalStoragePathState();
        }
        if (FileConstants.USE_EXTERNAL) {
            return Environment.getExternalStoragePathState();
        } else {
            return Environment.getInternalStoragePathState();
        }
    }

    public static File getCurrentStorageDirectory() {
        if (FileConstants.IS_NAND) {
            return Environment.getExternalStoragePath();
        }
        if (FileConstants.USE_EXTERNAL) {
            return Environment.getExternalStoragePath();
        } else {
            return Environment.getInternalStoragePath();
        }
    }

    public static String getCurrentBackupDir() {
        if (FileConstants.IS_NAND) {
            return FileConstants.EXTERNAL_BACKUP_DIR;
        }
        if (FileConstants.USE_EXTERNAL) {
            return FileConstants.EXTERNAL_BACKUP_DIR;
        } else {
            return FileConstants.INTERNAL_BACKUP_DIR;
        }
    }

    public static String getCurrentBackupApp() {
        if (FileConstants.IS_NAND) {
            return FileConstants.EXTERNAL_BACKUP_APP;
        }
        if (FileConstants.USE_EXTERNAL) {
            return FileConstants.EXTERNAL_BACKUP_APP;
        } else {
            return FileConstants.INTERNAL_BACKUP_APP;
        }
    }

    public static String getCurrentBackupFile() {
        if (FileConstants.IS_NAND) {
            return FileConstants.EXTERNAL_BACKUP_FILE;
        }
        if (FileConstants.USE_EXTERNAL) {
            return FileConstants.EXTERNAL_BACKUP_FILE;
        } else {
            return FileConstants.INTERNAL_BACKUP_FILE;
        }
    }
}
/* @} */