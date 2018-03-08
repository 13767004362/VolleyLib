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

import java.util.Collections;
import java.util.Map;

/**
 * An interface for a cache keyed by a String with a byte array as data.
 *
 * Created by xingen
 * 用途：
 *       1.使用string类型的缓存key,存储byte[]数据
 *       2.提供获取，存储，初始化，移除，清除的操作方法
 *       3.一个静态类，作为Cache的实体类，用于保存每个data和metadata
 *
 */
public interface Cache {
    /**
     * Retrieves an entry from the cache.
     * @param key Cache key
     * @return An {@link Entry} or null in the event of a cache miss
     */
    public Entry get(String key);

    /**
     * Adds or replaces an entry to the cache.
     * @param key Cache key
     * @param entry Data to store and metadata for cache coherency, TTL, etc.
     */
    public void put(String key, Entry entry);

    /**
     * Performs any potentially long-running actions needed to initialize the cache;
     * will be called from a worker thread.
     */
    public void initialize();

    /**
     * Invalidates an entry in the cache.
     * @param key Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    public void invalidate(String key, boolean fullExpire);

    /**
     * Removes an entry from the cache.
     * @param key Cache key
     */
    public void remove(String key);

    /**
     * Empties the cache.
     */
    public void clear();

    /**
     * Data and metadata for an entry returned by the cache.
     */
    public static class Entry {
        /** The data returned from cache.  从缓存返回的数据 */
        public byte[] data;

        /** ETag for cache coherency etag用于缓存一致性.   */
        public String etag;

        /** Date of this response as reported by the server. 从服务器响应的时间 */
        public long serverDate;

        /** TTL for this record.  */
        public long ttl;

        /** Soft TTL for this record.  */
        public long softTtl;

        /** Immutable response headers as received from server; must be non-null.  从服务器上响应的header */
        public Map<String, String> responseHeaders = Collections.emptyMap();

        /**
         * 返回true,实体已经过期
         **/
        public boolean isExpired() {
            return this.ttl < System.currentTimeMillis();
        }

        /**
         *  返回true，需要从原始数据源中刷新。
         * */
        public boolean refreshNeeded() {
            return this.softTtl < System.currentTimeMillis();
        }
    }

}
