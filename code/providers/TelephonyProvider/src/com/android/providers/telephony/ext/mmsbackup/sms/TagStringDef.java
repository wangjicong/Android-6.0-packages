
package com.android.providers.telephony.ext.mmsbackup.sms;

public interface TagStringDef {

    static final String _begin_vmsg = "BEGIN:VMSG";

    static final String _end_vmsg = "END:VMSG";

    static final String _begin_vcard = "BEGIN:VCARD";

    static final String _end_vcard = "END:VCARD";

    static final String _begin_vbody = "BEGIN:VBODY";

    static final String _end_vbody = "END:VBODY";

    static final String _version = "VERSION:";

    static final String _version1_1 = "1.1";

    static final String _locked = "X-LOCKED:";

    static final String ESCAPE_CHAR = "=";

    static final String CHUNK_SEPARATOR = "\r\n";

    static final String _simid = "X-SIMID:";

    static final String _type = "X-TYPE:";// sms or mms

    static final String _status = "X-READ:";

    static final String _box = "X-BOX:";

    static final String _date = "Date:";

    static final String _tel = "TEL:";

    static final String SHORT_MESSAGE_FILE = "sms.vmsg";

    static final String SHORT_MESSAGE_FILE_1 = "msg_box.xml";

    static final String SMS_XML = "backup.xml";

    // maybe,this will be change in the future
    final static String contentShareString = "Subject;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:";
    final static String xtypeString = "X-TYPE:SMS";

    // the meaning of number in db that the data stand for

    final static int INBOX = 1;

    final static int SEND = 2;

    final static int DRAFT = 3;

    final static int OUTBOX = 4;

    final static int FAILED = 5;

    final static int READ = 1;

    final static int UNREAD = 0;

    final static int LOCKED = 1;

    final static int UNLOCKED = 0;

    final static int PHONEID0 = 0;
    final static int PHONEID1 = 1;

    final static String _InBox = "INBOX";

    final static String _Send = "SENDBOX";

    final static String _Draft = "DRAFT";

    final static String _OutBox = "OUTBOX";

    final static String _Failed = "FAILED";

    final static String _Read = "READ";

    final static String _UnRead = "UNREAD";

    final static String _SimId0 = "0";
    final static String _SimId1 = "1";
    final static String _SimId2 = "2";

    final static String _Locked = "LOCKED";

    final static String _UnLocked = "UNLOCKED";

    // data in sms db
    static final String cADDRESS = "address";// text

    static final String cDATE = "date";// int

    static final String cREAD = "read";// int default 0

    static final String cTYPE = "type";// int 3:draft; 2:send; 1:inbox

    static final String cBODY = "body";// text

    static final String cTHREAD_ID = "thread_id";

    static final String cSEEN = "seen";

    static final String cSUBJECT = "subject";

    static final String cLOCKED = "locked";// 1 locked ; 0 unlocked

    static final String cPHONE_ID = "sub_id";

    // static final String[] projection = {
    // cADDRESS, cDATE, cREAD, cBODY, cTYPE, cTHREAD_ID,cLOCKED,cPHONE_ID
    // };
    static final int mADDRESS = 0;

    static final int mDATE = 1;

    static final int mREAD = 2;

    static final int mBODY = 3;

    static final int mTYPE = 4;

    static final int mTHREAD_ID = 5;

    static final int mLOCKED = 6;

    // cmcc has no phone_id,notice
    static final int mPHONE_ID = 7;

}
