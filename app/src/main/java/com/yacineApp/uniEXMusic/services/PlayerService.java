package com.yacineApp.uniEXMusic.services;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.yacineApp.uniEXMusic.activities.MainActivity;
import com.yacineApp.uniEXMusic.ApplicationSetup;
import com.yacineApp.uniEXMusic.activities.ScreenLockPlayerActivity;
import com.yacineApp.uniEXMusic.components.MediaAdapterInfo;
import com.yacineApp.uniEXMusic.components.MediaInfo;
import com.yacineApp.uniEXMusic.components.PlayerCore;
import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.components.utils.ColorPicker;
import com.yacineApp.uniEXMusic.receivers.MediaControlReceiver;

import java.util.List;

public class PlayerService extends MediaBrowserServiceCompat implements PlayerCore.ErrorListener {

    public class SBinder extends Binder { public PlayerService getService(){ return PlayerService.this; } }

    public class CanLaunchLookScreenActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null || intent.getAction() == null) return;
            Intent activity = new Intent(context, ScreenLockPlayerActivity.class);
            try {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    if (isPlaying() && !ScreenLockPlayerActivity.IS_ACTIVITY_RUNNING) PendingIntent.getActivity(context, 0, activity, PendingIntent.FLAG_UPDATE_CURRENT).send();
                }else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                    if (keyguardManager.isKeyguardSecure() && ScreenLockPlayerActivity.IS_ACTIVITY_RUNNING)
                        PendingIntent.getActivity(context, 0, activity.setAction(ScreenLockPlayerActivity.CLOSE_LOOK_SCREEN_ACTIVITY), PendingIntent.FLAG_UPDATE_CURRENT).send();
                }
            } catch (PendingIntent.CanceledException ignored) { }
        }
    }

    public static final int NOTIFICATION_ID = 0x058;

    public static boolean SERVICE_ALREADY_CREATED = false;

    public static final String ACTION_MEDIA_SERVICE_EXIT    = "com.dzteam.UniExPlayer.Services.PlayerService.ACTION_MEDIA_SERVICE_EXIT";
    public static final String ACTION_MEDIA_SERVICE_TO_ITEM = "com.dzteam.UniExPlayer.Services.PlayerService.ACTION_MEDIA_SERVICE_TO_ITEM";
    public static final String EXTRA_MEDIA_SERVICE_TO_ITEM  = "com.dzteam.UniExPlayer.Services.PlayerService.EXTRA_MEDIA_SERVICE_TO_ITEM";
    @SuppressWarnings("unused") public static final String MEDIA_ROOT_ID       = "com.dzteam.UniExPlayer.Services.PlayerService.MEDIA_ROOT_ID";
    @SuppressWarnings("unused") public static final String EMPTY_MEDIA_ROOT_ID = "com.dzteam.UniExPlayer.Services.PlayerService.EMPTY_MEDIA_ROOT_ID";
    public static final String PLAYER_SETTINGS_TAG          = "com.dzteam.UniExPlayer.Services.PlayerService.PLAYER_SETTINGS_TAG";
    public static final String PLAY_LIST_INDEX              = "com.dzteam.UniExPlayer.Services.PlayerService.PLAY_LIST_INDEX";
    public static final String PLAYER_LOOP_STATE            = "com.dzteam.UniExPlayer.Services.PlayerService.PLAYER_LOOP_STATE";
    public static final String PLAYER_LAST_TIME_POSITION    = "com.dzteam.UniExPlayer.Services.PlayerService.PLAYER_LAST_TIME_POSITION";
    public static final String START_SERVICE_FROM_ACTIVITY  = "com.dzteam.UniExPlayer.Services.PlayerService.START_SERVICE_FROM_ACTIVITY";

    private boolean bound = false;
    private boolean startedFromActivity = false;
    private boolean startedFromIntent = false;
    private int[] loopStates = new int[]{PlayerCore.LOOP_STATE_ALL, PlayerCore.LOOP_STATE_ALL_REPEAT, PlayerCore.LOOP_STATE_ONE_REPEAT};
    private int currentLoopIndex = 0;
    private KeyguardManager keyguardManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private MediaControlReceiver mediaControlReceiver = new MediaControlReceiver();
    private IntentFilter intentFilterBecomeNoisy = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private IntentFilter intentFilterScreenOn = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    private IntentFilter intentFilterUserPresent = new IntentFilter(Intent.ACTION_USER_PRESENT);
    private NotificationCompat.Builder notificationBuilder = null;
    private PlayerCore playerCore;
    private MediaAdapterInfo mediaAdapterInfo;
    private RemoteViews collapsedRemoveView, expandedRemoveView;
    private PendingIntent rewindAction, skipToPreviousAction, playPauseAction, skipToNextAction, fastForwardAction, closeAction;
    private SBinder sBinder = new SBinder();
    private SharedPreferences sharedPreferences;
    private CanLaunchLookScreenActivityReceiver screenOnReceiver = new CanLaunchLookScreenActivityReceiver();
    private MediaSessionCompat.Callback mediaCallBack = new MediaSessionCompat.Callback() {

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
            registerReceiver(mediaControlReceiver, intentFilterBecomeNoisy);
            registerReceiver(screenOnReceiver, intentFilterScreenOn);
            registerReceiver(screenOnReceiver, intentFilterUserPresent);
        }

        @Override
        public void onStop() {
            super.onStop();
            try {
                unregisterReceiver(mediaControlReceiver);
                unregisterReceiver(screenOnReceiver);
            }
            catch (IllegalArgumentException ignored){ }
        }

        @Override
        public void onPause() {
            super.onPause();
            updateNotification();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MediaButtonReceiver.handleIntent(playerCore.getMediaSession(), intent);

        if(intent != null && intent.getAction() != null){
            switch (intent.getAction()){
                case ACTION_MEDIA_SERVICE_EXIT:
                    if(bound){
                        playerCore.pause();
                        stopForeground(true);
                    }else {
                        stopForeground(true);
                        stopSelf();
                    }
                    break;
                case ACTION_MEDIA_SERVICE_TO_ITEM:
                    skipTo(intent.getIntExtra(EXTRA_MEDIA_SERVICE_TO_ITEM, 0));
                case START_SERVICE_FROM_ACTIVITY:
                    if(!startedFromIntent) startedFromActivity = true;
                    break;
                default:
                    startedFromIntent = true;
                    break;
            }
        }

        if(!startedFromActivity){

            startedFromActivity = true;
        }

        //Log.e(this.getClass().getName(), String.valueOf(intent.getAction()));

        return START_STICKY;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public void onCreate() {
        super.onCreate();
        mediaAdapterInfo = new MediaAdapterInfo(this);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + "::" + this.getClass().getName());
        wakeLock.acquire();
        SERVICE_ALREADY_CREATED = true;
        playerCore = new PlayerCore(this);
        playerCore.setMediaAdapterInfo(mediaAdapterInfo);
        sharedPreferences = getSharedPreferences(PLAYER_SETTINGS_TAG, Context.MODE_PRIVATE);
        playerCore.skipTo(sharedPreferences.getInt(PLAY_LIST_INDEX, 0), false);
        playerCore.setLoopState(sharedPreferences.getInt(PLAYER_LOOP_STATE, PlayerCore.LOOP_STATE_ALL));
        playerCore.seekTo(sharedPreferences.getLong(PLAYER_LAST_TIME_POSITION, 0L));
        //playerCore.setPlaylistLength(mediaAdapterInfo.getItemCount());
        playerCore.setErrorListener(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCH_PLAY_BACK).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(this, ApplicationSetup.Notification.NOTIFICATION_PLAYER_SERVICE);
        expandedRemoveView = new RemoteViews(getPackageName(), R.layout.notification_controller_layout_expanded);
        collapsedRemoveView = new RemoteViews(getPackageName(), R.layout.notification_controller_layout_collapsed);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification_uniex_logo_small_icon)
                .setCustomContentView(collapsedRemoveView)
                .setCustomBigContentView(expandedRemoveView)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        playerCore.addCallback(mediaCallBack);
        playPauseAction = createPendingAction(PlaybackStateCompat.ACTION_PLAY_PAUSE);
        skipToNextAction = createPendingAction(PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        skipToPreviousAction = createPendingAction(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        rewindAction = createPendingAction(PlaybackStateCompat.ACTION_REWIND);
        fastForwardAction = createPendingAction(PlaybackStateCompat.ACTION_FAST_FORWARD);
        closeAction = PendingIntent.getService(this, 0, new Intent(this, this.getClass()).setAction(ACTION_MEDIA_SERVICE_EXIT), PendingIntent.FLAG_UPDATE_CURRENT);

        collapsedRemoveView.setOnClickPendingIntent(R.id.skip_to_previous_action_notification, skipToPreviousAction);
        collapsedRemoveView.setOnClickPendingIntent(R.id.play_pause_action_notification, playPauseAction);
        collapsedRemoveView.setOnClickPendingIntent(R.id.skip_to_next_action_notification, skipToNextAction);
        collapsedRemoveView.setOnClickPendingIntent(R.id.close_action_notification, closeAction);

        expandedRemoveView.setOnClickPendingIntent(R.id.rewind_action_notification, rewindAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.skip_to_previous_action_notification, skipToPreviousAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.play_pause_action_notification, playPauseAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.skip_to_next_action_notification, skipToNextAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.fast_forward_action_notification, fastForwardAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.close_action_notification, closeAction);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PLAY_LIST_INDEX, getCurrentPlayIndex());
        editor.putInt(PLAYER_LOOP_STATE, getLoopState());
        editor.putLong(PLAYER_LAST_TIME_POSITION, (long) getCurrentPosition());
        editor.apply();
        playerCore.release();
        wakeLock.release();
        SERVICE_ALREADY_CREATED = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //stopForeground(true);
        //s();
        bound = true;
        Log.e("BINDER", "bound!");
        return sBinder;
    }

    /*private void s(){
        if (isPlaying()) startActivity(new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCH_PLAY_BACK).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }*/

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        //s();
        bound = true;
        //stopForeground(true);
        Log.e("BINDER", "rebound!");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //startForeground(NOTIFICATION_ID, notification(null));
        Log.e("BINDER", "unbound!");
        bound = false;
        return false;
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
    public void onError(@Nullable String message) {
        message = message != null ? message : "Unknown error!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private Bitmap createBitmap(@NonNull Drawable drawable, int width, int height){
        width = width == 0 ? Math.max(drawable.getIntrinsicWidth(), 1) : width;
        height = height == 0 ? Math.max(drawable.getIntrinsicHeight(), 1) : height;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void updateNotification(){
        //TODO
        MediaMetadataCompat info = playerCore.getMetaData();
        if(info == null)return;

        assert getMediaInfo() != null;
        ColorPicker.ColorResult colorResult = getMediaInfo().getColorResult();
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TR_BL,
                new int[]{0x0FFFFFFFF, colorResult.getHighColor()});
        gradientDrawable.setGradientRadius(0.0f);

        Bitmap bitmap = createBitmap(gradientDrawable, 1000, 130);

        expandedRemoveView = new RemoteViews(getPackageName(), R.layout.notification_controller_layout_expanded);
        collapsedRemoveView = new RemoteViews(getPackageName(), R.layout.notification_controller_layout_collapsed);

        collapsedRemoveView.setImageViewBitmap(R.id.image_color_theme_notification ,bitmap);
        expandedRemoveView.setImageViewBitmap(R.id.image_color_theme_notification ,bitmap);

        int playPauseIcon;
        String playPauseDes = isPlaying() ? getString(R.string.item_button_description_pause) : getString(R.string.item_button_description_play);;

        if(colorResult.isLightColor()){
            collapsedRemoveView.setTextColor(R.id.media_title_notification, ColorPicker.FIRST_WHITE);
            collapsedRemoveView.setTextColor(R.id.media_artist_notification, ColorPicker.SECOND_WHITE);
            collapsedRemoveView.setTextColor(R.id.app_name_notification, ColorPicker.FIRST_WHITE);
            collapsedRemoveView.setImageViewResource(R.id.close_action_notification, R.drawable.ic_close_action_icon_holo_dark);
            collapsedRemoveView.setImageViewResource(R.id.skip_to_previous_action_notification, R.drawable.ic_skip_to_previous_action_icon_holo_dark);
            collapsedRemoveView.setImageViewResource(R.id.skip_to_next_action_notification, R.drawable.ic_skip_to_next_action_icon_holo_dark);
            collapsedRemoveView.setImageViewResource(R.id.app_name_icon_notification, R.drawable.ic_notification_uniex_logo_holo_dark);
            playPauseIcon = isPlaying() ? R.drawable.ic_pause_action_icon_holo_dark : R.drawable.ic_play_action_icon_holo_dark;

            expandedRemoveView.setTextColor(R.id.media_title_notification, ColorPicker.FIRST_WHITE);
            expandedRemoveView.setTextColor(R.id.media_artist_notification, ColorPicker.SECOND_WHITE);
            expandedRemoveView.setTextColor(R.id.app_name_notification, ColorPicker.FIRST_WHITE);
            expandedRemoveView.setImageViewResource(R.id.close_action_notification, R.drawable.ic_close_action_icon_holo_dark);
            expandedRemoveView.setImageViewResource(R.id.skip_to_previous_action_notification, R.drawable.ic_skip_to_previous_action_icon_holo_dark);
            expandedRemoveView.setImageViewResource(R.id.skip_to_next_action_notification, R.drawable.ic_skip_to_next_action_icon_holo_dark);
            expandedRemoveView.setImageViewResource(R.id.rewind_action_notification, R.drawable.ic_rewind_action_icon_holo_dark);
            expandedRemoveView.setImageViewResource(R.id.fast_forward_action_notification, R.drawable.ic_fast_forward_action_icon_holo_dark);
            expandedRemoveView.setImageViewResource(R.id.app_name_icon_notification, R.drawable.ic_notification_uniex_logo_holo_dark);
        }else {
            collapsedRemoveView.setTextColor(R.id.media_title_notification, ColorPicker.FIRST_BLACK);
            collapsedRemoveView.setTextColor(R.id.media_artist_notification, ColorPicker.SECOND_BLACK);
            collapsedRemoveView.setTextColor(R.id.app_name_notification, ColorPicker.FIRST_BLACK);
            collapsedRemoveView.setImageViewResource(R.id.close_action_notification, R.drawable.ic_close_action_icon);
            collapsedRemoveView.setImageViewResource(R.id.skip_to_previous_action_notification, R.drawable.ic_skip_to_previous_action_icon);
            collapsedRemoveView.setImageViewResource(R.id.skip_to_next_action_notification, R.drawable.ic_skip_to_next_action_icon);
            collapsedRemoveView.setImageViewResource(R.id.app_name_icon_notification, R.drawable.ic_notification_uniex_logo);
            playPauseIcon = isPlaying() ? R.drawable.ic_pause_action_icon : R.drawable.ic_play_action_icon;

            expandedRemoveView.setTextColor(R.id.media_title_notification, ColorPicker.FIRST_BLACK);
            expandedRemoveView.setTextColor(R.id.media_artist_notification, ColorPicker.SECOND_BLACK);
            expandedRemoveView.setTextColor(R.id.app_name_notification, ColorPicker.FIRST_BLACK);
            expandedRemoveView.setImageViewResource(R.id.close_action_notification, R.drawable.ic_close_action_icon);
            expandedRemoveView.setImageViewResource(R.id.skip_to_previous_action_notification, R.drawable.ic_skip_to_previous_action_icon);
            expandedRemoveView.setImageViewResource(R.id.skip_to_next_action_notification, R.drawable.ic_skip_to_next_action_icon);
            expandedRemoveView.setImageViewResource(R.id.rewind_action_notification, R.drawable.ic_rewind_action_icon);
            expandedRemoveView.setImageViewResource(R.id.fast_forward_action_notification, R.drawable.ic_fast_forward_action_icon);
            expandedRemoveView.setImageViewResource(R.id.app_name_icon_notification, R.drawable.ic_notification_uniex_logo);
        }

        collapsedRemoveView.setOnClickPendingIntent(R.id.skip_to_previous_action_notification, skipToPreviousAction);
        collapsedRemoveView.setOnClickPendingIntent(R.id.play_pause_action_notification, playPauseAction);
        collapsedRemoveView.setOnClickPendingIntent(R.id.skip_to_next_action_notification, skipToNextAction);
        collapsedRemoveView.setOnClickPendingIntent(R.id.close_action_notification, closeAction);

        expandedRemoveView.setOnClickPendingIntent(R.id.rewind_action_notification, rewindAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.skip_to_previous_action_notification, skipToPreviousAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.play_pause_action_notification, playPauseAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.skip_to_next_action_notification, skipToNextAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.fast_forward_action_notification, fastForwardAction);
        expandedRemoveView.setOnClickPendingIntent(R.id.close_action_notification, closeAction);

        collapsedRemoveView.setImageViewBitmap(R.id.media_art_notification, info.getBitmap(MediaMetadataCompat.METADATA_KEY_ART));
        collapsedRemoveView.setTextViewText(R.id.media_title_notification, info.getText(MediaMetadataCompat.METADATA_KEY_TITLE));
        collapsedRemoveView.setTextViewText(R.id.media_artist_notification, info.getText(MediaMetadataCompat.METADATA_KEY_ARTIST));

        expandedRemoveView.setImageViewBitmap(R.id.media_art_notification, info.getBitmap(MediaMetadataCompat.METADATA_KEY_ART));
        expandedRemoveView.setTextViewText(R.id.media_title_notification, info.getText(MediaMetadataCompat.METADATA_KEY_TITLE));
        expandedRemoveView.setTextViewText(R.id.media_artist_notification, info.getText(MediaMetadataCompat.METADATA_KEY_ARTIST));


        collapsedRemoveView.setImageViewResource(R.id.play_pause_action_notification, playPauseIcon);
        collapsedRemoveView.setContentDescription(R.id.play_pause_action_notification, playPauseDes);
        expandedRemoveView.setImageViewResource(R.id.play_pause_action_notification, playPauseIcon);
        expandedRemoveView.setContentDescription(R.id.play_pause_action_notification, playPauseDes);


        notificationBuilder.setCustomContentView(collapsedRemoveView);
        notificationBuilder.setCustomBigContentView(expandedRemoveView);

        try {
            startForeground(NOTIFICATION_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }catch (NoSuchMethodError e){
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    public void setCurrentPlayIndex(){ if(mediaAdapterInfo != null) mediaAdapterInfo.setSelectedIndex(new MediaAdapterInfo.Index(getCurrentPlayIndex())); }

    /*public void setMediaQueue(@NonNull MediaAdapterInfo mediaAdapterInfo){
        this.mediaAdapterInfo = mediaAdapterInfo;
        //this.mediaInfoList = mediaAdapterInfo.getMediaInfoList();
        playerCore.setMediaAdapterInfo(mediaAdapterInfo);
        playerCore.setPlaylistLength(mediaAdapterInfo.getItemCount());
    }*/

    @Deprecated
    public void updateMediaAdapterInfo(){
        /*this.mediaAdapterInfo = new MediaAdapterInfo(mediaInfoList);
        this.mediaAdapterInfo.setSelectedIndex(new MediaAdapterInfo.Index(getCurrentPlayIndex()));*/
    }

    public void changeLoopState(){
        currentLoopIndex = currentLoopIndex + 1 >= loopStates.length ? 0 : currentLoopIndex + 1;
        playerCore.setLoopState(loopStates[currentLoopIndex]);
    }

    private PendingIntent createPendingAction(long action){ return MediaButtonReceiver.buildMediaButtonPendingIntent(this, action); }

    @Nullable
    public MediaAdapterInfo getMediaAdapterInfo() { return mediaAdapterInfo; }

    @Nullable
    public MediaInfo getMediaInfo() {
        if(mediaAdapterInfo == null) return null;
        return mediaAdapterInfo.getItem(getCurrentPlayIndex());
    }

    public int getDuration(){ return playerCore.getDuration(); }

    public int getCurrentPosition(){ return playerCore.getCurrentPosition(); }

    public int getCurrentPlayIndex(){ return playerCore.getCurrentPlayIndex(); }

    public int getAudioSessionId(){ return playerCore.getAudioSessionId(); }

    public boolean isPlaying(){ return playerCore.isPlaying(); }

    @SuppressWarnings("unused")
    public boolean isReady(){ return playerCore.isReady(); }

    public int getLoopState(){ return playerCore.getLoopState(); }

    public void skipTo(int pos){ playerCore.skipTo(pos); }

    public void playPause(){ if(isPlaying()) playerCore.pause(); else playerCore.play(); }

    public void seekTo(long progress){ playerCore.seekTo(progress); }

    public void fastForward(){ playerCore.fastForward(); }

    public void rewind(){ playerCore.rewind(); }

    public void setOnPreparedListener(PlayerCore.OnPreparedListener onPreparedListener){ playerCore.addOnPreparedListener(onPreparedListener); }

    public void removeOnPreparedListener(PlayerCore.OnPreparedListener onPreparedListener){ playerCore.removeOnPreparedListener(onPreparedListener); }

    public void setOnLoopChangedListener(PlayerCore.OnLoopChangedListener onLoopChangedListener){ playerCore.addOnLoopChangedListener(onLoopChangedListener); }

    public void removeOnLoopChangedListener(PlayerCore.OnLoopChangedListener onLoopChangedListener){ playerCore.removeOnLoopChangedListener(onLoopChangedListener); }

    public void setCallBack(MediaSessionCompat.Callback callBack){ playerCore.addCallback(callBack); }

    public void removeCallBack(MediaSessionCompat.Callback callback){ playerCore.removeCallBack(callback); }

}