package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-20.
 */
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.wx.hallview.ViewContorller;

public class RootView extends RelativeLayout
{
    private ViewGroup mContainer;
    private Context mContext;

    public RootView(Context paramContext)
    {
        this(paramContext, null);
    }

    public RootView(Context paramContext, AttributeSet paramAttributeSet)
    {
        this(paramContext, paramAttributeSet, -1);
    }

    public RootView(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
        this.mContext = paramContext;
    }

    private AnimatorSet createAnimator(View paramView1, View paramView2, boolean paramBoolean)
    {
        ObjectAnimator localObjectAnimator1 = ObjectAnimator.ofFloat(paramView2, "Alpha", new float[] { 0.0F, 0.01F, 1.0F });
        ObjectAnimator localObjectAnimator2 = ObjectAnimator.ofFloat(paramView1, "Alpha", new float[] { 1.0F, 0.01F, 0.0F });
        AnimatorSet localAnimatorSet = new AnimatorSet();
        localAnimatorSet.setDuration(500L);
        ObjectAnimator localObjectAnimator3;
        ObjectAnimator localObjectAnimator4;
        if (paramBoolean) {
            localObjectAnimator3 = ObjectAnimator.ofFloat(paramView1, "rotationY", new float[]{0.0F, -180.0F});
            localObjectAnimator4 = ObjectAnimator.ofFloat(paramView2, "rotationY", new float[]{180.0F, 0.0F});
        }else{
            localObjectAnimator3 = ObjectAnimator.ofFloat(paramView1, "rotationY", new float[] { 0.0F, 180.0F });
            localObjectAnimator4 = ObjectAnimator.ofFloat(paramView2, "rotationY", new float[] { -180.0F, 0.0F });
        }
        localAnimatorSet.playTogether(new Animator[] { localObjectAnimator2, localObjectAnimator1, localObjectAnimator3, localObjectAnimator4 });
        return localAnimatorSet;
    }

    public ViewGroup getContainer()
    {
        return (ViewGroup)getChildAt(0);
    }

    public void moveToFragmentView(View paramView, boolean paramBoolean, AnimationEnd paramAnimationEnd)
    {
        Log.d("RootView", "add view " + paramView.getId());
        AnimatorSet animatorSet;
        if (paramView.getParent() == null)
            this.mContainer.addView(paramView);
        if (this.mContainer.getChildCount() >= 2)
        {
            paramView = this.mContainer.getChildAt(0);
            View localView = this.mContainer.getChildAt(this.mContainer.getChildCount() - 1);
            localView.setAlpha(0.0F);
            animatorSet = createAnimator(paramView, localView, paramBoolean);
            final AnimationEnd animationEnd = paramAnimationEnd;
            animatorSet.addListener(new Animator.AnimatorListener()
            {
                public void onAnimationCancel(Animator paramAnimator)
                {
                }

                public void onAnimationEnd(Animator paramAnimator)
                {
                    int i = 0;
                    while (i < RootView.this.mContainer.getChildCount() - 1)
                    {
                        View view = RootView.this.mContainer.getChildAt(i);
                        RootView.this.mContainer.removeView(view);
                        Log.d("RootView", "remove old view" + view.getId());
                        i += 1;
                    }
                    animationEnd.showOrGoneButton();
                }

                public void onAnimationRepeat(Animator paramAnimator)
                {
                }

                public void onAnimationStart(Animator paramAnimator)
                {
                }
            });
            animatorSet.start();
        }
    }

    protected void onFinishInflate()
    {
        super.onFinishInflate();
        this.mContainer = ((ViewGroup)getChildAt(0));
    }

    public boolean onInterceptTouchEvent(MotionEvent paramMotionEvent)
    {
        ViewContorller.getInstance(this.mContext).handleTouchEvent(paramMotionEvent);
        return super.onInterceptTouchEvent(paramMotionEvent);
    }

    public boolean onTouchEvent(MotionEvent paramMotionEvent)
    {
        ViewContorller.getInstance(this.mContext).handleTouchEvent(paramMotionEvent);
        return false;
    }

    public static abstract interface AnimationEnd
    {
        public abstract void showOrGoneButton();
    }
}
