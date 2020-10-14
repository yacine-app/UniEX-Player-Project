package com.dzteam.UniExPlayer.Components;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dzteam.UniExPlayer.Activities.MainActivity;
import com.dzteam.UniExPlayer.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerCore implements AudioManager.OnAudioFocusChangeListener {

    public interface ErrorListener {
        void onError(@Nullable String message);
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnLoopChangedListener {
        void onChanged(int loopState);
    }

    public static final int LOOP_STATE_ALL        = 0x0F56;
    public static final int LOOP_STATE_ALL_REPEAT = 0x050E;
    public static final int LOOP_STATE_ONE_REPEAT = 0x082F;

    private int CURRENT_POSITION = -1, LOOP_STATE = LOOP_STATE_ALL;
    private float PLAY_BACK_SPEED = 1.0f;
    private boolean cannotBePlayed = false;
    protected boolean ready = false;
    @SuppressWarnings("unused")
    private boolean wasPlaying = false;

    private Context context;
    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private ErrorListener errorListener = null;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager audioManager;
    private List<MediaSessionCompat.Callback> callbacks = new ArrayList<>();
    private List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
    protected List<OnPreparedListener> onPreparedListeners = new ArrayList<>();
    private List<OnLoopChangedListener> onLoopChangedListeners = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mediaPlayer.setVolume(1.0f, 1.0f);
        }
    };
    private PlaybackStateCompat.Builder playBackStateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_FAST_FORWARD
                    | PlaybackStateCompat.ACTION_REWIND
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM);

    @SuppressWarnings("unused")
    public PlayerCore(@NonNull Context context, @Nullable String tag){ internalPrepare(context, tag); }

    public PlayerCore(@NonNull Context context){ internalPrepare(context, null); }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange){
            case AudioManager.AUDIOFOCUS_GAIN:
                cannotBePlayed = false;
                play();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mediaPlayer.setVolume(0.40f, 0.40f);
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 2000);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                cannotBePlayed = true;
                break;
        }
    }

    public void addCallback(MediaSessionCompat.Callback callback) { if(!this.callbacks.contains(callback)) this.callbacks.add(callback); }

    public void removeCallBack(MediaSessionCompat.Callback callback){ this.callbacks.remove(callback); }

    public void removeAllCallBacks(){ this.callbacks.clear(); }

    public void addOnLoopChangedListener(OnLoopChangedListener onLoopChangedListener) { if(!this.onLoopChangedListeners.contains(onLoopChangedListener)) this.onLoopChangedListeners.add(onLoopChangedListener); }

    public void removeOnLoopChangedListener(OnLoopChangedListener onLoopChangedListener){ this.onLoopChangedListeners.remove(onLoopChangedListener); }

    public void removeAllOnLoopChangedListeners(){ this.onLoopChangedListeners.clear(); }

    public void addOnPreparedListener(OnPreparedListener onPreparedListener) { if(!this.onPreparedListeners.contains(onPreparedListener)) this.onPreparedListeners.add(onPreparedListener); }

    public void removeOnPreparedListener(OnPreparedListener onPreparedListener) { this.onPreparedListeners.remove(onPreparedListener); }

    public void removeAllOnPreparedListeners(){ this.onPreparedListeners.clear(); }

    public void setQueueFromMediaAdapterInfo(@NonNull MediaAdapterInfo adapterInfo) {
        CURRENT_POSITION = 0;
        items = MediaInfo.fromMediaInfoList(adapterInfo.getMediaInfoList());
        mediaSession.setQueue(items);
    }

    //@RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    @SuppressWarnings("unused")
    public void setMediaSource(String path) {
        try {
            reset();
            this.mediaPlayer.setDataSource(path);
            prepare();
        }catch (IOException e){ if(errorListener != null) errorListener.onError(e.getMessage()); }
    }

    public void setMediaSource(Uri path) {
        try {
            reset();
            this.mediaPlayer.setDataSource(context, path);
            prepare();
        }catch (IOException e){ if(errorListener != null) errorListener.onError(e.getMessage()); }
    }

    public void fastForward(){
        this.mediaPlayer.seekTo(getCurrentPosition() + 5000);
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onFastForward();
    }

    public void rewind(){
        this.mediaPlayer.seekTo(getCurrentPosition() - 5000);
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onRewind();
    }

    public void setLoopState(int loopState){
        if(loopState != LOOP_STATE_ALL && loopState != LOOP_STATE_ALL_REPEAT && loopState != LOOP_STATE_ONE_REPEAT)return;
        this.LOOP_STATE = loopState;
        if(!onLoopChangedListeners.isEmpty())
            for(OnLoopChangedListener a: onLoopChangedListeners)
                a.onChanged(loopState);
    }

    public void skipToNext(){
        CURRENT_POSITION = CURRENT_POSITION + 1 >= items.size()? 0 : CURRENT_POSITION + 1;
        this.setMediaSource(items.get(CURRENT_POSITION).getDescription().getMediaUri());
        play();
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onSkipToNext();
    }

    public void skipToPrevious(){
        if(getCurrentPosition() > 5000){
            seekTo(0L);
            return;
        }
        CURRENT_POSITION = CURRENT_POSITION - 1 < 0? items.size() - 1 : CURRENT_POSITION - 1;
        this.setMediaSource(items.get(CURRENT_POSITION).getDescription().getMediaUri());
        play();
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onSkipToPrevious();
    }

    public void pause(){
        wasPlaying = isPlaying();
        this.mediaPlayer.pause();
        this.mediaSession.setPlaybackState(playBackStateBuilder
                .setState(PlaybackStateCompat.STATE_PAUSED, this.mediaPlayer.getCurrentPosition(), PLAY_BACK_SPEED)
                .build());
        this.mediaSession.setMetadata(getMetaData());
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onPause();
    }

    public void play(){
        if(cannotBePlayed){
            Toast.makeText(context, context.getResources().getString(R.string.unable_to_play_during_call), Toast.LENGTH_SHORT).show();
            return;
        }if(items.isEmpty())return;
        wasPlaying = isPlaying();
        if(!ready) setMediaSource(items.get(CURRENT_POSITION).getDescription().getMediaUri());
        this.mediaPlayer.start();
        this.mediaSession.setPlaybackState(playBackStateBuilder
                .setState(PlaybackStateCompat.STATE_PLAYING, this.mediaPlayer.getCurrentPosition(), PLAY_BACK_SPEED)
                .build());
        this.mediaSession.setMetadata(getMetaData());
        requestAudioFocus();
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onPlay();
    }

    public void stop(){
        wasPlaying = isPlaying();
        this.mediaPlayer.stop();
        this.mediaSession.setPlaybackState(playBackStateBuilder
                .setState(PlaybackStateCompat.STATE_STOPPED, this.mediaPlayer.getCurrentPosition(), PLAY_BACK_SPEED)
                .build());
        this.mediaSession.setMetadata(getMetaData());
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onStop();
    }

    public void skipToQueueItem(long id){
        for(MediaSessionCompat.QueueItem a: items)
            if(a.getQueueId() == id){
                skipTo(items.indexOf(a));
                break;
            }
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onSkipToQueueItem(id);
    }

    public void seekTo(long pos){
        this.mediaPlayer.seekTo((int) pos);
        if(!callbacks.isEmpty())
            for(MediaSessionCompat.Callback a: callbacks)
                a.onSeekTo(pos);
    }

    @SuppressWarnings("unused")
    public void prepareAsync(){ this.mediaPlayer.prepareAsync(); }

    public void prepare() throws IOException { this.mediaPlayer.prepare(); }

    public void reset(){
        ready = false;
        this.mediaPlayer.reset();
    }

    public void setErrorListener(ErrorListener errorListener) { this.errorListener = errorListener; }

    public void skipTo(int position){
        CURRENT_POSITION = position;
        setMediaSource(items.get(CURRENT_POSITION).getDescription().getMediaUri());
        this.mediaSession.setPlaybackState(playBackStateBuilder
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM, this.mediaPlayer.getCurrentPosition(), 1.0f)
                .build());
        this.mediaSession.setMetadata(getMetaData());
        play();
    }

    public void release(){
        stop();
        this.mediaSession.release();
        this.mediaPlayer.reset();
        this.mediaPlayer.release();
        removeAllCallBacks();
        removeAllOnLoopChangedListeners();
        removeAllOnPreparedListeners();
    }

    public int getCurrentPosition(){
        int res = 0;
        try{
            res = this.mediaPlayer.getCurrentPosition();
        }catch (IllegalStateException e){ Log.e(this.getClass().getName(), "", e); }
        return res;
    }

    public int getDuration(){ return this.mediaPlayer.getDuration(); }

    public boolean isPlaying(){ return this.mediaPlayer.isPlaying(); }

    public int getCurrentPlayIndex(){ return this.CURRENT_POSITION; }

    public int getLoopState() { return LOOP_STATE; }

    @Nullable
    public MediaMetadataCompat getMetaData(){
        if(items.isEmpty())return null;
        MediaSessionCompat.QueueItem queueItem = items.get(CURRENT_POSITION);
        return new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, queueItem.getDescription().getIconBitmap())
                .putText(MediaMetadataCompat.METADATA_KEY_TITLE, queueItem.getDescription().getTitle())
                .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, queueItem.getDescription().getSubtitle())
                .build();
    }

    @SuppressWarnings("unused")
    public Context getContext() { return context; }

    public MediaSessionCompat getMediaSession() { return mediaSession; }

    protected MediaPlayer getMediaPlayer() { return mediaPlayer; }

    private void requestAudioFocus(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioManager.requestAudioFocus(audioFocusRequest);
        }else audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void internalPrepare(Context context, String tag){
        tag = tag==null?"Media_Player":tag;
        this.context = context;
        this.mediaPlayer = new MediaPlayer();
        this.mediaSession = new MediaSessionCompat(context, tag);
        final MediaCallBack mediaCallBack = new MediaCallBack(this);
        this.mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        this.mediaSession.setCallback(mediaCallBack);
        this.mediaSession.setSessionActivity(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class).setAction(MainActivity.ACTION_LAUNCH_PLAY_BACK), PendingIntent.FLAG_UPDATE_CURRENT));
        this.mediaSession.setActive(true);
        this.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                switch (LOOP_STATE){
                    case LOOP_STATE_ALL:
                        if(!ready)break;
                        skipToNext();
                        if(CURRENT_POSITION <= 0){
                            stop();
                            reset();
                            CURRENT_POSITION = 0;
                        }
                        break;
                    case LOOP_STATE_ALL_REPEAT:
                        skipToNext();
                        break;
                    case LOOP_STATE_ONE_REPEAT:
                        seekTo(0L);
                        play();
                        break;
                }
            }
        });
        setAudioManager();
    }

    private void setAudioManager(){
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
    }

}