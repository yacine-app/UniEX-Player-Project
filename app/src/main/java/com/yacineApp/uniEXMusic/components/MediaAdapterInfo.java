package com.yacineApp.uniEXMusic.components;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.yacineApp.uniEXMusic.R;

import java.util.List;

public class MediaAdapterInfo extends BaseAdapter {

    public static class Index {
        private int index;
        public Index(int index){ this.index = Math.max(index, -1); }
    }

    private List<MediaInfo> mediaInfoList = null;
    private Index index = null;

    public MediaAdapterInfo(List<MediaInfo> mediaInfoList){ setMediaInfoList(mediaInfoList); }

    @SuppressWarnings("unused")
    public MediaAdapterInfo(){}

    public List<MediaInfo> getMediaInfoList() { return mediaInfoList; }

    @SuppressWarnings("unused")
    public void clear(){ mediaInfoList.clear(); }

    public void setMediaInfoList(List<MediaInfo> mediaInfoList) { this.mediaInfoList = mediaInfoList; }

    public void setSelectedIndex(Index index){
        if(index.index > -1) this.index = index;
        this.notifyDataSetChanged();
    }

    /*
    @Deprecated
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
     */

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
        @SuppressWarnings("unused")
        ViewTag tag = (ViewTag) view.getTag();
        final MediaInfo info = mediaInfoList.get(position);
        ImageView imageView = view.findViewById(R.id.art_image_list);
        TextView title = view.findViewById(R.id.media_info_title);
        TextView artist = view.findViewById(R.id.media_info_artist);
        title.setTextColor(0x0FF252525);
        artist.setTextColor(0x0FF2F2F2F);
        title.setSelected(false);
        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        if(index != null && index.index == position){
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
            title.setSelected(true);
            title.setTextColor(view.getContext().getResources().getColor(R.color.colorMainTheme, null));
            artist.setTextColor(view.getContext().getResources().getColor(R.color.colorMainTheme, null));
        }
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
