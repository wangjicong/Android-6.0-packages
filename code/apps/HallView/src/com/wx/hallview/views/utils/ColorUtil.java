package com.wx.hallview.views.utils;

/**
 * Created by Administrator on 16-1-22.
 */
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;

import android.view.View;

import com.wx.hallview.R;

import java.io.PrintStream;

public class ColorUtil
{
    private static TypedArray sPrimaryColors;
    private static TypedArray sSecondaryColors;

    public static int[] colorFromBitmap(Bitmap paramBitmap)
    {
        int[] arrayOfInt = new int[2];

        if (paramBitmap == null)
            return null;
        Palette palette = Palette.generate(paramBitmap, 24);

        if(palette == null){
        	return null;
        }

        if (palette.getMutedSwatch() != null){
            arrayOfInt[0] = palette.getMutedSwatch().getRgb();
        }else{
            arrayOfInt[0] = 0;
        }

        if (palette.getDarkVibrantSwatch() != null){

            arrayOfInt[1] = palette.getDarkVibrantSwatch().getRgb();
        }else{
            arrayOfInt[1] = 0;
        }
        return arrayOfInt;
    }

    public static void init(Resources paramResources)
    {
        sPrimaryColors = paramResources.obtainTypedArray(R.array.letter_tile_colors);
        sSecondaryColors = paramResources.obtainTypedArray(R.array.letter_tile_colors_dark);
    }

    public static void setBackgroundColor(View paramView, int paramInt)
    {
    	paramView.getBackground().setColorFilter(new PorterDuffColorFilter(paramInt, PorterDuff.Mode.SRC_ATOP));
	}
}