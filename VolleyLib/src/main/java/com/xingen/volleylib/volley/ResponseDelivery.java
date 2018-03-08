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

/**
 * 用途：
 *    1.传递服务器响应的数据
 *    2.这里提供传递请求数据，响应数据的方法
 *    3.这里提供传递请求数据，异常的方法
 */

public interface ResponseDelivery {
    /**
     *
     * 传递一个请求和及其对应的响应数据
     *
     *  适合： 在网络获取到数据或者还在缓存期间内的数据
     * @param request
     * @param response
     */
    void postResponse(Request<?> request, Response<?> response);

    /**
     *  传递一个请求和及其对应的响应数据
     *  且执行一个Runnable，执行刷新网络数据的操作。
     *
     *  适合： 过了缓存期间的数据，但没有过期，应该执行网络操作刷新
     * @param request
     * @param response
     * @param runnable
     */
    public void postResponse(Request<?> request, Response<?> response, Runnable runnable);

    /**
     * 传递 一个异常
     *
     * @param request
     * @param error
     */
    public void postError(Request<?> request, VolleyError error);
}
