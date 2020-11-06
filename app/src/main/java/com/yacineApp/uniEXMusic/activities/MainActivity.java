package com.yacineApp.uniEXMusic.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yacineApp.uniEXMusic.components.LoadInternalMedia;
import com.yacineApp.uniEXMusic.components.MediaAdapterInfo;
import com.yacineApp.uniEXMusic.components.MediaInfo;
import com.yacineApp.uniEXMusic.components.PlayerCore;
import com.yacineApp.uniEXMusic.components.RecycleOnItemClickListener;
import com.yacineApp.uniEXMusic.components.utils.ColorPicker;
import com.yacineApp.uniEXMusic.components.utils.TimeFormatter;
import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.services.PlayerService;
import com.yacineApp.uniEXMusic.UniEXActivity;

import com.gauravk.audiovisualizer.visualizer.CircleLineVisualizer;

import com.github.stefanodp91.android.circularseekbar.CircularSeekBar;
import com.github.stefanodp91.android.circularseekbar.OnCircularSeekBarChangeListener;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

public class MainActivity extends UniEXActivity.UniEXMusicActivity implements View.OnClickListener, LoadInternalMedia.OnDoneListener, OnCircularSeekBarChangeListener, View.OnLongClickListener {

    public static final String ACTION_LAUNCH_PLAY_BACK = "ACTION_LAUNCH_PLAY_BACK";
    public static final String ACTION_LAUNCH_PLAY_BACK_IF_PLAYING = "ACTION_LAUNCH_PLAY_BACK_IF_PLAYING";

    private boolean circularSeekBarChanging = false, bound = false;
    private int CIRCULAR_SEEK_BAR_UPDATE_DELAY = 300;
    private float CIRCULAR_PROGRESS = 0.0f;
    private float CURRENT_VOLUME_PROGRESS = 0.0f;
    private int behaviorDefaultLaunch = BottomSheetBehavior.STATE_COLLAPSED;
    private int previousState = 0;
    private int defaultPeekHeight = 55;
    private int listViewScrollState = 0;

    private PlayerService playerService = null;
    private AppCompatActivity activity;
    private Intent oldIntent;
    private Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView recyclerView;
    private Display windowDisplayView;
    private Toolbar mainActionBar, frameActionBar;
    private CircleLineVisualizer circleLineVisualizer;
    private CardView mediaArtCard;
    private MediaAdapterInfo mediaAdapterInfo;
    private Bundle savedState;
    private View includedLayout;
    private View mediaController;
    private View firstViewChild;
    private BottomSheetBehavior bottomSheetBehavior;
    private TimeFormatter timeFormatter;
    private FrameLayout volumePanelView;
    private CircularSeekBar circularSeekBar, volumePanelController;
    private ImageView mediaArt, frameArt;
    private TextView mediaTitle, mediaArtist, frameTitle, frameArtist, frameCurrentTime, frameDuration;
    private ImageButton playPause, playPauseFrame, changeLoop, skipToNextFrame, skipToPreviousFrame, openQuickList;
    private PlayerCore.OnPreparedListener onPreparedListener = new PlayerCore.OnPreparedListener() {
        @Override
        public void onPrepared() {
            timeFormatter = new TimeFormatter(playerService.getDuration());
            frameCurrentTime.setText(timeFormatter.getCurrentTime(0));
            frameDuration.setText(timeFormatter.getTotalTime());
            circularSeekBar.setProgress(0.0f);
            updateUi(playerService.getMediaInfo());
            playerService.setCurrentPlayIndex();
        }
    };
    private PlayerCore.OnLoopChangedListener onLoopChangedListener = new PlayerCore.OnLoopChangedListener() {
        @Override
        public void onChanged(int loopState) {
            MediaInfo mediaInfo = playerService.getMediaInfo();
            assert mediaInfo != null;
            int all_repeat, all_once, once;
            if(mediaInfo.getColorResult().isLightColor()){
                all_once = R.drawable.ic_play_all_once_icon_holo_dark;
                all_repeat = R.drawable.ic_repeat_all_icon_holo_dark;
                once = R.drawable.ic_repeat_one_icon_holo_dark;
            }else {
                all_once = R.drawable.ic_play_all_once_icon;
                all_repeat = R.drawable.ic_repeat_all_icon;
                once = R.drawable.ic_repeat_one_icon;
            }
            switch (loopState){
                case PlayerCore.LOOP_STATE_ALL:
                    changeLoop.setImageResource(all_once);
                    break;
                case PlayerCore.LOOP_STATE_ALL_REPEAT:
                    changeLoop.setImageResource(all_repeat);
                    break;
                case PlayerCore.LOOP_STATE_ONE_REPEAT:
                    changeLoop.setImageResource(once);
                    break;
            }
        }
    };
    private MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {

        @Override
        public void onPause() {
            super.onPause();
            updateUi(playerService.getMediaInfo());
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
            updateUi(playerService.getMediaInfo());
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
                updateUi(playerService.getMediaInfo());
                onPreparedListener.onPrepared();
            }
            recyclerView.setAdapter(playerService.getMediaAdapterInfo());
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
                //circleLineVisualizer.setRotation(pos / 110.0f);
            }
            if(playerService != null && playerService.isPlaying()) handler.postDelayed(this, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        }
    };

    @SuppressWarnings("unused")
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
        savedState = savedInstanceState;
        activity = this;
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
                mediaAdapterInfo = new MediaAdapterInfo(activity);
                if(playerService != null){
                    mediaAdapterInfo.setSelectedIndex(new MediaAdapterInfo.Index(playerService.getCurrentPlayIndex()));
                    playerService.setMediaQueue(mediaAdapterInfo);
                }
                recyclerView.setAdapter(mediaAdapterInfo);
            }
        });
    }

    /*@Override
    public void onItemClick(@NonNull View view, int position) {
        if(bound && playerService.getMediaAdapterInfo() != null)
            playerService.getMediaAdapterInfo().setSelectedIndex(new MediaAdapterInfo.Index(position));
        startService(
                new Intent(this, PlayerService.class)
                        .setAction(PlayerService.ACTION_MEDIA_SERVICE_TO_ITEM)
                        .putExtra(PlayerService.EXTRA_MEDIA_SERVICE_TO_ITEM, position));
    }*/

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
        }/*if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE){
            if(!circularSeekBarChanging) {
                //changeVolume(keyCode);
                return true;
            }
        }*/
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() { super.onStart(); }

    @Override
    protected void onResume() {
        super.onResume();
        if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && playerService != null) circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());
        bindService(new Intent(this, PlayerService.class), connection, BIND_ADJUST_WITH_ACTIVITY);
//        if(getIntent() != null && ACTION_LAUNCH_PLAY_BACK.equals(getIntent().getAction()))
//            behaviorDefaultLaunch = BottomSheetBehavior.STATE_EXPANDED;
//        else behaviorDefaultLaunch = bottomSheetBehavior.getState();
        handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        if(circleLineVisualizer != null && bound) circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());
        //handler.postDelayed(runnable1, LAYOUT_CHANGES_UPDATE_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && playerService != null) circleLineVisualizer.release();
            try {
            unbindService(connection);
        }catch (IllegalArgumentException e){
            Log.e(this.getClass().getName(), "Error: ", e);
        }
        bound = false;
        handler.removeCallbacks(runnable);
        circleLineVisualizer.release();
    }

    @Override
    protected void onStop() { super.onStop(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        circleLineVisualizer.release();
        if(playerService != null && !playerService.isPlaying()) startService(new Intent(this, PlayerService.class).setAction(PlayerService.ACTION_MEDIA_SERVICE_EXIT));
        handler.removeCallbacks(runnable);
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
        if(intent == null || oldIntent != null)return;
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
            oldIntent = null;
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

        mediaArtCard = findViewById(R.id.media_info_frame_art_card);

        mediaController = findViewById(R.id.media_controller);
        includedLayout = findViewById(R.id.included_frame_layout);

        volumePanelView = findViewById(R.id.volume_panel);
        //volumePanelView.setEnabled(false);
        volumePanelController = findViewById(R.id.volume_panel_controller);

        //volumePanelController.setOnRoundedSeekChangeListener(this);

        if(previousState == BottomSheetBehavior.STATE_COLLAPSED) includedLayout.setAlpha(0.0f);

        if(circleLineVisualizer != null) circleLineVisualizer.release();

        circleLineVisualizer = findViewById(R.id.visualizerView);

        if(circleLineVisualizer != null && playerService != null && isPermissionGranted(Manifest.permission.RECORD_AUDIO))
            circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());

        recyclerView = findViewById(R.id.list);

        recyclerView.scrollTo(0, listViewScrollState);

        listViewScrollState = recyclerView.getScrollY();

        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                listViewScrollState = scrollY;
            }
        });

        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        recyclerView.addOnItemTouchListener(new RecycleOnItemClickListener(recyclerView) {
            @Override
            public void onItemClick(@NonNull View view, int position) {
                if (bound && playerService.getMediaAdapterInfo() != null)
                    playerService.getMediaAdapterInfo().setSelectedIndex(new MediaAdapterInfo.Index(position));
                startService(
                        new Intent(getApplicationContext(), PlayerService.class)
                                .setAction(PlayerService.ACTION_MEDIA_SERVICE_TO_ITEM)
                                .putExtra(PlayerService.EXTRA_MEDIA_SERVICE_TO_ITEM, position));
            }

            @Override
            public void onLongItemClick(@NonNull View view, int position) {

            }
        });

        circularSeekBar.setIndicatorEnabled(false);
        FrameLayout bottomSheet = findViewById(R.id.frameLayout);

        bottomSheet.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

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

        //listView.setOnItemClickListener(this);

        firstViewChild = findViewById(R.id.firstViewChild);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainView), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                WindowInsetsCompat windowInsets = insets.replaceSystemWindowInsets(getCorrectSystemRect());
                mediaController.setPadding(windowInsets.getSystemWindowInsetLeft(), 0, windowInsets.getSystemWindowInsetRight(), 0);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) firstViewChild.getLayoutParams();
                params.bottomMargin = windowInsets.getSystemWindowInsetBottom() + bottomSheetBehavior.getPeekHeight();
                params.topMargin = windowInsets.getSystemWindowInsetTop();
                params.rightMargin = windowInsets.getSystemWindowInsetRight();
                params.leftMargin = windowInsets.getSystemWindowInsetLeft();
                firstViewChild.setLayoutParams(params);
                organizeBottomSheet(bottomSheetBehavior.getState());
                return windowInsets;
            }
        });

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

        //if (bound && playerService != null && playerService.getMediaAdapterInfo() != null) listView.setAdapter(playerService.getMediaAdapterInfo());
        //mediaAdapterInfo = new MediaAdapterInfo(getApplicationContext());
        //listView.setAdapter(mediaAdapterInfo);
    }

    private void prepareList(){
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            finish(getText(R.string.permission_non_granted_message));
            return;
        }
        /*mediaAdapterInfo = new MediaAdapterInfo(listView);
        if(playerService != null){
            mediaAdapterInfo.setSelectedIndex(new MediaAdapterInfo.Index(playerService.getCurrentPlayIndex()));
            playerService.setMediaQueue(mediaAdapterInfo);
        }
        listView.setAdapter(mediaAdapterInfo);*/
        LoadInternalMedia loadInternalMedia = new LoadInternalMedia(this);
        loadInternalMedia.setStart(0);
        loadInternalMedia.setLength(1);
        loadInternalMedia.setOnDoneListener(this);
        loadInternalMedia.execute();
    }

    @SuppressWarnings("deprecation")
    private void updateUi(@Nullable MediaInfo metaData){
        if(metaData == null)return;
        if(playerService.isPlaying()){
            playPause.setImageResource(R.drawable.ic_pause_action_icon);
        } else{
            playPause.setImageResource(R.drawable.ic_play_action_icon);
        }
        Bitmap art = metaData.getArt();
        CharSequence title = metaData.getTitle();
        CharSequence artist = metaData.getArtist();
        mediaArt.setImageBitmap(metaData.getSmallArt());
        frameArt.setImageBitmap(art);
        mediaTitle.setText(title);
        frameTitle.setText(title);
        mediaArtist.setText(artist);
        frameArtist.setText(artist);
        frameTitle.setSelected(true);
        mediaTitle.setSelected(true);
        int l = metaData.getColorResult().getHighColor();
        int[] r = new int[]{Color.WHITE, l};
        circleLineVisualizer.setColor(l);
        if(ColorPicker.isLightColor(l)){
            if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            if(playerService.isPlaying()) playPauseFrame.setImageResource(R.drawable.ic_pause_action_icon_holo_dark);
            else playPauseFrame.setImageResource(R.drawable.ic_play_action_icon_holo_dark);
            skipToNextFrame.setImageResource(R.drawable.ic_skip_to_next_action_icon_holo_dark);
            skipToPreviousFrame.setImageResource(R.drawable.ic_skip_to_previous_action_icon_holo_dark);
            openQuickList.setImageResource(R.drawable.ic_media_list_icon_holo_dark);
            frameTitle.setTextColor(Color.WHITE);
            frameArtist.setTextColor(Color.LTGRAY);
            frameCurrentTime.setTextColor(Color.LTGRAY);
            frameDuration.setTextColor(Color.LTGRAY);
            circularSeekBar.setColorList(new int[]{Color.LTGRAY, Color.LTGRAY});
            //Toast.makeText(getApplicationContext(), "Light", Toast.LENGTH_SHORT).show();
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            if(playerService.isPlaying()) playPauseFrame.setImageResource(R.drawable.ic_pause_action_icon);
            else playPauseFrame.setImageResource(R.drawable.ic_play_action_icon);
            skipToNextFrame.setImageResource(R.drawable.ic_skip_to_next_action_icon);
            skipToPreviousFrame.setImageResource(R.drawable.ic_skip_to_previous_action_icon);
            openQuickList.setImageResource(R.drawable.ic_media_list_icon);
            frameTitle.setTextColor(Color.BLACK);
            frameArtist.setTextColor(Color.DKGRAY);
            frameCurrentTime.setTextColor(Color.DKGRAY);
            frameDuration.setTextColor(Color.DKGRAY);
            circularSeekBar.setColorList(new int[]{Color.DKGRAY, Color.DKGRAY});
        }
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                r);
        gradientDrawable.setGradientRadius(0.0f);
        includedLayout.setBackground(gradientDrawable);
        onLoopChangedListener.onChanged(playerService.getLoopState());
    }

    @SuppressWarnings("deprecation")
    private void organizeBottomSheet(int state){
        if(playerService == null)return;
        MediaInfo mediaInfo = playerService.getMediaInfo();
        switch (state){
            case BottomSheetBehavior.STATE_EXPANDED:
                handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
                mediaController.setVisibility(View.GONE);
                frameActionBar.setVisibility(View.VISIBLE);
                includedLayout.setVisibility(View.VISIBLE);
                mediaController.animate().alpha(0.0f).start();
                frameActionBar.animate().alpha(1.0f).start();
                mainActionBar.setVisibility(View.GONE);
                if(mediaInfo != null){
                    if(mediaInfo.getColorResult().isLightColor() && windowDisplayView.getRotation() != Surface.ROTATION_270 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                    else {
                        getWindow().getDecorView().setSystemUiVisibility(0);
                        getWindow().getDecorView().requestFocus();
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                }
                if(playerService != null && isPermissionGranted(Manifest.permission.RECORD_AUDIO)) circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());
                circularSeekBarChanging = false;
                //  setSupportActionBar(frameActionBar);
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                setSupportActionBar(mainActionBar);
                break;
            case BottomSheetBehavior.STATE_DRAGGING:
                if(circularSeekBarChanging) includedLayout.setAlpha(0.0f);
                mainActionBar.setVisibility(View.VISIBLE);
                mediaController.setVisibility(View.VISIBLE);
                frameActionBar.setVisibility(View.VISIBLE);
                includedLayout.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig.orientation == Configuration.ORIENTATION_UNDEFINED){
            prepareUi(previousState);
            if(bound){
                updateUi(playerService.getMediaInfo());
                playerService.updateMediaAdapterInfo();
                recyclerView.setAdapter(playerService.getMediaAdapterInfo());
            }
        //}
    }

}