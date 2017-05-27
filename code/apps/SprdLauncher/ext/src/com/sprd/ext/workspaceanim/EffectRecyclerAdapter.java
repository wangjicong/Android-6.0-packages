package com.sprd.ext.workspaceanim;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.sprdlauncher3.Launcher;
import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.RecyclerViewAdapter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by SPREADTRUM on 17-2-23.
 */

public class EffectRecyclerAdapter extends RecyclerViewAdapter<EffectRecyclerAdapter.MyViewHolder> {
    private final static String TAG =  "EffectRecyclerAdapter";

    private Launcher mLauncher;
    private static final String XML_ITEM_TAG = "Effect";
    private HashMap<Integer, Pair<Integer, Integer>> mEffectsInfo;

    public EffectRecyclerAdapter(Launcher launcher,OnItemClickListener listener) {
        mLauncher = launcher;
        mEffectsInfo = loadEffectsInfo(launcher);
        if(listener == null){
            listener = loadDefaultListener();
        }
        setOnItemClickListener(listener);
    }

    public EffectRecyclerAdapter(Launcher launcher){
        this(launcher, null);
    }

    private OnItemClickListener loadDefaultListener(){
        OnItemClickListener defaultListener = new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(position != getSelectedPos()) {
                    try {
                        SharedPreferences.Editor mSharedEditor = WorkspaceEffect.getmAnimSharePref(mLauncher).edit();
                        mSharedEditor.putInt(WorkspaceEffect.KEY_ANIMATION_STYLE, position).commit();
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "could not persist animation setting", e);
                    }
                }
                mLauncher.getWorkspace().previewSwitchingEffect();
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        };
        return defaultListener;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView effectTextView;

        public MyViewHolder(View itemView) {
            super(itemView);
            effectTextView = (TextView) itemView.findViewById(R.id.effect_item);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.effect_recyclerview_item, viewGroup, false);
        viewGroup.setTag(view);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof MyViewHolder){
            MyViewHolder viewHolder = (MyViewHolder)holder;
            Pair<Integer,Integer> pair = mEffectsInfo.get(position);
            viewHolder.effectTextView.setText(pair.first);
            viewHolder.effectTextView.setCompoundDrawablesWithIntrinsicBounds(0, mEffectsInfo.get(position).second, 0, 0);
            viewHolder.effectTextView.setCompoundDrawableTintList((getSelectedPos() == position) ? ColorStateList.valueOf(Color.CYAN) : null);
            viewHolder.effectTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnItemClickListener listener = getOnItemClickListener();
                    if(v != null && !mLauncher.getWorkspace().isSwitchingState() && listener != null) {
                        listener.onItemClick(v,position);
                        onSelectedChanged(position);
                    }
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        if (null == mEffectsInfo) {
            return 0;
        }
        return mEffectsInfo.size();
    }

    private HashMap<Integer, Pair<Integer, Integer>> loadEffectsInfo(Context context) {
        HashMap<Integer, Pair<Integer, Integer>> effectsInfo = new HashMap<>();
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.workspace_switch_effects);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (XML_ITEM_TAG.equals(tagName)) {
                        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EffectInfo);
                        Integer effectName = a.getResourceId(R.styleable.EffectInfo_effectName,0);
                        Integer effectIcon = a.getResourceId(R.styleable.EffectInfo_effectIcon,0);
                        int index = a.getInteger(R.styleable.EffectInfo_effectIndex, 0);
                        effectsInfo.put(index, new Pair<>( effectName, effectIcon ));
                        a.recycle();
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException | RuntimeException e) {
            LogUtils.w(TAG, "parse xml failed", e);
        }
        return effectsInfo;
    }
}
