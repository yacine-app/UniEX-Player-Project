package com.dzteam.UniExPlayer.Services;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.dzteam.UniExPlayer.Activities.MainActivity;
import com.dzteam.UniExPlayer.ApplicationSetup;
import com.dzteam.UniExPlayer.Components.MediaAdapterInfo;
import com.dzteam.UniExPlayer.Components.PlayerCore;
import com.dzteam.UniExPlayer.R;
import com.dzteam.UniExPlayer.Receivers.MediaControlReceiver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PlayerService extends MediaBrowserServiceCompat implements PlayerCore.ErrorListener {

    public static final String ACTION_MEDIA_SERVICE_EXIT    = "com.dzteam.UniExPlayer.Services.PlayerService.ACTION_MEDIA_SERVICE_EXIT";

    public class SBinder extends Binder {
        public PlayerService getService(){
            return PlayerService.this;
        }
    }

    private int[] loopStates = new int[]{PlayerCore.LOOP_STATE_ALL, PlayerCore.LOOP_STATE_ALL_REPEAT, PlayerCore.LOOP_STATE_ONE_REPEAT};
    private int currentLoopIndex = 0;
    public final int NOTIFICATION_ID = 0x058;
    private MediaControlReceiver mediaControlReceiver = new MediaControlReceiver();
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private NotificationCompat.Builder notificationBuilder = null;
    private PlayerCore playerCore;
    private MediaAdapterInfo mediaAdapterInfo;
    private NotificationCompat.Action rewindAction, skipToPreviousAction, playAction, pauseAction, skipToNextAction, forwardAction;
    private SBinder sBinder = new SBinder();

    private void updateNotification(){
        //TODO
        MediaMetadataCompat info = playerCore.getMetaData();
        if(info == null)return;
        try{
            Field f = notificationBuilder.getClass().getDeclaredField("mActions");
            f.setAccessible(true);
            f.set(notificationBuilder, new ArrayList<>());
        }catch (NoSuchFieldException | IllegalAccessException e){ Log.e(this.getClass().getName(), "Error: ", e); }
        notificationBuilder.setContentTitle(info.getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setContentText(info.getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setLargeIcon(info.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))
                .addAction(rewindAction)
                .addAction(skipToPreviousAction);

        if(isPlaying()) notificationBuilder.addAction(pauseAction).setSmallIcon(R.drawable.ic_play_icon);
        else notificationBuilder.addAction(playAction).setSmallIcon(R.drawable.ic_pause_icon);
        notificationBuilder.addAction(skipToNextAction)
                .addAction(forwardAction);

        try {
            startForeground(NOTIFICATION_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }catch (NoSuchMethodError e){
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private NotificationCompat.Action createAction(int icon, String title, long action){
        return new NotificationCompat.Action.Builder(icon, title, createPendingAction(action)).build();
    }

    private PendingIntent createPendingAction(long action){
        return MediaButtonReceiver.buildMediaButtonPendingIntent(this, action);
    }

    @Nullable
    public MediaMetadataCompat getMetaData(){ return playerCore.getMetaData(); }

    public int getDuration(){ return playerCore.getDuration(); }

    public int getCurrentPosition(){ return playerCore.getCurrentPosition(); }

    public int getCurrentPlayIndex(){ return playerCore.getCurrentPlayIndex(); }

    public boolean isPlaying(){ return playerCore.isPlaying(); }

    public int getLoopState(){ return playerCore.getLoopState(); }

    public void setMediaQueue(MediaAdapterInfo mediaAdapterInfo){
        this.mediaAdapterInfo = mediaAdapterInfo;
        playerCore.setQueueFromMediaAdapterInfo(mediaAdapterInfo);
    }

    public void skipTo(int pos){ playerCore.skipTo(pos); }

    public void setCurrentPlayIndex(){
        //Toast.makeText(this, String.valueOf(getCurrentPlayIndex()), Toast.LENGTH_SHORT).show();
        if(mediaAdapterInfo != null) {
            mediaAdapterInfo.setSelected(this, getCurrentPlayIndex());
            mediaAdapterInfo.notifyDataSetChanged();
        }
    }

    public void playPause(){
        if(isPlaying()) playerCore.pause();
        else playerCore.play();
    }

    public void seekTo(long progress){ playerCore.seekTo(progress); }

    public void setOnPreparedListener(PlayerCore.OnPreparedListener onPreparedListener){ playerCore.addOnPreparedListener(onPreparedListener); }

    public void removeOnPreparedListener(PlayerCore.OnPreparedListener onPreparedListener){ playerCore.removeOnPreparedListener(onPreparedListener); }

    public void setOnLoopChangedListener(PlayerCore.OnLoopChangedListener onLoopChangedListener){ playerCore.addOnLoopChangedListener(onLoopChangedListener); }

    public void removeOnLoopChangedListener(PlayerCore.OnLoopChangedListener onLoopChangedListener){ playerCore.removeOnLoopChangedListener(onLoopChangedListener); }

    public void setCallBack(MediaSessionCompat.Callback callBack){ playerCore.addCallback(callBack); }

    public void removeCallBack(MediaSessionCompat.Callback callback){ playerCore.removeCallBack(callback); }

    public void changeLoopState(){
        currentLoopIndex = currentLoopIndex + 1 >= loopStates.length ? 0 : currentLoopIndex + 1;
        playerCore.setLoopState(loopStates[currentLoopIndex]);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO
        MediaButtonReceiver.handleIntent(playerCore.getMediaSession(), intent);

        if(intent != null && ACTION_MEDIA_SERVICE_EXIT.equals(intent.getAction())){
            stopForeground(true);
            stopSelf();
        }

        Log.e(this.getClass().getName(), String.valueOf(intent.getAction()));

        return START_STICKY;
    }

    @Override
    public void onError(@Nullable String message) {
        message = message != null ? message : "Unknown error!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playerCore = new PlayerCore(this);
        playerCore.setErrorListener(this);
        rewindAction = createAction(R.drawable.ic_rewind_icon, "Rewind", PlaybackStateCompat.ACTION_REWIND);
        skipToPreviousAction = createAction(R.drawable.ic_backward_icon, "Prev", PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        playAction = createAction(R.drawable.ic_play_icon, "Play", PlaybackStateCompat.ACTION_PLAY);
        pauseAction = createAction(R.drawable.ic_pause_icon, "Pause", PlaybackStateCompat.ACTION_PAUSE);
        skipToNextAction = createAction(R.drawable.ic_forward_icon, "Next", PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        forwardAction = createAction(R.drawable.ic_seek_forward_icon, "For", PlaybackStateCompat.ACTION_FAST_FORWARD);
        notificationBuilder = new NotificationCompat.Builder(this, ApplicationSetup.Notification.NOTIFICATION_PLAYER_SERVICE);
        notificationBuilder.setSmallIcon(R.drawable.ic_play_icon)
                .setContentTitle(getResources().getString(R.string.unknown_media_info))
                .setContentText(getResources().getString(R.string.unknown_media_info))
                .setShowWhen(false)
                .setColorized(true)
                .setAutoCancel(false)
                .setColor(0x0EF9A7E)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(playerCore.getMediaSession().getSessionToken())
                        .setShowActionsInCompactView(1, 2, 3))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.raw.ic_track_media))
                .addAction(rewindAction)
                .addAction(skipToPreviousAction)
                .addAction(pauseAction)
                .addAction(skipToNextAction)
                .addAction(forwardAction)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        playerCore.addCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                updateNotification();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                updateNotification();
            }

            @Override
            public void onPlay() {
                super.onPlay();
                updateNotification();
                registerReceiver(mediaControlReceiver, intentFilter);
            }

            @Override
            public void onStop() {
                super.onStop();
                unregisterReceiver(mediaControlReceiver);
            }

            @Override
            public void onPause() {
                super.onPause();
                updateNotification();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //playerCore.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        //stopForeground(true);
        Log.e("BINDER", "bound!");
        return sBinder;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        //stopForeground(true);
        Log.e("BINDER", "rebound!");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //startForeground(NOTIFICATION_ID, notification(null));
        Log.e("BINDER", "unbound!");
        return false;
    }
}