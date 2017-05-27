package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-22.
 */
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import com.wx.hallview.R;

public class AnswerRejectConainer extends RelativeLayout
{
    private float X;
    private float Y;
    private View mAnswerView;
    int mAnwserViewLeft = 0;
    private View mCenterView;
    int mCenterViewLeft = 0;
    int mCenterViewRight = 0;
    private View mCurrentMoveView = null;
    boolean mFirstInit = true;
    private AnswerRejctListener mListener;
    private View mRejectView;
    int mRejectViewLeft = 0;

    public AnswerRejectConainer(Context paramContext)
    {
        this(paramContext, null);
    }

    public AnswerRejectConainer(Context paramContext, AttributeSet paramAttributeSet)
    {
        this(paramContext, paramAttributeSet, 0);
    }

    public AnswerRejectConainer(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
    }

    private View getTouchDownView(MotionEvent paramMotionEvent)
    {
        float f1 = paramMotionEvent.getX();
        float f2 = paramMotionEvent.getY();
        int j = getChildCount();
        int i = 0;
        View view = null;
        while (i < j)
        {
            View localView = getChildAt(i);
            int k = localView.getLeft();
            int m = localView.getRight();
            int n = localView.getTop();
            int i1 = localView.getBottom();
            if ((k < f1) && (f1 < m) && (f2 > n) && (f2 < i1))
            {
                view = localView;
                if (localView.getId() == this.mCenterView.getId())
                    view = null;
                return view;
            }
            i += 1;
        }
        return null;
    }

    private void handleActionUp(View paramView, MotionEvent paramMotionEvent)
    {
        int j;
        int i;
        ObjectAnimator localObjectAnimator;
        ObjectAnimator objectAnimator;


        if (paramView.equals(this.mAnswerView)){
            j = this.mAnwserViewLeft;
            if (paramMotionEvent.getX() > this.mCenterViewLeft){
                i = 1;
            }else{
                i = 0;
            }
        }else if(paramView.equals(this.mRejectView)){
            j = this.mRejectViewLeft;
            if (paramMotionEvent.getX() < this.mCenterViewRight){
                i = 1;
            }else{
                i = 0;
            }
        }else {
             return;
        }

        if (i != 0){
            objectAnimator = ObjectAnimator.ofInt(paramView, "Left", new int[] { paramView.getLeft(), this.mCenterViewLeft });
            localObjectAnimator = ObjectAnimator.ofInt(paramView, "Right", new int[] { paramView.getRight(), this.mCenterViewRight });
        }else{
            objectAnimator = ObjectAnimator.ofInt(paramView, "Left", new int[] { paramView.getLeft(), j });
            localObjectAnimator = ObjectAnimator.ofInt(paramView, "Right", new int[] { paramView.getRight(), paramView.getMeasuredWidth() + j });
        }
        AnimatorSet localAnimatorSet = new AnimatorSet();
        localAnimatorSet.playTogether(new Animator[] { objectAnimator, localObjectAnimator });
        localAnimatorSet.start();

        if ((i != 0) && (this.mListener != null)){
            if(paramView.equals(this.mAnswerView)){
                this.mListener.doAction(AnswerRejectConainer.AnswerRejctListener.Action.Answer);
            }else if(paramView.equals(this.mRejectView)){
                this.mListener.doAction(AnswerRejectConainer.AnswerRejctListener.Action.Rejcet);
            }
        }
    }









    private void handleAnswerButtonMove(MotionEvent paramMotionEvent)
    {
        float f = paramMotionEvent.getX();
        if ((f > this.mCenterViewRight - this.mAnswerView.getMeasuredWidth() / 2) || (f < this.mAnwserViewLeft + this.mAnswerView.getMeasuredWidth() / 2)){
            return;
        }
        int i = (int)(this.X - this.mCurrentMoveView.getMeasuredWidth() * 0.5F);
        this.mAnswerView.layout(i, 0, this.mAnswerView.getMeasuredWidth() + i, getMeasuredHeight());
    }

    private void handleRejectButtonMove(MotionEvent paramMotionEvent)
    {
        float f = paramMotionEvent.getX();
        if ((f < this.mCenterViewLeft + this.mAnswerView.getMeasuredWidth() / 2) || (f > this.mRejectViewLeft + this.mRejectView.getMeasuredWidth() / 2)){
            return;
        }
        int i = (int)(this.X - this.mCurrentMoveView.getMeasuredWidth() * 0.5F);
        this.mRejectView.layout(i, 0, this.mRejectView.getMeasuredWidth() + i, getMeasuredHeight());
    }

    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        this.mAnswerView = findViewById(R.id.left_awnser_button);
        this.mRejectView = findViewById(R.id.right_reject_button);
        this.mCenterView = findViewById(R.id.center_view);
    }

    protected void onLayout(boolean paramBoolean, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
    {
        super.onLayout(paramBoolean, paramInt1, paramInt2, paramInt3, paramInt4);
        if (this.mFirstInit)
        {
            this.mAnwserViewLeft = this.mAnswerView.getLeft();
            this.mRejectViewLeft = this.mRejectView.getLeft();
            this.mCenterViewLeft = this.mCenterView.getLeft();
            this.mCenterViewRight = this.mCenterView.getRight();
            this.mFirstInit = false;
        }
    }

    public boolean onTouchEvent(MotionEvent paramMotionEvent)
    {
        int j = 0;
        int i = paramMotionEvent.getAction();
        this.X = paramMotionEvent.getX();
        this.Y = paramMotionEvent.getY();
        switch (i)
        {

            case 0:
                Log.d("AnswerRejectConainer", "onTouchEvent ACTION_DOWN");
                this.mCurrentMoveView = getTouchDownView(paramMotionEvent);
                if (this.mCurrentMoveView != null){
                    this.mCenterView.setVisibility(View.VISIBLE);
                    this.mCenterView.setBackground(this.mCurrentMoveView.getBackground());
                }
                break;
            case 2:

                Log.d("AnswerRejectConainer", "onTouchEvent ACTION_MOVE x:" + this.X + " Y:" + this.Y);
                if(this.mCurrentMoveView != null){
                    if (this.mCurrentMoveView.equals(this.mAnswerView))
                    {
                        handleAnswerButtonMove(paramMotionEvent);

                    }else if(this.mCurrentMoveView.equals(this.mRejectView)){
                        handleRejectButtonMove(paramMotionEvent);
                    }
                }
                break;
            case 1:
                Log.d("AnswerRejectConainer", "onTouchEvent ACTION_UP");
                if (this.mCurrentMoveView != null){
                    handleActionUp(this.mCurrentMoveView, paramMotionEvent);
                    this.mCenterView.setVisibility(View.INVISIBLE);
                    this.mCurrentMoveView = null;
                }
                break;
            default:
                break;

        }
        if(this.mCurrentMoveView != null){
            return true;
        }else{
            return false;
        }
    }

    public void setAnswerRejctListener(AnswerRejctListener paramAnswerRejctListener)
    {
        this.mListener = paramAnswerRejctListener;
    }

    public static abstract interface AnswerRejctListener
    {
        public abstract void doAction(Action paramAction);

        public static enum Action
        {
             Answer, Rejcet
        }
    }
}