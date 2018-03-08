package com.xingen.volleylib;

import android.content.Context;
import android.widget.ImageView;

import com.xingen.volleylib.cache.LruBitmapCache;
import com.xingen.volleylib.listener.BitmapResultListener;
import com.xingen.volleylib.listener.DownloadListener;
import com.xingen.volleylib.listener.FileProgressListener;
import com.xingen.volleylib.listener.GsonResultListener;
import com.xingen.volleylib.request.CircleBitmapImageRequest;
import com.xingen.volleylib.request.DownloadRequest;
import com.xingen.volleylib.request.FormRequest;
import com.xingen.volleylib.request.GsonRequest;
import com.xingen.volleylib.request.SingleFileRequest;
import com.xingen.volleylib.volley.Request;
import com.xingen.volleylib.volley.RequestQueue;
import com.xingen.volleylib.volley.toolbox.ImageLoader;
import com.xingen.volleylib.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Created by ${xinGen} on 2018/3/3.
 */

public class VolleyClient {
    private static VolleyClient instance;
    private RequestQueue mRequestQueue;
    private ImageLoader imageLoader;
    private LruBitmapCache lruBitmapCache;
    private VolleyClient() {}
    static {
        instance = new VolleyClient();
    }
    public static VolleyClient getInstance() {
        return instance;
    }
    public synchronized void init(Context context) {
        if (mRequestQueue == null) {
            this.mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
            this.lruBitmapCache = new LruBitmapCache(LruBitmapCache.getCacheSize(context));
            this.imageLoader = new ImageLoader(this.getRequestQueue(), this.lruBitmapCache);
        }
    }

    public CircleBitmapImageRequest loadCircleNetImage(String url, int maxWidth, int maxHeight, BitmapResultListener bitmapResultListener) {
        CircleBitmapImageRequest request = new CircleBitmapImageRequest(url, maxWidth, maxHeight, bitmapResultListener);
        this.mRequestQueue.add(request);
        return request;
    }

    /**
     * 发送Form表单请求
     *
     * @param url
     * @param body
     * @param resultListener
     * @param <T>
     * @return
     */
    public <T> FormRequest<T> sendFormRequest(String tag, String url, Map<String, String> body, GsonResultListener<T> resultListener) {
        return sendFormRequest(tag, url, body, null, resultListener);
    }

    public <T> FormRequest<T> sendFormRequest(String tag, String url, Map<String, String> body, Map<String, String> headers, GsonResultListener<T> resultListener) {
        FormRequest<T> request = new FormRequest<T>(url, body, resultListener);
        if (headers != null) {
            Set<Map.Entry<String, String>> set = headers.entrySet();
            for (Map.Entry<String, String> entry : set) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        request.setTag(tag);
        this.mRequestQueue.add(request);
        return request;
    }

    public <T> GsonRequest<T> sendGsonRequest(String tag, String url, Object body, GsonResultListener<T> resultListener) {
        return sendGsonRequest(tag, url, body, null, resultListener);
    }

    /**
     * 发送json请求
     *
     * @param tag
     * @param url
     * @param body
     * @param headers
     * @param resultListener
     * @param <T>
     * @return
     */
    public <T> GsonRequest<T> sendGsonRequest(String tag, String url, Object body, Map<String, String> headers, GsonResultListener<T> resultListener) {
        GsonRequest<T> request = new GsonRequest<T>(url, body, resultListener);
        if (headers != null) {
            Set<Map.Entry<String, String>> set = headers.entrySet();
            for (Map.Entry<String, String> entry : set) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        request.setTag(tag);
        this.mRequestQueue.add(request);
        return request;
    }

    public <T> GsonRequest<T> sendGsonRequest(String tag, String url, JSONObject jsonObject, GsonResultListener<T> resultListener) {
        return sendGsonRequest(tag, url, jsonObject, null, resultListener);
    }

    /**
     * 发送json请求
     *
     * @param tag
     * @param url
     * @param jsonObject
     * @param headers
     * @param resultListener
     * @param <T>
     * @return
     */
    public <T> GsonRequest<T> sendGsonRequest(String tag, String url, JSONObject jsonObject, Map<String, String> headers, GsonResultListener<T> resultListener) {
        GsonRequest<T> request = new GsonRequest<T>(url, jsonObject, resultListener);
        if (headers != null) {
            Set<Map.Entry<String, String>> set = headers.entrySet();
            for (Map.Entry<String, String> entry : set) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        request.setTag(tag);
        this.mRequestQueue.add(request);
        return request;
    }

    public <T> SingleFileRequest sendSingleFileRequest(String tag, String url, String filePath, FileProgressListener fileProgressListener, GsonResultListener<T> resultListener) {
        return sendSingleFileRequest(tag,url,SingleFileRequest.DEFAULT_NAME,filePath,fileProgressListener,resultListener);
    }
    public <T> SingleFileRequest sendSingleFileRequest(String tag, String url, File file, FileProgressListener fileProgressListener, GsonResultListener<T> resultListener) {
        return sendSingleFileRequest(tag,url,SingleFileRequest.DEFAULT_NAME,file,fileProgressListener,resultListener);
    }

    /**
     * 上传文件的请求
     * @param tag
     * @param url
     * @param name
     * @param file
     * @param fileProgressListener
     * @param resultListener
     * @param <T>
     * @return
     */
    public <T> SingleFileRequest sendSingleFileRequest(String tag, String url, String name, File file, FileProgressListener fileProgressListener, GsonResultListener<T> resultListener) {
        SingleFileRequest<T> request = new SingleFileRequest<T>(url, name, file, fileProgressListener, resultListener);
        request.setTag(tag);
        this.mRequestQueue.add(request);
        return request;
    }

    /**
     * 上传文件的请求
     * @param tag
     * @param url
     * @param name
     * @param filePath
     * @param fileProgressListener
     * @param resultListener
     * @param <T>
     * @return
     */
    public <T> SingleFileRequest sendSingleFileRequest(String tag, String url, String name, String filePath, FileProgressListener fileProgressListener, GsonResultListener<T> resultListener) {
        SingleFileRequest<T> request = new SingleFileRequest<T>(url, name, filePath, fileProgressListener, resultListener);
        request.setTag(tag);
        this.mRequestQueue.add(request);
        return request;
    }

    /**
     * 开启下载请求
     * @param tag
     * @param url
     * @param filePath
     * @param fileProgressListener
     * @param downloadListener
     * @return
     */
    public DownloadRequest startDownload(String tag, String url, String filePath, FileProgressListener fileProgressListener, DownloadListener downloadListener){
        DownloadRequest request=new DownloadRequest(url,filePath,fileProgressListener,downloadListener);
        request.setTag(tag);
        this.mRequestQueue.add(request);
        return request;
    }

    /**
     * 加载网络图片
     *
     * @param url
     * @param defaultImageResId
     * @param errorImageResId
     * @param imageView
     */
    public void loadNetImage(String url, final int defaultImageResId, final int errorImageResId, ImageView imageView) {
        this.imageLoader.get(url, ImageLoader.getImageListener(imageView, defaultImageResId, errorImageResId));
    }

    /**
     * 加载网络圆形图片
     *
     * @param url
     * @param imageView
     * @param bitmapResultListener
     * @return
     */
    public CircleBitmapImageRequest loadCircleNetImage(String url, ImageView imageView, BitmapResultListener bitmapResultListener) {
        CircleBitmapImageRequest request = new CircleBitmapImageRequest(url, imageView, bitmapResultListener);
        this.mRequestQueue.add(request);
        return request;
    }

    private RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public LruBitmapCache getLruBitmapCache() {
        return lruBitmapCache;
    }

    /*
      * 取消TAG标记的request
      */
    public void cancelRequest(String TAG) {
        RequestQueue queue = getRequestQueue();
        if (queue != null) {
            queue.cancelAll(TAG);
        }
    }
}
