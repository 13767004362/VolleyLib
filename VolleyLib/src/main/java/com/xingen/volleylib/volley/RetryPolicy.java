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
 * Retry policy for a request.
 *
 * 用途：
 *    1. 重试策略，一定时间，重新发起一个请求。
 *    2.获取当前时间，当前重试的请求个数
 */
public interface RetryPolicy {

    /**
     * Returns the current timeout (used for logging)
     * 获取当前时间.
     */
    public int getCurrentTimeout();

    /**
     * Returns the current retry count (used for logging).
     * 返回当前重试次数
     */
    public int getCurrentRetryCount();

    /**
     * Prepares for the next retry by applying a backoff to the timeout.
     * @param error The error code of the last attempt.
     * @throws VolleyError In the event that the retry could not be performed (for example if we
     * ran out of attempts), the passed in error is thrown.
     *
     *  当应用超时的时候，准备下一次的重试
     */
    public void retry(VolleyError error) throws VolleyError;
}
