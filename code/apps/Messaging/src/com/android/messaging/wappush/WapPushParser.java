package com.android.messaging.wappush;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Telephony.Sms;
import android.util.Config;
import android.util.Log;

import com.android.messaging.wappush.WbxmlParser;


public class WapPushParser {
    /**
     * The log tag.
     */
    private static final String LOG_TAG = "WapPushParser";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;
    private static final String DATE_FORMAT="yyyyMMddHHmmss";

    /**
     * The wap push data.
     */
    private ByteArrayInputStream mWapPushDataStream = null;
    private WbxmlParser mParser = null;
    private WapPushMsg mPushMsg = null;//store the parsing results.
    /**
     * Constructor.
     *
     * @param wapPushDataStream wap push data to be parsed
     */
    public WapPushParser(byte[] pushDataStream) {
        mWapPushDataStream = new ByteArrayInputStream(pushDataStream);
    }

    /**
     * Parse the wap push.
     * type  the push message type WAP_PUSH_TYPE_SI or WAP_PUSH_TYPE_SL
     * @return the push structure if parsing successfully.
     *         null if parsing error happened or mandatory fields are not set.
     */
    public  WapPushMsg parse(int type) {
        String tagName = null;

        if (mWapPushDataStream == null) {
            Log.e(LOG_TAG, "mWapPushDataStream is not set!");
            return null;
        }

        mPushMsg = new WapPushMsg(type);

        if (WapPushMsg.WAP_PUSH_TYPE_SI == type) {
            mParser = createSIParser();
        } else if (WapPushMsg.WAP_PUSH_TYPE_SL == type) {
            mParser = createSLParser();
        } else {
            Log.e(LOG_TAG, "wap push unknown type=" + type);
            return null;
        }

        try {
            mParser.setInput(mWapPushDataStream, null);
            if(LOCAL_LOGV) {
                Log.i(LOG_TAG, "Document charset : " + mParser.getInputEncoding());
            }

            int eventType = mParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        Log.i(LOG_TAG, "Start document");
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        Log.i(LOG_TAG, "End document");
                        break;
                    case XmlPullParser.START_TAG:
                        Log.i(LOG_TAG, "Start tag = "+mParser.getName());
                        elementParser(mParser.getName()); //parsing si or sl tag
                        //add by liguxiang 08-18-11 for NEWMS00109865 begin
                        String expires = mPushMsg.getAttributeValueString(WapPushMsg.WAP_PUSH_PROJECTION_SI_EXPIRED);
                        if (true) {
                          Log.d(LOG_TAG,"expires = " + expires);
                        }
                        SimpleDateFormat date = new SimpleDateFormat(DATE_FORMAT);
                        String currentTime = date.format(new Date());
                        if (true) {
                        Log.d(LOG_TAG,"currentSystemTime = " + currentTime);
                        }
                        if (expires != null) {
                            if (expires.compareTo(currentTime) < 0) {
                                return null;
                            }
                        }
                        //add by liguxiang 08-18-11 for NEWMS00109865 end
                        break;
                    case XmlPullParser.END_TAG:
                        Log.i(LOG_TAG, "End tag = "+mParser.getName());
                        break;
                    case XmlPullParser.TEXT:
                        Log.i(LOG_TAG, "Text = "+mParser.getText());
                        if (WapPushMsg.WAP_PUSH_TYPE_SI == type) {
                            mPushMsg.setAttributeValue(WapPushMsg.WAP_PUSH_PROJECTION_SI_TEXT, mParser.getText());
                        }
                        break;
                    default:
                        Log.i(LOG_TAG, "unknown event type =  "+eventType);
                        break;
                }

                eventType = mParser.next();
            }

        } catch (IOException e) {
            if(LOCAL_LOGV) {
                Log.e(LOG_TAG, e.toString());
            }
            return null;
        } catch (XmlPullParserException e) {
            if(LOCAL_LOGV) {
                Log.e(LOG_TAG, e.toString());
            }
            return null;
        }

        return mPushMsg;
    }
    public static int cleanExpiredWapPush(Context context) {
        int count = 0;
        String nowTime = new SimpleDateFormat(DATE_FORMAT)
                .format(new Date());
        if(true){
        Log.d(LOG_TAG, "cleanExpiredWapPush()-->>      nowTime = " + nowTime);
        }
        ContentResolver cr = context.getContentResolver();
        try {
            count = cr.delete(Sms.CONTENT_URI, " 0 < expired and expired <= " + nowTime +" and wap_push=1", null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "cleanExpiredWapPush", e);
        } finally {
        }
        return count;
    }
    private  static WbxmlParser createSIParser() {
        WbxmlParser p = new WbxmlParser();
        p.setTagTable(0, TAG_TABLE_SI);
        p.setAttrStartTable(0, ATTR_START_TABLE_SI);
        p.setAttrValueTable(0, ATTR_VALUE_TABLE_SI_SL);
        return p;
    }

    private  static WbxmlParser createSLParser() {
        WbxmlParser p = new WbxmlParser();
        p.setTagTable(0, TAG_TABLE_SL);
        p.setAttrStartTable(0, ATTR_START_TABLE_SL);
        p.setAttrValueTable(0, ATTR_VALUE_TABLE_SI_SL);
        return p;
    }

    private  void elementParser(String tagName){
        int attrCount = mParser.getAttributeCount();
        String attrName = null;
        String attrValue = null;

        if (tagName.equalsIgnoreCase(TAG_TABLE_SL[0])
            || tagName.equalsIgnoreCase(TAG_TABLE_SI[0])
            || tagName.equalsIgnoreCase(TAG_TABLE_SI[1])) {
            for (int i = 0; i < attrCount; i++) {
                attrValue = mParser.getAttributeValue(i);
                attrName = mParser.getAttributeName(i);
                if (LOCAL_LOGV) {
                    Log.i(LOG_TAG, "attrName = " + attrName + ", attrValue =" + attrValue);
                }

                if (PUSH_ATTR_NAME_MAP.containsKey(attrName)) {
                    int attr = PUSH_ATTR_NAME_MAP.get(attrName);
                    mPushMsg.setAttributeValue(attr, attrValue);
                }
            }
        } else {
            Log.e(LOG_TAG, "Unknown tag = " + tagName);
        }
    }

    //// SI Documents
    private static final String [] TAG_TABLE_SI = {
          "si",         // 05
          "indication", // 06
          "info",       // 07
          "item",       // 08
    };


    private static final String [] ATTR_START_TABLE_SI = {
        "action=signal-none",   // 05
        "action=signal-low",    // 06
        "action=signal-medium", // 07
        "action=signal-high",   // 08
        "action=signal-delete", // 09
        "created",              // 0A
        "href",                 // 0B
        "href=http://",         // 0C
        "href=http://www.",     // 0D
        "href=https://",        // 0E
        "href=https://www.",    // 0F
        "si-expires",           // 10
        "si-id",                // 11
        "class",                // 12
    };


    //// SL Documents
    private static final String [] TAG_TABLE_SL = {
          "sl", // 05
    };

    private static final String [] ATTR_START_TABLE_SL = {
        "action=execute-low", // 05
        "action=execute-high",// 06
        "action=cache",       // 07
        "href",               // 08
        "href=http://",       // 09
        "href=http://www.",   // 0A
        "href=https://",      // 0B
        "href=https://www.",  // 0C
    };

    //// COMMON FOR ALL PUHS DOCUMENTS
    private static final String [] ATTR_VALUE_TABLE_SI_SL = {
        ".com/", // 85
        ".edu/", // 86
        ".net/", // 87
        ".org/", // 88
    };

    private static Hashtable<String,Integer> PUSH_ATTR_NAME_MAP;
    static {
        PUSH_ATTR_NAME_MAP = new Hashtable<String, Integer>(5);
        PUSH_ATTR_NAME_MAP.put("action",     WapPushMsg.WAP_PUSH_PROJECTION_PRIOR);
        PUSH_ATTR_NAME_MAP.put("href",       WapPushMsg.WAP_PUSH_PROJECTION_HREF);
        PUSH_ATTR_NAME_MAP.put("si-expires", WapPushMsg.WAP_PUSH_PROJECTION_SI_EXPIRED);
        PUSH_ATTR_NAME_MAP.put("created",    WapPushMsg.WAP_PUSH_PROJECTION_SI_CREATED);
        PUSH_ATTR_NAME_MAP.put("si-id",      WapPushMsg.WAP_PUSH_PROJECTION_SI_ID);
    }

    private static Hashtable<String,Integer> PUSH_ATTR_VALUE_MAP;
    static {
        PUSH_ATTR_VALUE_MAP = new Hashtable<String, Integer>(7);
        PUSH_ATTR_VALUE_MAP.put("signal-none",   WapPushMsg.WAP_PUSH_PRIO_NONE);
        PUSH_ATTR_VALUE_MAP.put("signal-low",    WapPushMsg.WAP_PUSH_PRIO_LOW);
        PUSH_ATTR_VALUE_MAP.put("signal-medium", WapPushMsg.WAP_PUSH_PRIO_MEDIUM);
        PUSH_ATTR_VALUE_MAP.put("signal-high",   WapPushMsg.WAP_PUSH_PRIO_HIGH);
        PUSH_ATTR_VALUE_MAP.put("signal-delete", WapPushMsg.WAP_PUSH_PRIO_DELETE);
        PUSH_ATTR_VALUE_MAP.put("execute-low",   WapPushMsg.WAP_PUSH_SL_PRIO_LOW);
        PUSH_ATTR_VALUE_MAP.put("execute-high",  WapPushMsg.WAP_PUSH_SL_PRIO_HIGH);
        PUSH_ATTR_VALUE_MAP.put("cache",         WapPushMsg.WAP_PUSH_SL_PRIO_CACHE);
    }

    public static int getPushAttrValue(String key) {
        // If the attribute is not specified, the value "signal-medium" is
        // used.(copy from the document <wap-167-serviceind-20010731-a>)
        int nullresult = WapPushMsg.WAP_PUSH_PRIO_MEDIUM;

        if (key == null || key.trim().length() <= 0) {
            return nullresult;
        }
        Integer value = PUSH_ATTR_VALUE_MAP.get(key);
        if (value == null)
            return nullresult;
        return value.intValue();

    }
}

