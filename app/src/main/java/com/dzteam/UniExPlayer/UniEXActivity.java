package com.dzteam.UniExPlayer;

import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
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

public abstract class UniEXActivity extends AppCompatActivity {

    public static abstract class MediaPlayerActivity extends UniEXActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }
    }

    //TODO

    private View currentContentView = null;
    private int navigationBarHeight, navigationBarWidth, statusBarHeight;
    private boolean navigationBarShown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(navigationBarWidth != 0 && navigationBarHeight != 0 && statusBarHeight != 0)return;
        navigationBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        navigationBarWidth = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_width", "dimen", "android"));
        statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        navigationBarShown = getResources().getBoolean(getResources().getIdentifier("config_showNavigationBar", "bool", "android"));
        //getWindow().setNavigationBarColor(0x0FFFFFF);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        currentContentView = view;
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        this.currentContentView = view;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        currentContentView = LayoutInflater.from(this).inflate(layoutResID, null, true);
    }

    public boolean isPermissionGranted(String permission){
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isNavigationBarShown() { return navigationBarShown; }

    public int getNavigationBarHeight(){ return navigationBarShown ? navigationBarHeight : 0; }

    public int getNavigationBarWidth(){ return navigationBarShown ? navigationBarWidth : 0; }

    public int getStatusBarHeight(){
        return statusBarHeight;
    }

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

    /*public boolean isPermissionsGranted(String[] permissions){
        return ContextCompat.checkSelfPermission(this, permissions) == PackageManager.PERMISSION_GRANTED;
    }*/

}