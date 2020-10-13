package com.dzteam.UniExPlayer.Components;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MediaInfo {

    private byte[] rawArt;
    private long id;
    private Bitmap art = null;
    private String title, path, artist;
    private boolean enabled = true;

    public MediaInfo(long id, String title, String artist, String path){
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.path = path;
        mkPrepare();
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
