package com.xingen.volleylib.request;

import android.os.Handler;
import android.os.Looper;

import com.xingen.volleylib.listener.DownloadListener;
import com.xingen.volleylib.listener.FileProgressListener;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.Request;
import com.xingen.volleylib.volley.Response;
import com.xingen.volleylib.volley.VolleyError;

/**
 * Created by ${xinGen} on 2018/3/7.
 * <p>
 * 文件下载
 */

public class DownloadRequest extends Request<Object> {
    /**
     * 主线程的Handler
     */
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DownloadListener downloadListener;
    private final FileProgressListener progressListener;
    private final String downloadUrl, filePath;
    public DownloadRequest(String url, String filePath, FileProgressListener progressListener, DownloadListener downloadListener) {
        super(Method.GET, url, downloadListener);
        this.downloadUrl = url;
        this.filePath = filePath;
        this.downloadListener = downloadListener;
        this.progressListener = progressListener;
        this.setShouldCache(false);
    }
    @Override
    protected Response<Object> parseNetworkResponse(NetworkResponse response) {
        return Response.success(null,null);
    }
    @Override
    protected void deliverResponse(Object response) {
       if (downloadListener!=null){
           downloadListener.downloadFinish(this.downloadUrl,this.filePath);
       }
    }
    @Override
    public void deliverError(VolleyError error) {
         if (downloadListener!=null){
             downloadListener.downloadError(this.downloadUrl,error);
         }
    }
    /**
     * 回调传递进度
     *
     * @param progress
     */
    public void deliverProgress(final int progress) {
        if (isCanceled()) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressListener != null) {
                    progressListener.progress(progress);
                }
            }
        });
    }

    public String getFilePath() {
        return filePath;
    }
}
