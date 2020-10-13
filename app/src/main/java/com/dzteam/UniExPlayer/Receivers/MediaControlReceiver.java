package com.dzteam.UniExPlayer.Receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.session.MediaButtonReceiver;


public class MediaControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
            try {
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE).send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }
}