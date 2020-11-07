package com.yacineApp.uniEXMusic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.UniEXActivity;
import com.yacineApp.uniEXMusic.components.PlayerCore;
import com.yacineApp.uniEXMusic.components.utils.TimeFormatter;

public class MediaPlayerDialogActivity extends UniEXActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private boolean canUpdateSeek = true;
    private int seekBarUpdaterDelay = 300;
    private int progress = 0;

    private PlayerCore playerCore;
    private SeekBar seekBar;
    private ImageButton playPause;
    private TimeFormatter timeFormatter;
    private TextView timeDuration, timeCurrentPosition;
    private Uri uri;
    private MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            playPause.setImageResource(R.drawable.ic_pause_action_icon);
            handler.postDelayed(seekUpdater, seekBarUpdaterDelay);
        }

        @Override
        public void onPause() {
            super.onPause();
            playPause.setImageResource(R.drawable.ic_play_action_icon);
            handler.removeCallbacks(seekUpdater);
        }
    };
    private PlayerCore.OnPreparedListener onPreparedListener = new PlayerCore.OnPreparedListener(){
        @Override
        public void onPrepared() {
            timeFormatter = new TimeFormatter(playerCore.getDuration());
            timeDuration.setText(timeFormatter.getTotalTime());
            seekBar.setMax(timeFormatter.getTotalTimeInt());
        }
    };
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable seekUpdater = new Runnable() {
        @Override
        public void run() {
            if(canUpdateSeek){
                int pos = playerCore.getCurrentPosition();
                seekBar.setProgress(pos);
                timeCurrentPosition.setText(timeFormatter.getCurrentTime(pos));
            }
            handler.postDelayed(seekUpdater, seekBarUpdaterDelay);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.dialog_button_play_pause_action:
                if(playerCore.isPlaying()) playerCore.pause();
                else playerCore.play();
                break;
            case R.id.dialog_button_close_action:
                finish();
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player_dialog_activity_layout);
        playPause = findViewById(R.id.dialog_button_play_pause_action);
        seekBar = findViewById(R.id.seek_bar_dialog);
        TextView title = findViewById(R.id.dialog_media_title);
        ImageButton close = findViewById(R.id.dialog_button_close_action);
        timeCurrentPosition = findViewById(R.id.dialog_time_current_position);
        timeDuration = findViewById(R.id.dialog_time_duration);
        close.setOnClickListener(this);
        playPause.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        if(timeFormatter != null) timeDuration.setText(timeFormatter.getTotalTime());
        String titleText = null;
        if(uri != null) {
            titleText = PlayerCore.extractFileName(this, uri);
            title.setText(titleText);
        }
        if(playerCore != null) return;
        Intent intent = getIntent();
        if(intent == null){
            finish("");
            return;
        }
        uri = intent.getData();
        if(uri == null){
            finish("");
            return;
        }
        titleText = titleText == null ? PlayerCore.extractFileName(this, uri) : titleText;
        title.setText(titleText);
        playerCore = new PlayerCore(this);
        playerCore.addOnPreparedListener(onPreparedListener);
        playerCore.addCallback(callback);
        playerCore.setPlaylistLength(1);
        playerCore.setMediaSource(uri);
        playerCore.play();
        playerCore.getMediaSession().setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, titleText)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.unknown_media_info))
                .build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(playerCore != null) playerCore.release();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.progress = progress;
        timeCurrentPosition.setText(timeFormatter.getCurrentTime(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        canUpdateSeek = false;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        canUpdateSeek = true;
        playerCore.seekTo(progress);
    }
}
