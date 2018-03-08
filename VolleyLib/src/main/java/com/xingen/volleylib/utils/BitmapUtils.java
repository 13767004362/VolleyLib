package com.xingen.volleylib.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by ${xinGen} on 2018/1/30.
 */

public class BitmapUtils {
    /**
     *  crop circle bitmap
     * @param source
     * @return
     */
   public static Bitmap circleCrop(Bitmap source) {
        if (source == null) return null;
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
        Bitmap result = Bitmap.createBitmap(source.getWidth(),source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        //画布中背景图片与绘制图片交集部分
        paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        return result;
    }

    /**
     *
     * @param resources
     * @param resourceImageId
     * @return
     */
    public static Bitmap decodeResource(Resources resources, int resourceImageId){
        return decodeResource(resources,resourceImageId,new BitmapFactory.Options());
    }
    /**
     * @param resources
     * @param resourceImageId
     * @param options
     * @return
     */
    public static Bitmap decodeResource(Resources resources, int resourceImageId, BitmapFactory.Options options){
        return BitmapFactory.decodeResource(resources,resourceImageId,options);
    }
}
