package com.yacineApp.uniEXMusic.components;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecycleOnItemClickListener implements RecyclerView.OnItemTouchListener {

    private GestureDetector gestureDetector;
    @SuppressWarnings("unused")
    private RecycleOnItemClickListener(){}
    public RecycleOnItemClickListener(@NonNull final RecyclerView recyclerView){
        final RecycleOnItemClickListener listener = this;
        gestureDetector = new GestureDetector(recyclerView.getContext(), new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) { return true; }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if(view != null)
                    listener.onLongItemClick(view, recyclerView.getChildAdapterPosition(view));
            }
        });
    }


    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        View view = rv.findChildViewUnder(e.getX(), e.getY());
        if(view != null && gestureDetector.onTouchEvent(e)) {
            onItemClick(view, rv.getChildAdapterPosition(view));
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
    public abstract void onItemClick(@NonNull View view, int position);
    public abstract void onLongItemClick(@NonNull View view, int position);
}
