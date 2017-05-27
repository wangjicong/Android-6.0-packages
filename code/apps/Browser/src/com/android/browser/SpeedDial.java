/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import android.net.Uri;

public class SpeedDial {

    public static final Uri CONTENT_URI = Uri.parse("content://com.android.browser/speeddial");

    public static final String _ID = "_id";
    public static final String TITLE = "title";
    public static final String URL = "url";
    public static final String FAVICON = "favicon";

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/speeddial";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/speeddial";

    public static final int MAX_COUNT = 18;
}
