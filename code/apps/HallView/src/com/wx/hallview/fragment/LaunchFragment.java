package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-23.
 */
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.wx.hallview.ViewContorller;
import com.wx.hallview.ViewContorller.FragmentItem;
import com.wx.hallview.views.CircleLayout;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.wx.hallview.R;

public class LaunchFragment
        extends BaseFragmentView
        implements View.OnClickListener
{
    private OnItemClick mItemClickListener;

    public LaunchFragment(Context paramContext, OnItemClick paramOnItemClick)
    {
        super(paramContext);
        this.mItemClickListener = paramOnItemClick;
    }

    private void addChildViews(CircleLayout paramCircleLayout)
    {
        Iterator localIterator = ViewContorller.ADDED_FRAGMENT.keySet().iterator();

        while (localIterator.hasNext()){
            String str = (String)localIterator.next();
            if (((ViewContorller.FragmentItem)ViewContorller.ADDED_FRAGMENT.get(str)).iconRes != -1)//:cond_0
            {
                View localView = new View(getContext());
                int i = getContext().getResources().getDimensionPixelOffset(R.dimen.launch_icon_width);
                ViewGroup.LayoutParams localLayoutParams = new ViewGroup.LayoutParams(i, i);
                localView.setTag(str);
                localView.setClickable(true);
                localView.setBackgroundResource(((ViewContorller.FragmentItem)ViewContorller.ADDED_FRAGMENT.get(str)).iconRes);
                localView.setOnClickListener(this);
                paramCircleLayout.addView(localView, localLayoutParams);
            }
        }
    }

    public boolean handleBackPress()
    {
        ViewContorller.getInstance(this.mContext).moveToFirstPage(false);
        return true;
    }

    public boolean needShowBackButton()
    {
        return true;
    }

    public void onClick(View paramView)
    {
        if (this.mItemClickListener != null)
        {
            String s = (String)paramView.getTag();
            this.mItemClickListener.onItemClick(s);
        }
    }

    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup)
    {
        CircleLayout cl = (CircleLayout)paramLayoutInflater.inflate(R.layout.launcher_view, paramViewGroup, false);
        addChildViews(cl);
        return cl;
    }

    public static abstract interface OnItemClick
    {
        public abstract void onItemClick(String paramString);
    }
}

