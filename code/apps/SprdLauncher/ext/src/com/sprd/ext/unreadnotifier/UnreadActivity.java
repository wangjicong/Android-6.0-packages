package com.sprd.ext.unreadnotifier;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;


public class UnreadActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UnreadSettingsFragment())
                .commit();
    }

    public boolean onCreateOptionsMenu(Menu paramMenu)
    {
        return true;
    }

    protected void onDestroy()
    {
        super.onDestroy();
    }
}
