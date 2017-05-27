
package com.sprd.music.lrc;

import com.sprd.music.lrc.LRC.Offset;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRCParser
 *
 * @author lisc
 */
public class LRCParser {

    private static final String TAG = "LRCParser";

    private LRCParser() {
    }

    /**
     * parseFromFile
     *
     * @param path
     * @return LRC
     */
    public static LRC parseFromFile(String path, String tmpFilePath) {
        // by zhangjb add
        File file = new File(path);
        isValidLRCFile(file);
        FileInputStream in = null;
        InputStreamReader read = null;
        BufferedReader reader = null;
        File destFile = null;
        try {
            byte b[] = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(b);
            read = new InputStreamReader(new FileInputStream(file),
                    LyricConstants.TEXT_UTF_ENCODING);
            reader = new BufferedReader(read);
            String line;
            String type = "";
            while ((line = reader.readLine()) != null) {
                if (LyricConstants.DOWNLOAD_LRC_FLAY.equals(line)) {
                    type = LyricConstants.TEXT_UTF_ENCODING;
                    return parseFromFile(file);
                } else {
                    type = getCharEncoding(line, b);
                }
                break;
            }
            destFile = toUtf(type, file, tmpFilePath);
        } catch (IOException e) {
            Log.e(TAG, "PARSE FROM FILE ERROR:" + e.getMessage());
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "FILE OUT OF SIZE" + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "CLOSE BUFFERED READER ERROR:" + e);
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (IOException e) {
                    Log.e(TAG, "CLOSE INPUT STREAM READER ERROR:" + e);
                }
            }
        }
        return parseFromFile(destFile);
    }

    /**
     * parseFromFile
     *
     * @param path
     * @return LRC
     */
    public static LRC parseFromFile(File path) {

        isValidLRCFile(path);

        Pattern pattern = Pattern.compile(LyricConstants.REGEX);
        BufferedReader reader = null;
        String s = null;
        LRC lrc = new LRC();

        try {
            reader = new BufferedReader(new FileReader(path));
            Matcher matcher = null;
            while ((s = reader.readLine()) != null) {
                matcher = pattern.matcher(s);
                while (matcher.find()) {
                    String g = matcher.group(1);
                    if (g != null) {
                        extractHead(lrc, g);
                    } else {
                        g = matcher.group(2);
                        extractBody(lrc, g);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

        if (lrc != null) {
            int emptyLyricCount = 0;
            List<String> lyricsList = lrc.getLyrics();
            for (String lyric : lyricsList) {
                if (lyric.isEmpty()) {
                    emptyLyricCount++;
                }
            }
            // If all lyrics in the list are empty, this file should be invalid
            if (emptyLyricCount == lyricsList.size()) {
                // Return null to indicate invalid, isHaveLyc will use this
                // return value as a condition to decide to show lyrics or not
                return null;
            }
        }
        lrc.sortOffsets();
        return lrc;
    }

    /**
     * isValidLRCFile
     *
     * @param path
     */
    public static void isValidLRCFile(File path) {
        if (path == null) {
            throw new IllegalArgumentException("lrc file path is null");
        } else {
            if (!path.exists()) {
                throw new IllegalArgumentException("lrc file [" + path.getPath() + "] is invalid.");
            }
            if (!path.canRead()) {
                throw new IllegalArgumentException("lrc file [" + path.getPath()
                        + "] can not be read.");
            }
            if (!path.getName().toLowerCase().endsWith(LyricConstants.EXTENSION)) {
                throw new IllegalArgumentException("[" + path.getName()
                        + "] is not a valid lrc file.");
            }
        }
    }

    /**
     * toUtf
     *
     * @param type
     * @param sourceFile
     * @return File
     * @throws IOException
     */
    public static File toUtf(String type, File sourceFile, String tmpFileDir) throws IOException {
        if (LyricConstants.TEXT_UTF_ENCODING.equals(type)) {
            return sourceFile;
        } else {
            StringBuffer utfContent = new StringBuffer();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(
                        sourceFile), type));
                String singleline = null;
                while ((singleline = br.readLine()) != null) {
                    utfContent.append(singleline + System.getProperty("line.separator"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            File tempFile = null;
            Writer write = null;
            try {
                File dir = new File(/* StringConstant.CURRENT_PATH */tmpFileDir,
                        StringConstant.LRC_DIRECTORY
                                + File.separator);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                tempFile = new File(dir, LyricConstants.TEMP_LRC_FILE);
                write = new OutputStreamWriter(new FileOutputStream(tempFile),
                        LyricConstants.TEXT_UTF_ENCODING);
                write.write(new String(utfContent.toString().getBytes(
                        LyricConstants.TEXT_UTF_ENCODING)));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (write != null) {
                    try {
                        write.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return tempFile;
        }
    }

    /**
     * getCharEncoding
     *
     * @param sourceString
     * @return String
     */
    private static String getCharEncoding(String sourceString, byte[] b) {
        StringBuffer testString = new StringBuffer();
        String type = "";
        for (int i = 0; i < sourceString.length(); i++) {
            char sc = sourceString.charAt(i);
            testString.append(Integer.toHexString(sc).toUpperCase());
        }
        String destString = testString.toString();
        Log.d(TAG, "destString=" + destString);
        if (destString.startsWith("FEFF") || destString.startsWith("FFFE")) {
            type = LyricConstants.TEXT_UTF_ENCODING;
        } else if (destString.startsWith("FFFD")) {
            type = LyricConstants.TEXT_UNICODE_ENCODING;
        } else {
            if (IsUTF8Bytes(b)) {
                type = LyricConstants.TEXT_UTF_ENCODING;
            } else {
                type = LyricConstants.TEXT_GB_ENCODING;
            }
        }
        return type;
    }

    /**
     * extractHead
     *
     * @param lrc
     * @param head
     */
    private static void extractHead(LRC lrc, String head) {
        try {
            int l = head.indexOf('[');
            int r = head.indexOf(']');
            int m = head.indexOf(':');
            String key = head.substring(l + 1, m);
            String value = head.substring(m + 1, r);
            lrc.setBasicInfo(key, value);
        } catch (Exception e) {
            throw new LRCFormatException("The lrc file has a bad format.", e);
        }
    }

    /**
     * extractBody
     *
     * @param lrc
     * @param body
     */
    private static void extractBody(LRC lrc, String body) {
        try {
            List<Long> times = new ArrayList<Long>();
            while (true) {
                int l = body.indexOf('[');
                if (l < 0) {
                    break;
                }
                int r = body.indexOf(']');
                int m = body.indexOf(':');
                int d = body.indexOf('.');
                int min = Integer.valueOf(body.substring(l + 1, m));
                String tmpSec = body.substring(m + 1, d > 0 && d < r ? d : r);
                boolean hasNeg = tmpSec.indexOf('-') >= 0;
                int sec = Integer.valueOf(tmpSec);
                int mil = d > 0 && d < r ? Integer.valueOf(body.substring(d + 1, r)) : 0;
                mil = Math.abs(mil);
                if (mil < 100) {
                    mil *= 10;
                }
                long time = min * 60 * 1000 + sec * 1000 + (hasNeg ? -1 : 1) * mil;
                times.add(time);
                body = body.substring(r + 1);
            }

            int ind = lrc.addLyric(body);
            for (long t : times) {
                lrc.addOffset(t, ind);
            }
        } catch (Exception e) {
            throw new LRCFormatException("The lrc file has a bad format.", e);
        }
    }

    // ==========================================================

    /**
     * formatKeyValue
     */
    private static String formatKeyValue(String key, String value) {
        return LyricConstants.BORL + key + LyricConstants.SEP1 + value + LyricConstants.BORR;
    }

    /**
     * formatOffset
     *
     * @param t
     * @return String
     */
    private static String formatOffset(long t) {
        int min = (int) (t / 60000);
        t -= min * 60000;
        int sec = (int) (t / 1000);
        t -= sec * 1000;
        int mil = (int) t;

        boolean negMin = min < 0;
        boolean negSec = sec < 0;
        boolean negMil = mil < 0;

        min = Math.abs(min);
        sec = Math.abs(sec);
        mil = Math.abs(mil);

        mil /= 10;

        return "" + LyricConstants.BORL + (negMin ? "-" : "") + (min < 10 ? "0" + min : min)
                + LyricConstants.SEP1 + (negSec || (sec == 0 && negMil) ? "-" : "")
                + (sec < 10 ? "0" + sec : sec) + LyricConstants.SEP2 + (mil < 10 ? "0" : "") + mil
                + LyricConstants.BORR;
    }

    private static String[] populateLyrics(LRC lrc) {
        ArrayList<String> lyrics = new ArrayList<String>();
        lyrics.add(LyricConstants.DOWNLOAD_LRC_FLAY);
        if (!TextUtils.isEmpty(lrc.artist)) {
            lyrics.add(formatKeyValue(LyricConstants.KEY_AR, lrc.artist));
        }
        if (!TextUtils.isEmpty(lrc.title)) {
            lyrics.add(formatKeyValue(LyricConstants.KEY_TI, lrc.title));
        }
        if (!TextUtils.isEmpty(lrc.album)) {
            lyrics.add(formatKeyValue(LyricConstants.KEY_AL, lrc.album));
        }
        if (!TextUtils.isEmpty(lrc.by)) {
            lyrics.add(formatKeyValue(LyricConstants.KEY_BY, lrc.by));
        }
        if (!TextUtils.isEmpty(lrc.offset + "")) {
            lyrics.add(formatKeyValue(LyricConstants.KEY_OFFSET, lrc.offset + ""));
        }
        if (!TextUtils.isEmpty(lrc.key)) {
            lyrics.add(formatKeyValue(LyricConstants.KEY_KEY, lrc.key));
        }
        lyrics.add("");

        StringBuilder sb = new StringBuilder();
        List<Offset> offsets = lrc.getOffsets();
        boolean[] isRemoved = new boolean[offsets.size()];

        Offset o = null;
        int len = offsets.size();
        for (int i = 0; i < len; i++) {
            if (isRemoved[i]) {
                continue;
            }
            o = offsets.get(i);
            sb.append(formatOffset(o.time));
            int ind = o.lrcInd;
            isRemoved[i] = true;
            for (int j = i + 1; j < len; j++) {
                if (isRemoved[j]) {
                    continue;
                }
                o = offsets.get(j);
                if (o.lrcInd == ind) {
                    sb.append(formatOffset(o.time));
                    isRemoved[j] = true;
                }
            }
            sb.append(lrc.getLyrics().get(ind));
            lyrics.add(sb.toString());
            sb.delete(0, sb.length());
        }

        return lyrics.toArray(new String[lyrics.size()]);
    }

    /**
     * saveToFile
     *
     * @param path
     * @param lrc
     * @return boolean
     */
    public static boolean saveToFile(String path, LRC lrc) {
        if (path == null || lrc == null) {
            Log.v(TAG, "saveToFile failed. Because path = " + path + " | lrc = " + lrc);
            return false;
        }
        String newPath = path + LyricConstants.TMP_FILE_EXTENSION;
        File newLrc = new File(newPath);
        PrintWriter writer = null;

        try {
            if (newLrc.exists()) {
                newLrc.delete();
            }

            newLrc.createNewFile();

            String[] lyrics = populateLyrics(lrc);
            if (lyrics == null) {
                return false;
            }

            writer = new PrintWriter(newLrc);
            for (String l : lyrics) {
                writer.println(l);
            }
            writer.flush();

            File old = new File(path);
            if (old.delete()) {
                newLrc.renameTo(old);
            } else {
                Log.e(TAG, "saveToFile failed. Because delete old file failed.");
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "saveToFile failed. Because\n" + e.getMessage());
            newLrc.delete();
            return false;
        } finally {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        }
        return true;
    }

    public static boolean IsUTF8Bytes(byte[] data) {

        int i = 0;
        int size = data.length;

        while (i < size)
        {
            int step = 0;
            if ((data[i] & 0x80) == 0x00)
            {
                step = 1;
            }
            else if ((data[i] & 0xe0) == 0xc0)
            {
                if (i + 1 >= size)
                    return false;
                if ((data[i + 1] & 0xc0) != 0x80)
                    return false;

                step = 2;
            }
            else if ((data[i] & 0xf0) == 0xe0)
            {
                if (i + 2 >= size)
                    return false;
                if ((data[i + 1] & 0xc0) != 0x80)
                    return false;
                if ((data[i + 2] & 0xc0) != 0x80)
                    return false;

                step = 3;
            }
            else
            {
                return false;
            }

            i += step;
        }

        if (i == size)
            return true;

        return false;
    }
}
