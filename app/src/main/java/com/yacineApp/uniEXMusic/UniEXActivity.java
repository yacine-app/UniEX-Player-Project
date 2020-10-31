package com.yacineApp.uniEXMusic;

import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.yacineApp.uniEXMusic.components.utils.ColorPicker;

public abstract class UniEXActivity extends AppCompatActivity {

    private View currentContentView;

    public static abstract class UniEXMusicActivity extends UniEXActivity {

        private int navWidth = 0, navHeight = 0;
        private boolean navBarShown = false;

        private Rect activityInsetsRect = new Rect();
        private Display windowDisplayView, defaultDisplay;
        private Resources resources;
        private DisplayMetrics defaultDisplayMetrics = new DisplayMetrics(), activityDisplayMetrics = new DisplayMetrics();

        @SuppressWarnings("deprecation")
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            Window window = getWindow();
            resources = getResources();
            defaultDisplay = getWindowManager().getDefaultDisplay();
            navHeight = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"));
            navWidth = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_width", "dimen", "android"));
            navBarShown = resources.getBoolean(resources.getIdentifier("config_showNavigationBar", "bool", "android"));
            windowDisplayView = getDisplay();
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            onConfigurationChanged(getResources().getConfiguration());
            super.onCreate(savedInstanceState);
        }

        @SuppressWarnings("deprecation")
        protected Rect getCorrectSystemRect(){
            int statusHeight = resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));
            int addTop = statusHeight, addLeft = 0, addRight = 0, addBottom = 0;
            defaultDisplay.getMetrics(activityDisplayMetrics);
            DisplayCutout displayCutout = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    displayCutout = windowDisplayView.getCutout();
                    addTop = displayCutout != null ? displayCutout.getSafeInsetTop() : statusHeight;
                    addBottom = displayCutout != null ? displayCutout.getSafeInsetBottom() : 0;
                    addLeft = displayCutout != null ? displayCutout.getSafeInsetLeft() : 0;
                    addRight = displayCutout != null ? displayCutout.getSafeInsetRight() : 0;
                }
            }catch (NoSuchMethodError e){ e.printStackTrace(); }
            activityInsetsRect.top = 0;
            activityInsetsRect.right = 0;
            activityInsetsRect.left = 0;
            activityInsetsRect.bottom = 0;
            //Log.e("", activityDisplayMetrics.heightPixels + addTop + (navBarShown && resources.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE ? navHeight : 0) + addBottom + " / " + defaultDisplayMetrics.heightPixels);
            if(activityDisplayMetrics.heightPixels + addTop + (navBarShown && resources.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE ? navHeight : 0) + addBottom < defaultDisplayMetrics.heightPixels)return activityInsetsRect;
            switch (windowDisplayView.getRotation()){
                case Surface.ROTATION_0:
                    activityInsetsRect.top = addTop;
                case Surface.ROTATION_180:
                    activityInsetsRect.bottom = navHeight + addBottom;
                    break;
                case Surface.ROTATION_90:
                    activityInsetsRect.top = (displayCutout != null? addTop : 0) + statusHeight;
                    activityInsetsRect.right = navBarShown ? navWidth + addRight: 0;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        activityInsetsRect.left = displayCutout != null ? displayCutout.getSafeInsetLeft() : 0;
                    break;
                case Surface.ROTATION_270:
                    activityInsetsRect.top = (displayCutout != null? addTop : 0) + statusHeight;
                    activityInsetsRect.left = navBarShown ? navWidth + addLeft : 0;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        activityInsetsRect.right = displayCutout != null ? displayCutout.getSafeInsetRight() : 0;
                    break;
            }
            return activityInsetsRect;
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            defaultDisplay.getRealMetrics(defaultDisplayMetrics);
            super.onConfigurationChanged(newConfig);
        }

    }

    public boolean isPermissionGranted(String permission){
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        currentContentView = getLayoutInflater().inflate(layoutResID, null);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        currentContentView = view;
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        currentContentView = view;
    }

    @SuppressWarnings("unused")
    public void finish(@NonNull String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        super.finish();
    }

    public void finish(@NonNull CharSequence message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        super.finish();
    }

    public void sendMediaEvent(long action){
        try {
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, action).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    protected View getCurrentContentView() { return currentContentView; }

}