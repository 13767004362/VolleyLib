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

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A request dispatch queue with a thread pool of dispatchers.
 *
 * Calling {@link #add(Request)} will enqueue the given Request for dispatch,
 * resolving from either cache or network on a worker thread, and then delivering
 * a parsed response on the main thread.
 *
 *  具有线程池调度者的请求调度队列。
 *  调用add()将会调入给定的请求去执行调度，解决网络工作或者缓存是在工作线程中，并且将会将一个解析后的响应传递在主线程中。
 *
 *
 *
 *  添加一个Request:
 *  1. 先加入正在执行的队列中，若是该请求不需要缓存，直接加入网络队列中。
 *  2. 反之，需要缓存响应数据到磁盘中，则先判断等待队列中是否存在相同key的请求。
 *  3. 若是已经有等待队列中已经有相同key的请求，则加入等待队列中。
 *  4. 反之，则加入缓存队列中，读取磁盘数据，若是数据过期或者数据为空，则加入网络队列中。
 *
 */
public class RequestQueue {

    /**
     * 用户生成请求的递增序列号
     *  */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    /**
     *
     * 将具备相同的缓存key的请求存放到一个队列里，然后按key-队列的形式，将队列存放到Map集合中。
     * 注意点：若是具备相同的缓存key的请求只有一个，该请求是放到缓存队列中，被缓存线程执行。而这里Map存放一个null队列。
     */
    private final Map<String, Queue<Request<?>>> mWaitingRequests = new HashMap<>();

    /**
     *
     * 这个集合中全部请求是当前被请求队列处理的。
     */
    private final Set<Request<?>> mCurrentRequests = new HashSet<Request<?>>();

    /**   等待状态的请求 的队列 */
    private final PriorityBlockingQueue<Request<?>> mCacheQueue = new PriorityBlockingQueue<Request<?>>();

    /** 网络请求的队列 */
    private final PriorityBlockingQueue<Request<?>> mNetworkQueue = new PriorityBlockingQueue<Request<?>>();

    /**
     * 默认4个网络线程
     * */
    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;

    /** Cache interface for retrieving and storing responses. 用于存储从服务器响应的解析后的数据  */
    private final Cache mCache;

    /** Network interface for performing requests. 这个接口用于执行请求 */
    private final Network mNetwork;

    /**
     * 传递响应数据和异常到主线程的类
     * */
    private final ResponseDelivery mDelivery;

    /**
     * 网络线程池
     * */
    private NetworkDispatcher[] mDispatchers;

    /** 执行缓存请求的线程 */
    private CacheDispatcher mCacheDispatcher;

    /**
     * 初始化，磁盘缓存的操作类。执行请求的操作类，4个网络线程的数组，主线程中传递异常和响应结果的类
     *
     * @param cache A Cache to use for persisting responses to disk
     * @param network A Network interface for performing HTTP requests
     * @param threadPoolSize Number of network dispatcher threads to create
     * @param delivery A ResponseDelivery interface for posting responses and errors
     */
    public RequestQueue(Cache cache, Network network, int threadPoolSize,
            ResponseDelivery delivery) {
        mCache = cache;
        mNetwork = network;
        mDispatchers = new NetworkDispatcher[threadPoolSize];
        mDelivery = delivery;
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param cache A Cache to use for persisting responses to disk
     * @param network A Network interface for performing HTTP requests
     * @param threadPoolSize Number of network dispatcher threads to create
     *
     * 添加具备磁盘缓存的类，执行请求的类，网络线程的个数，在主线程的传递结果或者异常的类
     */
    public RequestQueue(Cache cache, Network network, int threadPoolSize) {
        this(cache, network, threadPoolSize,
                new ExecutorDelivery(new Handler(Looper.getMainLooper())));
    }


    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param cache A Cache to use for persisting responses to disk
     * @param network A Network interface for performing HTTP requests
     */
    public RequestQueue(Cache cache, Network network) {
        this(cache, network, DEFAULT_NETWORK_THREAD_POOL_SIZE);
    }

    /**
     * 开启线程
     */
    public void start() {
        //停止先前的创建线程，重新开启
        stop();
        //创建一个缓存调度线程，且开启线程
        mCacheDispatcher = new CacheDispatcher(mCacheQueue, mNetworkQueue, mCache, mDelivery);
        mCacheDispatcher.start();
        //开启4个网络线程，执行网络操作
        for (int i = 0; i < mDispatchers.length; i++) {
            NetworkDispatcher networkDispatcher = new NetworkDispatcher(mNetworkQueue, mNetwork, mCache, mDelivery);
            mDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }
    }
    /**
     * 停止缓存线程和网络线程池
     */
    public void stop() {
        if (mCacheDispatcher != null) {
            //缓存线程停止
            mCacheDispatcher.quit();
        }
        // 停止网络线程
        for (int i = 0; i < mDispatchers.length; i++) {
            if (mDispatchers[i] != null) {
                mDispatchers[i].quit();
            }
        }
    }

    /**
     * 获取到一个递增的序列号
     */
    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    /**
     * 获取到磁盘缓存的操作类对象。
     */
    public Cache getCache() {
        return mCache;
    }

    /**
     *
     * 一个用于过滤请求的接口。
     * 可以调用RequestQueue的cancelAll(RequestFilter)来取消指定的请求。
     */
    public interface RequestFilter {
        public boolean apply(Request<?> request);
    }

    /**
     *
     *  从当前执行的请求队列中，找与之匹配规则相同的请求，再取消该请求。
     */
    public void cancelAll(RequestFilter filter) {
        synchronized (mCurrentRequests) {
            for (Request<?> request : mCurrentRequests) {
                if (filter.apply(request)) {//若是符合筛选规则，该请求要被取消。
                    request.cancel();
                }
            }
        }
    }

    /**
     *
     * 根据给定的tag取消队列中全部请求。tag必须不能为了，且等于它本身。
     */
    public void cancelAll(final Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request.getTag() == tag;
            }
        });
    }

    /**
     * @param request The request to service
     * @return The passed-in request
     *
     * 添加一个请求到调度的队列中
     */
    public <T> Request<T> add(Request<T> request) {
        //标记这个请求属于当前队列
        request.setRequestQueue(this);
        //添加这个请求到，存放当前正在执行的请求的set集合中。
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }
        //给请求添加序列号
        request.setSequence(getSequenceNumber());
        //标记请求已经添加到队列中
        request.addMarker("add-to-queue");
        //若是请求不需要被缓存，将会跳过缓存请求的队列，直接在网络线程中执行该请求。
        if (!request.shouldCache()) {
            mNetworkQueue.add(request);
            return request;
        }
        /**
         * 等待需要执行的（相同key的）请求：
         *  1. 第一个请求加入缓存请求队列中，且在等待请求队列中标注
         *  2. 第二个请求，直接加入等待请求队列中，其余也是类似。
         */
        synchronized (mWaitingRequests) {
            String cacheKey = request.getCacheKey();
            //存在一个相同key的请求在等待中，则加入同一个等待组中。
            if (mWaitingRequests.containsKey(cacheKey)) {
                Queue<Request<?>> stagedRequests = mWaitingRequests.get(cacheKey);
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<Request<?>>();
                }
                //加入同一个等待执行的请求队列中
                stagedRequests.add(request);
                mWaitingRequests.put(cacheKey, stagedRequests);
                if (VolleyLog.DEBUG) {
                    VolleyLog.v("Request for cacheKey=%s is in flight, putting on hold.", cacheKey);
                }
            } else {
                //在储存（存放相同key的请求）队列的Map中，插入一个缓存key和null队列，这用于指示这个缓存可以对应的请求是在执行。
                mWaitingRequests.put(cacheKey, null);
                //添加请求到缓存队列中
                mCacheQueue.add(request);
            }
            return request;
        }
    }

    /**

     *
     *  调用Request的finish()后，指示这给定的请求已经被完成。
     *
     *  1. 从正在进行的请求队列中移除。
     *  2. 若是等待队列中还有相同key的请求，则将相同key的请求都加入缓存队列中，通过缓存线程，从磁盘中读取数据。
     *
     */
    void finish(Request<?> request) {
        //从当前执行的请求队列中移除该请求
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
        //需要缓存的request，才会有相同key的请求在等待队列中。
        if (request.shouldCache()) {
            synchronized (mWaitingRequests) {
                String cacheKey = request.getCacheKey();
                //在储存（存放相同key的请求）队列的Map中，获取指定key所对应的请求队列
                Queue<Request<?>> waitingRequests = mWaitingRequests.remove(cacheKey);
                if (waitingRequests != null) {//相同key的请求在等待执行状态。
                    if (VolleyLog.DEBUG) {
                        VolleyLog.v("Releasing %d waiting requests for cacheKey=%s.",
                                waitingRequests.size(), cacheKey);
                    }
                    //将等待队列中的请求，添加到缓存队列中，这些请求将执行在缓存线程中，读取磁盘中缓存数据。
                    mCacheQueue.addAll(waitingRequests);
                }
            }
        }
    }
}
