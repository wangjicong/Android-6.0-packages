
package com.android.providers.telephony.ext.mmsbackup.mms;

import com.android.providers.telephony.ext.mmsbackup.BackupLog;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class PduFile {
    private static final String TAG = "PduFile";
    private static final int SIZE = 1024;

    public static final byte[] read(InputStream file) {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream(SIZE);
            byte[] tmp = new byte[SIZE];
            int size;
            while ((size = in.read(tmp)) != -1) {
                out.write(tmp, 0, size);
            }
            in.close();
            byte[] c = out.toByteArray();
            return c;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            BackupLog.logE(TAG, "read Pdu write to file Error !");
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
