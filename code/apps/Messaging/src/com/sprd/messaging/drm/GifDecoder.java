package com.sprd.messaging.drm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import android.content.Context;
import android.drm.DrmManagerClient;
import android.drm.DecryptHandle;
import android.net.Uri;
import android.nfc.Tag;
import android.util.Log;

public class GifDecoder {
    private Context mContext;
    private DrmManagerClient mClient = null;
    private static final String TAG = "GifDecoder";
    private static GifDecoder sGifDecoder;

    private GifDecoder(DrmManagerClient client) {
        mClient = client;
    }

    public static GifDecoder get(DrmManagerClient client) {
        if (sGifDecoder == null) {
            sGifDecoder = new GifDecoder(client);
        }
        return sGifDecoder;
    }

    public InputStream readDrmInputStream(String dataPath) {
        InputStream is = null;
        int fileSize = 0;
        FileInputStream fis = null;
        Log.d(TAG, " readDrmInputStream path is " + dataPath);
        DecryptHandle decryptHandle = mClient.openDecryptSession(dataPath);
        try {
            File file = new File(dataPath);
            if (file.exists()) {
                fis = new FileInputStream(file);
                fileSize = fis.available();
            }
        } catch (Exception e) {
            Log.d(TAG, "readDrmUri.file error");
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                Log.d(TAG, "readDrmUri.file close error");
                e.printStackTrace();
            }
        }
        byte[] ret = mClient.pread(decryptHandle, fileSize, 0);
        Log.d(TAG, "Drm loadGif pread ret = " + ret);
        is = new ByteArrayInputStream(ret);
        if (decryptHandle != null) {
            mClient.closeDecryptSession(decryptHandle);
        }
        return is;
    }
}
