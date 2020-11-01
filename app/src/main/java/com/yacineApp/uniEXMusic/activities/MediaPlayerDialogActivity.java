package com.yacineApp.uniEXMusic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.UniEXActivity;
import com.yacineApp.uniEXMusic.components.PlayerCore;

public class MediaPlayerDialogActivity extends UniEXActivity {

    private PlayerCore playerCore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent == null){
            finish("");
            return;
        }
        Uri uri = intent.getData();
        if(uri == null){
            finish("");
            return;
        }
        if("file".equals(intent.getScheme())){

        }else if("content".equals(intent.getScheme())){

        }
        setContentView(R.layout.media_player_dialog_activity_layout);
        playerCore = new PlayerCore(this);
        playerCore.setMediaSource(uri);
        playerCore.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(playerCore != null) playerCore.release();
    }
}
