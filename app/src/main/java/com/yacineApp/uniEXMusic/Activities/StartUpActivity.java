package com.yacineApp.uniEXMusic.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.Services.PlayerService;
import com.yacineApp.uniEXMusic.UniEXActivity;

import java.util.Arrays;

public class StartUpActivity extends UniEXActivity implements View.OnClickListener {

    public static final int RUN_DELAY = 200;

    private final String[] requiredPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    private boolean permissionAlreadyGranted = false;
    private boolean notReadyToScroll = true;

    private Button acceptButton;
    private ImageView logoImage;
    private View firstView, secondView;
    private ScrollView permissionRequiredScroll;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(Arrays.equals(permissions, requiredPermissions) && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            runMainActivity();
            finish();
        }else finish(getResources().getText(R.string.permission_non_granted_message));
    }

    @Override
    public void onClick(View view){
        switch (view.getId()) {
            case R.id.image_logo:

                break;
            case R.id.ask_for_permission_button:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(requiredPermissions, 1);
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(PlayerService.SERVICE_ALREADY_CREATED){
            permissionAlreadyGranted = true;
            runMainActivity();
            return;
        }
        permissionAlreadyGranted = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.permission_required_activity);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            acceptButton = findViewById(R.id.ask_for_permission_button);
            logoImage = findViewById(R.id.image_logo);
            firstView = findViewById(R.id.required_permission_layout);
            secondView = findViewById(R.id.optimal_permission_layout);
            permissionRequiredScroll = findViewById(R.id.required_permission_scrollView);
            permissionRequiredScroll.setFillViewport(true);
            permissionRequiredScroll.setVerticalScrollBarEnabled(false);
            permissionRequiredScroll.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return notReadyToScroll;
                }
            });
            if(permissionAlreadyGranted) logoImage.setBackgroundResource(R.drawable.animated_start_logo);
            AnimatedVectorDrawable animationDrawable = (AnimatedVectorDrawable) logoImage.getBackground();
            animationDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    animationEnd2();
                }
            });
            animationDrawable.start();
            logoImage.setOnClickListener(this);
        }else runMainActivity();
    }

    private void animationEnd2(){
        if(permissionAlreadyGranted){
            runMainActivity();
            return;
        }
        final float y = logoImage.getY();
        logoImage.animate()
                .setDuration(1500)
                .y(getCurrentContentView().getPaddingTop())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animationEnd3(y);
                    }
                })
                .start();
    }

    private void animationEnd3(float y){
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) logoImage.getLayoutParams();
        params.removeRule(RelativeLayout.CENTER_IN_PARENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        logoImage.setY(y);
        logoImage.setLayoutParams(params);
        firstView.animate()
                .alpha(1.0f)
                .start();
        secondView.animate()
                .setStartDelay(200L)
                .alpha(1.0f)
                .start();
        acceptButton.animate()
                .setStartDelay(400L)
                .alpha(1.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animationEnd4();
                    }
                })
                .start();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) permissionRequiredScroll.getLayoutParams();
            params.removeRule(RelativeLayout.ABOVE);
            params.addRule(RelativeLayout.START_OF, R.id.ask_for_permission_button);
            permissionRequiredScroll.setLayoutParams(params);
        } else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig.orientation == Configuration.ORIENTATION_UNDEFINED){
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) permissionRequiredScroll.getLayoutParams();
            params.removeRule(RelativeLayout.START_OF);
            params.addRule(RelativeLayout.ABOVE, R.id.ask_for_permission_button);
            permissionRequiredScroll.setLayoutParams(params);
        }
    }

    private void animationEnd4(){
        acceptButton.setClickable(true);
        acceptButton.setOnClickListener(this);
        notReadyToScroll = false;
    }

    private void runMainActivity(){
        Runnable runnable = new Runnable(){
            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setAction(MainActivity.ACTION_LAUNCH_PLAY_BACK_IF_PLAYING));
                finish();
            }
        };
        if(permissionAlreadyGranted) handler.postDelayed(runnable, RUN_DELAY);
        else handler.post(runnable);
    }

}