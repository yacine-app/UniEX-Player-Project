package com.yacineApp.uniEXMusic.Components.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Surface;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.yacineApp.uniEXMusic.UniEXActivity;

public class DisplayDimensions {

    private int neededPaddingLeft = 0, neededPaddingTop = 0, neededPaddingRight = 0, neededPaddingBottom = 0;

    private int navigationBarHeight, navigationBarWidth, statusBarHeight, rotation = Surface.ROTATION_0;
    private int[] displayCutoutPos = new int[]{0, 0, 0, 0};
    private boolean navigationBarShown;

    public DisplayDimensions(){}

    @SuppressWarnings("deprecation")
    public DisplayDimensions(@NonNull UniEXActivity activity){
        Resources resources = activity.getResources();
        Display windowDisplayView = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        rotation = windowDisplayView.getRotation();
        navigationBarHeight = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"));
        navigationBarWidth = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_width", "dimen", "android"));
        statusBarHeight = resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));
        navigationBarShown = resources.getBoolean(resources.getIdentifier("config_showNavigationBar", "bool", "android"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets windowInsets = activity.getWindow().getDecorView().getRootWindowInsets();
            DisplayCutout d = windowInsets != null ? windowInsets.getDisplayCutout() : null;
            if (d != null){
                displayCutoutPos = new int[]{d.getSafeInsetLeft(), d.getSafeInsetTop(), d.getSafeInsetRight(), d.getSafeInsetRight()};
            }
        }
        calculate();
    }

    private void calculate(){
        switch (rotation){
            case Surface.ROTATION_0:
                neededPaddingTop = statusBarHeight + displayCutoutPos[1];
                break;
            case Surface.ROTATION_90:

                break;
            case Surface.ROTATION_180:

                break;
            case Surface.ROTATION_270:

                break;
        }
    }

    public boolean hasChanged(DisplayDimensions displayDimensions){
        if(displayDimensions == null)return true;
        return getRotation() != displayDimensions.getRotation();
    }

    public int getRotation() { return rotation; }
    public int getNeededPaddingBottom() { return neededPaddingBottom; }
    public int getNeededPaddingLeft() { return neededPaddingLeft; }
    public int getNeededPaddingRight() { return neededPaddingRight; }
    public int getNeededPaddingTop() { return neededPaddingTop; }
}
