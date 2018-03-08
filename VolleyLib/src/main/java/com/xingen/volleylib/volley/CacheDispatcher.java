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

import android.os.Process;

import java.util.concurrent.BlockingQueue;

/**
 * 一个缓存线程：
 *
 * 1. 先从缓存队列中获取到请求
 * 2. 从磁盘中获取数据，若是为空，将该请求加入网络队列中，执行网络操作。
 * 3. 反之，获取缓存数据，若是过期，则将该请求加入网络队列中，执行网络操作。
 * 4. 反之，获取的缓存数据没有过期，若是该数据还在缓存期间，则传递响应数据。
 * 4. 反之，该数据不在缓存期间，则先传递响应数据，且开启网络线程去刷新，二次传递新数据。
 */
public class CacheDispatcher extends Thread {

    private static final boolean DEBUG = VolleyLog.DEBUG;

    /** The queue of requests coming in for triage. */
    private final BlockingQueue<Request<?>> mCacheQueue;

    /** The queue of requests going out to the network. */
    private final BlockingQueue<Request<?>> mNetworkQueue;

    /** The cache to read from. */
    private final Cache mCache;

    /** For posting responses. */
    private final ResponseDelivery mDelivery;

    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

    /**
     *
     * @param cacheQueue Queue of incoming requests for triage
     * @param networkQueue Queue to post requests that require network to
     * @param cache Cache interface to use for resolution
     * @param delivery Delivery interface to use for posting responses
     *
     *  初始化，缓存请求的队列，网络请求的队列，磁盘缓存的操作类，在主线程中传递结果或者异常的操作类
     */
    public CacheDispatcher(
            BlockingQueue<Request<?>> cacheQueue, BlockingQueue<Request<?>> networkQueue,
            Cache cache, ResponseDelivery delivery) {
        mCacheQueue = cacheQueue;
        mNetworkQueue = networkQueue;
        mCache = cache;
        mDelivery = delivery;
    }
    /**
     *
     * 调度工作立刻停止，队列中的请求也不会被执行调度操作。
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }
    @Override
    public void run() {
        if (DEBUG) VolleyLog.v("start new dispatcher");
        //设置线程优先级，这里是后台线程
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        //进行一个阻塞调用，来初始化这缓存。实际上是进行 DiskBasedCache上读取到全部文件的详细信息，不包括文件内容。
        mCache.initialize();
       //while循环，从缓存请求的队列中取出请求，执行操作
        while (true) {
            try {
                 //从缓存队列中取出一个请求
                final Request<?> request = mCacheQueue.take();
                //标记请求已经从缓存队列中取出
                request.addMarker("cache-queue-take");
                //  //若是请求已经被取消，则不会执行该请求
                if (request.isCanceled()) {
                    request.finish("cache-discard-canceled");
                    continue;
                }
                //从磁盘中获取该请求需要的数据，若是没有则加入网络队列中，执行网络操作。
                Cache.Entry entry = mCache.get(request.getCacheKey());
                if (entry == null) {
                    request.addMarker("cache-miss");
                    //磁盘中无缓存数据，添加到网络请求中，执行网络获取数据
                    mNetworkQueue.put(request);
                    continue;
                }
                //若是这缓存数据已经过期，则进行网络获取新数据
                if (entry.isExpired()) {
                    request.addMarker("cache-hit-expired");
                    //确保缓存一致性，使用从缓存中读取到的entry.
                    request.setCacheEntry(entry);
                    mNetworkQueue.put(request);
                    continue;
                }
                request.addMarker("cache-hit");
                //解析从磁盘中读取到数据
                Response<?> response = request.parseNetworkResponse(new NetworkResponse(entry.data, entry.responseHeaders));
                request.addMarker("cache-hit-parsed");
                 //数据是否需要刷新
                if (!entry.refreshNeeded()) {
                    //数据还在缓存期间中，响应动作在ExecutorDelivery中，将结果回调到主线程中
                    mDelivery.postResponse(request, response);
                } else {
                    /**
                     * 缓存数据过了缓存时间，但没有过期，则先回调加载磁盘数据
                     * ，然后执行网络操作，刷新最新数据，二次回调响应。
                     */
                    request.addMarker("cache-hit-refresh-needed");
                    request.setCacheEntry(entry);
                    //标记响应是中间，还会继续刷新
                    response.intermediate = true;
                    //传递一个中间的响应回调到监听器中，同时也会传递一个请求去执行网络工作。
                    mDelivery.postResponse(request, response, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mNetworkQueue.put(request);
                            } catch (InterruptedException e) {
                            }
                        }
                    });
                }
            } catch (InterruptedException e) {
                if (mQuit) {
                    return;
                }
                continue;
            }
        }
    }
}
