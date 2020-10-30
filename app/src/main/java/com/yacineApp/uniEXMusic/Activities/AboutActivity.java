package com.yacineApp.uniEXMusic.Activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.yacineApp.uniEXMusic.R;
import com.yacineApp.uniEXMusic.UniEXActivity;

public class AboutActivity extends UniEXActivity implements View.OnClickListener {

    public static final String GITHUB_ACCOUNT_LINK = "https://github.com/yacine-app/";
    public static final String TWITTER_ACCOUNT_LINK = "https://twitter.com/YacineApp/";
    public static final String WEBSITE_LINK = "https://yacine-app.github.io/UniEX-Player-Project/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity_layout);
        Toolbar toolbar = findViewById(R.id.main_action_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        findViewById(R.id.github_button).setOnClickListener(this);
        findViewById(R.id.twitter_button).setOnClickListener(this);
        findViewById(R.id.website_button).setOnClickListener(this);
        TextView textView = findViewById(R.id.version_text);
        try {
            textView.setText(textView.getText().toString().replace("%s", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        //TODO
    }

    @Override
    public void onClick(View v) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            switch (v.getId()) {
                case R.id.github_button:
                    intent.setData(Uri.parse(GITHUB_ACCOUNT_LINK));
                    break;
                case R.id.twitter_button:
                    intent.setData(Uri.parse(TWITTER_ACCOUNT_LINK));
                    break;
                case R.id.website_button:
                    intent.setData(Uri.parse(WEBSITE_LINK));
                    break;
            }
            startActivity(intent);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this, getText(R.string.about_activity_text_no_default_web_browser), Toast.LENGTH_SHORT).show();
        }
    }
}
