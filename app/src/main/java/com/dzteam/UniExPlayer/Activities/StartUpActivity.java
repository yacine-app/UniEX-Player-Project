package com.dzteam.UniExPlayer.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dzteam.UniExPlayer.R;
import com.dzteam.UniExPlayer.UniEXActivity;

import java.util.Arrays;

public class StartUpActivity extends UniEXActivity implements View.OnClickListener {

    private final String[] storagePermission = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    private void runMainActivity(){
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(Arrays.equals(permissions, storagePermission) && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            runMainActivity();
            finish();
        }else finish(getResources().getText(R.string.permission_non_granted_message));
    }

    @Override
    public void onClick(View view){
        requestPermissions(storagePermission, 1);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)){
            runMainActivity();
            return;
        }
        setTheme(R.style.AppTheme);
        setContentView(R.layout.permission_required_activity);
        findViewById(R.id.ask_for_permission_button).setOnClickListener(this);
    }

}