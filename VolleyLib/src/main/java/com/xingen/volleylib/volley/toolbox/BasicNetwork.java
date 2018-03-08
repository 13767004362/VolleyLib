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

import android.os.SystemClock;
import android.util.Log;

import com.xingen.volleylib.request.DownloadRequest;
import com.xingen.volleylib.volley.AuthFailureError;
import com.xingen.volleylib.volley.Cache;
import com.xingen.volleylib.volley.Network;
import com.xingen.volleylib.volley.NetworkError;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.NoConnectionError;
import com.xingen.volleylib.volley.Request;
import com.xingen.volleylib.volley.RetryPolicy;
import com.xingen.volleylib.volley.ServerError;
import com.xingen.volleylib.volley.TimeoutError;
import com.xingen.volleylib.volley.VolleyError;
import com.xingen.volleylib.volley.VolleyLog;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.cookie.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A network performing Volley requests over an {@link HttpStack}.
 * <p>
 * 用途：
 * <p>
 * 在HttpStack子类之上执行网络请求的工具类
 * <p>
 * 1. 执行网络请求
 * 2. for循环方式执行重试策略
 * <p>
 * 这里有一个内存问题，服务器返回的数据都转成了byte数组，若是返回文件数据，会导致占用内存巨大，内存溢出。
 */
public class BasicNetwork implements Network {
    protected static final boolean DEBUG = VolleyLog.DEBUG;

    private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

    /**
     * 默认线程池中线程的数量
     */
    private static int DEFAULT_POOL_SIZE = 4096;


    protected final HttpStack mHttpStack;

    protected final ByteArrayPool mPool;

    /**
     * @param httpStack HTTP stack to be used
     */
    public BasicNetwork(HttpStack httpStack) {

        //若是不传入一个字节池，则构建一个小的默认池，这将带来很多好处，不会浪费太多的内存。
        this(httpStack, new ByteArrayPool(DEFAULT_POOL_SIZE));
    }

    /**
     * @param httpStack HTTP stack to be used
     * @param pool      a buffer pool that improves GC performance in copy operations
     */
    public BasicNetwork(HttpStack httpStack, ByteArrayPool pool) {
        mHttpStack = httpStack;
        mPool = pool;
    }

    /**
     * 执行网络请求，返回响应数据
     *
     * @param request Request to process
     * @return
     * @throws VolleyError 执行网络请求，for循环的方式，执行重试策略。
     *                     <p>
     *                     若是执行成功或者重试策略执行完，跳出for循环。
     */
    @Override
    public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        //引导后的毫秒数（包含睡眠花费的时间）
        long requestStart = SystemClock.elapsedRealtime();
        while (true) {
            HttpResponse httpResponse = null;
            byte[] responseContents = null;
            Map<String, String> responseHeaders = new HashMap<String, String>();
            try {
                // 创一个Map来，存储上一次相同缓存key所对应请求的标头
                Map<String, String> headers = new HashMap<String, String>();
                //因磁盘中缓存的数据，已经过期，需要重新执行网络数据，而添加磁盘中缓存数据的标头
                addCacheHeaders(headers, request.getCacheEntry());
                //在HurlStack 中，执行HttpURLConnection，返回响应数据
                httpResponse = mHttpStack.performRequest(request, headers);
                StatusLine statusLine = httpResponse.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                //获取到响应数据的标头
                responseHeaders = convertHeaders(httpResponse.getAllHeaders());
                //下载请求
                if (statusCode == 200 && request instanceof DownloadRequest) {
                    return writeFileStreamIfExist((DownloadRequest) request, responseHeaders, httpResponse);
                }
                //处理缓存验证，若是服务器返回304，返回磁盘中读取到的数据
                if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                    return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED, request.getCacheEntry() == null ? null : request.getCacheEntry().data
                            , responseHeaders, true);
                }
                //在服务器返回204的情况下，需检查内容是否为空
                if (httpResponse.getEntity() != null) {
                    /**
                     * 注意点： 将响应数据都转成byte数组，这个若是返回文件数据，可能会导致内存溢出。
                     */
                    responseContents = entityToBytes(httpResponse.getEntity());
                } else {
                    //当响应数据为空，则添加空字节
                    responseContents = new byte[0];
                }
                //若是请求缓慢，请记录
                long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
                logSlowRequests(requestLifetime, request, responseContents, statusLine);
                //若是服务器返回状态码在小于200或者待遇299时，抛出一个异常
                if (statusCode < 200 || statusCode > 299) {
                    throw new IOException();
                }
                return new NetworkResponse(statusCode, responseContents, responseHeaders, false);
            } catch (SocketTimeoutException e) {
                attemptRetryOnException("socket", request, new TimeoutError());
            } catch (ConnectTimeoutException e) {
                attemptRetryOnException("connection", request, new TimeoutError());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            } catch (IOException e) {
                int statusCode = 0;
                NetworkResponse networkResponse = null;
                if (httpResponse != null) {
                    statusCode = httpResponse.getStatusLine().getStatusCode();
                } else {
                    throw new NoConnectionError(e);
                }
                VolleyLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());
                if (responseContents != null) {
                    networkResponse = new NetworkResponse(statusCode, responseContents, responseHeaders, false);
                    if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
                        attemptRetryOnException("auth", request, new AuthFailureError(networkResponse));
                    } else {
                        // TODO: Only throw ServerError for 5xx status codes.
                        throw new ServerError(networkResponse);
                    }
                } else {
                    throw new NetworkError(networkResponse);
                }
            }
        }
    }

    /**
     * Logs requests that took over SLOW_REQUEST_THRESHOLD_MS to complete.
     * <p>
     * 记录请求缓慢的情况
     */
    private void logSlowRequests(long requestLifetime, Request<?> request,
                                 byte[] responseContents, StatusLine statusLine) {
        if (DEBUG || requestLifetime > SLOW_REQUEST_THRESHOLD_MS) {
            VolleyLog.d("HTTP response for request=<%s> [lifetime=%d], [size=%s], " +
                            "[rc=%d], [retryCount=%s]", request, requestLifetime,
                    responseContents != null ? responseContents.length : "null",
                    statusLine.getStatusCode(), request.getRetryPolicy().getCurrentRetryCount());
        }
    }

    /**
     * 执行，重试策略。
     * <p>
     * 若是请求中已经没有更多的重试策略，会引发超时异常。
     *
     * @param request The request to use.
     */
    private static void attemptRetryOnException(String logPrefix, Request<?> request, VolleyError exception) throws VolleyError {
        RetryPolicy retryPolicy = request.getRetryPolicy();
        int oldTimeout = request.getTimeoutMs();
        try {
            retryPolicy.retry(exception);
        } catch (VolleyError e) {
            request.addMarker(String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
            throw e;
        }
        request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
    }

    /**
     * 添加磁盘中缓存数据的一些标头，若是没有缓存，不需要任何操作。
     * <p>
     * 1. If-None-Match
     * 2. If-Modified-Since
     *
     * @param headers
     * @param entry
     */
    private void addCacheHeaders(Map<String, String> headers, Cache.Entry entry) {
        //
        if (entry == null) {
            return;
        }
        if (entry.etag != null) {//添加是否匹配的标头
            headers.put("If-None-Match", entry.etag);
        }
        if (entry.serverDate > 0) {//
            Date refTime = new Date(entry.serverDate);
            headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
        }
    }

    protected void logError(String what, String url, long start) {
        long now = SystemClock.elapsedRealtime();
        VolleyLog.v("HTTP ERROR(%s) %d ms to fetch %s", what, (now - start), url);
    }

    /**
     * 将HttpEntity中数据(stream)转成 一个byte[]中
     */
    private byte[] entityToBytes(HttpEntity entity) throws IOException, ServerError {
        PoolingByteArrayOutputStream bytes =
                new PoolingByteArrayOutputStream(mPool, (int) entity.getContentLength());
        byte[] buffer = null;
        try {
            InputStream in = entity.getContent();
            if (in == null) {
                throw new ServerError();
            }
            buffer = mPool.getBuf(1024);
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        } finally {
            try {
                // 关闭流资源
                entity.consumeContent();
            } catch (IOException e) {
                // This can happen if there was an exception above that left the entity in
                // an invalid state.
                VolleyLog.v("Error occured when calling consumingContent");
            }
            mPool.returnBuf(buffer);
            bytes.close();
        }
    }

    /**
     * 将响应数据的标头，存储到一个Map中
     */
    private static Map<String, String> convertHeaders(Header[] headers) {
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < headers.length; i++) {
            result.put(headers[i].getName(), headers[i].getValue());
        }
        return result;
    }

    private static NetworkResponse writeFileStreamIfExist(DownloadRequest request, Map<String, String> responseHeaders, HttpResponse httpResponse) throws IOException {
        Log.i("DownloadRequest" ,"下载");
        File file=new File(request.getFilePath());
        if (file!=null&&file.exists()){
            file.delete();
        }
        InputStream inputStream=httpResponse.getEntity().getContent();
        FileOutputStream outputStream=new FileOutputStream(file);
        long fileLength = httpResponse.getEntity().getContentLength();
        byte[] buffer = new byte[4096];
        int count;
        long total = 0;
        while ((count = inputStream.read(buffer)) != -1) {
            if (request.isCanceled()){
                outputStream.close();
                inputStream.close();
                file.deleteOnExit();
                throw  new IOException("DownloadRequest cancel 被取消");
            }
            outputStream.write(buffer, 0, count);
            total += count;
            if (fileLength > 0) {
                int progress = (int) ((float) total * 100 / fileLength);
                request.deliverProgress(progress);
            }
        }
        outputStream.flush();
        inputStream.close();
        outputStream.close();
        return new NetworkResponse(httpResponse.getStatusLine().getStatusCode(), new byte[0], responseHeaders, false);
    }
}
