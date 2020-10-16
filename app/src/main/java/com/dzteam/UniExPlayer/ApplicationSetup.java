package com.dzteam.UniExPlayer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class ApplicationSetup extends Application {

    public interface Notification {
        String NOTIFICATION_PLAYER_SERVICE = "NOTIFICATION_PLAYER_SERVICE";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        List<NotificationChannel> channels = new ArrayList<>();
        NotificationManager manager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) manager = getSystemService(NotificationManager.class);
        else return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel playerService = new NotificationChannel(Notification.NOTIFICATION_PLAYER_SERVICE, getResources().getString(R.string.notification_player_service), NotificationManager.IMPORTANCE_LOW);
            playerService.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            try{
                playerService.setAllowBubbles(false);
            }catch (NoSuchMethodError e){
                Log.e(this.getClass().getName(), "NotificationChannel#setAllowBubbles() is not existed", e);
            }
            playerService.setShowBadge(false);
            channels.add(playerService);
            manager.createNotificationChannels(channels);
        }
    }
}
