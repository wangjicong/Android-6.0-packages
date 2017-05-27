package com.sprd.cellbroadcastreceiver.provider;
import com.sprd.android.config.OptConfig;

public interface CommonSettingTableDefine {

    public static final String TABLE_NAME               = "common_setting";

    public static final String _ID                      = "_id";
    public static final String SUBID                    = "sub_id";
    public static final String RING_URL                 = "ring_url";
    public static final String ENABLED_CELLBROADCAST    = "enable";

    public static final String[] QUERY_COLUMNS          = { _ID, SUBID, RING_URL, ENABLED_CELLBROADCAST};

    public static final String CREATE_COMMOMSETTING_TABLE = OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA ?
    		"CREATE TABLE "+ TABLE_NAME + " ("
    	            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    	            + SUBID + " INTEGER,"
    	            + ENABLED_CELLBROADCAST + " INTEGER DEFAULT 0,"
    	            + RING_URL + " TEXT);"
    	  : "CREATE TABLE "+ TABLE_NAME + " ("
    	            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    	            + SUBID + " INTEGER,"
    	            + ENABLED_CELLBROADCAST + " INTEGER DEFAULT 1,"
    	            + RING_URL + " TEXT);";
}
