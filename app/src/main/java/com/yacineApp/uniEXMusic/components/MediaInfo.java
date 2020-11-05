package com.yacineApp.uniEXMusic.components;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yacineApp.uniEXMusic.components.utils.ColorPicker;

import java.util.ArrayList;
import java.util.List;

public class MediaInfo {

    private byte[] rawArt;
    private long id;
    private int year = 0;
    private Bitmap art = null;
    private Bitmap smallArt = null;
    private String title, path, artist, album = null, albumArtist = null, genre = null, comment = null, composer = null, lyrics = null, encoder = null, language = null;
    private boolean enabled = true;
    private ColorPicker.ColorResult colorResult;

    private MediaInfo(){}

    public MediaInfo(long id, String title, String artist, String path){
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.path = path;
        mkPrepare(null);
    }

    public static void fillListFromCursor(@NonNull Cursor cursor, @NonNull List<MediaInfo> mediaInfoList, @Nullable Bitmap defaultIcon) {
        fillListFromCursor(cursor, mediaInfoList, defaultIcon, 0, -1);
    }

    public static void fillListFromCursor(@NonNull Cursor cursor, @NonNull List<MediaInfo> mediaInfoList, @Nullable Bitmap defaultIcon, int start, int length){
        int mediaId = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int mediaTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int mediaArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int mediaAlbum = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int mediaPath = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        //int mediaYear = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR);
        int mediaAlbumArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST);
        length = Math.max(length, -1);
        int count = length < 0 ? -2 : -1;
        cursor.move(Math.max(start, 0));
        do {
            long id = cursor.getLong(mediaId);
            long nId = mediaInfoList.size() > 0 && count > -1 ? mediaInfoList.get(count).id : 0L;
            if(count > length - 2 && length > -1) break;
            if(length < 0 || nId != id) {
                MediaInfo mediaInfo = new MediaInfo();
                mediaInfo.id = id;
                mediaInfo.title = cursor.getString(mediaTitle);
                mediaInfo.artist = cursor.getString(mediaArtist);
                mediaInfo.path = cursor.getString(mediaPath);
                mediaInfo.album = cursor.getString(mediaAlbum);
                //.year = cursor.getInt(mediaYear);
                mediaInfo.albumArtist = cursor.getString(mediaAlbumArtist);
                //.e("50985er", String.valueOf(cursor.getString(mediaYear)));
                mediaInfo.mkPrepare(defaultIcon);
                mediaInfoList.add(mediaInfo);
            }
            if(length > 0) count++;
        }while (cursor.moveToNext());
    }

    private void mkPrepare(Bitmap def){
        //if (def == null)return;
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            BitmapFactory.Options options = new BitmapFactory.Options();
            retriever.setDataSource(this.path);
            this.rawArt = retriever.getEmbeddedPicture();
            if (this.rawArt != null)
                this.art = BitmapFactory.decodeByteArray(this.rawArt, 0, this.rawArt.length, options);
            else this.art = def;
            this.smallArt = Bitmap.createScaledBitmap(this.art, 80, 80, false);
            this.colorResult = this.rawArt != null ? ColorPicker.valueOf(this.art) : ColorPicker.valueOf(0x0FFEF9A7E, 0x0FF040404);
            retriever.close();
        }catch (NoSuchMethodError e){
            Log.e(this.getClass().getName(), "Error: ", e);
        }
    }

    @SuppressWarnings("unused")
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @SuppressWarnings("unused")
    public boolean isEnabled() { return enabled; }
    public long getId() { return id; }
    @NonNull
    public Bitmap getArt() { return art; }
    @NonNull
    public Bitmap getSmallArt() { return smallArt; }
    @Nullable
    @SuppressWarnings("unused")
    public byte[] getRawArt() { return rawArt; }
    @SuppressWarnings("unused")
    public int getYear() { return year; }
    @SuppressWarnings("unused")
    public String getAlbumArtist() { return albumArtist; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }
    public ColorPicker.ColorResult getColorResult() { return colorResult; }

    @SuppressWarnings("unused")
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
