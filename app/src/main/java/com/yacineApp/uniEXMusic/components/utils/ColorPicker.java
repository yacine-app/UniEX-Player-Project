package com.yacineApp.uniEXMusic.components.utils;

import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Comparator;

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
         * @param a is an colors array that is returned from image pixels.
         */
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
                }catch (ArrayIndexOutOfBoundsException ignored){ }
                l[v] = new int[]{a[i++], b};
            }
            l = Arrays.copyOf(l, v);
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
        float r = ((color >> 24) & 0xFF);
        float g = ((color >> 16) & 0xFF);
        float b = ((color >>  8) & 0xFF);
        float l = (r * 299.0f + g * 587.0f + b * 114.0f) / 1000.0f;
        return l <= 169.5f;
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
                if(a == 1.0f) colors[i] = color;
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
        this.colors = new int[bitmap.getWidth() * bitmap.getHeight()];
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
