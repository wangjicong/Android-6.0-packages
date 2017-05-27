package com.sprd.compass;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RelativeLayout;
import android.view.MenuItem;

public class CompassSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        OnPreferenceClickListener {
    private static String TAG = CompassSettings.class.getSimpleName();
    public static final String KEY_BACKGROUND_KEY = "background_pref_key";
    public static final String KEY_POINTER_TYPE_KEY = "pointer_type_pref_key";
    public static final String KEY_SHARE_KEY = "share_pref_key";
    public static final String DEFAULT_STRING = "1";
    public static final int DEFAULT_VALUE = 1;
    public static final String CUSTOMIZE_CONFIG = "customize";

    private final int IMAGE_CODE = 0;
    private final int SELECT_PIC_KITKAT = 1;

    RelativeLayout mContainer;

    private ListPreference mBackgroundPreference;
    private ListPreference mPointerTypePreference;
    private PreferenceScreen mSharePreference;
    private SharedPreferences mSharedPreferences;
    private String mLocationStr = "";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        mLocationStr = getIntent().getStringExtra("mylocation");

        addPreferencesFromResource(R.xml.settings_preference);
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        actionbar.setDisplayHomeAsUpEnabled(true);

        mSharedPreferences = getSharedPreferences(CUSTOMIZE_CONFIG, Activity.MODE_PRIVATE);

        mBackgroundPreference = (ListPreference) findPreference(KEY_BACKGROUND_KEY);
        mPointerTypePreference = (ListPreference) findPreference(KEY_POINTER_TYPE_KEY);
        mSharePreference = (PreferenceScreen) findPreference(KEY_SHARE_KEY);

        if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 1) {
            mBackgroundPreference.setSummary(R.string.earth);
        } else if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 2) {
            mBackgroundPreference.setSummary(R.string.kraft);
        } else if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 3) {
            mBackgroundPreference.setSummary(R.string.grain);
        } else {
            mBackgroundPreference.setSummary(getResources().getString(R.string.custom_background_summary));
        }

        if (mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE) == 1) {
            mPointerTypePreference.setSummary(R.string.fashion);
        } else if (mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE) == 2) {
            mPointerTypePreference.setSummary(R.string.sinan);
        } else if (mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE) == 3) {
            mPointerTypePreference.setSummary(R.string.restore_ancient);
        }

        mBackgroundPreference.setValue(String.valueOf(mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE)));

        mSharedPreferences.edit()
                .putInt(KEY_BACKGROUND_KEY, mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE)).commit();
        mBackgroundPreference.setOnPreferenceChangeListener(this);
        mPointerTypePreference.setValue(String.valueOf(mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE)));
        mPointerTypePreference.setOnPreferenceChangeListener(this);

        mSharePreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        if (KEY_SHARE_KEY.equals(preference.getKey())) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            // intent.putExtra(Intent.EXTRA_SUBJECT, "Share");
            intent.putExtra(Intent.EXTRA_TEXT, mLocationStr);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, /* getTitle() */"Share"));
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub

        if (preference == mBackgroundPreference) {
            int value = 0;
            try {
                value = Integer.parseInt((String) newValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }

            if (value != 4) {

                mSharedPreferences.edit().putInt(KEY_BACKGROUND_KEY, value).commit();
            }

            if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 1) {
                mBackgroundPreference.setSummary(R.string.earth);
            } else if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 2) {
                mBackgroundPreference.setSummary(R.string.kraft);
            } else if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 3) {
                mBackgroundPreference.setSummary(R.string.grain);
            } else {
                mBackgroundPreference.setSummary(getResources().getString(R.string.custom_background_summary));
            }

            mBackgroundPreference.setValue((String) newValue);
            if (value == 4) {

                mBackgroundPreference.setSummary(getResources().getString(R.string.custom_background_summary));

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    startActivityForResult(intent, SELECT_PIC_KITKAT);
                } else {
                    startActivityForResult(intent, IMAGE_CODE);
                }

            }
        } else if (preference == mPointerTypePreference) {
            int value = 0;
            try {
                value = Integer.parseInt((String) newValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
            mSharedPreferences.edit().putInt(KEY_POINTER_TYPE_KEY, value).commit();

            if (mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE) == 1) {
                mPointerTypePreference.setSummary(R.string.fashion);
            } else if (mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE) == 2) {
                mPointerTypePreference.setSummary(R.string.sinan);
            } else if (mSharedPreferences.getInt(KEY_POINTER_TYPE_KEY, DEFAULT_VALUE) == 3) {
                mPointerTypePreference.setSummary(R.string.restore_ancient);
            }

            mPointerTypePreference.setValue((String) newValue);
        }

        return false;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub

        if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 1) {
            mBackgroundPreference.setSummary(R.string.earth);
        } else if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 2) {
            mBackgroundPreference.setSummary(R.string.kraft);
        } else if (mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE) == 3) {
            mBackgroundPreference.setSummary(R.string.grain);
        }
        super.onResume();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return false;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            Log.e("TAG", "ActivityResult resultCode error");
            mBackgroundPreference
                    .setValue(String.valueOf(mSharedPreferences.getInt(KEY_BACKGROUND_KEY, DEFAULT_VALUE)));
            return;
        }

        mSharedPreferences.edit().putInt(KEY_BACKGROUND_KEY, 4).commit();
        Bitmap bm = null;
        ContentResolver resolver = getContentResolver();

        if (requestCode == IMAGE_CODE) {
            try {
//                Uri originalUri = data.getData();

                String mFilename = CompassSettings.getPath(getApplicationContext(), data.getData());
//                String[] proj = { MediaStore.Images.Media.DATA };
//                Cursor cursor = getContentResolver().query(originalUri, proj, null, null, null);
//                cursor.moveToFirst();
//                int column_index = cursor.getColumnIndex(proj[0]);
//                String path = cursor.getString(column_index);
                Intent intent = new Intent();
                intent.putExtra("path", mFilename);
                setResult(RESULT_OK, intent);

                finish();

            } catch (Exception e) {
                Log.e("TAG", e.toString());
            }

        } else if (requestCode == SELECT_PIC_KITKAT) {

            String mFilename = CompassSettings.getPath(getApplicationContext(), data.getData());
            Intent intent = new Intent();
            intent.putExtra("path", mFilename);
            setResult(RESULT_OK, intent);

            finish();
        }

    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
