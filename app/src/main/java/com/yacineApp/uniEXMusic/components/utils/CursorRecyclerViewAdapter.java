package com.yacineApp.uniEXMusic.components.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private boolean validId;
    private int rowIdColumn;

    private Cursor cursor;
    private Context context;
    private DataSetObserver dataSetObserver;

    public CursorRecyclerViewAdapter(@NonNull Context context, @Nullable Cursor cursor){
        this.context = context;
        this.cursor = cursor;
        this.validId = cursor != null;
        this.rowIdColumn = validId ? cursor.getColumnIndex(MediaStore.Audio.Media._ID) : -1;
        this.dataSetObserver = new RecyclerDataObserver();
        if(cursor != null) cursor.registerDataSetObserver(dataSetObserver);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if(!validId) throw new IllegalStateException("non valid cursor set");
        if(!cursor.moveToPosition(position)) throw new IllegalStateException("couldn't move to position " + position);
        onBindViewHolder(holder, cursor);
    }

    @SuppressWarnings("unused")
    public void release(){
        if(validId && cursor != null) {
            cursor.unregisterDataSetObserver(dataSetObserver);
            cursor.close();
        }
    }

    @SuppressWarnings("unused")
    @Nullable
    public Cursor getCursor() { return cursor; }
    @SuppressWarnings("unused")
    @NonNull
    public Context getContext() { return context; }

    @Override
    public int getItemCount() {
        if(validId && cursor != null) return cursor.getCount();
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        //Log.e(getClass().getName(), "id: " + cursor.getLong(rowIdColumn));
        if(validId && cursor != null && cursor.moveToPosition(position)) return cursor.getLong(rowIdColumn);
        return 0;
    }

    public abstract void onBindViewHolder(@NonNull VH holder, @Nullable Cursor cursor);

    public class RecyclerDataObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            validId = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            validId = false;
            notifyDataSetChanged();
        }
    }

    @Nullable
    public static Cursor createCursor(@NonNull Context context){
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        return context.getContentResolver().query(uri, null, selection, null, sortOrder);
    }

}
