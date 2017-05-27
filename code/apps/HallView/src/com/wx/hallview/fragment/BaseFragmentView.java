package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-23.
 */
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseFragmentView
{
    private boolean mAttached = false;
    protected Context mContext;
    private View mRootView;

    public BaseFragmentView(Context paramContext)
    {
        this.mContext = paramContext;
    }

    public boolean attached()
    {
        return this.mAttached;
    }

    protected Context getContext()
    {
        return this.mContext;
    }

    public View getRootView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup)
    {
        if (this.mRootView == null) {
            this.mRootView = onCreateView(paramLayoutInflater, paramViewGroup);
        }
        return this.mRootView;
    }

    public boolean handleBackPress()
    {
        return false;
    }

    public boolean needShowBackButton()
    {
        return true;
    }

    protected void onAttach() {}

    protected abstract View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup);

    protected void onDetach() {}

    public void onHide() {}

    public void onScreenOff() {}

    public void onScreenOn() {}

    public void onShow() {}

    public void performAttach()
    {
        if (!this.mAttached) {
            onAttach();
        }
        Log.d("BaseFragmentView", "perform attach with attached");
        this.mAttached = true;
        return;


    }

    public void performDetach()
    {
        Log.d("BaseFragmentView", "performDetach");
        if (this.mAttached) {
            onDetach();
        }
        Log.d("BaseFragmentView", "perform detach with no attached");
        this.mAttached = false;
        return;


    }
}

