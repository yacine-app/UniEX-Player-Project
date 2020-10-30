package com.yacineApp.uniEXMusic.Components;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.yacineApp.uniEXMusic.Components.Utils.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class LoadInternalMedia2 extends AsyncTask<Void, Void, List<MediaInfo>> {

    public interface OnDoneListener {
        void onDone(@NonNull List<MediaInfo> mediaInfoList);
    }

    private ContentResolver contentResolver;
    private OnDoneListener onDoneListener = null;

    public LoadInternalMedia2(ContentResolver contentResolver){this.contentResolver = contentResolver;}

    public void setOnDoneListener(OnDoneListener onDoneListener) { this.onDoneListener = onDoneListener; }

    @Override
    protected void onPostExecute(List<MediaInfo> mediaInfoList) {
        super.onPostExecute(mediaInfoList);
        if(onDoneListener != null) onDoneListener.onDone(mediaInfoList);
    }

    @Override
    protected List<MediaInfo> doInBackground(Void... voids) {
        List<MediaInfo> result = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        if(cursor != null && cursor.moveToFirst()){
            int mediaId = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int mediaTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int mediaArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int mediaPath = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            do {
                long id = cursor.getLong(mediaId);
                String title = cursor.getString(mediaTitle);
                String artist = cursor.getString(mediaArtist);
                String path = cursor.getString(mediaPath);
                result.add(new MediaInfo(id, title, artist, path));
            }while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }
}
