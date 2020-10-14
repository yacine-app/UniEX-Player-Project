package com.dzteam.UniExPlayer.Components;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MediaInfo {

    private byte[] rawArt;
    private long id;
    private int year = 0;
    private Bitmap art = null;
    private String title, path, artist, album = null, albumArtist = null, genre = null, comment = null, composer = null, lyrics = null, encoder = null, language = null;
    private boolean enabled = true;

    private MediaInfo(){}

    public MediaInfo(long id, String title, String artist, String path){
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.path = path;
        mkPrepare();
    }

    public static void fillListFromCursor(Cursor cursor, List<MediaInfo> mediaInfoList){
        int mediaId = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int mediaTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int mediaArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int mediaAlbum = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int mediaPath = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        //int mediaYear = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR);
        int mediaAlbumArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST);
        do {
            MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.id = cursor.getLong(mediaId);
            mediaInfo.title = cursor.getString(mediaTitle);
            mediaInfo.artist = cursor.getString(mediaArtist);
            mediaInfo.path = cursor.getString(mediaPath);
            mediaInfo.album = cursor.getString(mediaAlbum);
//.year = cursor.getInt(mediaYear);
            mediaInfo.albumArtist = cursor.getString(mediaAlbumArtist);
            //.e("50985er", String.valueOf(cursor.getString(mediaYear)));
            mediaInfo.mkPrepare();
            mediaInfoList.add(mediaInfo);
        }while (cursor.moveToNext());
    }

    private void mkPrepare(){
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            BitmapFactory.Options options = new BitmapFactory.Options();
            retriever.setDataSource(this.path);
            this.rawArt = retriever.getEmbeddedPicture();
            if (this.rawArt != null)
                this.art = BitmapFactory.decodeByteArray(this.rawArt, 0, this.rawArt.length, options);
            retriever.close();
        }catch (NoSuchMethodError e){
            Log.e(this.getClass().getName(), "Error: ", e);
        }
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public long getId() { return id; }
    @Nullable
    public Bitmap getArt() { return art; }
    @Nullable
    public byte[] getRawArt() { return rawArt; }
    public int getYear() { return year; }
    public String getAlbumArtist() { return albumArtist; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }

    protected void setArt(Bitmap art) { this.art = art; }

    public static List<MediaSessionCompat.QueueItem> fromMediaInfoList(List<MediaInfo> mediaInfoList){
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        for(MediaInfo a: mediaInfoList){
            items.add(
                new MediaSessionCompat.QueueItem(
                    new MediaDescriptionCompat.Builder()
                            .setIconBitmap(a.getArt())
                            .setTitle(a.getTitle())
                            .setSubtitle(a.getArtist())
                            .setMediaUri(Uri.parse(a.getPath()))
                            .build(), a.getId()));
        }
        return items;
    }
}
