/* Add by spreadst */

package com.android.launcher3;

import com.android.launcher3.AutoInstallsLayout.LayoutParserCallback;
import com.android.launcher3.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.SystemProperties;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class VolteAppsProvider extends ContentProvider {
    private static final String TAG = "VolteAppsProvider";

    static final String EMPTY_DATABASE_CREATED = "APPS_EMPTY_DATABASE_CREATED";

    private static final UriMatcher mUriMatcher;
    public static final String AUTHORITY = "com.android.launcher3.applist";
    private static final int MATCH_APP_LIST = 1;
    private static final int MATCH_APP_LIST_SCREEN = 2;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, Apps.TABLE_NAME, MATCH_APP_LIST);
        mUriMatcher.addURI(AUTHORITY, AppScreens.TABLE_NAME, MATCH_APP_LIST_SCREEN);
    }

    private DatabaseHelper mDBHelper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate called.");
        mDBHelper = new DatabaseHelper(getContext());
        LauncherAppState.setLauncherAppsProvider(this);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query called.");
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        switch (mUriMatcher.match(uri)) {
            case MATCH_APP_LIST:
                builder.setTables(Apps.TABLE_NAME);
                break;
            case MATCH_APP_LIST_SCREEN:
                builder.setTables(AppScreens.TABLE_NAME);
                break;
        }
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        return builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case MATCH_APP_LIST:
                return "vnd.android.cursor.dir/" + Apps.TABLE_NAME;
            case MATCH_APP_LIST_SCREEN:
                return "vnd.android.cursor.dir/" + AppScreens.TABLE_NAME;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert called.");
        if (values == null) {
            return null;
        }

        long rowId = -1;
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        addModifiedTime(values);

        switch (mUriMatcher.match(uri)) {
            case MATCH_APP_LIST:
            case MATCH_APP_LIST_SCREEN:
                rowId = dbInsertAndCheck(mDBHelper, db, args.table, null, values);
                break;
            default:
                Log.d(TAG, "insert, uri can not match, uri: " + uri);
        }

        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        getContext().getContentResolver().notifyChange(uri, null);
        return uri;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        Log.d(TAG, "applyBatch called.");
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result =  super.applyBatch(operations);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        Log.d(TAG, String.format("delete %d record(s).", count));
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.d(TAG, "update called.");
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        addModifiedTime(values);

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);

        if (count > 0) {
            Log.d(TAG, "update ok, count: " + count);
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    public static class Apps implements ChangeLogColumns {
        public static final String TABLE_NAME = "apps";

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String TITLE = "title";
        public static final String COMPONENT_PKG = "component_pkg";
        public static final String COMPONENT_CLS = "component_cls";
        public static final String CONTAINER = "container";
        public static final String SCREEN = "screen";
        public static final String CELLX = "cell_x";
        public static final String CELLY = "cell_y";
        public static final String ITEM_TYPE = "item_type";
        static final String PROFILE_ID = "profile_id";

        public static final int ITEM_TYPE_APPLICATION = 0;
        public static final int ITEM_TYPE_FOLDER = 2;

        public static Uri getContentUri(long id) {
            return Uri.parse("content://" + AUTHORITY + "/" + Apps.TABLE_NAME + "/" + id);
        }
    }

    public static class AppScreens implements ChangeLogColumns {
        public static final String TABLE_NAME = "screens";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        public static final String SCREEN_RANK = "screen_rank";
    }

    public long generateNewItemId() {
        return mDBHelper.generateNewItemId();
    }

    public long generateNewScreenId() {
        return mDBHelper.generateNewScreenId();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper implements LayoutParserCallback {
        private static final String DATABASE_NAME = "applist.db";
        private static final int DATABASE_VERSION = 1;

        private long mMaxItemId = -1;
        private long mMaxScreenId = -1;

        private final Context mContext;
        private final PackageManager mPackageManager;

        // Tags
        private static final String TAG_RESOLVE = "resolve";
        private static final String TAG_FAVORITES = "favorites";
        private static final String TAG_FAVORITE = "favorite";
        private static final String TAG_SHORTCUT = "shortcut";
        private static final String TAG_FOLDER = "folder";

        // Style attrs -- "Favorite"
        private static final String ATTR_CLASS_NAME = "className";
        private static final String ATTR_PACKAGE_NAME = "packageName";
        private static final String ATTR_SCREEN = "screen";
        private static final String ATTR_CONTAINER = "container";
        private static final String ATTR_X = "x";
        private static final String ATTR_Y = "y";
        private static final String ATTR_TITLE = "title";
        private static final String ATTR_ENABLE="enable";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
            mPackageManager = context.getPackageManager();
            Log.d(TAG, "DatabaseHelper.constructor called.");

            // In the case where neither onCreate nor onUpgrade gets called, we read the maxId from
            // the DB here
            if (mMaxItemId == -1) {
                mMaxItemId = initializeMaxItemId(getReadableDatabase());
            }
            if (mMaxScreenId == -1) {
                mMaxScreenId = initializeMaxScreenId(getReadableDatabase());
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "DatabaseHelper.onCreate called.");
            UserManagerCompat userManager = UserManagerCompat.getInstance(mContext);
            long userSerialNumber = userManager.getSerialNumberForUser(
                    UserHandleCompat.myUserHandle());

            // Create apps table
            db.execSQL("CREATE TABLE IF NOT EXISTS " + Apps.TABLE_NAME
                    + "("
                    + Apps._ID + " INTEGER PRIMARY KEY,"
                    + Apps.TITLE + " TEXT,"
                    + Apps.COMPONENT_PKG + " TEXT,"
                    + Apps.COMPONENT_CLS + " TEXT,"
                    + Apps.CONTAINER + " INTEGER,"
                    + Apps.SCREEN + " INTEGER,"
                    + Apps.CELLX + " INTEGER,"
                    + Apps.CELLY + " INTEGER,"
                    + Apps.ITEM_TYPE + " INTEGER,"
                    + Apps.MODIFIED + " INTEGER NOT NULL DEFAULT 0,"
                    + Apps.PROFILE_ID + " INTEGER DEFAULT " + userSerialNumber
                    + ");");

            // Create screens table
            db.execSQL("CREATE TABLE IF NOT EXISTS " + AppScreens.TABLE_NAME
                    + "("
                    + AppScreens._ID + " INTEGER,"
                    + AppScreens.SCREEN_RANK + " INTEGER,"
                    + LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0"
                    + ");");

            setFlagEmptyDbCreated();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Do nothing now.
        }

        private void setFlagEmptyDbCreated() {
            Log.d(TAG, "DatabaseHelper.setFlagEmptyDbCreated called.");
            String spKey = LauncherAppState.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(EMPTY_DATABASE_CREATED, true);
            editor.commit();
        }

        public void loadApps(SQLiteDatabase db) {
            Log.d(TAG, "loadApps called.");
            ArrayList<Long> screenIds = new ArrayList<Long>();

            // load from xml file
            loadFavoritesRecursive(db, mContext.getResources(), R.xml.default_apps, screenIds);

            // Ensure that the max ids are initialized
            mMaxItemId = initializeMaxItemId(db);
            mMaxScreenId = initializeMaxScreenId(db);
        }

        private void loadFavoritesRecursive(SQLiteDatabase db, Resources res, int resourceId,
                                            ArrayList<Long> screenIds) {
            HashMap<Long, Integer> screenApps = new HashMap<Long, Integer>();
            ContentValues values = new ContentValues();
            try {
                XmlResourceParser parser = res.getXml(resourceId);
                beginDocument(parser, TAG_FAVORITES);

                final int depth = parser.getDepth();

                int type;
                while (((type = parser.next()) != XmlPullParser.END_TAG ||
                        parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue;
                    }
                    boolean added = false;
                    final String name = parser.getName();

                    // Assuming it's a <favorite> at this point
                    long container = LauncherSettings.Favorites.CONTAINER_APPLIST;
                    String strContainer = getAttributeValue(parser, ATTR_CONTAINER);
                    if (strContainer != null) {
                        container = Long.valueOf(strContainer);
                    }

                    String screen = getAttributeValue(parser, ATTR_SCREEN);
                    String x = getAttributeValue(parser, ATTR_X);
                    String y = getAttributeValue(parser, ATTR_Y);

                    final String pkg = getAttributeValue(parser, ATTR_PACKAGE_NAME);
                    final String cls = getAttributeValue(parser, ATTR_CLASS_NAME);

                    String enable = getAttributeValue(parser, ATTR_ENABLE);

                    Log.v(TAG, String.format(("%" + (2 * (depth + 1)) + "s<name[%s] pkg[%s] cls[%s] container[%d] screen[%s] x[%s] y[%s]>"),
                            "", name, (pkg == null ? "null" : (" \"" + pkg + "\"")), (cls == null ? "null" : (" \"" + cls + "\"")),
                            container, screen, x, y));

                    values.clear();
                    values.put(Apps.CONTAINER, container);
                    values.put(Apps.SCREEN, screen);

                    long screenId = Long.valueOf(screen);
                    int screenAppsCount = 0;
                    if (screenApps.containsKey(screenId)) {
                        screenAppsCount = screenApps.get(screenId);
                    } else {
                        screenApps.put(Long.valueOf(screen), 0);
                    }

                    if (TAG_FAVORITE.equals(name)) {
                        long id = addApp(db, values, parser);
                        added = id >= 0;
                    } else if (TAG_FOLDER.equals(name)) {
                        added = loadFolder(db, values, res, parser);
                    }

                    if (added) {
                        screenApps.put(screenId, screenApps.get(screenId) + 1);
                        // Keep track of the set of screens which need to be added to the db.
                        if (!screenIds.contains(screenId)) {
                            screenIds.add(screenId);
                        }
                    }
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Got exception parsing favorites.", e);
            } catch (IOException e) {
                Log.w(TAG, "Got exception parsing favorites.", e);
            } catch (RuntimeException e) {
                Log.w(TAG, "Got exception parsing favorites.", e);
            }
        }

        private boolean loadFolder(SQLiteDatabase db, ContentValues values, Resources res,
                                   XmlResourceParser parser) throws IOException, XmlPullParserException {
            final String title;
            final int titleResId = getAttributeResourceValue(parser, ATTR_TITLE, 0);
            if (titleResId != 0) {
                title = res.getString(titleResId);
            } else {
                title = mContext.getResources().getString(R.string.folder_name);
            }

            values.put(Apps.TITLE, title);
            long folderId = addFolder(db, values);
            boolean added = folderId >= 0;

            ArrayList<Long> folderItems = new ArrayList<Long>();
            addToFolder(db, res, parser, folderItems, folderId);

            // We can only have folders with >= 2 items, so we need to remove the
            // folder and clean up if less than 2 items were included, or some
            // failed to add, and less than 2 were actually added
            if (folderItems.size() < 2 && folderId >= 0) {
                // Delete the folder
                deleteId(db, folderId);

                // If we have a single item, promote it to where the folder
                // would have been.
                if (folderItems.size() == 1) {
                    final ContentValues childValues = new ContentValues();
                    copyInteger(values, childValues, Apps.CONTAINER);
                    copyInteger(values, childValues, Apps.SCREEN);
                    copyInteger(values, childValues, Apps.CELLX);
                    copyInteger(values, childValues, Apps.CELLY);

                    final long id = folderItems.get(0);
                    db.update(Apps.TABLE_NAME, childValues, Apps._ID + "=" + id, null);
                } else {
                    added = false;
                }
            }
            return added;
        }

        /**
         * Parse folder items starting at {@link XmlPullParser} location. Allow recursive
         * includes of items.
         */
        private void addToFolder(SQLiteDatabase db, Resources res, XmlResourceParser parser,
                                 ArrayList<Long> folderItems, long folderId) throws IOException, XmlPullParserException {
            int type;
            int folderDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > folderDepth) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                final String tag = parser.getName();

                final ContentValues childValues = new ContentValues();
                childValues.put(Apps.CONTAINER, folderId);

                // Read cellX, cellY
                String x = getAttributeValue(parser, ATTR_X);
                String y = getAttributeValue(parser, ATTR_Y);
                childValues.put(Apps.CELLX, x);
                childValues.put(Apps.CELLY, y);
//                String e = getAttributeValue(parser, ATTR_ENABLE);
//                childValues.put(Apps.ENABLE, Integer.parseInt(e));

                if (TAG_FAVORITE.equals(tag) && folderId >= 0) {
                    final long id = addApp(db, childValues, parser);
                    if (id >= 0) {
                        folderItems.add(id);
                    }
                } else {
                    throw new RuntimeException("Folders can contain only shortcuts");
                }
            }
        }

        private long addFolder(SQLiteDatabase db, ContentValues values) {
            values.put(Apps.ITEM_TYPE, Apps.ITEM_TYPE_FOLDER);
            long id = generateNewItemId();
            values.put(Apps._ID, id);
            if (dbInsertAndCheck(this, db, Apps.TABLE_NAME, null, values) <= 0) {
                return -1;
            } else {
                return id;
            }
        }

        private long addApp(SQLiteDatabase db, ContentValues values, XmlResourceParser parser) {
            final String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
            final String className = getAttributeValue(parser, ATTR_CLASS_NAME);

            // Test package name
            if (!TextUtils.isEmpty(packageName)) {
                // Test class name
                if (!TextUtils.isEmpty(className)) {
                    try {
                        ComponentName cn;
                        try {
                            cn = new ComponentName(packageName, className);
                            // If not exist will trigger exception
                            mPackageManager.getActivityInfo(cn, 0);
                        } catch (PackageManager.NameNotFoundException nnfe) {
                            String[] packages = mPackageManager.currentToCanonicalPackageNames(
                                    new String[]{packageName});
                            cn = new ComponentName(packages[0], className);
                            mPackageManager.getActivityInfo(cn, 0);
                        }
                        return addApp(db, values, packageName, className);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Unable to add favorite: " + packageName + "/" + className, e);
                    }
                } else {
                    return addApp(db, values, packageName, null);
                }
                return -1;
            } else {
                Log.e(TAG, String.format("Skipping invalid <favorite> with pkg[%s] cls[%s]",
                        packageName, className));
                return -1;
            }
        }

        private long addApp(SQLiteDatabase db, ContentValues values, String pkgName, String clsName) {
            long id = generateNewItemId();
            values.put(Apps._ID, id);
            values.put(Apps.COMPONENT_PKG, pkgName);
            values.put(Apps.COMPONENT_CLS, clsName);
            values.put(Apps.ITEM_TYPE, Apps.ITEM_TYPE_APPLICATION);
            if (dbInsertAndCheck(this, db, Apps.TABLE_NAME, null, values) < 0) {
                return -1;
            } else {
                return id;
            }
        }

        private void removeFromInstalledApps(String pkg, String cls, ArrayList<AppInfo> installedApps) {
            if (pkg == null) {
                return;
            }
            int removeIndex = -1;
            if (cls != null) {
                // remove exact
                for (int i=0; i<installedApps.size(); i++) {
                    AppInfo info = installedApps.get(i);
                    if (pkg.equals(info.componentName.getPackageName())
                            && cls.equals(info.componentName.getClassName())) {
                        removeIndex = i;
                        break;
                    }
                }
            } else {
                // remove by package name
            }
            if (removeIndex != -1) {
                AppInfo info = installedApps.remove(removeIndex);
                if (info != null) {
                    Log.d(TAG, "Remove app info from installed list: " + info);
                }
            }
        }

        /**
         * Return attribute value, attempting launcher-specific namespace first
         * before falling back to anonymous attribute.
         */
        private static String getAttributeValue(XmlResourceParser parser, String attribute) {
            String value = parser.getAttributeValue(
                    "http://schemas.android.com/apk/res-auto/com.android.launcher3", attribute);
            if (value == null) {
                value = parser.getAttributeValue(null, attribute);
            }
            return value;
        }

        private static int getAttributeResourceValue(XmlResourceParser parser, String attribute,
                                                     int defaultValue) {
            int value = parser.getAttributeResourceValue(
                    "http://schemas.android.com/apk/res-auto/com.android.launcher3", attribute,
                    defaultValue);
            if (value == defaultValue) {
                value = parser.getAttributeResourceValue(null, attribute, defaultValue);
            }
            return value;
        }

        private static final void beginDocument(XmlPullParser parser, String firstElementName)
                throws XmlPullParserException, IOException {
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                ;
            }

            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            if (!parser.getName().equals(firstElementName)) {
                throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                        ", expected " + firstElementName);
            }
        }

        public void checkId(String table, ContentValues values) {
            long id = values.getAsLong(LauncherSettings.BaseLauncherColumns._ID);
            if (table == AppScreens.TABLE_NAME) {
                mMaxScreenId = Math.max(id, mMaxScreenId);
            } else {
                mMaxItemId = Math.max(id, mMaxItemId);
            }
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            return initializeMaxId(db, Apps.TABLE_NAME);
        }

        private long initializeMaxScreenId(SQLiteDatabase db) {
            return initializeMaxId(db, AppScreens.TABLE_NAME);
        }

        private long initializeMaxId(SQLiteDatabase db, String table) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + table, null);

            // get the result
            final int maxIdIndex = 0;
            long id = -1;
            if (c != null && c.moveToNext()) {
                id = c.getLong(maxIdIndex);
            }
            if (c != null) {
                c.close();
            }

            if (id == -1) {
                throw new RuntimeException("Error: could not query max screen id");
            }

            Log.d(TAG, "initializeMaxId for tab: " + table + ", maxId: " + id);
            return id;
        }

        // Generates a new ID to use for an object in your database. This method should be only
        // called from the main UI thread. As an exception, we do call it when we call the
        // constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        @Override
        public long generateNewItemId() {
            if (mMaxItemId < 0) {
                throw new RuntimeException("Error: max item id was not initialized");
            }
            mMaxItemId += 1;
            return mMaxItemId;
        }

        // Generates a new ID to use for an workspace screen in your database. This method
        // should be only called from the main UI thread. As an exception, we do call it when we
        // call the constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        public long generateNewScreenId() {
            if (mMaxScreenId < 0) {
                throw new RuntimeException("Error: max screen id was not initialized");
            }
            mMaxScreenId += 1;
            return mMaxScreenId;
        }

        @Override
        public long insertAndCheck(SQLiteDatabase db, ContentValues values) {
            return dbInsertAndCheck(this, db, Apps.TABLE_NAME, null, values);
        }
    }

    private static long dbInsertAndCheck(DatabaseHelper helper,
                                         SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        if (!values.containsKey(LauncherSettings.ChangeLogColumns._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        helper.checkId(table, values);
        return db.insert(table, nullColumnHack, values);
    }

    /**
     *
     * @return true if we init default icons from xml file.
     */
    synchronized public boolean loadDefaultAppsIfNecessary() {
        Log.d(TAG, "loadDefaultAppsIfNecessary called.");
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE);

        if (sp.getBoolean(EMPTY_DATABASE_CREATED, false)) {
            Log.d(TAG, "loading default apps");

            // Populate favorites table with initial favorites
            SharedPreferences.Editor editor = sp.edit().remove(EMPTY_DATABASE_CREATED);
            mDBHelper.loadApps(mDBHelper.getWritableDatabase());
            editor.commit();
            return true;
        }
        return false;
    }

    private static void deleteId(SQLiteDatabase db, long id) {
        Uri uri = Apps.getContentUri(id);
        SqlArguments args = new SqlArguments(uri, null, null);
        db.delete(args.table, args.where, args.args);
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

    private static void copyInteger(ContentValues from, ContentValues to, String key) {
        to.put(key, from.getAsInteger(key));
    }

    private void addModifiedTime(ContentValues values) {
        values.put(Apps.MODIFIED, System.currentTimeMillis());
    }
}
