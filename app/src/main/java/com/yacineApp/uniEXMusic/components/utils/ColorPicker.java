package com.yacineApp.uniEXMusic.components.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Comparator;

/**
 * ColorPicker from image source
 */
public class ColorPicker {

    public static class ColorResult {
        private int highColor, lowColor;
        private boolean isLight = false;
        private ColorResult(@NonNull int[] a){
            //TODO
            Arrays.sort(a);
            int b, i, v;
            int[][] l = new int[a.length][2];
            for(i = 0, v = 0;i < a.length;v++){
                b = 1;
                try {
                    while (a[i] == a[i + 1]) {
                        i++;
                        b++;
                        if (i + 1 >= a.length) break;
                    }
                }catch (ArrayIndexOutOfBoundsException e){ break; }
                l[v] = new int[]{a[i++], b};
            }
            Arrays.sort(l, new Comparator<int[]>() {
                @Override
                public int compare(int[] o1, int[] o2) {
                    return o2[1] - o1[1];
                }
            });
            lowColor  = l[10 * v / 100][0];
            highColor = l[80 * v / 100][0];
            isLight = ColorPicker.isLightColor(this.highColor);
        }
        public int getLowColor() { return lowColor; }
        public int getHighColor() { return highColor; }
        public boolean isLightColor() { return isLight; }
    }

    public interface OnDoneListener {
        void onDone(ColorResult colorResult);
    }

    public static boolean isLightColor(int color){
        float r = ((color >> 24) & 0xFF);
        float g = ((color >> 16) & 0xFF);
        float b = ((color >>  8) & 0xFF);
        float l = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        return l <= 168.0f;
    }

    @NonNull
    public static ColorResult valueOf(@NonNull Bitmap bitmap){
        ColorPicker colorPicker = new ColorPicker(bitmap);
        colorPicker.runnable.run();
        return new ColorResult(colorPicker.colors);
    }

    private int[] colors;

    private OnDoneListener onDoneListener;
    private Thread thread;
    private Bitmap bitmap;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int i = 0;
            for(;i < colors.length;i++) {
                final int color = bitmap.getPixel(i % bitmap.getWidth(), i / bitmap.getHeight());
                float a = ((color >> 24) & 0xFF) / 255.0f;
                if(a >= 0.9f) colors[i] = color;
            }
            if(onDoneListener != null) onDoneListener.onDone(new ColorResult(colors));
            bitmap.recycle();
        }
    };

    public ColorPicker(@NonNull Bitmap image){
        bitmap = Bitmap.createScaledBitmap(image, 68, 68, true);
        this.colors = new int[bitmap.getWidth() * bitmap.getHeight()];
        this.thread = new Thread(runnable);
    }
    public synchronized void start(){ thread.start(); }
    public void setOnDoneListener(OnDoneListener onDoneListener) { this.onDoneListener = onDoneListener; }
}
