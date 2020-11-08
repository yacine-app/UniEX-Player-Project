package com.yacineApp.uniEXMusic.components;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.components.utils.CursorRecyclerViewAdapter;

import java.util.HashMap;

public class MediaAdapterInfo extends CursorRecyclerViewAdapter<MediaAdapterInfo.ViewHolder> {

    public static class Index {
        private int index;
        public Index(int index){ this.index = Math.max(index, -1); }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        private ImageView art;
        public ViewHolder(@NonNull ConstraintLayout constraintLayout) {
            super(constraintLayout);
            art = itemView.findViewById(R.id.art_image_list);
            title = itemView.findViewById(R.id.media_info_title);
            artist = itemView.findViewById(R.id.media_info_artist);
            title.setTextColor(0x0FF252525);
            artist.setTextColor(0x0FF2F2F2F);
            title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        public void setArt(Bitmap art) { this.art.setImageBitmap(art); }
        public void setArtist(String artist) { this.artist.setText(artist); }
        public void setTitle(String title) { this.title.setText(title); }

    }

    //private List<MediaInfo> mediaInfoList = new ArrayList<>();
    private HashMap<Long, MediaInfo> mediaInfoHashMap = new HashMap<>();
    private Index index = null;
    private AppCompatActivity activity;
    private Bitmap defaultIcon;
    private MediaInfo.OnDonePreparingListener onDonePreparingListener = new MediaInfo.OnDonePreparingListener() {
        @Override
        public void onPrepared(@NonNull final MediaInfo mediaInfo, @NonNull final ViewHolder holder) {
            if(activity == null) return;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    holder.setArt(mediaInfo.getSmallArt());
                }
            });
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ConstraintLayout constraintLayout = (ConstraintLayout) LayoutInflater.from(getContext()).inflate(R.layout.media_item_info_list_layout, parent, false);
        return new ViewHolder(constraintLayout);
    }

    @Nullable
    public MediaInfo getItem(int pos){
        return getItem(pos, null);
    }

    @Nullable
    private MediaInfo getItem(int pos, @Nullable ViewHolder holder){
        Cursor cursor = getCursor();
        assert cursor != null;
        long id = getItemId(pos);
        if(!cursor.moveToPosition(pos)) if(!cursor.moveToFirst()) return null;
        //if(mediaInfoList.size() != getItemCount()) setSize(getItemCount());
        MediaInfo mediaInfo = mediaInfoHashMap.get(id);
        if(mediaInfo == null){
            mediaInfo = MediaInfo.valueOf(cursor);
            if(holder == null) mediaInfo.prepare(defaultIcon);
            else {
                mediaInfo.setOnDonePreparingListener(onDonePreparingListener);
                mediaInfo.prepareSync(defaultIcon, holder);
            }
            mediaInfoHashMap.put(id, mediaInfo);
        }
        return mediaInfo;

    }

    @Nullable
    public MediaSessionCompat.QueueItem getItemQueue(int pos){
        MediaInfo a = getItem(pos);
        if(a == null) return null;
        return new MediaSessionCompat.QueueItem(
                new MediaDescriptionCompat.Builder()
                        .setIconBitmap(a.getArt())
                        .setTitle(a.getTitle())
                        .setSubtitle(a.getArtist())
                        .setMediaUri(Uri.parse(a.getPath()))
                        .build(), a.getId());
    }

    //public void setSize(int size){
        //mediaInfoList = new ArrayList<>(Collections.<MediaInfo>nCopies(size, null));
    //}

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @Nullable Cursor cursor) {
        if(cursor == null) return;
        //if(getItemCount() != mediaInfoList.size()) setSize(getItemCount());
        holder.title.setTextColor(0x0FF252525);
        holder.artist.setTextColor(0x0FF2F2F2F);
        holder.title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        if(index != null && index.index == cursor.getPosition()){
            holder.title.setTypeface(holder.title.getTypeface(), Typeface.BOLD);
            holder.title.setTextColor(getContext().getResources().getColor(R.color.colorMainTheme, null));
            holder.artist.setTextColor(getContext().getResources().getColor(R.color.colorMainTheme, null));
        }
        int pos = cursor.getPosition();
        MediaInfo mediaInfo = getItem(pos, holder);
        /*if (MediaInfo.contains(mediaInfoList, getItemId(pos))) {
            mediaInfo = MediaInfo.valueOf(cursor);
            mediaInfo.setOnDonePreparingListener(onDonePreparingListener);
            mediaInfo.prepareSync(defaultIcon, holder);
            mediaInfoList.add(mediaInfo);
        }else mediaInfo = mediaInfoList.get(pos);*/
        if(mediaInfo == null) return;
        holder.setArtist(mediaInfo.getArtist());
        holder.setTitle(mediaInfo.getTitle());
        holder.setArt(mediaInfo.getSmallArt());
    }

    @SuppressWarnings("unused")
    public MediaAdapterInfo(@NonNull AppCompatActivity activity){
        super(activity, CursorRecyclerViewAdapter.createCursor(activity));
        this.activity = activity;
        defaultIcon = BitmapFactory.decodeResource(activity.getResources(), R.raw.default_media_icon);
    }

    public MediaAdapterInfo(@NonNull Context context){
        super(context, CursorRecyclerViewAdapter.createCursor(context));
        defaultIcon = BitmapFactory.decodeResource(context.getResources(), R.raw.default_media_icon);
    }

    public void setActivity(AppCompatActivity activity) {
        this.activity = activity;
        changeCursor(CursorRecyclerViewAdapter.createCursor(activity));
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setSelectedIndex(@NonNull Index index){
        int lastIndex = this.index != null ? this.index.index : -1;
        if(index.index > -1) this.index = index;
        this.notifyItemChanged(lastIndex);
        this.notifyItemChanged(index.index);
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
