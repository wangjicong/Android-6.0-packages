package com.sprd.ext;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by SPREADTRUM on 17-2-23.
 */

public abstract class RecyclerViewAdapter<M extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener{
        public abstract void onItemClick(View view, int position);
        public abstract void onItemLongClick(View view, int position);
    }

    private OnItemClickListener mItemClickListener;
    private int mSelectedPos = 0;

    public void setSelectedPos(int position){
        mSelectedPos = position;
    };

    public int getSelectedPos(){
        return mSelectedPos;
    };

    protected void onSelectedChanged(int position){
        if(position != mSelectedPos) {
            notifyItemChanged(mSelectedPos);
            mSelectedPos = position;
            notifyItemChanged(mSelectedPos);
        }
    }

    public void setOnItemClickListener(OnItemClickListener l){
        mItemClickListener = l;
    }

    public OnItemClickListener getOnItemClickListener(){
        return mItemClickListener;
    }

}
