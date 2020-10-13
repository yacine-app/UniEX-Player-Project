package com.dzteam.UniExPlayer.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dzteam.UniExPlayer.R;

import java.util.ArrayList;
import java.util.List;

public class MediaAdapterInfo extends BaseAdapter {

    private List<MediaInfo> mediaInfoList = new ArrayList<>();
    private List<View> views = new ArrayList<>();

    protected List<MediaInfo> getMediaInfoList() { return mediaInfoList; }

    public void setMediaInfoList(List<MediaInfo> mediaInfoList) { this.mediaInfoList = mediaInfoList; }

    public MediaAdapterInfo(List<MediaInfo> mediaInfoList){
        setMediaInfoList(mediaInfoList);
    }

    public MediaAdapterInfo(){}

    public void setSelected(Context context, int id){
        TextView title, artist;
        for (View v: views){
            title = v.findViewById(R.id.media_info_title);
            artist = v.findViewById(R.id.media_info_artist);
            title.setTextColor(0x0FF252525);
            artist.setTextColor(0x0FF2F2F2F);
            title.setSelected(false);
            title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        title = views.get(id).findViewById(R.id.media_info_title);
        artist = views.get(id).findViewById(R.id.media_info_artist);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setSelected(true);
        title.setTextColor(context.getResources().getColor(R.color.colorMainTheme, null));
        artist.setTextColor(context.getResources().getColor(R.color.colorMainTheme, null));
    }

    @Override
    public boolean isEmpty() {
        return mediaInfoList.isEmpty();
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isEnabled();
    }

    @Override
    public int getCount() {
        return mediaInfoList.size();
    }

    @Override
    public MediaInfo getItem(int position) {
        return mediaInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(view == null){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item_info_list_layout, parent, false);
            view.setTag(new ViewTag(view.findViewById(R.id.media_info_item_list)));
        }
        ViewTag tag = (ViewTag) view.getTag();
        if(!views.contains(view)) views.add(view);
        final MediaInfo info = mediaInfoList.get(position);
        ImageView imageView = view.findViewById(R.id.art_image_list);
        TextView title = view.findViewById(R.id.media_info_title);
        TextView artist = view.findViewById(R.id.media_info_artist);
        //title.setSelected(true);
        title.setText(info.getTitle());
        artist.setText(info.getArtist());
        imageView.setImageBitmap(info.getArt());
        //view.setFocusable(false);
        return view;
    }

    private static class ViewTag {
        View view;
        ViewTag(@NonNull View view){this.view = view;}
    }
}
