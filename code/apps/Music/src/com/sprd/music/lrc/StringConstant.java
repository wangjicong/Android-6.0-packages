
package com.sprd.music.lrc;

import java.io.File;

import android.os.Environment;

/**
 *
 */
public class StringConstant {
    /**
     *
     */
    public static final String LRC_LYRIC_PATTERN = "(?<=LRC/Lyric - <a href=\").*?(?=\"  target=\"_blank\" >HTML版</a>)";

    /**
     *
     */
    public static final String TEXTSTRING = "LRC/Lyric - <a href=\"";

    /**
     *
     */
    public static final String HREFSTRING = "<base href=\"";

    /**
     *
     */
    public static final String BODYSTRING = "<body>";

    /**
     *
     */
    public static final String BODY_BODY_PATTERN = "(?<=<body>).*?(?=</body>)";

    /**
     *
     */
    public static final String CHARSET_GBK = "GBK";

    /**
     *
     */
    public static final String CHARSET_UTF8 = "UTF-8";

    /**
     *
     */
    public static final String CHARSET_GB2312 = "GB2312";

    /**
     *
     */
    public static final String HOST = "Host";

    /**
     *
     */
    public static final String HOST_VALUE = "www.baidu.com";

    /**
     *
     */
    public static final String FILETYPE_LRC = "filetype:lrc ";

    /**
     *
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     *
     */
    public static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11";

    /**
     *
     */
    public static final String ACCEPT = "Accept";

    /**
     *
     */
    public static final String ACCEPT_VALUE = "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5";

    /**
     *
     */
    public static final String ACCEPT_LANGUAGE_VALUE = "zh-cn,zh;q=0.5";

    /**
     *
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    /**
     *
     */
    public static final String KEEP_ALIVE_VALUE = "300";

    /**
     *
     */
    public static final String KEEP_ALIVE = "Keep-Alive";

    /**
     *
     */
    public static final String REFERER_VALUE = "http://www.baidu.com/";

    /**
     *
     */
    public static final String REFERER = "Referer";

    /**
     *
     */
    public static final String CONNECTION_VALUE = "keep-alive";

    /**
     *
     */
    public static final String CONNECTION = "Connection";

    /**
     *
     */
    public static final String LINK_FACTOR = " - ";

    /**
     *
     */
    public static final String LYRIC_SUFFIX = ".lrc";

    /**
     *
     */
    public static final File CURRENT_PATH = Environment.getExternalStorageDirectory();

    /**
     *
     */
    public static final String LRC_DIRECTORY = "Lyrics";

    /**
     *
     */
    public static final String DOWNLOAD_FROM_BAIDU_HEAD = "http://www.baidu.com/s?wd=";

    /**
     *
     */
    public static final String SINGLE_RESULT_URL = "http://yoyolrc.appspot.com/YOYO?cmd=getSingleResult&artist={0}&title={1}";

    /**
    *
    */
    public static final String MULTI_RESULTLIST_URL = "http://yoyolrc.appspot.com/YOYO?cmd=getResultList&artist={0}&title={1}";

    /**
     *
     */
    public static final String LYRIC_CONTENT_URL = "http://yoyolrc.appspot.com/YOYO?cmd=getLyricContent&id={0}&lrcId={1}&lrcCode={2}&artist={3}&title={4}";

}
