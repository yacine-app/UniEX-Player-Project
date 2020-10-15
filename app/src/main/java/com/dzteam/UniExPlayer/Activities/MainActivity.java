package com.dzteam.UniExPlayer.Activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.dzteam.UniExPlayer.Components.LoadInternalMedia;
import com.dzteam.UniExPlayer.Components.MediaAdapterInfo;
import com.dzteam.UniExPlayer.Components.MediaInfo;
import com.dzteam.UniExPlayer.Components.PlayerCore;
import com.dzteam.UniExPlayer.Components.Utils.TimeFormatter;
import com.dzteam.UniExPlayer.R;
import com.dzteam.UniExPlayer.Services.PlayerService;
import com.dzteam.UniExPlayer.UniEXActivity;

import com.github.stefanodp91.android.circularseekbar.CircularSeekBar;
import com.github.stefanodp91.android.circularseekbar.OnCircularSeekBarChangeListener;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class MainActivity extends UniEXActivity.MediaPlayerActivity implements View.OnClickListener, LoadInternalMedia.OnDoneListener, AdapterView.OnItemClickListener, OnCircularSeekBarChangeListener, View.OnLongClickListener {

    public static final String ACTION_LAUNCH_PLAY_BACK = "ACTION_LAUNCH_PLAY_BACK";
    public static final String ACTION_LAUNCH_PLAY_BACK_IF_PLAYING = "ACTION_LAUNCH_PLAY_BACK_IF_PLAYING";

    private boolean circularSeekBarChanging = false, bound = false;
    private int CIRCULAR_SEEK_BAR_UPDATE_DELAY = 300;
    private float CIRCULAR_PROGRESS = 0.0f;
    private int behaviorDefaultLaunch = BottomSheetBehavior.STATE_COLLAPSED;
    private int previousState = 0;

    private PlayerService playerService = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ListView listView;
    private MediaAdapterInfo mediaAdapterInfo;
    private View includedLayout, mediaController, toolBarBehavior;
    private BottomSheetBehavior bottomSheetBehavior;
    private TimeFormatter timeFormatter;
    private CircularSeekBar circularSeekBar;
    private ImageView mediaArt, frameArt;
    private TextView mediaTitle, mediaArtist, frameTitle, frameArtist, frameCurrentTime, frameDuration;
    private ImageButton skipToPrevious, skipToNext, skipToPreviousFrame, skipToNextFrame, playPause, playPauseFrame, changeLoop, openQuickList;
    private PlayerCore.OnPreparedListener onPreparedListener = new PlayerCore.OnPreparedListener() {
        @Override
        public void onPrepared() {
            timeFormatter = new TimeFormatter(playerService.getDuration());
            frameCurrentTime.setText(timeFormatter.getCurrentTime(0));
            frameDuration.setText(timeFormatter.getTotalTime());
        }
    };
    private PlayerCore.OnLoopChangedListener onLoopChangedListener = new PlayerCore.OnLoopChangedListener() {
        @Override
        public void onChanged(int loopState) {
            switch (loopState){
                case PlayerCore.LOOP_STATE_ALL:
                    changeLoop.setBackgroundResource(R.drawable.ic_play_all_once_icon);
                    break;
                case PlayerCore.LOOP_STATE_ALL_REPEAT:
                    changeLoop.setBackgroundResource(R.drawable.ic_repeat_all_icon);
                    break;
                case PlayerCore.LOOP_STATE_ONE_REPEAT:
                    changeLoop.setBackgroundResource(R.drawable.ic_repeat_one_icon);
                    break;
            }
        }
    };
    private MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {

        @Override
        public void onPause() {
            super.onPause();
            updateUi(playerService.getMetaData());
            handler.removeCallbacks(runnable);
        }

        @Override
        public void onStop() {
            super.onStop();
            onPause();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            updateUi(playerService.getMetaData());
            playerService.setCurrentPlayIndex();
            handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        }
    };
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            PlayerService.SBinder sBinder = (PlayerService.SBinder) service;
            playerService = sBinder.getService();
            playerService.setCallBack(callback);
            playerService.setOnPreparedListener(onPreparedListener);
            playerService.setOnLoopChangedListener(onLoopChangedListener);
            if(playerService.getMediaAdapterInfo() == null){
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) prepareList();
                else finish(getResources().getText(R.string.permission_non_granted_message));
            }else {
                playerService.updateMediaAdapterInfo();
                updateUi(playerService.getMetaData());
                onPreparedListener.onPrepared();
            }
            listView.setAdapter(playerService.getMediaAdapterInfo());
            onPreparedListener.onPrepared();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //playerService = null;
            if(playerService != null) {
                playerService.removeCallBack(callback);
                playerService.removeOnPreparedListener(onPreparedListener);
                playerService.removeOnLoopChangedListener(onLoopChangedListener);
            }
            bound = false;
        }
    };
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(!circularSeekBarChanging && timeFormatter != null){
                int pos = playerService.getCurrentPosition();
                frameCurrentTime.setText(timeFormatter.getCurrentTime(pos));
                circularSeekBar.setProgress(pos * 100.0f / timeFormatter.getTotalTimeInt());
            }
            handler.postDelayed(this, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareUi(0);
        if(!PlayerService.SERVICE_ALREADY_CREATED) startService(new Intent(this, PlayerService.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_tracks_menu, menu);
        return true;
    }

    @Override
    public void onDone(@NonNull final List<MediaInfo> mediaInfoList) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mediaAdapterInfo = new MediaAdapterInfo(mediaInfoList);
                if(playerService != null){
                    mediaAdapterInfo.setSelectedIndex(new MediaAdapterInfo.Index(playerService.getCurrentPlayIndex()));
                    playerService.setMediaQueue(mediaAdapterInfo);
                }
                listView.setAdapter(mediaAdapterInfo);
            }
        });
    }

    @Override
    public void onItemClick(@NonNull AdapterView<?> parent, @NonNull View view, int position, long id) {
        MediaAdapterInfo adapterInfo = (MediaAdapterInfo) parent.getAdapter();
        adapterInfo.setSelected(this, position);
        startService(
                new Intent(this, PlayerService.class)
                        .setAction(PlayerService.ACTION_MEDIA_SERVICE_TO_ITEM)
                        .putExtra(PlayerService.EXTRA_MEDIA_SERVICE_TO_ITEM, position));
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()){
            case R.id.play_pause:
            case R.id.play_pause_frame:
                playerService.playPause();
                break;
            case R.id.skip_to_next:
            case R.id.skip_to_next_frame:
                sendMediaEvent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
                break;
            case R.id.skip_to_previous:
            case R.id.skip_to_previous_frame:
                sendMediaEvent(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
                break;
            case R.id.frameLayout:
                //TODO
                //if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                //organizeBottomSheet(BottomSheetBehavior.STATE_EXPANDED);
                break;
            case R.id.change_loop_mode_frame:
                playerService.changeLoopState();
                break;
            case R.id.open_quick_list_frame:
                //TODO
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if((BottomSheetBehavior.STATE_EXPANDED == bottomSheetBehavior.getState() || BottomSheetBehavior.STATE_DRAGGING == bottomSheetBehavior.getState()) && keyCode == KeyEvent.KEYCODE_BACK){
            bottomSheetBehavior.setDraggable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            organizeBottomSheet(BottomSheetBehavior.STATE_COLLAPSED);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, PlayerService.class), connection, BIND_ADJUST_WITH_ACTIVITY);
        if(getIntent() != null && ACTION_LAUNCH_PLAY_BACK.equals(getIntent().getAction()))
            behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
        else behaviorDefaultLaunch = bottomSheetBehavior.getState();
        organizeBottomSheet(behaviorDefaultLaunch);
        bottomSheetBehavior.setState(behaviorDefaultLaunch);
        handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unbindService(connection);
        }catch (IllegalArgumentException e){
            Log.e(this.getClass().getName(), "Error: ", e);
        }
        bound = false;
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(playerService != null && !playerService.isPlaying()) startService(new Intent(this, PlayerService.class).setAction(PlayerService.ACTION_MEDIA_SERVICE_EXIT));
    }

    @Override
    public void onProgressChange(CircularSeekBar CircularSeekBar, float progress) {
        CIRCULAR_PROGRESS = progress;
        frameCurrentTime.setText(timeFormatter.getCurrentTime((int) (CIRCULAR_PROGRESS * playerService.getDuration() / 100.0f)));
    }

    @Override
    public void onStartTrackingTouch(@NonNull CircularSeekBar CircularSeekBar) {
        circularSeekBarChanging = true;
    }

    @Override
    public void onStopTrackingTouch(@NonNull CircularSeekBar CircularSeekBar) {
        circularSeekBarChanging = false;
        playerService.seekTo((long) (CIRCULAR_PROGRESS * playerService.getDuration() / 100.0f));
    }

    @Override
    public boolean onLongClick(@NonNull View v) {
        switch (v.getId()){
            case R.id.skip_to_previous:
            case R.id.skip_to_previous_frame:
                playerService.rewind();
                break;
            case R.id.skip_to_next:
            case R.id.skip_to_next_frame:
                playerService.fastForward();
                break;
        }
        return true;
    }

    private void handleIntent(Intent intent){
        if(intent != null && intent.getAction() != null){
            switch (intent.getAction()) {
                case ACTION_LAUNCH_PLAY_BACK:
                    behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
                    break;
                case ACTION_LAUNCH_PLAY_BACK_IF_PLAYING:
                    if(bound && playerService.isPlaying())
                        behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
            }
            organizeBottomSheet(behaviorDefaultLaunch);
            bottomSheetBehavior.setState(behaviorDefaultLaunch);
        }
    }

    @SuppressWarnings("deprecation")
    private void prepareUi(final int state){
        if(getIntent() != null && ACTION_LAUNCH_PLAY_BACK.equals(getIntent().getAction()))
            behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;

        setContentView(R.layout.main_activity_layout);

        Toolbar mainActionBar = findViewById(R.id.main_action_bar);

        skipToNext = findViewById(R.id.skip_to_next);
        skipToPrevious = findViewById(R.id.skip_to_previous);
        playPause = findViewById(R.id.play_pause);
        skipToNextFrame = findViewById(R.id.skip_to_next_frame);
        skipToPreviousFrame = findViewById(R.id.skip_to_previous_frame);
        playPauseFrame = findViewById(R.id.play_pause_frame);
        openQuickList = findViewById(R.id.open_quick_list_frame);
        changeLoop = findViewById(R.id.change_loop_mode_frame);

        circularSeekBar = findViewById(R.id.seek_circular_bar_frame);

        mediaArt = findViewById(R.id.media_info_art);
        frameArt = findViewById(R.id.media_info_frame_art);

        mediaArtist = findViewById(R.id.media_info_artist);
        mediaTitle = findViewById(R.id.media_info_title);
        frameTitle = findViewById(R.id.media_info_title_frame);
        frameArtist = findViewById(R.id.media_info_artist_frame);
        frameCurrentTime = findViewById(R.id.current_time_frame);
        frameDuration = findViewById(R.id.duration_time_frame);

        mediaController = findViewById(R.id.media_controller);
        toolBarBehavior = findViewById(R.id.main_action_bar_frame);
        includedLayout = findViewById(R.id.included_frame_layout);

        listView = findViewById(R.id.list);

        circularSeekBar.setIndicatorEnabled(false);
        View bottomSheet = findViewById(R.id.frameLayout);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback(){
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                organizeBottomSheet(newState);
                previousState = newState;
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mediaController.setAlpha(1.0f - slideOffset);
                toolBarBehavior.setAlpha(slideOffset);
            }
        });

        listView.setOnItemClickListener(this);

        bottomSheet.setOnClickListener(this);
        playPause.setOnClickListener(this);
        skipToPrevious.setOnClickListener(this);
        skipToNext.setOnClickListener(this);
        playPauseFrame.setOnClickListener(this);
        skipToPreviousFrame.setOnClickListener(this);
        skipToNextFrame.setOnClickListener(this);
        openQuickList.setOnClickListener(this);
        changeLoop.setOnClickListener(this);

        circularSeekBar.setOnRoundedSeekChangeListener(this);

        setSupportActionBar(mainActionBar);

        organizeBottomSheet(behaviorDefaultLaunch);
        bottomSheetBehavior.setState(behaviorDefaultLaunch);

        if(state != 0){
            organizeBottomSheet(state);
            bottomSheetBehavior.setState(state);
        }

        if (bound && playerService != null && playerService.getMediaAdapterInfo() != null) listView.setAdapter(playerService.getMediaAdapterInfo());
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    private void prepareList(){
        LoadInternalMedia loadInternalMedia = new LoadInternalMedia(this);
        loadInternalMedia.setOnDoneListener(this);
        loadInternalMedia.execute();
    }

    private void updateUi(@Nullable MediaMetadataCompat metaData){
        if(metaData == null)return;
        if(playerService.isPlaying()){
            playPause.setImageResource(R.drawable.ic_pause_icon);
            playPauseFrame.setBackgroundResource(R.drawable.ic_pause_icon);
        } else{
            playPause.setImageResource(R.drawable.ic_play_icon);
            playPauseFrame.setBackgroundResource(R.drawable.ic_play_icon);
        }
        Bitmap art = metaData.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
        CharSequence title = metaData.getText(MediaMetadataCompat.METADATA_KEY_TITLE);
        CharSequence artist = metaData.getText(MediaMetadataCompat.METADATA_KEY_ARTIST);
        mediaArt.setImageBitmap(art);
        frameArt.setImageBitmap(art);
        mediaTitle.setText(title);
        frameTitle.setText(title);
        mediaArtist.setText(artist);
        frameArtist.setText(artist);
        frameTitle.setSelected(true);
        mediaTitle.setSelected(true);
        onLoopChangedListener.onChanged(playerService.getLoopState());
    }

    private void organizeBottomSheet(int state){
        switch (state){
            case BottomSheetBehavior.STATE_EXPANDED:
                mediaController.setVisibility(View.GONE);
                toolBarBehavior.setVisibility(View.VISIBLE);
                includedLayout.setVisibility(View.VISIBLE);
                mediaController.animate().alpha(0.0f).start();
                toolBarBehavior.animate().alpha(1.0f).start();
                circularSeekBarChanging = false;
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
                mediaController.setVisibility(View.VISIBLE);
                toolBarBehavior.setVisibility(View.GONE);
                includedLayout.setVisibility(View.GONE);
                mediaController.animate().alpha(1.0f).start();
                toolBarBehavior.animate().alpha(0.0f).start();
                circularSeekBarChanging = true;
                break;
            case BottomSheetBehavior.STATE_DRAGGING:
                mediaController.setVisibility(View.VISIBLE);
                toolBarBehavior.setVisibility(View.VISIBLE);
                includedLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig.orientation == Configuration.ORIENTATION_UNDEFINED){
            prepareUi(previousState);
            if(bound){
                updateUi(playerService.getMetaData());
                playerService.updateMediaAdapterInfo();
                listView.setAdapter(playerService.getMediaAdapterInfo());
            }
        }
    }
}