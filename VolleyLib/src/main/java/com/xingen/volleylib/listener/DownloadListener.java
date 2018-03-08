package com.xingen.volleylib.listener;

import com.xingen.volleylib.volley.Response;
import com.xingen.volleylib.volley.VolleyError;

/**
 * Created by ${xinGen} on 2018/3/7.
 */

public abstract class DownloadListener implements Response.ErrorListener {

    /**
     * 下载完成的方法
     *
     * @param downloadUrl
     * @param filePath
     */
 public    abstract void downloadFinish(String downloadUrl, String filePath);

    /**
     * 下载失败的方法
     * @param downloadUrl
     * @param volleyError
     */
   public  abstract void downloadError(String downloadUrl, VolleyError volleyError);

    @Override
    public void onErrorResponse(VolleyError error) {

    }
}
