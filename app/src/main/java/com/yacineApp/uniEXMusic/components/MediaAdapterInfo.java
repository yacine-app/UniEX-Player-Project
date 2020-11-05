package com.yacineApp.uniEXMusic.components;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yacineApp.uniEXMusic.R;

import java.util.ArrayList;
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
        public ViewHolder(@NonNull ConstraintLayout itemView) {
            super(itemView);
            context = itemView.getContext();
            art = itemView.findViewById(R.id.art_image_list);
            title = itemView.findViewById(R.id.media_info_title);
            artist = itemView.findViewById(R.id.media_info_artist);
            title.setTextColor(0x0FF252525);
            artist.setTextColor(0x0FF2F2F2F);
            title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        public Context getContext() { return context; }
        public void setArt(Bitmap art) { this.art.setImageBitmap(art); }
        public void setArtist(String artist) { this.artist.setText(artist); }
        public void setTitle(String title) { this.title.setText(title); }

    }

    private List<MediaInfo> mediaInfoList = new ArrayList<>();
    private Index index = null;
    private RecyclerView recyclerView;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ConstraintLayout constraintLayout = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item_info_list_layout, parent, false);
        /*LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        LoadInternalMedia loadInternalMedia = new LoadInternalMedia(parent.getContext());
        loadInternalMedia.setMediaInfoList(mediaInfoList);
        assert layoutManager != null;
        loadInternalMedia.setLength(layoutManager.getHeight() / 63);
        loadInternalMedia.setStart(layoutManager.findFirstVisibleItemPosition());
        loadInternalMedia.setOnDoneListener(new LoadInternalMedia.OnDoneListener() {
            @Override
            public void onDone(@NonNull List<MediaInfo> mediaInfoList) {
                MediaAdapterInfo.this.mediaInfoList = mediaInfoList;
                notifyDataSetChanged();
            }
        });
        loadInternalMedia.execute();*/
        return new ViewHolder(constraintLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.title.setTextColor(0x0FF252525);
        holder.artist.setTextColor(0x0FF2F2F2F);
        holder.title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        if(index != null && index.index == position){
            holder.title.setTypeface(holder.title.getTypeface(), Typeface.BOLD);
            holder.title.setTextColor(holder.getContext().getResources().getColor(R.color.colorMainTheme, null));
            holder.artist.setTextColor(holder.getContext().getResources().getColor(R.color.colorMainTheme, null));
        }
        MediaInfo mediaInfo = mediaInfoList.get(position);
        holder.setArt(mediaInfo.getSmallArt());
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

    public MediaAdapterInfo(@NonNull RecyclerView recyclerView){
        this.recyclerView = recyclerView;
    }

    public List<MediaInfo> getMediaInfoList() { return mediaInfoList; }

    @SuppressWarnings("unused")
    public void clear(){ mediaInfoList.clear(); }

    public void setMediaInfoList(List<MediaInfo> mediaInfoList) { this.mediaInfoList = mediaInfoList; }

    public void setSelectedIndex(@NonNull Index index){
        int lastIndex = this.index != null ? this.index.index : -1;
        if(index.index > -1) this.index = index;
        this.notifyItemChanged(lastIndex);
        this.notifyItemChanged(index.index);
    }

    @Override
    public long getItemId(int position) {
        return mediaInfoList.get(position).getId();
    }

    @Nullable
    public MediaInfo getItem(int position){
        return mediaInfoList.size() == 0 ? null : mediaInfoList.get(position);
    }

    @SuppressWarnings("unused")
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
