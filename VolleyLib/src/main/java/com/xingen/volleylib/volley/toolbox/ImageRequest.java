/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xingen.volleylib.volley.toolbox;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.xingen.volleylib.volley.DefaultRetryPolicy;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.ParseError;
import com.xingen.volleylib.volley.Request;
import com.xingen.volleylib.volley.Response;
import com.xingen.volleylib.volley.VolleyLog;


/**
 *
 * 用途：一个根据Url获取图片的请求，且回调一个bitmap
 */
public class ImageRequest extends Request<Bitmap> {
    /**
     * 执行图片请求的连接时间
     * */
    private static final int IMAGE_TIMEOUT_MS = 1000;

    /**
     *  默认图片请求的重试次数，这里2次
     * */
    private static final int IMAGE_MAX_RETRIES = 2;

    /**
     *  图像请求的默认退避倍数
     *  */
    private static final float IMAGE_BACKOFF_MULT = 2f;

    private final Response.Listener<Bitmap> mListener;
    private final Config mDecodeConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;

    /**
     *  一个同步锁，避免在同一时刻解码太多Bitmap，导致内存溢出
     * */
    private static final Object sDecodeLock = new Object();

    /**
     *
     * @param url URL of the image
     * @param listener Listener to receive the decoded bitmap
     * @param maxWidth Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight Maximum height to decode this bitmap to, or zero for none
     * @param decodeConfig Format to decode the bitmap to
     * @param errorListener Error listener, or null to ignore errors
     */
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
                        Config decodeConfig, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
        mListener = listener;
        mDecodeConfig = decodeConfig;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    /**
     * 压缩矩形的一边长度,计算合适的宽高比.
     *
     * @param maxPrimary 最大的主要尺寸
     * @param maxSecondary 最大的辅助尺寸
     * @param actualPrimary 实际主要尺寸
     * @param actualSecondary 实际辅助尺寸
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
            int actualSecondary) {
        // 当最大主要尺寸和最大的辅助尺寸同时为0，无法计算，直接返回实际主要尺寸。
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }
        // 当最大的主要尺寸为0，合适的尺寸=(最大的辅助尺寸/实际辅助尺寸）* 实际主要尺寸
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }
         //当最大的主要尺寸不为0，最大辅助尺寸为0时，合适的尺寸=最大的主要尺寸。
        if (maxSecondary == 0) {
            return maxPrimary;
        }
        /**
         *
         *  1. 先计算出一个比率=实际最大尺寸/实际辅助尺寸。
         *  2  合适尺寸=比率*最大的主要尺寸
         *  3.若是合适尺寸>最大的辅助尺寸，则合适尺寸=最大的辅助尺寸/比率。
         */
        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        // 多线程的同步锁： 某一个时刻，只有一个Bitmap在解析，避免内存溢出。
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    /**
     * 解析Byte数组生成Bitmap
     * @param response
     * @return
     */
    private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] data = response.data;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        /**
         *  若是没有指定宽度，同时也没指定高度，则直接加载原始图片的大小，生成Bitmap。
         */
        if (mMaxWidth == 0 && mMaxHeight == 0) {
            decodeOptions.inPreferredConfig = mDecodeConfig;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } else {
            /**
             *  调整图片的大小，先获取到图片的Bounds范围
             */
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            //获取到图片的真实长度。
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;
            //根据真实的宽高和指定宽高，计算处合适的宽高
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth);

            // 设置真正解码Bitmap.
            decodeOptions.inJustDecodeBounds = false;
            // 这里注释，要这个还是没关系，因为API 8不支持它？
            // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
            // 通过合适的宽高，计算出压缩比例。
            decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

            // 若是生成的Bitmap中宽高值任何一个超出指定的合适的宽高值，则进行裁剪。.
            if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }
        }
        //若是解析出来的Bitmap为空，则传递一个异常。
        if (bitmap == null) {
            return Response.error(new ParseError(response));
        } else {
            return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @Override
    protected void deliverResponse(Bitmap response) {
        mListener.onResponse(response);
    }

    /**
     *
     * 返回一个2的最大的冥除数，用于当做图片的压缩比例，
     * 以确保压缩出来的图片不会超出指定宽高值。
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    static int findBestSampleSize(int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        //比较两个值，返回最小的值
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }
        return (int) n;
    }

    @Override
    public String getCacheKey() {
        return super.getCacheKey();
    }
}
