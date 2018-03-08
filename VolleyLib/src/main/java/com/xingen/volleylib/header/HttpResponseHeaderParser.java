package com.xingen.volleylib.header;

import com.xingen.volleylib.volley.Cache;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.toolbox.HttpHeaderParser;

/**
 * Created by ${xinGen} on 2018/3/5.
 * <p>
 * 参考：overflow.com/questions/16781244/android-volley-jsonobjectrequest-caching
 */

public class HttpResponseHeaderParser extends HttpHeaderParser {


    /**
     * 忽视`Cache-control`表头，默认指定缓存时间，SoftTtl == 3 mins, ttl == 24 hours
     *
     * @param response
     * @return
     */
    public static Cache.Entry parseSpecifiedTimeCacheHeaders(NetworkResponse response) {
        Cache.Entry entry = parseCacheHeaders(response);
        long now = System.currentTimeMillis();
        //3分钟内，会缓存中，过完会刷新
        final long cacheHitButRefreshed = 3 * 60 * 1000;
        // in 12 hours this cache entry expires completely
        final long cacheExpired = 3 * 60 * 60 * 1000;
        //截止的缓存时间
        final long softExpire = now + cacheHitButRefreshed;
        //过期时间
        final long ttl = now + cacheExpired;
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        return entry;
    }

}
