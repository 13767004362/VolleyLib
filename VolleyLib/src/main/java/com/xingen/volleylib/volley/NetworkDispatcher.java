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

package com.xingen.volleylib.volley;

import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;

import java.util.concurrent.BlockingQueue;

/**
 * Provides a thread for performing network dispatch from a queue of requests.
 *
 * Requests added to the specified queue are processed from the network via a
 * specified {@link Network} interface. Responses are committed to cache, if
 * eligible, using a specified {@link Cache} interface. Valid responses and
 * errors are posted back to the caller via a {@link ResponseDelivery}.
 *
 *
 * 用途：
 *      1. 提供一个网络调度队列中请求的线程
 *      2.请求时添加到一个指定的队列中，完成请求是在Network接口中。
 *      3.使用一个Cache接口，来缓存响应结果
 *      4.有效的响应和异常是传递到在一个ResponseDelivery类中进行回调操作。
 */
public class NetworkDispatcher extends Thread {
    /** 需要执行网络操作的请求队列  */
    private final BlockingQueue<Request<?>> mQueue;
    /** 网络执行工具类 */
    private final Network mNetwork;
    /** 磁盘缓存，操作类 */
    private final Cache mCache;
    /** 响应结果和异常的回调传递 */
    private final ResponseDelivery mDelivery;
    /** 用于告诉，线程失败标识 */
    private volatile boolean mQuit = false;

    /**
     *
     * @param queue Queue of incoming requests for triage
     * @param network Network interface to use for performing requests
     * @param cache Cache interface to use for writing responses to cache
     * @param delivery Delivery interface to use for posting responses
     *
     * 初始化，存放着需执行网络工作的请求的队列，执行网络工作的类，磁盘缓存的操作类，主线程中传递异常和响应结果的类
     * 创建一个网络调度线程，必须调用start(),来开启执行任务
     */
    public NetworkDispatcher(BlockingQueue<Request<?>> queue,
            Network network, Cache cache,
            ResponseDelivery delivery) {
        mQueue = queue;
        mNetwork = network;
        mCache = cache;
        mDelivery = delivery;
    }
    /**
     * Forces this dispatcher to quit immediately.  If any requests are still in
     * the queue, they are not guaranteed to be processed.
     *
     * 调度工作立刻停止，队列中的请求也不会被执行调度操作。
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void addTrafficStatsTag(Request<?> request) {
        // Tag the request (if API >= 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
        }
    }
    @Override
    public void run() {
        //设置线程优先级，这里是后台线程
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Request<?> request;
        //while循环，从网络队列中获取要执行的请求。
        while (true) {
            try {
                // 从网络队列中获取一个请求
                request = mQueue.take();
            } catch (InterruptedException e) {
              //当队列中抛出一个异常，且程序需要关闭网络线程池，则停止该线程。
                if (mQuit) {
                    return;
                }
                continue;
            }
            try {
                //添加被执行的标记
                request.addMarker("network-queue-take");

                //若是请求已经被取消，则不执行网络请求
                if (request.isCanceled()) {
                    request.finish("network-discard-cancelled");
                    continue;
                }
                addTrafficStatsTag(request);
                //在NetWork子类类中执行网络请求的操作，返回网络响应数据
                NetworkResponse networkResponse = mNetwork.performRequest(request);
                //在请求中添加网络操作完成的标志
                request.addMarker("network-http-complete");
                /**
                 *  若是服务器返回304 和请求已经传递一个响应，则不会再二次传递一个相同的响应。
                 *  服务器返回304代表，url指向的资源文件中内容没有发生变化。
                 *
                 *  request.hasHadResponseDelivered()返回true，这表明，该请求在缓存线程中读取到了磁盘中缓存数据，但是数据需要被刷新。
                 */
                if (networkResponse.notModified && request.hasHadResponseDelivered()) {
                    request.finish("not-modified");
                    continue;
                }

                //在网络线程中指向解析响应的数据
                Response<?> response = request.parseNetworkResponse(networkResponse);
                //在请求中添加网络解析已经完成的标志
                request.addMarker("network-parse-complete");
                //若是需要缓存，则将解析后数据写入缓存中。
                //注意点：在304s情况下（即内容数据相同时），只会更新缓存的metadata, 不会更新内容数据。
                if (request.shouldCache() && response.cacheEntry != null) {
                    mCache.put(request.getCacheKey(), response.cacheEntry);
                    //在请求中添加已经被写入缓存的标记
                    request.addMarker("network-cache-written");
                }
                // Post the response back.
                request.markDelivered();
                //在ResponseDelivery类中回调请求和解析后响应数据
                mDelivery.postResponse(request, response);
            } catch (VolleyError volleyError) {
                parseAndDeliverNetworkError(request, volleyError);
            } catch (Exception e) {
                VolleyLog.e(e, "Unhandled exception %s", e.toString());
                mDelivery.postError(request, new VolleyError(e));
            }
        }
    }

    /**
     * 解析，传递网络异常。
     * @param request
     * @param error
     */
    private void parseAndDeliverNetworkError(Request<?> request, VolleyError error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }
}
