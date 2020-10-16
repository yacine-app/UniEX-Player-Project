package com.dzteam.UniExPlayer.Components;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.dzteam.UniExPlayer.R;

import java.util.ArrayList;
import java.util.List;

public class LoadInternalMedia implements Runnable {

    public interface OnDoneListener {
        void onDone(@NonNull List<MediaInfo> mediaInfoList);
    }

    private boolean started = false;
    private ContentResolver contentResolver;
    private OnDoneListener onDoneListener = null;
    private Bitmap defaultIcon;
    private Thread thread = new Thread(this);

    public void setContentResolver(ContentResolver contentResolver) { this.contentResolver = contentResolver; }

    public LoadInternalMedia(@NonNull Context context){
        setContentResolver(context.getContentResolver());
        defaultIcon = BitmapFactory.decodeResource(context.getResources(), R.raw.logo10_15_82143);
    }

    public void setOnDoneListener(OnDoneListener onDoneListener) { this.onDoneListener = onDoneListener; }
    public void execute(){ if(!started) thread.start(); }

    private void done(List<MediaInfo> result){
        if(onDoneListener != null) onDoneListener.onDone(result);
        started = false;
    }

    @Override
    public void run() {
        started = true;
        List<MediaInfo> result = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        if(cursor != null && cursor.moveToFirst()){
            MediaInfo.fillListFromCursor(cursor, result, defaultIcon);
            Log.e("ZEZFGTZERGGRH", String.valueOf(result.size()));
            cursor.close();
        }
        done(result);
    }
}
