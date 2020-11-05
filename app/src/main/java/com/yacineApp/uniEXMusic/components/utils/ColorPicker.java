package com.yacineApp.uniEXMusic.components.utils;

import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ColorPicker from image source.
 * @author yacine-app
 * @version 1.0
 * @see ColorInt
 */
public class ColorPicker {

    /**
     * ColorResult that gives the most and the least color that are used within bitmap image source.
     */
    public static class ColorResult {
        private int highColor, lowColor;
        private boolean isLight;
        private ColorResult(){}

        /**
         *
         * @param a is an colors hashMap that is returned from image pixels.
         */
        private ColorResult(@NonNull HashMap<Integer, Integer> a) {
            final Set<Map.Entry<Integer, Integer>> entries = a.entrySet();
            Map.Entry<Integer, Integer> maxEntry, minEntry;

            maxEntry = Collections.max(entries, new Comparator<Map.Entry<Integer, Integer>>() {
                @Override
                public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                    return (o1.getKey().compareTo(o2.getKey()) - o1.getValue().compareTo(o2.getValue())) / 2 + entries.size() / 5;
                }
            });

            minEntry = Collections.min(entries, new Comparator<Map.Entry<Integer, Integer>>() {
                @Override
                public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                    return (o1.getKey().compareTo(o2.getKey()) - o1.getValue().compareTo(o2.getValue())) / 2 + entries.size() / 5;
                }
            });
            highColor = maxEntry.getKey();
            lowColor = minEntry.getKey();
            isLight = ColorPicker.isLightColor(this.highColor);
            //Log.e(getClass().getName(), String.format("#%06X", highColor) + ", n: " + maxEntry.getValue() + ", index: " + pp++);
        }

        /**
         *
         * @return int the least color that is in image source from its colors array.
         */
        @SuppressWarnings("unused")
        @ColorInt
        public int getLowColor() { return lowColor; }
        /**
         *
         * @return int the most color that is in image source from its colors array.
         */
        @ColorInt
        public int getHighColor() { return highColor; }
        /**
         *
         * @return boolean get if the color is light or no.
         */
        public boolean isLightColor() { return isLight; }
    }

    public interface OnDoneListener {
        /**
         *
         * @param colorResult color result from ColorPicker.
         * @see ColorResult
         */
        void onDone(ColorResult colorResult);
    }

    /**
     * get if color given is light or no.
     * @param color color set as integer.
     * @return return true if color is light or false if no so.
     * @see ColorInt
     */
    public static boolean isLightColor(@ColorInt int color){
        return getLum(color) <= 169.5f;
    }

    public static float getLum(@ColorInt int color){
        float[] c = toColor(color);
        return (c[0] * 299.0f + c[1] * 587.0f + c[2] * 114.0f) / 1000.0f;
    }

    @NonNull
    public static float[] toColor(@ColorInt int color){
        return new float[]{(color & 0xFF0000) >>> 16, (color & 0xFF00  ) >>>  8, color & 0xFF};
    }

    /**
     * get the ColorResult from given high or low color that you choose.
     * @param highColor set the high color to create ColorResult.
     * @param lowColor set the low color to create ColorResult.
     * @return return ColorResult from given high or low color that you choose.
     * @see ColorResult
     * @see ColorInt
     */
    @NonNull
    public static ColorResult valueOf(@ColorInt int highColor, @ColorInt int lowColor){
        ColorResult colorResult = new ColorResult();
        colorResult.isLight = isLightColor(highColor);
        colorResult.highColor = highColor;
        colorResult.lowColor = lowColor;
        return colorResult;
    }

    /**
     * get the ColorResult from given bitmap source.
     * @param bitmap set the high color to create ColorResult.
     * @return return ColorResult from given bitmap source.
     * @see ColorResult
     * @see Bitmap
     */
    @NonNull
    public static ColorResult valueOf(@NonNull Bitmap bitmap){
        ColorPicker colorPicker = new ColorPicker(bitmap);
        colorPicker.runnable.run();
        return new ColorResult(colorPicker.colors);
    }

    private HashMap<Integer, Integer> colors = new HashMap<>();

    private OnDoneListener onDoneListener;
    private Thread thread;
    private Bitmap bitmap;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int i = 0, length = bitmap.getWidth() * bitmap.getHeight();
            for(;i < length;i++) {
                final int color = bitmap.getPixel(i % bitmap.getWidth(), i / bitmap.getHeight());
                float a = ((color & 0xFF000000) >>> 24) / 255.0f;
                if(a == 1.0f) {
                    Integer l = colors.get(color);
                    if(l == null) colors.put(color, 1);
                    else colors.put(color, l + 1);
                }
            }
            if(onDoneListener != null) onDoneListener.onDone(new ColorResult(colors));
            bitmap.recycle();
        }
    };

    /**
     * ColorPicker class that return ColorResult class from given bitmap source, and run it into an other thread after you call start() method.
     * @param image bitmap source to pick color from it.
     * @see ColorResult
     * @see Bitmap
     */
    public ColorPicker(@NonNull Bitmap image){
        bitmap = Bitmap.createScaledBitmap(image, 82, 82, true);
        this.thread = new Thread(runnable);
    }
    @SuppressWarnings("unused")
    public synchronized void start(){ thread.start(); }

    /**
     * @param onDoneListener set onDone listener that's called back when done picking color.
     * @see OnDoneListener
     * @see ColorResult
     */
    @SuppressWarnings("unused")
    public void setOnDoneListener(OnDoneListener onDoneListener) { this.onDoneListener = onDoneListener; }
}
