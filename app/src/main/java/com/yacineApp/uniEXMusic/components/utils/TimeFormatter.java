package com.yacineApp.uniEXMusic.components.utils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {
    private int mTotalTime;
    private SimpleDateFormat simpleDateFormat;

    public TimeFormatter(int ms){
        mTotalTime = ms;
        this.simpleDateFormat = new SimpleDateFormat((ms>=3600000?"HH:":"") + "mm:ss", Locale.US);
    }

    public int getTotalTimeInt(){
        return mTotalTime;
    }

    @NonNull
    public String getCurrentTime(int currentTime) { return this.simpleDateFormat.format(new Date(currentTime)); }

    @NonNull
    public String getCurrentTime(float currentTime){ return getCurrentTime((int) currentTime); }

    @SuppressWarnings("unused")
    @NonNull
    public String getCurrentTime(long currentTime){ return getCurrentTime((int) currentTime); }

    @SuppressWarnings("unused")
    @NonNull
    public String getCurrentTime(short currentTime){ return getCurrentTime((int) currentTime); }

    @NonNull
    public String getTotalTime() { return this.simpleDateFormat.format(new Date(mTotalTime)); }
}