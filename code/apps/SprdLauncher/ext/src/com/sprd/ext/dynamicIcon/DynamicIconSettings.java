package com.sprd.ext.dynamicIcon;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by SPRD on 12/2/16.
 */
public class DynamicIconSettings extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new DynamicIconSettingsFragment())
                .commit();
    }
}
