package com.yacineApp.uniEXMusic.activities;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gauravk.audiovisualizer.visualizer.CircleLineVisualizer;
import com.github.stefanodp91.android.circularseekbar.CircularSeekBar;
import com.github.stefanodp91.android.circularseekbar.OnCircularSeekBarChangeListener;
import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.UniEXActivity;
import com.yacineApp.uniEXMusic.components.MediaInfo;
import com.yacineApp.uniEXMusic.components.PlayerCore;
import com.yacineApp.uniEXMusic.components.utils.ColorPicker;
import com.yacineApp.uniEXMusic.components.utils.TimeFormatter;
import com.yacineApp.uniEXMusic.services.PlayerService;

public class ScreenLockPlayerActivity extends UniEXActivity.UniEXMusicActivity implements View.OnClickListener, OnCircularSeekBarChangeListener {

    private boolean circularSeekBarChanging = false;
    private int CIRCULAR_SEEK_BAR_UPDATE_DELAY = 300;
    private float CIRCULAR_PROGRESS = 0.0f;
    private Handler handler = new Handler(Looper.getMainLooper());

    private CircleLineVisualizer circleLineVisualizer;
    private Display defaultDisplay;
    private TimeFormatter timeFormatter;
    private CircularSeekBar circularSeekBar;
    private RelativeLayout mediaControllerFrame;
    private CardView mediaInfoFrameArtCard;
    private ImageView frameArt;
    private TextView frameTitle, frameArtist, frameCurrentTime, frameDuration;
    private ImageButton playPauseFrame, changeLoop, skipToNextFrame, skipToPreviousFrame, openQuickList;
    private KeyguardManager keyguardManager;
    private UniEXMusicActivity activity;

    private PlayerService playerService;
    private ConstraintLayout playerFrameLayout;
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
    private MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            updateUi(playerService.getMediaInfo());
            handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
        }

        @Override
        public void onPause() {
            super.onPause();
            updateUi(playerService.getMediaInfo());
            handler.removeCallbacks(runnable);
        }
    };
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
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.SBinder sBinder = (PlayerService.SBinder) service;
            playerService = sBinder.getService();
            playerService.setCallBack(callback);
            playerService.setOnPreparedListener(onPreparedListener);
            playerService.setOnLoopChangedListener(onLoopChangedListener);
            onPreparedListener.onPrepared();
            circleLineVisualizer.setAudioSessionId(playerService.getAudioSessionId());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(playerService != null) {
                circleLineVisualizer.release();
                playerService.removeCallBack(callback);
                playerService.removeOnPreparedListener(onPreparedListener);
                playerService.removeOnLoopChangedListener(onLoopChangedListener);
            }
        }
    };

    @SuppressWarnings("deprecation")
    private void updateUi(@Nullable MediaInfo metaData){
        if(metaData == null)return;
        Bitmap art = metaData.getArt();
        CharSequence title = metaData.getTitle();
        CharSequence artist = metaData.getArtist();
        frameArt.setImageBitmap(art);
        frameTitle.setText(title);
        frameArtist.setText(artist);
        frameTitle.setSelected(true);
        int l = metaData.getColorResult().getHighColor();
        int[] r = new int[]{Color.WHITE, l};
        circleLineVisualizer.setColor(l);
        if(ColorPicker.isLightColor(l)){
            if(defaultDisplay.getRotation() != Surface.ROTATION_270) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
        playerFrameLayout.setBackground(gradientDrawable);
        onLoopChangedListener.onChanged(playerService.getLoopState());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        onConfigurationChanged(getResources().getConfiguration());
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()){
            case R.id.play_pause_frame:
                playerService.playPause();
                break;
            case R.id.skip_to_next_frame:
                sendMediaEvent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
                break;
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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, PlayerService.class), connection, BIND_ADJUST_WITH_ACTIVITY);
        handler.postDelayed(runnable, CIRCULAR_SEEK_BAR_UPDATE_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unbindService(connection);
            circleLineVisualizer.release();
        }catch (IllegalArgumentException e){
            Log.e(this.getClass().getName(), "Error: ", e);
        }
        handler.removeCallbacks(runnable);
    }

    float rx = 0, ry = 0, x = 0, y = 0;
    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("deprecation")
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        defaultDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        setContentView(R.layout.player_frame_layout);
        playerFrameLayout = findViewById(R.id.playerFrameLayout);

        mediaInfoFrameArtCard = findViewById(R.id.media_info_frame_art_card);

        mediaControllerFrame = findViewById(R.id.media_controller_frame);

        skipToNextFrame = findViewById(R.id.skip_to_next_frame);
        skipToPreviousFrame = findViewById(R.id.skip_to_previous_frame);
        playPauseFrame = findViewById(R.id.play_pause_frame);
        openQuickList = findViewById(R.id.open_quick_list_frame);
        changeLoop = findViewById(R.id.change_loop_mode_frame);

        circularSeekBar = findViewById(R.id.seek_circular_bar_frame);

        frameArt = findViewById(R.id.media_info_frame_art);

        frameTitle = findViewById(R.id.media_info_title_frame);
        frameArtist = findViewById(R.id.media_info_artist_frame);
        frameCurrentTime = findViewById(R.id.current_time_frame);
        frameDuration = findViewById(R.id.duration_time_frame);

        if(circleLineVisualizer != null){
            circleLineVisualizer.release();
        }

        circleLineVisualizer = findViewById(R.id.visualizerView);

        circularSeekBar.setIndicatorEnabled(false);

        playPauseFrame.setOnClickListener(this);
        skipToPreviousFrame.setOnClickListener(this);
        skipToNextFrame.setOnClickListener(this);
        openQuickList.setOnClickListener(this);
        changeLoop.setOnClickListener(this);

        circularSeekBar.setOnRoundedSeekChangeListener(this);

        if(timeFormatter != null) frameDuration.setText(timeFormatter.getTotalTime());

        ViewCompat.setOnApplyWindowInsetsListener(playerFrameLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                WindowInsetsCompat windowInsets = insets.replaceSystemWindowInsets(getCorrectSystemRect());
                if(defaultDisplay.getRotation() == Surface.ROTATION_270 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                v.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
                if(playerService != null) updateUi(playerService.getMediaInfo());
                return windowInsets;
            }
        });

        playerFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            private float r = 0.0f;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        rx = event.getX();
                        ry = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        x = event.getX();
                        y = event.getY();
                        r = (float) Math.sqrt(Math.pow(rx - x, 2) + Math.pow(ry - y, 2));
                        setViewsAlpha(1.0f - r / 360.0f, false);
                        break;
                    case MotionEvent.ACTION_UP:
                        if(r > 360.0f) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) keyguardManager.requestDismissKeyguard(activity, null);
                            finish();
                        }else setViewsAlpha(1.0f, true);
                        break;
                }
                return true;
            }
        });

    }

    private void setViewsAlpha(float alpha, boolean animated){
        if(animated){
            mediaInfoFrameArtCard.animate().alpha(alpha).start();
            circleLineVisualizer.animate().alpha(alpha).start();
            frameArtist.animate().alpha(alpha).start();
            frameTitle.animate().alpha(alpha).start();
            mediaControllerFrame.animate().alpha(alpha).start();
            return;
        }
        mediaInfoFrameArtCard.setAlpha(alpha);
        circleLineVisualizer.setAlpha(alpha);
        frameArtist.setAlpha(alpha);
        frameTitle.setAlpha(alpha);
        mediaControllerFrame.setAlpha(alpha);
    }

    /*@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent == null)return;
        if(CLOSE_LOOK_SCREEN_ACTIVITY.equals(intent.getAction())) finish();
    }*/

    @Override
    public void onProgressChange(CircularSeekBar circularSeekBar, float progress) {
        switch (circularSeekBar.getId()) {
            case R.id.seek_circular_bar_frame:
            case R.id.seek_circular_bar_frame_layout:
                CIRCULAR_PROGRESS = progress;
                frameCurrentTime.setText(timeFormatter.getCurrentTime(CIRCULAR_PROGRESS * playerService.getDuration() / 100.0f));
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
}
