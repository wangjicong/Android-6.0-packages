/* Created by Spreadst */

package com.android.launcher3;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Display list of known storage backend roots.
 */
public class LauncherSortFragment extends Fragment {

    private ListView mList;
    private RootsAdapter mAdapter;
    private String[] mSortListType;

    private static Launcher mLauncher;

    public static void show(Launcher context,FragmentManager fm) {
        mLauncher = context;
        final LauncherSortFragment fragment = new LauncherSortFragment();

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_roots, fragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sort_item_list, container, false);
        mList = (ListView) view.findViewById(android.R.id.list);
        mList.setOnItemClickListener(mItemListener);
        mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        return view;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSortListType = getResources().getStringArray(R.array.edit_sort_choices);
        mAdapter = new RootsAdapter();
        mList.setAdapter(mAdapter);
    }

    private OnItemClickListener mItemListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mLauncher.setRootsDrawerOpen(false);
            LauncherModel model = mLauncher.getModel();
            model.updateSortedApps(position);
        }
    };

    private class RootsAdapter extends BaseAdapter {
        public RootsAdapter() {
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            final View view = inflater.inflate(R.layout.sort_item, parent, false);

            TextView sortItemView = (TextView) view.findViewById(R.id.sort_items);
            sortItemView.setText(mSortListType[position]);
            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != 1;
        }

        @Override
        public int getCount() {
            return mSortListType.length;
        }

        @Override
        public Object getItem(int position) {
            return mSortListType[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }
}
