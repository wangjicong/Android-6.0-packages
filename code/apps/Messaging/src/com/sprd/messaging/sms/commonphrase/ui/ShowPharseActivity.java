
package com.sprd.messaging.sms.commonphrase.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.messaging.R;
import com.sprd.messaging.sms.commonphrase.model.PharserManager;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ShowPharseActivity extends ListActivity {

    private ListView mPhraseList;
    private static String TAG = "ShowPharseActivity";

    /* Modify by SPRD for Bug:527166  2015.01.22 Start */
//    private View mEmptyView;
    public static final String KEY_NO_PHRASE = "k-n-p";
    /* Modify by SPRD for Bug:527166  2015.01.22 End */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phrase_list_show_ex);
        mPhraseList = (ListView) findViewById(android.R.id.list);

        /* Delete by SPRD for Bug:527166  2015.01.22 Start */
//        mEmptyView = (View) findViewById(R.id.empty);
        /* Delete by SPRD for Bug:527166  2015.01.22 End */

        final SimpleAdapter pAdapter = new SimpleAdapter(this, loadPhraseEntry(this),
                R.layout.phrase_item_show_ex, new String[] {
                    "text"
                }, new int[] {
                    R.id.common_phrase
                });
        mPhraseList.setAdapter(pAdapter);
        if (pAdapter == null || pAdapter.getCount() == 0) {
            mPhraseList.setVisibility(View.GONE);

            /* Modify by SPRD for Bug:527166  2015.01.22 Start */
//            mEmptyView.setVisibility(View.VISIBLE);
            Intent intent = getIntent();
            intent.putExtra(KEY_NO_PHRASE, true);
            setResult(RESULT_OK, intent);
            finish();
            /* Modify by SPRD for Bug:527166  2015.01.22 End */

        }

        mPhraseList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (pAdapter != null) {
                    HashMap<String, Object> item = (HashMap<String, Object>) pAdapter
                            .getItem(position);

                    Log.d(TAG,
                            "==onItemClick======pAdapter.getItem(position)==="
                                    + pAdapter.getItem(position));

                    String clickedItem = (String) (item.get("text"));
                    Log.d(TAG, "===============>position=" + position);

                    Intent intent = ShowPharseActivity.this.getIntent();
                    if (clickedItem != null) {

                        intent.putExtra("clickPhrase", clickedItem);

                    }
                    ShowPharseActivity.this.setResult(RESULT_OK, intent);
                    ShowPharseActivity.this.finish();
                }
            }
        });

        /* Delete by SPRD for Bug:527166  2015.01.22 Start */
//        mEmptyView = findViewById(R.id.empty);
//        this.getListView().setEmptyView(mEmptyView);
        /* Delete by SPRD for Bug:527166  2015.01.22 Start */

    }

    private List<Map<String, ?>> loadPhraseEntry(Context context) {
        List<String> phraseList = PharserActivity.intentCommonPhrase(PharserManager.MMS, context);
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        if(phraseList == null)
            return entries;
        for (String phraseItem : phraseList) {
            HashMap<String, Object> entry = new HashMap<String, Object>();
            entry.put("text", phraseItem);
            entries.add(entry);
        }
        return entries;
    }

}
