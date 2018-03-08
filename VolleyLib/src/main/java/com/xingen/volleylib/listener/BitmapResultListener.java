package com.xingen.volleylib.listener;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.MainThread;

import com.xingen.volleylib.VolleyClient;
import com.xingen.volleylib.cache.LruBitmapCache;
import com.xingen.volleylib.utils.BitmapUtils;
import com.xingen.volleylib.volley.Response;
import com.xingen.volleylib.volley.VolleyError;


/**
 * Created by ${xinGen} on 2018/1/30.
 */

public abstract class BitmapResultListener implements Response.Listener<Bitmap>, Response.ErrorListener {
    private int defaultImageId, errorImageId;
    /**
     * 添加一个锁，防止某一时刻，内存溢出。
     */
    private static final Object lock = new Object();
    private Resources resources;
    private LruBitmapCache lruBitmapCache;

    private BitmapResultListener() {
        lruBitmapCache = VolleyClient.getInstance().getLruBitmapCache();
    }

    public BitmapResultListener(Resources resources, int defaultImageId, int errorImageId) {
        this();
        this.resources = resources;
        this.defaultImageId = defaultImageId;
        this.errorImageId = errorImageId;
    }

    public BitmapResultListener(Context context, int defaultImageId, int errorImageId) {
        this();
        this.resources = context.getResources();
        this.defaultImageId = defaultImageId;
        this.errorImageId = errorImageId;

    }

    @Override
    public void onResponse(Bitmap response) {
        if (response != null) {
            success(response);
        } else {
            loadPreviewBitmap();
        }
    }

    public void loadPreviewBitmap() {
        synchronized (lock) {
            Bitmap defaultBitmap=null;
            if (resources != null && defaultImageId > 0) {
               defaultBitmap=loadBitmap(defaultImageId);
            }
            success(defaultBitmap);
        }
    }

    /**
     *  先从LruCache中获取，若是没有从原始数据源中获取。
     * @param imageId
     * @return
     */
    private Bitmap loadBitmap(int imageId) {
        String key = String.valueOf(imageId);
        Bitmap bitmap = lruBitmapCache.getBitmap(key);
        if (bitmap == null) {
            bitmap = BitmapUtils.decodeResource(resources, imageId);
            lruBitmapCache.putBitmap(key, bitmap);
        }
        return bitmap;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        synchronized (lock) {
            Bitmap errorBitmap = null;
            if (resources != null && errorImageId > 0) {
                errorBitmap = loadBitmap(errorImageId);
            }
            error(error, errorBitmap);
        }
    }

    @MainThread
    public abstract void success(Bitmap bitmap);

    @MainThread
    public abstract void error(VolleyError volleyError, Bitmap bitmap);
}
