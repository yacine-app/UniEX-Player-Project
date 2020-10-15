package com.dzteam.UniExPlayer.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dzteam.UniExPlayer.R;
import com.dzteam.UniExPlayer.UniEXActivity;

import java.util.Arrays;

public class StartUpActivity extends UniEXActivity implements View.OnClickListener {

    public static final int RUN_DELAY = 200;

    private final String[] requiredPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    private boolean permissionGranted = false;

    private Button acceptButton;
    private ImageView logoImage;
    private View firstView, secondView;
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
                requestPermissions(requiredPermissions, 1);
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionGranted = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.permission_required_activity);
        acceptButton = findViewById(R.id.ask_for_permission_button);
        logoImage = findViewById(R.id.image_logo);
        firstView = findViewById(R.id.required_permission_layout);
        secondView = findViewById(R.id.optimal_permission_layout);
        if(permissionGranted) logoImage.setBackgroundResource(R.drawable.animated_start_logo);
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

    }

    private void animationEnd2(){
        if(permissionGranted){
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

    private void animationEnd4(){
        acceptButton.setClickable(true);
        acceptButton.setOnClickListener(this);
    }

    private void runMainActivity(){
        Runnable runnable = new Runnable(){
            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        };
        if(permissionGranted) handler.postDelayed(runnable, RUN_DELAY);
        else handler.post(runnable);
    }

}