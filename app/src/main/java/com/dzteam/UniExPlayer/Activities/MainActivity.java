package com.dzteam.UniExPlayer.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.dzteam.UniExPlayer.Components.LoadInternalMedia;
import com.dzteam.UniExPlayer.Components.MediaAdapterInfo;
import com.dzteam.UniExPlayer.Components.MediaInfo;
import com.dzteam.UniExPlayer.Components.PlayerCore;
import com.dzteam.UniExPlayer.Components.Utils.TimeFormatter;
import com.dzteam.UniExPlayer.R;
import com.dzteam.UniExPlayer.Services.PlayerService;
import com.dzteam.UniExPlayer.UniEXActivity;

import com.gauravk.audiovisualizer.visualizer.CircleLineVisualizer;

import com.github.stefanodp91.android.circularseekbar.CircularSeekBar;
import com.github.stefanodp91.android.circularseekbar.OnCircularSeekBarChangeListener;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class MainActivity extends UniEXActivity implements View.OnClickListener, LoadInternalMedia.OnDoneListener, AdapterView.OnItemClickListener, OnCircularSeekBarChangeListener, View.OnLongClickListener {

    public static final String ACTION_LAUNCH_PLAY_BACK = "ACTION_LAUNCH_PLAY_BACK";
    public static final String ACTION_LAUNCH_PLAY_BACK_IF_PLAYING = "ACTION_LAUNCH_PLAY_BACK_IF_PLAYING";

    private boolean circularSeekBarChanging = false, bound = false, updateLayoutFit = false;
    private int CIRCULAR_SEEK_BAR_UPDATE_DELAY = 300;
    private int LAYOUT_CHANGES_UPDATE_DELAY = 1000 / 60;
    private float CIRCULAR_PROGRESS = 0.0f;
    private float CURRENT_VOLUME_PROGRESS = 0.0f;
    private int behaviorDefaultLaunch = BottomSheetBehavior.STATE_COLLAPSED;
    private int previousState = 0;
    private int defaultPeekHeight = 55;

    private PlayerService playerService = null;
    private Intent oldIntent;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ListView listView;
    private Display windowDisplayView;
    private Toolbar mainActionBar, frameActionBar;
    private CircleLineVisualizer circleLineVisualizer;
    private CardView mediaArtCard;
    private MediaAdapterInfo mediaAdapterInfo;
    private View includedLayout;
    private View mediaController;
    private View firstViewChild;
    private BottomSheetBehavior bottomSheetBehavior;
    private TimeFormatter timeFormatter;
    private FrameLayout volumePanelView;
    private CircularSeekBar circularSeekBar, volumePanelController;
    private ImageView mediaArt, frameArt;
    private TextView mediaTitle, mediaArtist, frameTitle, frameArtist, frameCurrentTime, frameDuration;
    private ImageButton playPause, playPauseFrame, changeLoop;
    private PlayerCore.OnPreparedListener onPreparedListener = new PlayerCore.OnPreparedListener() {
        @Override
        public void onPrepared() {
            timeFormatter = new TimeFormatter(playerService.getDuration());
            frameCurrentTime.setText(timeFormatter.getCurrentTime(0));
            frameDuration.setText(timeFormatter.getTotalTime());
            circularSeekBar.setProgress(0.0f);
            updateUi(playerService.getMetaData());
            playerService.setCurrentPlayIndex();
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
            if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
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
            if(playerService.getMediaAdapterInfo() == null) prepareList();
            else {
                playerService.updateMediaAdapterInfo();
                updateUi(playerService.getMetaData());
                onPreparedListener.onPrepared();
            }
            listView.setAdapter(playerService.getMediaAdapterInfo());
            onPreparedListener.onPrepared();
            changeBehaviorState(behaviorDefaultLaunch);
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
            if(!circularSeekBarChanging && timeFormatter != null) {
                int pos = playerService.getCurrentPosition();
                frameCurrentTime.setText(timeFormatter.getCurrentTime(pos));
                circularSeekBar.setProgress(pos * 100.0f / timeFormatter.getTotalTimeInt());
                circleLineVisualizer.setRotation(pos / 110.0f);
            }
            if(playerService != null && playerService.isPlaying()) handler.postDelayed(this, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        }
    };
    private Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            updateLayoutFit = true;
            setWithOrientation();
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) circleLineVisualizer.getLayoutParams();
            ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) mediaArtCard.getLayoutParams();
            if (includedLayout.getWidth() > 100 && includedLayout.getHeight() > 100) {
                int a = includedLayout.getWidth();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) a = includedLayout.getHeight();
                params.width = a;
                params.height = a;
                params1.width = params.width / 2;
                params1.height = params.height / 2;
                params1.circleRadius = params1.width / 2;
                circleLineVisualizer.setLayoutParams(params);
                mediaArtCard.setLayoutParams(params1);
            }
            if(includedLayout.getAlpha() == 0.0f && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                includedLayout.animate().alpha(1.0f).start();
            handler.postDelayed(this, LAYOUT_CHANGES_UPDATE_DELAY);
        }
    };
    private Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            volumePanelView.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    Toast.makeText(getApplicationContext(), "00", Toast.LENGTH_SHORT).show();
                    volumePanelView.setEnabled(false);
                    volumePanelView.setAlpha(0.0f);
                    volumePanelView.setVisibility(View.GONE);
                }
            }).start();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        handler.postDelayed(runnable1, LAYOUT_CHANGES_UPDATE_DELAY);
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
        if(circularSeekBarChanging)menuInflater.inflate(R.menu.main_media_track_menu, menu);
        else menuInflater.inflate(R.menu.main_media_track_menu_frame, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_volume:

                break;
            case R.id.menu_equalizer:
                Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                if(intent.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(intent, 0);
                }else Toast.makeText(this, getText(R.string.text_message_no_default_equalizer), Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_search:
                //TODO
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
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
        if(bound && playerService.getMediaAdapterInfo() != null)
            playerService.getMediaAdapterInfo().setSelectedIndex(new MediaAdapterInfo.Index(position));
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
                //setWithOrientation();
                break;
            case R.id.back_button_arrow_frame:
                changeBehaviorState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if((BottomSheetBehavior.STATE_EXPANDED == bottomSheetBehavior.getState() || BottomSheetBehavior.STATE_DRAGGING == bottomSheetBehavior.getState()) && keyCode == KeyEvent.KEYCODE_BACK){
            bottomSheetBehavior.setDraggable(true);
            changeBehaviorState(BottomSheetBehavior.STATE_COLLAPSED);
            return false;
        }if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE){
            if(!circularSeekBarChanging) {
                //changeVolume(keyCode);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() { super.onStart(); }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, PlayerService.class), connection, BIND_ADJUST_WITH_ACTIVITY);
        if(getIntent() != null && ACTION_LAUNCH_PLAY_BACK.equals(getIntent().getAction()))
            behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
        else behaviorDefaultLaunch = bottomSheetBehavior.getState();
        handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        handler.postDelayed(runnable1, LAYOUT_CHANGES_UPDATE_DELAY);
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
        handler.removeCallbacks(runnable1);
        updateLayoutFit = false;
        setWithOrientation();
    }

    @Override
    protected void onStop() { super.onStop(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        circleLineVisualizer.release();
        if(playerService != null && !playerService.isPlaying()) startService(new Intent(this, PlayerService.class).setAction(PlayerService.ACTION_MEDIA_SERVICE_EXIT));
        handler.removeCallbacks(runnable);
        handler.removeCallbacks(runnable1);
        updateLayoutFit = false;
    }

    @Override
    public void onProgressChange(CircularSeekBar circularSeekBar, float progress) {
        switch (circularSeekBar.getId()) {
            case R.id.seek_circular_bar_frame:
            case R.id.seek_circular_bar_frame_layout:
                CIRCULAR_PROGRESS = progress;
                frameCurrentTime.setText(timeFormatter.getCurrentTime(CIRCULAR_PROGRESS * playerService.getDuration() / 100.0f));
                break;
            case R.id.volume_panel_controller:
                CURRENT_VOLUME_PROGRESS = progress;
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(@NonNull CircularSeekBar circularSeekBar) {
        switch (circularSeekBar.getId()) {
            case R.id.seek_circular_bar_frame:
            case R.id.seek_circular_bar_frame_layout:
                circularSeekBarChanging = true;
                break;
        }
    }

    @Override
    public void onStopTrackingTouch(@NonNull CircularSeekBar circularSeekBar) {
        switch (circularSeekBar.getId()) {
            case R.id.seek_circular_bar_frame:
            case R.id.seek_circular_bar_frame_layout:
                circularSeekBarChanging = false;
                playerService.seekTo((long) (CIRCULAR_PROGRESS * playerService.getDuration() / 100.0f));
                break;
            case R.id.volume_panel_controller:
                //changeVolume(CURRENT_VOLUME_PROGRESS);
                break;
        }
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

    @SuppressWarnings("unused")
    private void changeVolume(float progress){
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) return;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int i = (int) (progress * maxVolume / 100.0f);
        volumePanelController.setText(String.valueOf(i));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, AudioManager.FLAG_PLAY_SOUND);
    }

    @SuppressWarnings("unused")
    private void changeVolume(int key) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        volumePanelView.setVisibility(View.VISIBLE);
        if (audioManager == null) return;
        int type = AudioManager.ADJUST_SAME;
        switch (key) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                type = AudioManager.ADJUST_LOWER;
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                type = AudioManager.ADJUST_RAISE;
                break;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                type = AudioManager.ADJUST_MUTE;
                break;
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, type, AudioManager.FLAG_PLAY_SOUND);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(!volumePanelView.isEnabled()) {
            volumePanelView.setEnabled(true);
            volumePanelView.setVisibility(View.VISIBLE);
            volumePanelView.animate().alpha(1.0f).start();
            Toast.makeText(this, String.valueOf(volumePanelView.isEnabled()), Toast.LENGTH_SHORT).show();
        }
        volumePanelController.setText(String.valueOf(currentVolume));
        volumePanelController.setProgress(currentVolume * 100.0f / maxVolume);
    }

    private void handleIntent(Intent intent){
        if(intent == null || intent.equals(oldIntent))return;
        oldIntent = intent;
        if(intent.getAction() != null){
            switch (intent.getAction()) {
                case ACTION_LAUNCH_PLAY_BACK:
                    behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
                    break;
                case ACTION_LAUNCH_PLAY_BACK_IF_PLAYING:
                    if(bound && playerService.isPlaying())
                        behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
            }
            changeBehaviorState(behaviorDefaultLaunch);
        }
    }

    private void changeBehaviorState(int state){
        organizeBottomSheet(state);
        bottomSheetBehavior.setState(state);
    }

    @SuppressWarnings("deprecation")
    private void prepareUi(int state){
        windowDisplayView = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        if(getIntent() != null && ACTION_LAUNCH_PLAY_BACK.equals(getIntent().getAction()))
            behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;

        setContentView(R.layout.main_activity_layout);

        mainActionBar = findViewById(R.id.main_action_bar);
        frameActionBar = findViewById(R.id.main_action_bar_frame);

        frameActionBar.setTitle("");

        ImageButton skipToNext = findViewById(R.id.skip_to_next);
        ImageButton skipToPrevious = findViewById(R.id.skip_to_previous);
        playPause = findViewById(R.id.play_pause);
        ImageButton skipToNextFrame = findViewById(R.id.skip_to_next_frame);
        ImageButton skipToPreviousFrame = findViewById(R.id.skip_to_previous_frame);
        playPauseFrame = findViewById(R.id.play_pause_frame);
        ImageButton openQuickList = findViewById(R.id.open_quick_list_frame);
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

        mediaArtCard = findViewById(R.id.media_info_frame_art_card);

        mediaController = findViewById(R.id.media_controller);
        includedLayout = findViewById(R.id.included_frame_layout);

        volumePanelView = findViewById(R.id.volume_panel);
        //volumePanelView.setEnabled(false);
        volumePanelController = findViewById(R.id.volume_panel_controller);

        //volumePanelController.setOnRoundedSeekChangeListener(this);

        if(previousState == BottomSheetBehavior.STATE_COLLAPSED) includedLayout.setAlpha(0.0f);

        if(circleLineVisualizer != null){
            circleLineVisualizer.release();
        }

        circleLineVisualizer = findViewById(R.id.visualizerView);

        if(circleLineVisualizer != null && playerService != null && isPermissionGranted(Manifest.permission.RECORD_AUDIO)){
            circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());
        }

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
                includedLayout.setAlpha(slideOffset);
                //mainActionBar.setAlpha(mediaController.getAlpha());
            }
        });

        defaultPeekHeight = bottomSheetBehavior.getPeekHeight();

        listView.setOnItemClickListener(this);

        firstViewChild = findViewById(R.id.firstViewChild);

        bottomSheet.setOnClickListener(this);
        playPause.setOnClickListener(this);
        skipToPrevious.setOnClickListener(this);
        skipToNext.setOnClickListener(this);
        playPauseFrame.setOnClickListener(this);
        skipToPreviousFrame.setOnClickListener(this);
        skipToNextFrame.setOnClickListener(this);
        openQuickList.setOnClickListener(this);
        changeLoop.setOnClickListener(this);
        findViewById(R.id.back_button_arrow_frame).setOnClickListener(this);

        circularSeekBar.setOnRoundedSeekChangeListener(this);

        setSupportActionBar(mainActionBar);

        changeBehaviorState(behaviorDefaultLaunch);

        if(state != 0) changeBehaviorState(state);

        if(timeFormatter != null) frameDuration.setText(timeFormatter.getTotalTime());

        if (bound && playerService != null && playerService.getMediaAdapterInfo() != null) listView.setAdapter(playerService.getMediaAdapterInfo());
    }

    private void prepareList(){
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            finish(getText(R.string.permission_non_granted_message));
            return;
        }
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
                handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
                mediaController.setVisibility(View.GONE);
                frameActionBar.setVisibility(View.VISIBLE);
                includedLayout.setVisibility(View.VISIBLE);
                mediaController.animate().alpha(0.0f).start();
                frameActionBar.animate().alpha(1.0f).start();
                mainActionBar.setVisibility(View.GONE);
                if(playerService != null && isPermissionGranted(Manifest.permission.RECORD_AUDIO)) circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());
                circularSeekBarChanging = false;
                setSupportActionBar(frameActionBar);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
                mediaController.setVisibility(View.VISIBLE);
                frameActionBar.setVisibility(View.GONE);
                includedLayout.setVisibility(View.GONE);
                mainActionBar.setVisibility(View.VISIBLE);
                mediaController.animate().alpha(1.0f).start();
                frameActionBar.animate().alpha(0.0f).start();
                circleLineVisualizer.release();
                circularSeekBarChanging = true;
                setSupportActionBar(mainActionBar);
                break;
            case BottomSheetBehavior.STATE_DRAGGING:
                if(circularSeekBarChanging) includedLayout.setAlpha(0.0f);
                mainActionBar.setVisibility(View.VISIBLE);
                mediaController.setVisibility(View.VISIBLE);
                frameActionBar.setVisibility(View.VISIBLE);
                includedLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    @SuppressWarnings("deprecation")
    public void setWithOrientation(){
        if(!updateLayoutFit)return;
        DisplayMetrics displayMetrics = new DisplayMetrics(), realDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        getWindowManager().getDefaultDisplay().getRealMetrics(realDisplayMetrics);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) firstViewChild.getLayoutParams();
        switch (windowDisplayView.getRotation()){
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if(realDisplayMetrics.heightPixels - getNavigationBarHeight() == displayMetrics.heightPixels) {
                    includedLayout.setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight());
                    bottomSheetBehavior.setPeekHeight(getNavigationBarHeight() + mediaController.getHeight());
                    mediaController.setPadding(0, 0, 0, 0);
                    params.topMargin = getStatusBarHeight();
                }else {
                    includedLayout.setPadding(0, 0, 0, 0);
                    bottomSheetBehavior.setPeekHeight(mediaController.getHeight());
                    mediaController.setPadding(0, 0, 0, 0);
                    params.topMargin = 0;
                }
                params.bottomMargin = bottomSheetBehavior.getPeekHeight();
                firstViewChild.setLayoutParams(params);
                break;
            case Surface.ROTATION_270:
                if(realDisplayMetrics.heightPixels == displayMetrics.heightPixels) {
                    includedLayout.setPadding(getNavigationBarWidth(), getStatusBarHeight(), 0, 0);
                    bottomSheetBehavior.setPeekHeight(defaultPeekHeight);
                    mediaController.setPadding(getNavigationBarWidth(), 0, 0, 0);
                    params.rightMargin = 0;
                    params.topMargin = getStatusBarHeight();
                    params.leftMargin = getNavigationBarWidth();
                }else {
                    includedLayout.setPadding(0, 0, 0, 0);
                    bottomSheetBehavior.setPeekHeight(defaultPeekHeight);
                    mediaController.setPadding(0, 0, 0, 0);
                    params.rightMargin = 0;
                    params.topMargin = 0;
                    params.leftMargin = 0;
                }
                params.bottomMargin = bottomSheetBehavior.getPeekHeight();
                firstViewChild.setLayoutParams(params);
                break;
            case Surface.ROTATION_90:
                if(realDisplayMetrics.heightPixels == displayMetrics.heightPixels) {
                    includedLayout.setPadding(0, getStatusBarHeight(), getNavigationBarWidth(), 0);
                    bottomSheetBehavior.setPeekHeight(defaultPeekHeight);
                    mediaController.setPadding(0, 0, getNavigationBarWidth(), 0);
                    params.topMargin = getStatusBarHeight();
                    params.rightMargin = getNavigationBarWidth();
                }else {
                    includedLayout.setPadding(0, 0, 0, 0);
                    bottomSheetBehavior.setPeekHeight(defaultPeekHeight);
                    mediaController.setPadding(0, 0, 0, 0);
                    params.topMargin = 0;
                    params.rightMargin = 0;
                }
                params.leftMargin = 0;
                params.bottomMargin = bottomSheetBehavior.getPeekHeight();
                firstViewChild.setLayoutParams(params);
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