package com.yacineApp.uniEXMusic.components;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import com.yacineApp.uniEXMusic.R;

import java.util.ArrayList;
import java.util.List;

public class LoadInternalMedia implements Runnable {

    public interface OnDoneListener {
        void onDone(@NonNull List<MediaInfo> mediaInfoList);
    }

    private boolean started = false;
    private int start = 0, length = -1;
    private ContentResolver contentResolver;
    private OnDoneListener onDoneListener = null;
    private Bitmap defaultIcon;
    private Thread thread = new Thread(this);
    private List<MediaInfo> result = new ArrayList<>();

    public void setContentResolver(ContentResolver contentResolver) { this.contentResolver = contentResolver; }

    public LoadInternalMedia(@NonNull Context context){
        setContentResolver(context.getContentResolver());
        defaultIcon = BitmapFactory.decodeResource(context.getResources(), R.raw.default_media_icon);
    }

    public void setLength(int length) { this.length = length; }
    public void setStart(int start) { this.start = start; }
    public void setOnDoneListener(OnDoneListener onDoneListener) { this.onDoneListener = onDoneListener; }
    public void execute(){ if(!started) thread.start(); }
    public void setMediaInfoList(List<MediaInfo> mediaInfoList) { this.result = mediaInfoList; }

    private void done(List<MediaInfo> result){
        if(onDoneListener != null) onDoneListener.onDone(result);
        started = false;
    }

    @Override
    public void run() {
        started = true;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        if(cursor != null && cursor.moveToFirst()){
            MediaInfo.fillListFromCursor(cursor, result, defaultIcon, start, length);
            Log.e("ZEZFGTZERGGRH", String.valueOf(result.size()));
            cursor.close();
        }
        done(result);
    }
}
