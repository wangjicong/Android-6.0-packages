
package com.sprd.music.lrc;

public class LyricConstants {

    static final String KEY_AR = "ar";

    static final String KEY_TI = "ti";

    static final String KEY_AL = "al";

    static final String KEY_BY = "by";

    static final String KEY_OFFSET = "offset";

    static final String KEY_KEY = "key";

    static final char BORL = '[';

    static final char BORR = ']';

    static final char SEP1 = ':';

    static final char SEP2 = '.';

    static final String REGEX = "(\\[[a-zA-Z]+:[^\\]]*\\])|((\\[[+-]?[0-9]+:[+-]?[0-5]?[0-9](\\.[+-]?[0-9]{1,3})?\\])+[^\\[]*)";

    static final String EXTENSION = ".lrc";

    static final String TMP_FILE_EXTENSION = ".ltf";

    static final String TEXT_UTF_ENCODING = "UTF_8";

    static final String TEXT_UNICODE_ENCODING = "UNICODE";

    static final String TEXT_GB_ENCODING = "GB2312";

    static final String TEMP_LRC_FILE = "/lrc_temp.lrc";

    static final String DOWNLOAD_LRC_FLAY = "==download==";

    static final int RIC_STATE_FLAG_INIT = 0;

    public static final int RIC_STATE_MULTI_LRC = 10;

    public static final int RIC_STATE_FLAG_NO_CONNECTION = 11;

    public static final int RIC_STATE_FLAG_NO_SDCARD = 16;

    public static final int RIC_STATE_FLAG_NO_SPACE = 17;

    public static final int RIC_STATE_FLAG_SDCARD_NOT_WRITE = 18;

    public static final int RIC_STATE_FLAG_DOWNLOAD_OK = 19;

    public static final int RIC_STATE_NO_FIND_LRC = 20;

    static final String STRING_UTF_ENCOIDNG = "UTF-8";

    static final int LYRIC_SCROLL = 1;

    static final int LYRIC_AJUST = 2;

    static final int LYRIC_SEEK_DOWN = 3;

    static final int LYRIC_SEEK_UP = 4;

    static final int DEFAULT_DELAY = 500;

    static final int DEFAULT_OFFSET = 0;

    static final int DEFAULT_AJUST_TIME = 500;

    static final int DEFAULT_ITEM_HEIGHT = 29;

}
