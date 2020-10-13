package com.dzteam.UniExPlayer.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.dzteam.UniExPlayer.Activities.MainActivity;
import com.dzteam.UniExPlayer.ApplicationSetup;
import com.dzteam.UniExPlayer.Components.MediaAdapterInfo;
import com.dzteam.UniExPlayer.Components.PlayerCore;
import com.dzteam.UniExPlayer.R;
import com.dzteam.UniExPlayer.UniEXService;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class PlayerService extends UniEXService implements PlayerCore.ErrorListener {

    public static final String ACTION_DEBUG_RUN             = "com.dzteam.UniExPlayer.Services.PlayerService.ACTION_DEBUG_RUN";
    public static final String EXTRA_QUEUE_POSITION         = "com.dzteam.UniExPlayer.Services.PlayerService.EXTRA_QUEUE_POSITION";
    public static final String ACTION_MEDIA_SERVICE_EXIT    = "com.dzteam.UniExPlayer.Services.PlayerService.ACTION_MEDIA_SERVICE_EXIT";

    public class SBinder extends Binder {
        public PlayerService getService(){
            return PlayerService.this;
        }
    }

    private int[] loopStates = new int[]{PlayerCore.LOOP_STATE_ALL, PlayerCore.LOOP_STATE_ALL_REPEAT, PlayerCore.LOOP_STATE_ONE_REPEAT};
    private int currentLoopIndex = 0;
    public final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notificationBuilder = null;
    private PlayerCore playerCore;
    private NotificationCompat.Action rewindAction, skipToPreviousAction, playAction, pauseAction, skipToNextAction, forwardAction;
    private SBinder sBinder = new SBinder();

    private Notification notification(MediaMetadataCompat info){
        //TODO
        if(info == null)return notificationBuilder.build();
        try{
            Field f = notificationBuilder.getClass().getDeclaredField("mActions");
            f.setAccessible(true);
            f.set(notificationBuilder, new ArrayList<>());
        }catch (NoSuchFieldException | IllegalAccessException e){ Log.e(this.getClass().getName(), "Error: ", e);}
        notificationBuilder.setContentTitle(info.getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setContentText(info.getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setLargeIcon(info.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))
                .addAction(rewindAction)
                .addAction(skipToPreviousAction);

        if(playerCore.isPlaying()) notificationBuilder.addAction(pauseAction).setOngoing(false).setSmallIcon(R.drawable.ic_play_icon);
        else notificationBuilder.addAction(playAction).setOngoing(true).setSmallIcon(R.drawable.ic_pause_icon);
        notificationBuilder.addAction(skipToNextAction)
                .addAction(forwardAction);

        return notificationBuilder.build();
    }

    private NotificationCompat.Action createAction(int icon, String title, long action){
        return new NotificationCompat.Action.Builder(icon, title, createPendingAction(action)).build();
    }

    private PendingIntent createPendingAction(long action){
        return MediaButtonReceiver.buildMediaButtonPendingIntent(this, action);
        //return PendingIntent.getService(this, 0, new Intent(this, this.getClass()).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /*public void setMediaInfo(MediaInfo info){
        startForeground(NOTIFICATION_ID, notification(info));
        playerCore.setMediaSource(info.getPath());
        playerCore.play();
        Toast.makeText(this, info.getPath(), Toast.LENGTH_SHORT).show();
    }*/

    @Nullable
    public MediaMetadataCompat getMetaData(){ return playerCore.getMetaData(); }

    public int getDuration(){ return playerCore.getDuration(); }

    public int getCurrentPosition(){ return playerCore.getCurrentPosition(); }

    public boolean isPlaying(){ return playerCore.isPlaying(); }

    public int getLoopState(){ return playerCore.getLoopState(); }

    public void setLoopState(int state){ playerCore.setLoopState(state); }

    public void setMediaQueue(MediaAdapterInfo mediaAdapterInfo){
        playerCore.setQueueFromMediaAdapterInfo(mediaAdapterInfo);
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

        if(intent != null && ACTION_DEBUG_RUN.equals(intent.getAction())){
            playerCore.skipTo(intent.getIntExtra(EXTRA_QUEUE_POSITION, 0));
            playerCore.play();
        }//else startForeground(NOTIFICATION_ID, notification(null));
        if(intent != null && ACTION_MEDIA_SERVICE_EXIT.equals(intent.getAction())){
            stopForeground(true);
            stopSelf();
        }
        Log.e(this.getClass().getName(), String.valueOf(intent.getAction()));

        return START_NOT_STICKY;
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
            public void onPlay() {
                super.onPlay();
                startForeground(NOTIFICATION_ID, notification(playerCore.getMetaData()));
            }

            @Override
            public void onPause() {
                super.onPause();
                startForeground(NOTIFICATION_ID, notification(playerCore.getMetaData()));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playerCore.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        //Toast.makeText(this, "Bound", Toast.LENGTH_SHORT).show();
        Log.e("BINDER", "bound!");
        return sBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        stopForeground(true);
        Log.e("BINDER", "rebound!");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        startForeground(NOTIFICATION_ID, notification(null));
        Log.e("BINDER", "unbound!");
        return true;
    }
}