/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;

import com.xingen.volleylib.volley.Network;
import com.xingen.volleylib.volley.RequestQueue;

import java.io.File;

/**
 * 用途：
 * 初始化Volley中网络配置，异步线程配置，磁盘缓存配置
 */
public class Volley {

    /**
     * 磁盘中默认缓存的文件名
     */
    private static final String DEFAULT_CACHE_DIR = "volley";


    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, (Network) null);
    }
    /**
     * 创建一个默认的工作池对象，且调用RequestQueue的start()
     * 参数Context用于创建磁盘缓存的文件夹
     * 参数HttpStack用于网络工作，默认是null
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack) {
        return newRequestQueue(context, new BasicNetwork(stack));
    }
    /**
     * 可以定制HttpStack 和Network
     *
     * @param context
     * @param network
     * @return
     */
    public static RequestQueue newRequestQueue(Context context, Network network) {
        //在手机中创建一个缓存数据的文件夹
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        //创建一个执行网络工作的操作类
        if (network == null) {
            network = new BasicNetwork(new HurlStack());
        }
        //创建一个请求队列，添加磁盘缓存的操作类，执行网络工作的操作类
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        //开启。
        queue.start();
        return queue;
    }


}
