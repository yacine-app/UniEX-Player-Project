package com.yacineApp.uniEXMusic.components;

import android.media.MediaPlayer;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.io.IOException;

class MediaCallBack extends MediaSessionCompat.Callback {

    private PlayerCore core;

    MediaCallBack(PlayerCore core){
        this.core = core;
        MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                MediaCallBack.this.core.ready = true;
                if (!MediaCallBack.this.core.onPreparedListeners.isEmpty())
                    for (PlayerCore.OnPreparedListener a : MediaCallBack.this.core.onPreparedListeners)
                        a.onPrepared();
            }
        };
        this.core.getMediaPlayer().setOnPreparedListener(preparedListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.core.stop();
        Log.e(MediaCallBack.class.getName(), "Stop");
    }

    @Override
    public void onPause() {
        super.onPause();
        this.core.pause();
        Log.e(MediaCallBack.class.getName(), "Pause");
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        try {
            this.core.prepare();
        }catch (IOException e){ Log.e(this.getClass().getName(), String.valueOf(e.getMessage()), e);}
        Log.e(MediaCallBack.class.getName(), "Prepare");
    }

    @Override
    public void onPlay() {
        super.onPlay();
        this.core.play();
        Log.e(MediaCallBack.class.getName(), "Play");
    }

    @Override
    public void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
        //this.core.skipToQueueItem(id);
    }

    @Override
    public void onSeekTo(long pos) {
        super.onSeekTo(pos);
        this.core.seekTo(pos);
        Log.e(MediaCallBack.class.getName(), "SeekTo: " + pos);
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        this.core.skipToNext();
        Log.e(MediaCallBack.class.getName(), "SkipToNext");
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        this.core.skipToPrevious();
        Log.e(MediaCallBack.class.getName(), "SkipToPrevious");
    }

    @Override
    public void onFastForward() {
        super.onFastForward();
        this.core.fastForward();
    }

    @Override
    public void onRewind() {
        super.onRewind();
        this.core.rewind();
    }
}
