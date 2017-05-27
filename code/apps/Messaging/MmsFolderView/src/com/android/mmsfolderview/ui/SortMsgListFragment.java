package com.android.mmsfolderview.ui;

import java.util.ArrayList;

import com.android.mmsfolderview.data.SortMsgDataCollector;
import com.android.mmsfolderview.data.SortMsgListData;
import com.android.mmsfolderview.data.SortMsgListData.SortMsgListDataListener;
import com.android.mmsfolderview.data.SortMsgListItemData;
import com.android.mmsfolderview.util.IntentUiUtils;
import com.android.mmsfolderview.util.OsUtil;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mmsfolderview.R;

public class SortMsgListFragment extends Fragment implements SortMsgListDataListener,
        SortMsgListItemView.HostInterface {

    private SortMsgListData mListData;
    public SortMsgListAdapter mAdapter;
    private TextView mPageTitle;
    private RecyclerView mRecyclerView;
    private ListEmptyView mEmptyListMessageView;
    private SortMsgListFragmentHost mHost;
    private Context mContext;

    // Saved Instance State Data - only for temporal data which is nice to
    // maintain but not
    // critical for correctness.
    private static final String SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY = "MessageListViewState";
    private Parcelable mListState;

    public interface SortMsgListFragmentHost {
        public void onMessageClicked(final SortMsgListData listData,
                final SortMsgListItemData conversationListItemData, final boolean isLongClick,
                final SortMsgListItemView conversationView);

        public void onCreateConversationClick();

        public boolean isMessageSelected(final int messageId);

        public boolean isInActionMode();

        public boolean isSwipeAnimatable();

        public boolean isSelectionMode();

        public boolean hasWindowFocus();

        void deleteMessages(ArrayList<Integer> messagesId, boolean isFromContextMenu);

        // add for bug 532539 begin
        public boolean isEnableVoLte();
        // add for bug 532539 end

        void copySimSmsToPhone(int subId, String bobyText,
                               long receivedTimestamp, int messageStatus,
                               boolean isRead, String address);

        void copySmsToSim(int messageId, int subId);

        void deleteSimSms(int indexOnIcc, int subId);
    }

    public SortMsgListFragment() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListData = new SortMsgListData(activity, this);
        mContext = activity;
    }

    /**
     * Call this immediately after attaching the fragment
     */
    public void setHost(final SortMsgListFragmentHost host) {
        mHost = host;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new SortMsgListAdapter(getActivity(), null, this);
    }

    public void initMessage(int loaderId) {
        mListData.initLoader(getLoaderManager(), loaderId);
    }

    public void reloadMessage(int loaderId) {
        mListData.restartLoader(getLoaderManager(), loaderId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.sort_msg_list_fragment,
                container, false);
        // mPageTitle = (TextView) rootView.findViewById(R.id.page_title);
        mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mEmptyListMessageView = (ListEmptyView) rootView.findViewById(R.id.no_msg_view);
        mEmptyListMessageView.setImageHint(R.drawable.ic_oobe_conv_list);

        final Activity activity = getActivity();
        final LinearLayoutManager manager = new LinearLayoutManager(activity) {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        };
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        // mRecyclerView.addOnItemTouchListener(new
        // SortMsgListSwipeHelper(mRecyclerView));
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY);
        }
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListState != null) {
            outState.putParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY, mListState);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mListState = mRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSortMsgListCursorUpdated(SortMsgListData data, Cursor cursor) {
        final Cursor oldCursor = mAdapter.swapCursor(cursor);
        updateEmptyListUi(cursor == null || cursor.getCount() == 0);
        if (mListState != null && cursor != null && oldCursor == null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
        }
    }

    private void updateEmptyListUi(final boolean isEmpty) {
        Log.d("tim_V6_ept", "isEmpty="+isEmpty);
        if (isEmpty) {
            int emptyListText;
            emptyListText = R.string.sort_msg_list_empty_text;

            mEmptyListMessageView.setTextHint(emptyListText);
            mEmptyListMessageView.setVisibility(View.VISIBLE);
            mEmptyListMessageView.setIsImageVisible(true);
            mEmptyListMessageView.setIsVerticallyCentered(true);
        } else {
            mEmptyListMessageView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isMessageSelected(int MessageId) {
        return mHost.isMessageSelected(MessageId);
    }

    @Override
    public void onMessageClicked(SortMsgListItemData sortMsgListItemData, boolean isLongClick,
            SortMsgListItemView sortMsgListItemView) {
        mHost.onMessageClicked(mListData, sortMsgListItemData, isLongClick, sortMsgListItemView);
    }

    @Override
    public void deleteMessages(ArrayList<Integer> messagesId, boolean isFromContextMenu) {
        // TODO Auto-generated method stub
        mHost.deleteMessages (messagesId, isFromContextMenu);
    }
    // add for bug 532539 begin
    @Override
    public boolean isEnableVoLte() {
        return mHost.isEnableVoLte();
    }
    // add for bug 532539 end

    @Override
    public void copySimSmsToPhone(int subId, String bobyText,
                                  long receivedTimestamp, int messageStatus,
                                  boolean isRead, String address) {
        mHost.copySimSmsToPhone(subId, bobyText,
                                receivedTimestamp, messageStatus,
                                isRead, address);
    }

    @Override
    public void copySmsToSim(int messageId, int subId) {
        mHost.copySmsToSim(messageId, subId);
    }

    @Override
    public void deleteSimSms(int indexOnIcc, int subId) {
        mHost.deleteSimSms(indexOnIcc, subId);
    }

    @Override
    public boolean isInActionMode() {
        return mHost.isInActionMode();
    }

    public void updateUi() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean isSwipeAnimatable() {
        // TODO Auto-generated method stub
        return false;
    }

    /*Modify by SPRD for Bug:534532  2016.03.09 Start */
    @Override
    public void startFullScreenPhotoViewer(Uri photosUri,String photoType) {
        IntentUiUtils.launchFullScreenPhotoViewer(mContext, photosUri,photoType);
    }
    /*Modify by SPRD for Bug:534532  2016.03.09 End */

    @Override
    public void startFullScreenVideoViewer(Uri videoUri) {
        IntentUiUtils.launchFullScreenVideoViewer(mContext, videoUri);
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public boolean isSelectionMode() {
        return false;
    }

}
