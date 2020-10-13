package com.dzteam.UniExPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class UniEXReceiver extends BroadcastReceiver {
    //TODO

    public abstract static class MediaReceiver extends UniEXReceiver {

        public static final String MEDIA_BUTTON_PLAY        = "MEDIA_BUTTON_PLAY";
        public static final String MEDIA_BUTTON_PAUSE       = "MEDIA_BUTTON_PAUSE";
        public static final String MEDIA_BUTTON_NEXT        = "MEDIA_BUTTON_NEXT";
        public static final String MEDIA_BUTTON_PREVIOUS    = "MEDIA_BUTTON_PREVIOUS";
        public static final String MEDIA_BUTTON_SEEK_TO     = "MEDIA_BUTTON_SEEK_TO";
        public static final String MEDIA_BUTTON_STOP        = "MEDIA_BUTTON_STOP";

        protected abstract void onMediaButton(Context context);

        @Override
        public void onReceive(Context context, Intent intent) {
            onMediaButton(context);
        }
    }

}
