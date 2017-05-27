package com.sprd.compass;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;

import java.util.List;
import java.util.Locale;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CompassActivity extends Activity {
    private static String TAG = CompassActivity.class.getSimpleName();
    private final float MAX_ROATE_DEGREE = 1.0f; // most rotate 360°
    private SensorManager mSensorManager;
    private Sensor mOrientationSensor;
    private LocationManager mLocationManager;
    private String mLocationProvider; // Location provider name, GPS or network
    private float mDirection; // current direction
    private float mTargetDirection; // target direction
    private AccelerateInterpolator mInterpolator;
    protected final Handler mHandler = new Handler();
    private boolean mStopDrawing; // stop dram flag
    private boolean mChinease = true; // current language
    private SharedPreferences msp;
    private static final int UNCONSTRAINED = -1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    public final static int PERMISSION_ALL_ALLOWED = 16;
    public final static int PERMISSION_ALL_DENYED = 17;
    public final static int PERMISSION_READ_GPS_ONLY = 18;
    public final static int PERMISSION_READ_EXTERNAL_STORAGE_ONLY = 19;

    private CompassView mCompassView;
    private TextView mLatitudeTV;// Latitude
    private TextView mLongitudeTV;// Longitude
    private TextView mAltitudeTV;
    private ImageButton mSettings;

    private TextView mDirectionTV;

    private RelativeLayout mContainer;

    private int[] mBackgroundArray = new int[3];

    protected Runnable mCompassViewUpdater = new Runnable() {
        @Override
        public void run() {
            if (mCompassView != null && !mStopDrawing) {
                if (mDirection != mTargetDirection) {

                    // calculate the short routine
                    float to = mTargetDirection;
                    if (to - mDirection > 180) {
                        to -= 360;
                    } else if (to - mDirection < -180) {
                        to += 360;
                    }

                    // limit the max speed to MAX_ROTATE_DEGREE
                    float distance = to - mDirection;
                    if (Math.abs(distance) > MAX_ROATE_DEGREE) {
                        distance = distance > 0 ? MAX_ROATE_DEGREE : (-1.0f * MAX_ROATE_DEGREE);
                    }

                    // need to slow down if the distance is short
                    mDirection = normalizeDegree(mDirection
                            + ((to - mDirection) * mInterpolator
                                    .getInterpolation(Math.abs(distance) > MAX_ROATE_DEGREE ? 0.4f : 0.3f)));
                    mCompassView.updateDirection(mDirection);
                }

                updateDirectionInfo();
                mHandler.postDelayed(mCompassViewUpdater, 20);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* SPRD: 512028 check permisson @{ */

        switch (CompassActivity.checkPermission(this)) {
            case CompassActivity.PERMISSION_ALL_ALLOWED:
                break;
            case CompassActivity.PERMISSION_ALL_DENYED:
                requestPermissions(new String[] { READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION },
                    STORAGE_PERMISSION_REQUEST_CODE);
                break;
            case CompassActivity.PERMISSION_READ_GPS_ONLY:
                requestPermissions(new String[] { READ_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_REQUEST_CODE);
            break;
            case CompassActivity.PERMISSION_READ_EXTERNAL_STORAGE_ONLY:
                requestPermissions(new String[] { ACCESS_FINE_LOCATION },
                    STORAGE_PERMISSION_REQUEST_CODE);
                break;
            default:
                break;
        }
        initResources();
        initServices();
        /* @} */

    }

    static int checkPermission(Context context) {
        boolean canReadGpsState =
                context.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean canReadExternalStorage =
                context.checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (canReadGpsState && canReadExternalStorage) {
            return PERMISSION_ALL_ALLOWED;
        } else {
            if (!canReadGpsState && !canReadExternalStorage) {
                return PERMISSION_ALL_DENYED;
            } else if (canReadGpsState && !canReadExternalStorage) {
                return PERMISSION_READ_GPS_ONLY;
            } else {
                return PERMISSION_READ_EXTERNAL_STORAGE_ONLY;
            }
        }
    }

    /* SPRD 512028 check permisson @{ */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean isPermitted = true;
        switch (requestCode) {
            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            isPermitted = false;
                            showConfirmDialog();
                            break;
                        }
                    }
                    if (isPermitted)
                        Log.d(TAG, "STORAGE_PERMISSION_REQUEST_CODE");
                } else {
                    showConfirmDialog();
                }
                break;
            default:
                break;
            }
    }

    public void showConfirmDialog() {
        Log.d(TAG, "showConfirmDialog");
        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.toast_compass_internal_error))
                .setMessage(getResources().getString(R.string.error_permissions))
                .setCancelable(false)
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            finish();
                        }
                        return true;
                    }
                })
                .setPositiveButton(getResources().getString(R.string.dialog_dismiss),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();
    }

    /* @} */

    @Override
    protected void onResume() {
        super.onResume();

        int style = getSharedPreferences(CompassSettings.CUSTOMIZE_CONFIG, Activity.MODE_PRIVATE).getInt(
                CompassSettings.KEY_POINTER_TYPE_KEY, CompassSettings.DEFAULT_VALUE);
        mCompassView.setCompassStype(style);

        style = getSharedPreferences(CompassSettings.CUSTOMIZE_CONFIG, Activity.MODE_PRIVATE).getInt(
                CompassSettings.KEY_BACKGROUND_KEY, CompassSettings.DEFAULT_VALUE);
        Log.d(TAG, "KEY_BACKGROUND_KEY, style: " + style);
        SharedPreferences stylesp = getSharedPreferences("test", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor01 = stylesp.edit();
        String sty = style + "";
        editor01.putString("sty", sty);
        editor01.commit();

        if (style > 0 && style < 4) {
            mContainer.setBackgroundResource(mBackgroundArray[style - 1]);
            String back = mBackgroundArray[style - 1] + "";

            SharedPreferences msp = getSharedPreferences("test", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = msp.edit();
            editor.putString("back", back);
            editor.commit();

        } else {
            SharedPreferences sp = getSharedPreferences("test", Activity.MODE_PRIVATE);
            String path = sp.getString("path", "");
            if (path != "") {
                try {
                    mContainer.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeFile(path)));
                } catch (OutOfMemoryError err) {
                    Log.e(TAG, "err + " + err);
                }

            } else {
                SharedPreferences msp = getSharedPreferences("test", Activity.MODE_PRIVATE);
                String back01 = sp.getString("back", "");
                mContainer.setBackgroundResource(Integer.parseInt(back01));
            }
        }

        Log.d(TAG, "mDirectionTV: " + mDirectionTV);
        if (mLocationProvider != null) {
            updateLocation(mLocationManager.getLastKnownLocation(mLocationProvider));
            mLocationManager.requestLocationUpdates(mLocationProvider, 2000, 10, mLocationListener);// 2s or 10m,upadate
        } else {
            mLatitudeTV.setText(getResources().getString(R.string.getting_location));
        }
        if (mOrientationSensor != null) {
            mSensorManager.registerListener(mOrientationSensorEventListener, mOrientationSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            mHandler.postDelayed(mCompassViewUpdater, 20);
        } else {
            mDirectionTV.setText(getResources().getString(R.string.message_no_orientation_sensor));
            mDirectionTV.setTextColor(Color.RED);
        }
        mStopDrawing = false;

    }

    @Override
    protected void onPause() {
        super.onPause();
        mStopDrawing = true;
        if (mOrientationSensor != null) {
            mSensorManager.unregisterListener(mOrientationSensorEventListener);
        }
        if (mLocationProvider != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    // initial view
    private void initResources() {
        setContentView(R.layout.compass);
        mContainer = (RelativeLayout) findViewById(R.id.container);
        mDirection = 0.0f;
        mTargetDirection = 0.0f;
        mInterpolator = new AccelerateInterpolator();
        mStopDrawing = true;
        // TODO: here maybe need be enhanced in future if support multi-language
        // mChinease = TextUtils.equals(Locale.getDefault().getLanguage(),
        // "zh");
        mDirectionTV = (TextView) findViewById(R.id.degree);
        mCompassView = (CompassView) findViewById(R.id.compass_pointer);
        mLongitudeTV = (TextView) findViewById(R.id.longitude);
        mLatitudeTV = (TextView) findViewById(R.id.latitude);
        mAltitudeTV = (TextView) findViewById(R.id.altitude);

        mSettings = (ImageButton) findViewById(R.id.settings);

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CompassActivity.this, CompassSettings.class);
                StringBuilder sb = new StringBuilder();

                sb.append(mDirectionTV.getText()).append("\n").append(mLatitudeTV.getText()).append(" ")
                        .append(mLongitudeTV.getText()).append(mAltitudeTV.getText());
                intent.putExtra("mylocation", sb.toString());
                startActivityForResult(intent, 0);
            }
        });

        mBackgroundArray[0] = R.drawable.bg01_compass;
        mBackgroundArray[1] = R.drawable.bg02_compass;
        mBackgroundArray[2] = R.drawable.bg03_compass;
    }

    private void initServices() {
        // sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        Log.d(TAG, "mOrientationSensor: " + mOrientationSensor);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // better accuracy
        criteria.setAltitudeRequired(true); // need altitude?
        // criteria.setBearingRequired(false);
        criteria.setCostAllowed(true); // cast?
        criteria.setPowerRequirement(Criteria.POWER_LOW); // low power
        mLocationProvider = mLocationManager.getBestProvider(criteria, true); // get best Provider
        Log.d(TAG, "mLocationProvider: " + mLocationProvider);

        // TODO: here maybe need be enhanced in future
        List<String> providers = mLocationManager.getAllProviders();
        Log.d(TAG, "All support providers: " + providers);
    }

    private void updateDirectionInfo() {
        String east = null;
        String west = null;
        String south = null;
        String north = null;

        StringBuilder sb = new StringBuilder();
        String degree = getResources().getString(R.string.compass_degree_symbol);

        float direction = normalizeDegree(mTargetDirection * -1.0f);

        if (direction > 22.5f && direction < 157.5f) {
            // east
            east = getResources().getString(R.string.direction_east);
        } else if (direction > 202.5f && direction < 337.5f) {
            // west
            west = getResources().getString(R.string.direction_west);
        }

        if (direction > 112.5f && direction < 247.5f) {
            // south
            south = getResources().getString(R.string.direction_south);
        } else if (direction < 67.5 || direction > 292.5f) {
            // north
            north = getResources().getString(R.string.direction_north);
        }

        // TODO now, we only support Chinese, this may be enhanced in future
        if (mChinease) {
            // east/west should be before north/south
            if (east != null) {
                sb.append(east);
            }
            if (west != null) {
                sb.append(west);
            }
            if (south != null) {
                sb.append(south);
            }
            if (north != null) {
                sb.append(north);
            }
        } else {
            // north/south should be before east/west
            if (south != null) {
                sb.append(south);
            }
            if (north != null) {
                sb.append(north);
            }
            if (east != null) {
                sb.append(east);
            }
            if (west != null) {
                sb.append(west);
            }
        }
        sb.append(" ");

        int direction2 = (int) direction;

        sb.append(direction2).append(degree);

        mDirectionTV.setText(sb.toString());
    }

    // update location information
    private void updateLocation(Location location) {
        Log.d(TAG, "location: " + location);
        StringBuilder latitudeSB = new StringBuilder();
        StringBuilder longitudeSB = new StringBuilder();
        StringBuilder altitudeSB = new StringBuilder(getResources().getString(R.string.altitude_text));
        String failed = getResources().getString(R.string.cannot_get_location);

        if (location == null) {
            latitudeSB.append(getResources().getString(R.string.message_location_fail));
            mLongitudeTV.setVisibility(View.GONE);
            mAltitudeTV.setVisibility(View.GONE);
        } else {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String latitudeStr;
            String longitudeStr;
            if (latitude >= 0.0f) {
                latitudeStr = getString(R.string.location_north, getLocationString(latitude));
            } else {
                latitudeStr = getString(R.string.location_south, getLocationString(-1.0 * latitude));
            }

            latitudeSB.append(latitudeStr);

            if (longitude >= 0.0f) {
                longitudeStr = getString(R.string.location_east, getLocationString(longitude));
            } else {
                longitudeStr = getString(R.string.location_west, getLocationString(-1.0 * longitude));
            }

            longitudeSB.append(longitudeStr);

            if (!location.hasAltitude()) {
                altitudeSB.append(" ").append(failed);
            } else {
                altitudeSB.append(" ").append(String.valueOf(location.getAltitude()))
                        .append(getResources().getString(R.string.altitude_unit));
            }
            mLongitudeTV.setVisibility(View.VISIBLE);
            mAltitudeTV.setVisibility(View.VISIBLE);
        }

        mLatitudeTV.setText(latitudeSB.toString());
        mLongitudeTV.setText(longitudeSB.toString());
        mAltitudeTV.setText(altitudeSB.toString());
    }

    private String getLocationString(double input) {
        int du = (int) input;
        int fen = (((int) ((input - du) * 3600))) / 60;
        int miao = (((int) ((input - du) * 3600))) % 60;
        return String.valueOf(du) + "°" + String.valueOf(fen) + "′" + String.valueOf(miao) + "″";
    }

    private SensorEventListener mOrientationSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float direction = event.values[mSensorManager.DATA_X] * -1.0f;
            mTargetDirection = normalizeDegree(direction);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    // adjust the value from orientation sensor
    private float normalizeDegree(float degree) {
        return (degree + 720) % 360;
    }

    LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged, provider: " + provider);
            if (status != LocationProvider.OUT_OF_SERVICE) {
                updateLocation(mLocationManager.getLastKnownLocation(mLocationProvider));
            } else {
                updateLocation(null);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged...location: " + location);
            updateLocation(location);
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        msp = getSharedPreferences("test", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = msp.edit();

        if (resultCode != RESULT_OK) {
            Log.e("TAG", "ActivityResult resultCode error");

            SharedPreferences sp = getSharedPreferences("test", Activity.MODE_PRIVATE);
            String sty = sp.getString("sty", "");
            if (Integer.parseInt(sty) > 0 && Integer.parseInt(sty) < 4) {
                String path = "";
                editor.putString("path", path);
                editor.commit();
            }
            return;
        }

        switch (requestCode) {
        case 0:
            if (data.getStringExtra("path") == null) {
                return;
            }
            String path = data.getStringExtra("path");
            if (path != null) {

                Bitmap mBigBitmap = null;
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;

                mBigBitmap = BitmapFactory.decodeFile(path, opts);
                opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128);
                opts.inJustDecodeBounds = false;

                try {
                    mContainer.setBackgroundDrawable(new BitmapDrawable(mBigBitmap));
                } catch (OutOfMemoryError err) {
                    Log.e(TAG, "err + " + err);
                }

                editor.putString("path", path);
                editor.commit();
                Toast.makeText(this, R.string.set_background_ok, Toast.LENGTH_LONG).show();
            }

        }

    }

    /*
     * Compute the sample size as a function of minSideLength and
     * maxNumOfPixels. minSideLength is used to specify that minimal width or
     * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
     * pixels that is tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints. Both size
     * and minSideLength can be passed in as IImage.UNCONSTRAINED, which
     * indicates no care of the corresponding constraint. The functions prefers
     * returning a sample size that generates a smaller bitmap, unless
     * minSideLength = IImage.UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way. For example,
     * BitmapFactory downsamples an image by 2 even though the request is 3. So
     * we round up the sample size to avoid OOM.
     */
    private static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
                Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

}
