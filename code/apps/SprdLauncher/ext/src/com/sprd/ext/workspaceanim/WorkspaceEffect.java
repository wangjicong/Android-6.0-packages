package com.sprd.ext.workspaceanim;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import com.android.sprdlauncher3.Launcher;
import com.android.sprdlauncher3.LauncherAnimUtils;
import com.android.sprdlauncher3.R;
import com.sprd.ext.workspaceanim.effect.EffectFactory;
import com.sprd.ext.workspaceanim.effect.EffectInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by SPREADTRUM on 10/24/16.
 */

public final class WorkspaceEffect {
    private static String TAG = "WorkspaceEffect";

    private static WorkspaceEffect instance = null;
    private static Launcher mLauncher = null;
    protected static float mLayoutScale = 1.0f;
    private static int mMinimumWidth;
    private ViewGroup mEffectsPanel;
    private RecyclerView mRecyclerView;
    private static boolean mEffectPreviewMode;


    public static final int EFFECT_PREVIEW_DURATION = 500;
    public static final int PANLE_IN_DURATION = 200;
    public static final int PANLE_OUT_DURATION = 100;
    public static final int PANLE_DELAY_DURATION = 80;
    public static float CAMERA_DISTANCE = 5000;
    public static final int MIN_PAGE_NUN = 2;


    public static final int ANIMATION_DEFAULT = 0;
    // for SharedPreference XML NAME
    public static final String WORKSPACE_STYLE = "workspace_style";
    // the workspace_setting.xml key and SharePreference key are same
    public static final String KEY_ANIMATION_STYLE = "workspace_pref_key_animation";

    public WorkspaceEffect(Launcher launcher) {
        mLauncher = launcher;
        initEffectsButton();
    }

    public static int getScaledMeasuredWidth(View child) {
        // This functions are called enough times that it actually makes a difference in the
        // profiler -- so just inline the max() here
        if (child != null) {
            final int measuredWidth = child.getMeasuredWidth();
            final int minWidth = mMinimumWidth;
            final int maxWidth = (minWidth > measuredWidth) ? minWidth
                    : measuredWidth;
            return (int) (maxWidth * mLayoutScale + 0.5f);
        } else {
            return (int) (mMinimumWidth * mLayoutScale + 0.5f);
        }
    }

    private void initEffectsButton(){
        View effectsButton = mLauncher.findViewById(R.id.effects_button);
        effectsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!mLauncher.getWorkspace().isSwitchingState()) {
                    onClickEffectsButton(arg0);
                }
            }
        });
        effectsButton.setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());
        effectsButton.setVisibility(View.VISIBLE);

        mEffectsPanel = (ViewGroup) mLauncher.findViewById(R.id.effects_panel);

        SharedPreferences sharePref = getmAnimSharePref(mLauncher);
        int effectIndex = sharePref.getInt(WorkspaceEffect.KEY_ANIMATION_STYLE, WorkspaceEffect.ANIMATION_DEFAULT);

        mRecyclerView = (RecyclerView)mLauncher.findViewById(R.id.effects_recyclerview);
        LinearLayoutManager lManager = new LinearLayoutManager(mLauncher);
        lManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(lManager);

        EffectRecyclerAdapter adapter = new EffectRecyclerAdapter(mLauncher);
        mRecyclerView.setAdapter(adapter);
    }

    /**
     * Event handler for the Effects button that appears after a long press
     * on the home screen.
     */
    protected void onClickEffectsButton(View v) {
        setEffectsPreviewMode(true, true, false);
    }


    public void setEffectsPreviewMode(boolean previewMode, boolean animated, boolean hideAll){
        if(mEffectPreviewMode == previewMode) {
            return;
        }
        mEffectPreviewMode = previewMode;
        View fromView = previewMode ? mLauncher.getOverviewPanel() : mEffectsPanel;
        View toView = previewMode ? mEffectsPanel : mLauncher.getOverviewPanel();
        panelTransitionAnimation(fromView, toView, animated, hideAll);
        mLauncher.getWorkspace().updateFreeScrollStatus();
        mLauncher.getWorkspace().resetAllWorksapceChild();
    }

    public void panelTransitionAnimation(final View fromView, final View toView, boolean animated, boolean hideAll){
        if(fromView == null || toView == null ) return;

        if(!animated){
            fromView.setTranslationY(0f);
            fromView.setAlpha(0f);
            fromView.setVisibility(View.GONE);
            toView.setTranslationY(0f);
            toView.setAlpha(hideAll ? 0f : 1f);
            toView.setVisibility(hideAll ? View.GONE : View.VISIBLE);
        }else {
            final AnimatorSet animator = LauncherAnimUtils.createAnimatorSet();
            final Collection<Animator> bounceAnims = new ArrayList<Animator>();
            float height = fromView.getMeasuredHeight();

            fromView.setTranslationX(0f);
            toView.setTranslationY(height);

            Animator outTraY = LauncherAnimUtils.ofFloat(fromView, "translationY",  0, height);
            outTraY.setDuration(WorkspaceEffect.PANLE_OUT_DURATION);
            outTraY.setInterpolator(new AccelerateInterpolator(1f));
            bounceAnims.add(outTraY);

            Animator outAlpha = LauncherAnimUtils.ofFloat(fromView, "alpha", 1f, 0f);
            outTraY.setDuration(WorkspaceEffect.PANLE_OUT_DURATION);
            outTraY.setInterpolator(new AccelerateInterpolator(1f));
            bounceAnims.add(outAlpha);

            Animator inTraY = LauncherAnimUtils.ofFloat(toView, "translationY", height, 0);
            inTraY.setDuration(WorkspaceEffect.PANLE_IN_DURATION);
            inTraY.setStartDelay(WorkspaceEffect.PANLE_DELAY_DURATION);
            inTraY.setInterpolator(new AccelerateInterpolator(1f));
            bounceAnims.add(inTraY);

            Animator inAlpha = LauncherAnimUtils.ofFloat(toView, "alpha", 0f, 1f);
            inAlpha.setDuration(WorkspaceEffect.PANLE_IN_DURATION);
            inAlpha.setStartDelay(WorkspaceEffect.PANLE_DELAY_DURATION);
            inAlpha.setInterpolator(new AccelerateInterpolator(1f));
            bounceAnims.add(inAlpha);

            animator.playTogether(bounceAnims);

            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    toView.setVisibility(View.VISIBLE);

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    fromView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    fromView.setTranslationX(0f);
                    toView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
    }

    public static SharedPreferences getmAnimSharePref(Context context) {
        if(context == null) return null;
        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences(WORKSPACE_STYLE, Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);
        return sharedPref;
    }


    public static EffectInfo getCurentAnimInfo(Context context){
        SharedPreferences sharedPref = getmAnimSharePref(context);
        if(sharedPref == null) return null;
        int type = sharedPref.getInt(KEY_ANIMATION_STYLE, WorkspaceEffect.ANIMATION_DEFAULT);
        return EffectFactory.getEffect(type);
    }

    public ViewGroup getEffectsPanel(){
        return mEffectsPanel;
    }

    public boolean isEffectsPreviewMode() {
        if(mLauncher == null){
            return false;
        }
        return mEffectPreviewMode;
    }
}
