package com.xingen.volleylib.request;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.xingen.volleylib.header.HttpResponseHeaderParser;
import com.xingen.volleylib.listener.BitmapResultListener;
import com.xingen.volleylib.utils.BitmapUtils;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.Response;
import com.xingen.volleylib.volley.toolbox.ImageRequest;

/**
 * Created by ${xinGen} on 2018/1/30.
 *
 *  圆形图片的请求
 */

public class CircleBitmapImageRequest extends ImageRequest {
    private static final android.graphics.Bitmap.Config bitmapConfig = Bitmap.Config.RGB_565;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private  final BitmapResultListener resultListener;
    public CircleBitmapImageRequest(String url, ImageView imageView, BitmapResultListener resultListener) {
        this(url, imageView.getWidth(), imageView.getHeight(), bitmapConfig, resultListener);
    }
    public CircleBitmapImageRequest(String url, int maxWidth, int maxHeight, BitmapResultListener resultListener) {
        this(url, maxWidth, maxHeight, bitmapConfig, resultListener);
    }
    public CircleBitmapImageRequest(String url, int maxWidth, int maxHeight, Bitmap.Config decodeConfig, BitmapResultListener resultListener) {
        super(url, resultListener, maxWidth, maxHeight, decodeConfig, resultListener);
        this.resultListener = resultListener;
        loadPreviewBitmap();
    }
    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        Response<Bitmap> bitmapResponse=  super.parseNetworkResponse(response);
        if (bitmapResponse.isSuccess()){
            //重新添加具备缓存期间和过期时间的header
            bitmapResponse =Response.success(bitmapResponse.result,HttpResponseHeaderParser.parseSpecifiedTimeCacheHeaders(response));
        }
        return bitmapResponse;
    }

    @Override
    protected void deliverResponse(Bitmap response) {
        // circle crop bitmap
        Bitmap circleBitmap = BitmapUtils.circleCrop(response);
        if (resultListener!=null){
            resultListener.onResponse(circleBitmap);
        }
    }
    private void loadPreviewBitmap() {
        if (resultListener != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    resultListener.loadPreviewBitmap();
                }
            });
        }
    }

}
