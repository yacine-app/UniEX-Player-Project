package com.yacineApp.uniEXMusic.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.yacineApp.uniEXMusic.R;

import java.util.List;

public class MediaAdapterInfo extends RecyclerView.Adapter<MediaAdapterInfo.ViewHolder> {

    public static class Index {
        private int index;
        public Index(int index){ this.index = Math.max(index, -1); }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        private ImageView art;
        private Context context;
        private ConstraintLayout itemView;
        public ViewHolder(@NonNull ConstraintLayout itemView) {
            super(itemView);
            this.itemView = itemView;
            context = itemView.getContext();
            art = itemView.findViewById(R.id.art_image_list);
            title = itemView.findViewById(R.id.media_info_title);
            artist = itemView.findViewById(R.id.media_info_artist);
            title.setTextColor(0x0FF252525);
            artist.setTextColor(0x0FF2F2F2F);
        }
        public Context getContext() { return context; }
        public void setArt(Bitmap art) { this.art.setImageBitmap(art); }
        public void setArtist(String artist) { this.artist.setText(artist); }
        public void setTitle(String title) { this.title.setText(title); }

    }

    private List<MediaInfo> mediaInfoList = null;
    private Index index = null;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ConstraintLayout constraintLayout = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item_info_list_layout, parent, false);
        return new ViewHolder(constraintLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewTag viewTag = (ViewTag) holder.itemView.getTag();
        if(viewTag == null) holder.itemView.setTag(new ViewTag(holder.itemView, position));
        holder.title.setTextColor(0x0FF252525);
        holder.artist.setTextColor(0x0FF2F2F2F);
        holder.title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        if(index != null && index.index == position){
            holder.title.setTypeface(holder.title.getTypeface(), Typeface.BOLD);
            holder.title.setTextColor(holder.getContext().getResources().getColor(R.color.colorMainTheme, null));
            holder.artist.setTextColor(holder.getContext().getResources().getColor(R.color.colorMainTheme, null));
        }
        MediaInfo mediaInfo = mediaInfoList.get(position);
        holder.setArt(mediaInfo.getArt());
        holder.setArtist(mediaInfo.getArtist());
        holder.setTitle(mediaInfo.getTitle());
    }

    @Override
    public int getItemCount() {
        return mediaInfoList.size();
    }

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

    @Override
    public long getItemId(int position) {
        return mediaInfoList.get(position).getId();
    }

    public MediaInfo getItem(int position){ return mediaInfoList.get(position); }

    protected static class ViewTag {
        private View view;
        private int pos;
        private ViewTag(@NonNull View view, int pos){ this.pos = pos; this.view = view;}
        @SuppressWarnings("unused")
        protected int getPos(){ return pos; }
        @SuppressWarnings("unused")
        protected View getView() { return view; }
    }
}
